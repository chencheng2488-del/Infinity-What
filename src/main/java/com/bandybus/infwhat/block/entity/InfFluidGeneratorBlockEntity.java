package com.bandybus.infwhat.block.entity;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.config.InfinityWhatConfig;
import com.bandybus.infwhat.menu.InfFluidGeneratorMenu;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;


/**
 * Stores a single fluid type. When the infinite source is enabled (config), inserting any
 * allowed fluid immediately turns it into an unlimited source. Output goes through the
 * Fabric {@link FluidStorage} lookup, which is what AE2 / GregTech Modern / Pipez / Mekanism /
 * Create and every other transfer-aware pipe mod use.
 *
 * <p>Fabric's transfer API measures fluids in droplets (1 mB = 81 droplets, 1 bucket = 81000),
 * so the internal amount is kept in droplets. The GUI keeps reporting millibuckets and uses
 * {@link Long#MAX_VALUE} as the conceptual amount while infinite.
 */
public class InfFluidGeneratorBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory<BlockPos> {

    private static final String KEY_FLUID = "Fluid";
    private static final String KEY_AMOUNT = "Amount";
    private static final String KEY_INFINITE = "Infinite";
    private static final String KEY_OUTPUT_ENABLED = "OutputEnabled";
    private static final String KEY_OUTPUT_SIDES = "OutputSides";
    private static final String KEY_OUTPUT_RATE_MB = "OutputRateMb";
    private static final String KEY_OUTPUT_RATE_INDEX = "OutputRateIndex";
    public static final long MAX_OUTPUT_RATE_MB = (long) Integer.MAX_VALUE * 1_000L;

    /** Droplets per millibucket: one bucket is 81000 droplets, i.e. 1000 mB. */
    public static final long DROPLETS_PER_MB = FluidConstants.BUCKET / 1_000L;

    /** Stored fluid. When {@link #infinite} is true the amount is only a placeholder (1). */
    private FluidVariant tank = FluidVariant.blank();
    private long amount;
    private boolean infinite = false;
    private boolean outputEnabled = false;
    private int outputSideMask = (1 << Direction.values().length) - 1;
    private long outputAmountPerTickMb = 1_000L;

    private final InfiniteFluidTank fluidStorage = new InfiniteFluidTank();

    private record FluidSnapshot(FluidVariant tank, long amount, boolean infinite) {
    }

    public InfFluidGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INF_FLUID_GENERATOR, pos, state);
    }

    // ------------------------------------------------------------------
    // Data exposed to the GUI
    // ------------------------------------------------------------------

    public FluidVariant getStoredFluid() {
        return this.tank;
    }

    public boolean isInfinite() {
        return effectivelyInfinite();
    }

    /**
     * The persisted {@code infinite} flag is only honoured while the balance switch is enabled,
     * so turning {@code balance.enableInfiniteSource} off also disables generators that were
     * already placed and converted to infinite sources.
     */
    private boolean effectivelyInfinite() {
        return this.infinite && InfinityWhatConfig.infiniteEnabled();
    }

    /** Display amount: Long.MAX_VALUE millibuckets when infinite, otherwise the stored mB amount. */
    public long getStoredAmount() {
        return effectivelyInfinite() ? Long.MAX_VALUE : this.amount / DROPLETS_PER_MB;
    }

    public boolean isOutputEnabled() {
        return this.outputEnabled;
    }

    public void setOutputEnabled(boolean enabled) {
        if (this.outputEnabled == enabled) {
            return;
        }
        this.outputEnabled = enabled;
        markUpdated();
    }

    public boolean isOutputSideEnabled(Direction direction) {
        return (this.outputSideMask & (1 << direction.get3DDataValue())) != 0;
    }

    public void toggleOutputSide(Direction direction) {
        this.outputSideMask ^= 1 << direction.get3DDataValue();
        markUpdated();
    }

    public long getOutputAmountPerTick() {
        return this.outputAmountPerTickMb;
    }

    public void setOutputAmountPerTick(long millibuckets) {
        long clamped = Math.max(1L, Math.min(MAX_OUTPUT_RATE_MB, millibuckets));
        if (this.outputAmountPerTickMb == clamped) {
            return;
        }
        this.outputAmountPerTickMb = clamped;
        markUpdated();
    }

    public int getOutputSideMask() {
        return this.outputSideMask;
    }

    public void applyOutputConfiguration(boolean enabled, int sideMask, long rateMb) {
        this.outputEnabled = enabled;
        this.outputSideMask = sideMask & ((1 << Direction.values().length) - 1);
        this.outputAmountPerTickMb = Math.max(1L, Math.min(MAX_OUTPUT_RATE_MB, rateMb));
        markUpdated();
    }

    public void clearFluid() {
        if (this.tank.isBlank() && !this.infinite) {
            return;
        }
        this.tank = FluidVariant.blank();
        this.amount = 0L;
        this.infinite = false;
        markUpdated();
    }

    public float getFillRatio() {
        if (effectivelyInfinite()) {
            return 1.0F;
        }
        if (this.tank.isBlank()) {
            return 0.0F;
        }
        long capacity = finiteCapacityDroplets();
        if (capacity <= 0) {
            return 0.0F;
        }
        return Math.min(1.0F, (float) this.amount / (float) capacity);
    }

    private static long finiteCapacityDroplets() {
        return (long) InfinityWhatConfig.finiteCapacity() * DROPLETS_PER_MB;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container." + InfinityWhat.MOD_ID + ".inf_fluid_generator");
    }

    /**
     * The tank exposed to the bucket interaction in the GUI. This is the same storage that is
     * published through the fluid lookup, so a bucket moved onto the GUI tank behaves exactly
     * like a pipe interacting with the block.
     */
    public Storage<FluidVariant> getFluidStorage() {
        return this.fluidStorage;
    }

    @Override
    public BlockPos getScreenOpeningData(ServerPlayer player) {
        return this.worldPosition;
    }
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new InfFluidGeneratorMenu(containerId, inventory, this.worldPosition);
    }

    // ------------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!this.tank.isBlank()) {
            ResourceLocation id = BuiltInRegistries.FLUID.getKey(this.tank.getFluid());
            tag.putString(KEY_FLUID, id == null ? "" : id.toString());
            tag.putLong(KEY_AMOUNT, this.amount);
        }
        tag.putBoolean(KEY_INFINITE, this.infinite);
        tag.putBoolean(KEY_OUTPUT_ENABLED, this.outputEnabled);
        tag.putInt(KEY_OUTPUT_SIDES, this.outputSideMask);
        tag.putLong(KEY_OUTPUT_RATE_MB, this.outputAmountPerTickMb);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.tank = FluidVariant.blank();
        this.amount = 0L;
        this.infinite = false;
        if (tag.contains(KEY_FLUID, Tag.TAG_STRING)) {
            ResourceLocation id = ResourceLocation.tryParse(tag.getString(KEY_FLUID));
            if (id != null) {
                Fluid fluid = BuiltInRegistries.FLUID.get(id)
                        .map(net.minecraft.core.Holder.Reference::value)
                        .orElse(null);
                if (fluid != null) {
                    FluidVariant loaded = FluidVariant.of(fluid);
                    if (!loaded.isBlank()) {
                        this.tank = loaded;
                        this.amount = Math.max(0L, tag.getLong(KEY_AMOUNT));
                    }
                }
            }
        }
        this.infinite = tag.getBoolean(KEY_INFINITE) && !this.tank.isBlank();
        this.outputEnabled = tag.getBoolean(KEY_OUTPUT_ENABLED);
        this.outputSideMask = tag.contains(KEY_OUTPUT_SIDES, Tag.TAG_INT)
                ? tag.getInt(KEY_OUTPUT_SIDES)
                : (1 << Direction.values().length) - 1;
        if (tag.contains(KEY_OUTPUT_RATE_MB, Tag.TAG_LONG)) {
            this.outputAmountPerTickMb = Math.max(1L, Math.min(MAX_OUTPUT_RATE_MB,
                    tag.getLong(KEY_OUTPUT_RATE_MB)));
        } else if (tag.contains(KEY_OUTPUT_RATE_INDEX, Tag.TAG_INT)) {
            // Migrate worlds saved by the previous preset-based release.
            int[] legacyRates = {1, 10, 100, 1_000, 10_000, 100_000, 1_000_000,
                    10_000_000, 100_000_000, 1_000_000_000};
            int index = Math.max(0, Math.min(legacyRates.length - 1, tag.getInt(KEY_OUTPUT_RATE_INDEX)));
            this.outputAmountPerTickMb = legacyRates[index];
        } else {
            this.outputAmountPerTickMb = 1_000L;
        }
    }

    // ------------------------------------------------------------------
    // Client sync
    // ------------------------------------------------------------------

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void markUpdated() {
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private boolean isDisallowed(FluidVariant variant) {
        if (variant.isBlank()) {
            return false;
        }
        return !InfinityWhatConfig.isFluidAllowed(BuiltInRegistries.FLUID.getKey(variant.getFluid()));
    }

    // ------------------------------------------------------------------
    // Server tick: active output into selected adjacent fluid storages
    // ------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  InfFluidGeneratorBlockEntity blockEntity) {
        if (!level.isClientSide && blockEntity.outputEnabled && !blockEntity.tank.isBlank()) {
            blockEntity.pushToNeighbors();
        }
    }

    private void pushToNeighbors() {
        if (this.level == null || this.level.isClientSide || this.tank.isBlank()) {
            return;
        }

        boolean infiniteSource = effectivelyInfinite();
        long rate = this.outputAmountPerTickMb * DROPLETS_PER_MB;
        for (Direction direction : Direction.values()) {
            if (!isOutputSideEnabled(direction)) {
                continue;
            }
            BlockPos neighborPos = this.worldPosition.relative(direction);
            if (!this.level.isLoaded(neighborPos)) {
                continue;
            }
            BlockEntity neighbor = this.level.getBlockEntity(neighborPos);
            if (neighbor == null || neighbor == this) {
                continue;
            }
            Storage<FluidVariant> target = FluidStorage.SIDED.find(this.level, neighborPos,
                    direction.getOpposite());
            if (target == null) {
                continue;
            }

            long remaining = infiniteSource ? rate : Math.min(rate, this.amount);
            if (remaining <= 0) {
                continue;
            }
            try (Transaction transaction = Transaction.openOuter()) {
                long accepted = target.insert(this.tank, remaining, transaction);
                if (accepted > 0 && !infiniteSource) {
                    // Consume from our own tank inside the same transaction so both sides commit together.
                    accepted = this.fluidStorage.extract(this.tank, accepted, transaction);
                }
                transaction.commit();
            }
            if (!infiniteSource && this.tank.isBlank()) {
                break;
            }
        }
    }

    // ------------------------------------------------------------------
    // Fluid storage (shared by every side)
    // ------------------------------------------------------------------

    private final class InfiniteFluidTank extends SnapshotParticipant<FluidSnapshot>
            implements SingleSlotStorage<FluidVariant> {

        @Override
        protected FluidSnapshot createSnapshot() {
            return new FluidSnapshot(InfFluidGeneratorBlockEntity.this.tank,
                    InfFluidGeneratorBlockEntity.this.amount,
                    InfFluidGeneratorBlockEntity.this.infinite);
        }

        @Override
        protected void readSnapshot(FluidSnapshot snapshot) {
            InfFluidGeneratorBlockEntity.this.tank = snapshot.tank();
            InfFluidGeneratorBlockEntity.this.amount = snapshot.amount();
            InfFluidGeneratorBlockEntity.this.infinite = snapshot.infinite();
        }

        @Override
        protected void onFinalCommit() {
            markUpdated();
        }

        @Override
        public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            if (resource.isBlank() || maxAmount <= 0L || isDisallowed(resource)) {
                return 0L;
            }
            FluidVariant stored = InfFluidGeneratorBlockEntity.this.tank;
            // fluid_type_limit = 1: only one fluid type may be stored at a time.
            if (!stored.isBlank() && !stored.isOf(resource.getFluid())) {
                return 0L;
            }

            if (InfinityWhatConfig.infiniteEnabled()) {
                updateSnapshots(transaction);
                // Keep one droplet of the type and flip the source to infinite.
                InfFluidGeneratorBlockEntity.this.tank = resource;
                InfFluidGeneratorBlockEntity.this.amount = 1L;
                InfFluidGeneratorBlockEntity.this.infinite = true;
                // The spec says the amount jumps to the max immediately, so accept it all.
                return maxAmount;
            }

            long capacity = finiteCapacityDroplets();
            long current = stored.isBlank() ? 0L : InfFluidGeneratorBlockEntity.this.amount;
            long accepted = Math.min(capacity - current, maxAmount);
            if (accepted <= 0L) {
                return 0L;
            }
            updateSnapshots(transaction);
            if (stored.isBlank()) {
                InfFluidGeneratorBlockEntity.this.tank = resource;
            }
            InfFluidGeneratorBlockEntity.this.amount = current + accepted;
            return accepted;
        }

        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            if (resource.isBlank() || maxAmount <= 0L) {
                return 0L;
            }
            FluidVariant stored = InfFluidGeneratorBlockEntity.this.tank;
            if (stored.isBlank() || !stored.isOf(resource.getFluid())) {
                return 0L;
            }
            if (effectivelyInfinite()) {
                // Unlimited source: never depletes, always hands out the requested amount.
                return maxAmount;
            }
            long drained = Math.min(maxAmount, InfFluidGeneratorBlockEntity.this.amount);
            if (drained <= 0L) {
                return 0L;
            }
            updateSnapshots(transaction);
            long remaining = InfFluidGeneratorBlockEntity.this.amount - drained;
            InfFluidGeneratorBlockEntity.this.amount = remaining;
            if (remaining <= 0L) {
                InfFluidGeneratorBlockEntity.this.tank = FluidVariant.blank();
                InfFluidGeneratorBlockEntity.this.infinite = false;
            }
            return drained;
        }

        @Override
        public boolean isResourceBlank() {
            return InfFluidGeneratorBlockEntity.this.tank.isBlank();
        }

        @Override
        public FluidVariant getResource() {
            return InfFluidGeneratorBlockEntity.this.tank;
        }

        @Override
        public long getAmount() {
            if (effectivelyInfinite()) {
                return Long.MAX_VALUE;
            }
            return InfFluidGeneratorBlockEntity.this.tank.isBlank()
                    ? 0L
                    : InfFluidGeneratorBlockEntity.this.amount;
        }

        @Override
        public long getCapacity() {
            return effectivelyInfinite() ? Long.MAX_VALUE : finiteCapacityDroplets();
        }
    }
}

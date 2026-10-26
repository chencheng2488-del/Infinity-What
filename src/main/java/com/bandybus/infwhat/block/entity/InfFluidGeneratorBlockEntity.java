package com.bandybus.infwhat.block.entity;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.config.InfinityWhatConfig;
import com.bandybus.infwhat.menu.InfFluidGeneratorMenu;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
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
import net.minecraft.world.level.material.Fluids;

/**
 * Stores a single fluid type. When the infinite source is enabled (config), inserting any
 * allowed fluid immediately turns it into an unlimited source. Output goes through the
 * generic Fabric {@link FluidStorage} api, which is what AE2 / GregTech Modern /
 * Pipez / Mekanism / Create and every other storage-aware pipe mod use.
 *
 * The source is tracked as droplets (1 mB = 81 droplets). While infinite the exposed
 * storage reports {@link Long#MAX_VALUE} and extraction never depletes it.
 */
public class InfFluidGeneratorBlockEntity extends BlockEntity
        implements ExtendedScreenHandlerFactory<BlockPos> {

    private static final String KEY_FLUID = "Fluid";
    private static final String KEY_AMOUNT = "Amount";
    private static final String KEY_INFINITE = "Infinite";
    private static final String KEY_OUTPUT_ENABLED = "OutputEnabled";
    private static final String KEY_OUTPUT_SIDES = "OutputSides";
    private static final String KEY_OUTPUT_RATE_MB = "OutputRateMb";
    private static final String KEY_OUTPUT_RATE_INDEX = "OutputRateIndex";
    public static final long MAX_OUTPUT_RATE_MB = (long) Integer.MAX_VALUE * 1_000L;
    public static final long DROPLETS_PER_MB = 81L;

    /** Stored fluid. When {@link #infinite} is true the amount is only a placeholder (1 droplet). */
    private FluidVariant tank = FluidVariant.blank();
    private long tankAmount;
    private boolean infinite = false;
    private boolean outputEnabled = false;
    private int outputSideMask = (1 << Direction.values().length) - 1;
    private long outputAmountPerTickMb = 1_000L;

    private final InfiniteFluidTank fluidHandler = new InfiniteFluidTank();

    public InfFluidGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INF_FLUID_GENERATOR, pos, state);
    }

    // ------------------------------------------------------------------
    // Data exposed to the GUI
    // ------------------------------------------------------------------

    public FluidVariant getStoredVariant() {
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
        return this.infinite && !this.tank.isBlank() && InfinityWhatConfig.infiniteEnabled();
    }

    /** Display amount: Long.MAX_VALUE buckets when infinite, otherwise the stored mB amount. */
    public long getStoredAmount() {
        return effectivelyInfinite() ? Long.MAX_VALUE : this.tankAmount / DROPLETS_PER_MB;
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
        this.tankAmount = 0;
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
        long capacity = capacityDroplets();
        if (capacity <= 0) {
            return 0.0F;
        }
        return Math.min(1.0F, (float) this.tankAmount / (float) capacity);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container." + InfinityWhat.MOD_ID + ".inf_fluid_generator");
    }

    /**
     * The tank exposed to the bucket interaction in the GUI. This is the same storage that is
     * published through {@link FluidStorage}, so a bucket moved onto the GUI tank behaves
     * exactly like a pipe interacting with the block.
     */
    public Storage<FluidVariant> getFluidHandler() {
        return this.fluidHandler;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new InfFluidGeneratorMenu(containerId, inventory, this.worldPosition);
    }

    @Override
    public BlockPos getScreenOpeningData(ServerPlayer player) {
        return this.worldPosition;
    }

    // ------------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!this.tank.isBlank()) {
            ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(this.tank.getFluid());
            tag.putString(KEY_FLUID, fluidId.toString());
            tag.putLong(KEY_AMOUNT, this.tankAmount);
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
        this.tankAmount = 0;
        if (tag.contains(KEY_FLUID, Tag.TAG_STRING)) {
            ResourceLocation fluidId = ResourceLocation.tryParse(tag.getString(KEY_FLUID));
            if (fluidId != null) {
                Fluid fluid = BuiltInRegistries.FLUID.get(fluidId);
                if (fluid != null && fluid != Fluids.EMPTY) {
                    this.tank = FluidVariant.of(fluid);
                    this.tankAmount = Math.max(0L, tag.getLong(KEY_AMOUNT));
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
            long[] legacyRates = {1, 10, 100, 1_000, 10_000, 100_000, 1_000_000,
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

    public void markUpdated() {
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

    private long capacityDroplets() {
        return (long) InfinityWhatConfig.finiteCapacity() * DROPLETS_PER_MB;
    }

    private static long toDroplets(long millibuckets) {
        return millibuckets > Long.MAX_VALUE / DROPLETS_PER_MB
                ? Long.MAX_VALUE
                : millibuckets * DROPLETS_PER_MB;
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

        long rate = toDroplets(this.outputAmountPerTickMb);
        long moved = 0L;
        try (Transaction transaction = Transaction.openOuter()) {
            for (Direction direction : Direction.values()) {
                if (!isOutputSideEnabled(direction)) {
                    continue;
                }
                BlockPos neighborPos = this.worldPosition.relative(direction);
                if (!this.level.isLoaded(neighborPos)) {
                    continue;
                }
                Storage<FluidVariant> target = FluidStorage.SIDED.find(this.level, neighborPos,
                        direction.getOpposite());
                if (target == null) {
                    continue;
                }
                moved += StorageUtil.move(this.fluidHandler, target, variant -> true, rate, transaction);
                if (!effectivelyInfinite() && this.tank.isBlank()) {
                    break;
                }
            }
            if (moved > 0L) {
                transaction.commit();
            } else {
                transaction.abort();
            }
        }
        if (moved > 0L && !effectivelyInfinite()) {
            markUpdated();
        }
    }

    // ------------------------------------------------------------------
    // Fluid storage (shared by every side)
    // ------------------------------------------------------------------

    private record FluidSnapshot(FluidVariant variant, long amount, boolean infinite) {
    }

    private final class InfiniteFluidTank extends SnapshotParticipant<FluidSnapshot>
            implements SingleSlotStorage<FluidVariant> {

        @Override
        public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            if (resource.isBlank() || maxAmount <= 0L || isDisallowed(resource)) {
                return 0L;
            }
            // fluid_type_limit = 1: a different type cannot be accepted once one is stored.
            if (!tank.isBlank() && (!tank.isOf(resource.getFluid())
                    || !tank.componentsMatch(resource.getComponents()))) {
                return 0L;
            }
            if (effectivelyInfinite()) {
                return maxAmount;
            }
            if (InfinityWhatConfig.infiniteEnabled()) {
                updateSnapshots(transaction);
                // Keep one droplet of the type and flip the source to infinite.
                tank = resource;
                tankAmount = 1L;
                infinite = true;
                markUpdated();
                return maxAmount;
            }

            long capacity = capacityDroplets();
            long accepted = Math.min(capacity - tankAmount, maxAmount);
            if (accepted <= 0L) {
                return 0L;
            }
            updateSnapshots(transaction);
            tank = resource;
            tankAmount += accepted;
            markUpdated();
            return accepted;
        }

        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            if (tank.isBlank() || resource.isBlank() || maxAmount <= 0L
                    || !resource.isOf(tank.getFluid())) {
                return 0L;
            }
            if (effectivelyInfinite()) {
                // Unlimited source: never depletes, always hands out the requested amount.
                return maxAmount;
            }
            long extracted = Math.min(tankAmount, maxAmount);
            if (extracted <= 0L) {
                return 0L;
            }
            updateSnapshots(transaction);
            tankAmount -= extracted;
            if (tankAmount <= 0L) {
                tank = FluidVariant.blank();
                tankAmount = 0L;
                infinite = false;
            }
            markUpdated();
            return extracted;
        }

        @Override
        public boolean isResourceBlank() {
            return tank.isBlank();
        }

        @Override
        public FluidVariant getResource() {
            return tank;
        }

        @Override
        public long getAmount() {
            return effectivelyInfinite() ? Long.MAX_VALUE : tankAmount;
        }

        @Override
        public long getCapacity() {
            return effectivelyInfinite() ? Long.MAX_VALUE : capacityDroplets();
        }

        @Override
        protected FluidSnapshot createSnapshot() {
            return new FluidSnapshot(tank, tankAmount, infinite);
        }

        @Override
        protected void readSnapshot(FluidSnapshot snapshot) {
            tank = snapshot.variant();
            tankAmount = snapshot.amount();
            infinite = snapshot.infinite();
            markUpdated();
        }
    }
}

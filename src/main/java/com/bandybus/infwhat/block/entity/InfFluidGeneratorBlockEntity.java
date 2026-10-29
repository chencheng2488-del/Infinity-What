package com.bandybus.infwhat.block.entity;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.config.InfinityWhatConfig;
import com.bandybus.infwhat.menu.InfFluidGeneratorMenu;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Stores a single fluid type. When the infinite source is enabled (config), inserting any
 * allowed fluid immediately turns it into an unlimited source. Output goes through the
 * Fabric transfer API {@link Storage} view, which is what AE2 / GregTech Modern / Pipez /
 * Mekanism / Create and every other pipe mod use.
 *
 * AMOUNT UNIT: Fabric stores fluid amounts in droplets, so 1 mB = 81 droplets and one bucket is
 * 81000 droplets. The GUI and the output-rate configuration still speak millibuckets; the
 * conversion happens at this class' boundary.
 */
public class InfFluidGeneratorBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory {

    public static final long DROPLETS_PER_MB = 81L;

    private static final String KEY_TANK = "Tank";
    private static final String KEY_TANK_AMOUNT = "TankAmount";
    private static final String KEY_INFINITE = "Infinite";
    private static final String KEY_OUTPUT_ENABLED = "OutputEnabled";
    private static final String KEY_OUTPUT_SIDES = "OutputSides";
    private static final String KEY_OUTPUT_RATE_MB = "OutputRateMb";
    private static final String KEY_OUTPUT_RATE_INDEX = "OutputRateIndex";
    public static final long MAX_OUTPUT_RATE_MB = (long) Integer.MAX_VALUE * 1_000L;

    private static final int ALL_SIDES = (1 << Direction.values().length) - 1;

    /** Stored fluid. When {@link #infinite} is true the amount is only a placeholder (1 mB). */
    private FluidVariant tank = FluidVariant.blank();
    private long tankAmount;
    private boolean infinite = false;
    private boolean outputEnabled = false;
    private int outputSideMask = ALL_SIDES;
    private long outputAmountPerTickMb = 1_000L;

    private final InfiniteFluidTank fluidStorage = new InfiniteFluidTank();

    public InfFluidGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INF_FLUID_GENERATOR, pos, state);
    }

    // ------------------------------------------------------------------
    // Fluid storage
    // ------------------------------------------------------------------

    public Storage<FluidVariant> getFluidStorage() {
        return this.fluidStorage;
    }

    private static long finiteCapacityDroplets() {
        return (long) InfinityWhatConfig.finiteCapacity() * DROPLETS_PER_MB;
    }

    private static boolean isFluidAllowed(FluidVariant variant) {
        return InfinityWhatConfig.isFluidAllowed(Registry.FLUID.getKey(variant.getFluid()));
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

    /** Display amount in millibuckets: Long.MAX_VALUE when infinite, otherwise the stored amount. */
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
        this.outputSideMask = sideMask & ALL_SIDES;
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
        long capacity = finiteCapacityDroplets();
        if (capacity <= 0 || this.tankAmount <= 0) {
            return 0.0F;
        }
        return Math.min(1.0F, (float) this.tankAmount / (float) capacity);
    }

    @Override
    public Component getDisplayName() {
        return new TranslatableComponent("container." + InfinityWhat.MOD_ID + ".inf_fluid_generator");
    }

    @Override
    public void writeScreenOpeningData(ServerPlayer player, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(this.worldPosition);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new InfFluidGeneratorMenu(containerId, inventory, this);
    }

    // ------------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!this.tank.isBlank() && this.tankAmount > 0) {
            tag.put(KEY_TANK, this.tank.toNbt());
            tag.putLong(KEY_TANK_AMOUNT, this.tankAmount);
        }
        tag.putBoolean(KEY_INFINITE, this.infinite);
        tag.putBoolean(KEY_OUTPUT_ENABLED, this.outputEnabled);
        tag.putInt(KEY_OUTPUT_SIDES, this.outputSideMask);
        tag.putLong(KEY_OUTPUT_RATE_MB, this.outputAmountPerTickMb);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.tank = FluidVariant.blank();
        this.tankAmount = 0;
        this.infinite = false;
        if (tag.contains(KEY_TANK, Tag.TAG_COMPOUND)) {
            FluidVariant loaded = FluidVariant.fromNbt(tag.getCompound(KEY_TANK));
            long loadedAmount = tag.getLong(KEY_TANK_AMOUNT);
            if (!loaded.isBlank() && loadedAmount > 0) {
                this.tank = loaded;
                this.tankAmount = loadedAmount;
            }
        }
        this.infinite = tag.getBoolean(KEY_INFINITE) && !this.tank.isBlank();
        this.outputEnabled = tag.getBoolean(KEY_OUTPUT_ENABLED);
        this.outputSideMask = tag.contains(KEY_OUTPUT_SIDES, Tag.TAG_INT)
                ? tag.getInt(KEY_OUTPUT_SIDES)
                : ALL_SIDES;
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
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void markUpdated() {
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(),
                    Block.UPDATE_CLIENTS);
        }
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
        FluidVariant resource = this.tank;
        long rate = getOutputAmountPerTick() * DROPLETS_PER_MB;
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

            long remaining = infiniteSource ? rate : Math.min(rate, this.tankAmount);
            if (remaining <= 0) {
                break;
            }
            // 目标先接收，成功后再从本机扣除；两者处于同一事务，失败不会留下半截状态。
            try (Transaction transaction = Transaction.openOuter()) {
                long accepted = target.insert(resource, remaining, transaction);
                if (accepted > 0) {
                    this.fluidStorage.extract(resource, accepted, transaction);
                }
                transaction.commit();
            }
            if (this.tank.isBlank() && !infiniteSource) {
                break;
            }
        }
    }

    // ------------------------------------------------------------------
    // Fluid storage implementation (single slot, transactional)
    // ------------------------------------------------------------------

    private record FluidSnapshot(FluidVariant fluid, long amount, boolean infinite) {
    }

    private final class InfiniteFluidTank extends SnapshotParticipant<FluidSnapshot>
            implements SingleSlotStorage<FluidVariant> {

        @Override
        protected FluidSnapshot createSnapshot() {
            return new FluidSnapshot(InfFluidGeneratorBlockEntity.this.tank,
                    InfFluidGeneratorBlockEntity.this.tankAmount,
                    InfFluidGeneratorBlockEntity.this.infinite);
        }

        @Override
        protected void readSnapshot(FluidSnapshot snapshot) {
            InfFluidGeneratorBlockEntity.this.tank = snapshot.fluid();
            InfFluidGeneratorBlockEntity.this.tankAmount = snapshot.amount();
            InfFluidGeneratorBlockEntity.this.infinite = snapshot.infinite();
        }

        @Override
        protected void onFinalCommit() {
            markUpdated();
        }

        @Override
        public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            if (resource.isBlank() || maxAmount <= 0 || !isFluidAllowed(resource)) {
                return 0;
            }
            // fluid_type_limit = 1: only one fluid type may be stored at a time
            if (!InfFluidGeneratorBlockEntity.this.tank.isBlank()
                    && !InfFluidGeneratorBlockEntity.this.tank.equals(resource)) {
                return 0;
            }

            if (InfinityWhatConfig.infiniteEnabled()) {
                if (!InfFluidGeneratorBlockEntity.this.infinite
                        || !InfFluidGeneratorBlockEntity.this.tank.equals(resource)
                        || InfFluidGeneratorBlockEntity.this.tankAmount <= 0) {
                    updateSnapshots(transaction);
                    // Keep one unit of the type and flip the source to infinite.
                    InfFluidGeneratorBlockEntity.this.tank = resource;
                    InfFluidGeneratorBlockEntity.this.tankAmount = DROPLETS_PER_MB;
                    InfFluidGeneratorBlockEntity.this.infinite = true;
                }
                // The spec says the amount jumps to the max immediately, so accept it all.
                return maxAmount;
            }

            long capacity = finiteCapacityDroplets();
            long accepted = Math.min(maxAmount,
                    capacity - InfFluidGeneratorBlockEntity.this.tankAmount);
            if (accepted <= 0) {
                return 0;
            }
            updateSnapshots(transaction);
            InfFluidGeneratorBlockEntity.this.tank = resource;
            InfFluidGeneratorBlockEntity.this.tankAmount += accepted;
            InfFluidGeneratorBlockEntity.this.infinite = false;
            return accepted;
        }

        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            if (resource.isBlank() || maxAmount <= 0
                    || InfFluidGeneratorBlockEntity.this.tank.isBlank()
                    || !InfFluidGeneratorBlockEntity.this.tank.equals(resource)) {
                return 0;
            }
            if (effectivelyInfinite()) {
                // Unlimited source: never depletes, always hands out the requested amount.
                return maxAmount;
            }
            long drained = Math.min(maxAmount, InfFluidGeneratorBlockEntity.this.tankAmount);
            if (drained <= 0) {
                return 0;
            }
            updateSnapshots(transaction);
            InfFluidGeneratorBlockEntity.this.tankAmount -= drained;
            if (InfFluidGeneratorBlockEntity.this.tankAmount <= 0) {
                InfFluidGeneratorBlockEntity.this.tank = FluidVariant.blank();
                InfFluidGeneratorBlockEntity.this.tankAmount = 0;
                InfFluidGeneratorBlockEntity.this.infinite = false;
            }
            return drained;
        }

        @Override
        public boolean isResourceBlank() {
            return InfFluidGeneratorBlockEntity.this.tank.isBlank()
                    || InfFluidGeneratorBlockEntity.this.tankAmount <= 0;
        }

        @Override
        public FluidVariant getResource() {
            return InfFluidGeneratorBlockEntity.this.tank;
        }

        @Override
        public long getAmount() {
            return effectivelyInfinite() ? Long.MAX_VALUE
                    : InfFluidGeneratorBlockEntity.this.tankAmount;
        }

        @Override
        public long getCapacity() {
            return effectivelyInfinite() ? Long.MAX_VALUE : finiteCapacityDroplets();
        }
    }
}

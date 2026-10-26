package com.bandybus.infwhat.block.entity;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.config.InfinityWhatConfig;
import com.bandybus.infwhat.menu.InfFluidGeneratorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.capabilities.Capability;
import net.neoforged.neoforge.common.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.LazyOptional;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.minecraft.core.registries.BuiltInRegistries;

import javax.annotation.Nullable;

/**
 * Stores a single fluid type. When the infinite source is enabled (config), inserting any
 * allowed fluid immediately turns it into an unlimited source. Output goes through the
 * standard Forge {@link IFluidHandler} capability, which is what AE2 / GregTech Modern /
 * Pipez / Mekanism / Create and every other pipe mod use.
 *
 * ENGINE LIMIT: {@link FluidStack} stores its amount as an {@code int} in millibuckets, so a
 * {@code Long.MAX_VALUE} bucket count cannot be represented by the Forge capability. Internally
 * the source is tracked as "infinite" and the capability reports {@link Integer#MAX_VALUE} mB;
 * the GUI presents the conceptual amount as {@code Long.MAX_VALUE B} (buckets, 1 B = 1000 mB).
 */
public class InfFluidGeneratorBlockEntity extends BlockEntity implements MenuProvider {

    private static final String KEY_TANK = "Tank";
    private static final String KEY_INFINITE = "Infinite";
    private static final String KEY_OUTPUT_ENABLED = "OutputEnabled";
    private static final String KEY_OUTPUT_SIDES = "OutputSides";
    private static final String KEY_OUTPUT_RATE_MB = "OutputRateMb";
    private static final String KEY_OUTPUT_RATE_INDEX = "OutputRateIndex";
    public static final long MAX_OUTPUT_RATE_MB = (long) Integer.MAX_VALUE * 1_000L;

    /** Stored fluid. When {@link #infinite} is true the amount is only a placeholder (1). */
    private FluidStack tank = FluidStack.EMPTY;
    private boolean infinite = false;
    private boolean outputEnabled = false;
    private int outputSideMask = (1 << Direction.values().length) - 1;
    private long outputAmountPerTickMb = 1_000L;

    private final IFluidHandler fluidHandler = new InfiniteFluidTank();
    private LazyOptional<IFluidHandler> fluidOptional = LazyOptional.of(() -> this.fluidHandler);

    public InfFluidGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INF_FLUID_GENERATOR.get(), pos, state);
    }

    // ------------------------------------------------------------------
    // Capability
    // ------------------------------------------------------------------

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (capability == Capabilities.FLUID_HANDLER) {
            return this.fluidOptional.cast();
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        this.fluidOptional.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        this.fluidOptional = LazyOptional.of(() -> this.fluidHandler);
    }

    // ------------------------------------------------------------------
    // Data exposed to the GUI
    // ------------------------------------------------------------------

    public FluidStack getStoredFluid() {
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

    /** Display amount: Long.MAX_VALUE buckets when infinite, otherwise the stored mB amount. */
    public long getStoredAmount() {
        return effectivelyInfinite() ? Long.MAX_VALUE : this.tank.getAmount();
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
        if (this.tank.isEmpty() && !this.infinite) {
            return;
        }
        this.tank = FluidStack.EMPTY;
        this.infinite = false;
        markUpdated();
    }

    public float getFillRatio() {
        if (effectivelyInfinite()) {
            return 1.0F;
        }
        if (this.tank.isEmpty()) {
            return 0.0F;
        }
        int capacity = InfinityWhatConfig.finiteCapacity();
        if (capacity <= 0) {
            return 0.0F;
        }
        return Math.min(1.0F, (float) this.tank.getAmount() / (float) capacity);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container." + InfinityWhat.MOD_ID + ".inf_fluid_generator");
    }

    /**
     * The tank exposed to the bucket interaction in the GUI. This is the same handler that is
     * published through the Forge fluid capability, so a bucket moved onto the GUI tank behaves
     * exactly like a pipe interacting with the block.
     */
    public IFluidHandler getFluidHandler() {
        return this.fluidHandler;
    }

    @Nullable
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
        if (!this.tank.isEmpty()) {
            CompoundTag tankTag = new CompoundTag();
            this.tank.writeToNBT(tankTag);
            tag.put(KEY_TANK, tankTag);
        }
        tag.putBoolean(KEY_INFINITE, this.infinite);
        tag.putBoolean(KEY_OUTPUT_ENABLED, this.outputEnabled);
        tag.putInt(KEY_OUTPUT_SIDES, this.outputSideMask);
        tag.putLong(KEY_OUTPUT_RATE_MB, this.outputAmountPerTickMb);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.tank = FluidStack.EMPTY;
        this.infinite = false;
        if (tag.contains(KEY_TANK, Tag.TAG_COMPOUND)) {
            FluidStack loaded = FluidStack.loadFluidStackFromNBT(tag.getCompound(KEY_TANK));
            if (!loaded.isEmpty()) {
                this.tank = loaded;
            }
        }
        this.infinite = tag.getBoolean(KEY_INFINITE) && !this.tank.isEmpty();
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
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }

    @Nullable
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

    private boolean isDisallowed(FluidStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return !InfinityWhatConfig.isFluidAllowed(BuiltInRegistries.FLUID.getKey(stack.getFluid()));
    }

    // ------------------------------------------------------------------
    // Server tick: active output into selected adjacent fluid handlers
    // ------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  InfFluidGeneratorBlockEntity blockEntity) {
        if (!level.isClientSide && blockEntity.outputEnabled && !blockEntity.tank.isEmpty()) {
            blockEntity.pushToNeighbors();
        }
    }

    private void pushToNeighbors() {
        if (this.level == null || this.level.isClientSide || this.tank.isEmpty()) {
            return;
        }

        boolean infiniteSource = effectivelyInfinite();
        boolean changed = false;
        long rate = getOutputAmountPerTick();
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
            IFluidHandler target = neighbor
                    .getCapability(Capabilities.FLUID_HANDLER, direction.getOpposite())
                    .orElse(null);
            if (target == null) {
                continue;
            }

            long remaining = infiniteSource ? rate : Math.min(rate, this.tank.getAmount());
            while (remaining > 0) {
                int offered = (int) Math.min((long) Integer.MAX_VALUE, remaining);
                FluidStack offer = new FluidStack(this.tank, offered);
                int accepted = Math.max(0, Math.min(offered,
                        target.fill(offer, IFluidHandler.FluidAction.EXECUTE)));
                if (accepted <= 0) {
                    break;
                }
                remaining -= accepted;
                if (!infiniteSource) {
                    this.tank.shrink(accepted);
                    changed = true;
                    if (this.tank.isEmpty()) {
                        this.infinite = false;
                        break;
                    }
                }
                if (accepted < offered) {
                    break;
                }
            }
            if (this.tank.isEmpty() && !infiniteSource) break;
        }
        if (changed) {
            markUpdated();
        }
    }

    // ------------------------------------------------------------------
    // Fluid handler (shared by every side)
    // ------------------------------------------------------------------

    private final class InfiniteFluidTank implements IFluidHandler {

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int index) {
            if (index != 0 || InfFluidGeneratorBlockEntity.this.tank.isEmpty()) {
                return FluidStack.EMPTY;
            }
            int amount = InfFluidGeneratorBlockEntity.this.effectivelyInfinite()
                    ? Integer.MAX_VALUE
                    : InfFluidGeneratorBlockEntity.this.tank.getAmount();
            return new FluidStack(InfFluidGeneratorBlockEntity.this.tank, amount);
        }

        @Override
        public int getTankCapacity(int index) {
            return InfFluidGeneratorBlockEntity.this.effectivelyInfinite()
                    ? Integer.MAX_VALUE
                    : InfinityWhatConfig.finiteCapacity();
        }

        @Override
        public boolean isFluidValid(int index, FluidStack stack) {
            if (index != 0 || stack.isEmpty() || isDisallowed(stack)) {
                return false;
            }
            // fluid_type_limit = 1: a different type cannot be accepted once one is stored.
            FluidStack stored = InfFluidGeneratorBlockEntity.this.tank;
            return stored.isEmpty() || stored.isFluidEqual(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || isDisallowed(resource)) {
                return 0;
            }
            FluidStack stored = InfFluidGeneratorBlockEntity.this.tank;
            // fluid_type_limit = 1: only one fluid type may be stored at a time
            if (!stored.isEmpty() && !stored.isFluidEqual(resource)) {
                return 0;
            }

            if (InfinityWhatConfig.infiniteEnabled()) {
                if (action.execute()) {
                    // Keep one unit of the type and flip the source to infinite.
                    InfFluidGeneratorBlockEntity.this.tank = new FluidStack(resource, 1);
                    InfFluidGeneratorBlockEntity.this.infinite = true;
                    markUpdated();
                }
                // The spec says the amount jumps to the max immediately, so accept it all.
                return resource.getAmount();
            }

            int capacity = InfinityWhatConfig.finiteCapacity();
            int current = stored.isEmpty() ? 0 : stored.getAmount();
            int accepted = Math.min(capacity - current, resource.getAmount());
            if (accepted <= 0) {
                return 0;
            }
            if (action.execute()) {
                if (stored.isEmpty()) {
                    InfFluidGeneratorBlockEntity.this.tank = new FluidStack(resource, accepted);
                } else {
                    stored.grow(accepted);
                }
                markUpdated();
            }
            return accepted;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || InfFluidGeneratorBlockEntity.this.tank.isEmpty()
                    || !InfFluidGeneratorBlockEntity.this.tank.isFluidEqual(resource)) {
                return FluidStack.EMPTY;
            }
            return drain(resource.getAmount(), action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            FluidStack stored = InfFluidGeneratorBlockEntity.this.tank;
            if (stored.isEmpty() || maxDrain <= 0) {
                return FluidStack.EMPTY;
            }
            if (InfFluidGeneratorBlockEntity.this.effectivelyInfinite()) {
                // Unlimited source: never depletes, always hands out the requested amount.
                return new FluidStack(stored, maxDrain);
            }
            int drained = Math.min(maxDrain, stored.getAmount());
            FluidStack result = new FluidStack(stored, drained);
            if (action.execute()) {
                stored.shrink(drained);
                if (stored.getAmount() <= 0) {
                    InfFluidGeneratorBlockEntity.this.tank = FluidStack.EMPTY;
                    InfFluidGeneratorBlockEntity.this.infinite = false;
                }
                markUpdated();
            }
            return result;
        }
    }
}

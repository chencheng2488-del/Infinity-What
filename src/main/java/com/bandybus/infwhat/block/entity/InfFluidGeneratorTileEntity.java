package com.bandybus.infwhat.block.entity;

import com.bandybus.infwhat.api.IConfigurableMachine;
import com.bandybus.infwhat.config.InfinityWhatConfig;
import com.bandybus.infwhat.core.SourceOutputSettings;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

import javax.annotation.Nullable;

/**
 * Stores a single fluid type. When the infinite source switch is enabled, inserting any allowed
 * fluid immediately turns it into an unlimited source. Output goes through the standard Forge
 * {@link IFluidHandler} capability, which is what pipes and fluid cables use in 1.12.2.
 *
 * <p>ENGINE LIMIT: {@link FluidStack} stores its amount as an {@code int} in millibuckets, so the
 * capability reports {@link Integer#MAX_VALUE} mB while infinite. The GUI presents the
 * conceptual amount as {@code Long.MAX_VALUE B} (buckets, 1 B = 1000 mB).
 */
public class InfFluidGeneratorTileEntity extends TileEntity
        implements ITickable, IConfigurableMachine {

    private static final String KEY_TANK = "Tank";
    private static final String KEY_INFINITE = "Infinite";
    private static final String KEY_OUTPUT_ENABLED = "OutputEnabled";
    private static final String KEY_OUTPUT_SIDES = "OutputSides";
    private static final String KEY_OUTPUT_RATE_MB = "OutputRateMb";
    private static final String KEY_OUTPUT_RATE_INDEX = "OutputRateIndex";
    /** Card profile key; the tile's own saved rate keeps the legacy {@code Mb} suffix. */
    private static final String KEY_CARD_OUTPUT_RATE = "OutputRate";
    public static final long MAX_OUTPUT_RATE_MB = (long) Integer.MAX_VALUE * 1000L;

    /** Stored fluid; {@code null} means empty. While infinite the amount is only a placeholder. */
    private FluidStack tank;
    private boolean infinite;
    /** Shared output switch, face mask and per-face rate. */
    private final SourceOutputSettings output =
            new SourceOutputSettings(1000L, 1L, MAX_OUTPUT_RATE_MB);

    private final IFluidHandler fluidHandler = new InfiniteFluidTank();

    public InfFluidGeneratorTileEntity() {
    }

    // ------------------------------------------------------------------
    // Capability
    // ------------------------------------------------------------------

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY
                || super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(this.fluidHandler);
        }
        return super.getCapability(capability, facing);
    }

    // ------------------------------------------------------------------
    // Data exposed to the GUI
    // ------------------------------------------------------------------

    @Nullable
    public FluidStack getStoredFluid() {
        return this.tank;
    }

    public boolean isInfinite() {
        return effectivelyInfinite();
    }

    private boolean effectivelyInfinite() {
        return this.infinite && this.tank != null && InfinityWhatConfig.infiniteEnabled();
    }

    public long getStoredAmount() {
        return effectivelyInfinite() ? Long.MAX_VALUE : (this.tank == null ? 0L : this.tank.amount);
    }

    public boolean isOutputEnabled() {
        return this.output.isEnabled();
    }

    public void setOutputEnabled(boolean enabled) {
        if (this.output.isEnabled() == enabled) {
            return;
        }
        this.output.setEnabled(enabled);
        markUpdated();
    }

    public boolean isOutputSideEnabled(EnumFacing direction) {
        return this.output.isSideEnabled(direction);
    }

    public void toggleOutputSide(EnumFacing direction) {
        this.output.toggleSide(direction);
        markUpdated();
    }

    public long getOutputAmountPerTick() {
        return this.output.getRate();
    }

    public void setOutputAmountPerTick(long millibuckets) {
        if (this.output.getRate() == millibuckets) {
            return;
        }
        this.output.setRate(millibuckets);
        markUpdated();
    }

    public int getOutputSideMask() {
        return this.output.getSideMask();
    }

    public void applyOutputConfiguration(boolean enabled, int sideMask, long rateMb) {
        this.output.apply(enabled, sideMask, rateMb);
        markUpdated();
    }

    // ------------------------------------------------------------------
    // Configuration card snapshot
    // ------------------------------------------------------------------

    @Override
    public String getConfigurationType() {
        return "fluid";
    }

    @Override
    public NBTTagCompound writeConfiguration() {
        NBTTagCompound configuration = new NBTTagCompound();
        configuration.setBoolean(KEY_OUTPUT_ENABLED, isOutputEnabled());
        configuration.setInteger(KEY_OUTPUT_SIDES, getOutputSideMask());
        configuration.setLong(KEY_CARD_OUTPUT_RATE, getOutputAmountPerTick());
        return configuration;
    }

    @Override
    public void applyConfiguration(NBTTagCompound configuration) {
        applyOutputConfiguration(configuration.getBoolean(KEY_OUTPUT_ENABLED),
                configuration.getInteger(KEY_OUTPUT_SIDES),
                configuration.getLong(KEY_CARD_OUTPUT_RATE));
    }

    public void clearFluid() {
        if (this.tank == null && !this.infinite) {
            return;
        }
        this.tank = null;
        this.infinite = false;
        markUpdated();
    }

    public float getFillRatio() {
        if (effectivelyInfinite()) {
            return 1.0F;
        }
        if (this.tank == null) {
            return 0.0F;
        }
        int capacity = InfinityWhatConfig.finiteCapacity();
        if (capacity <= 0) {
            return 0.0F;
        }
        return Math.min(1.0F, (float) this.tank.amount / (float) capacity);
    }

    public IFluidHandler getFluidHandler() {
        return this.fluidHandler;
    }

    // ------------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------------

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        if (this.tank != null) {
            NBTTagCompound tankTag = new NBTTagCompound();
            this.tank.writeToNBT(tankTag);
            compound.setTag(KEY_TANK, tankTag);
        }
        compound.setBoolean(KEY_INFINITE, this.infinite);
        compound.setBoolean(KEY_OUTPUT_ENABLED, this.output.isEnabled());
        compound.setInteger(KEY_OUTPUT_SIDES, this.output.getSideMask());
        compound.setLong(KEY_OUTPUT_RATE_MB, this.output.getRate());
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        this.tank = null;
        this.infinite = false;
        if (compound.hasKey(KEY_TANK, 10)) {
            FluidStack loaded = FluidStack.loadFluidStackFromNBT(compound.getCompoundTag(KEY_TANK));
            if (loaded != null && loaded.amount > 0) {
                this.tank = loaded;
            }
        }
        this.infinite = compound.getBoolean(KEY_INFINITE) && this.tank != null;
        this.output.setEnabled(compound.getBoolean(KEY_OUTPUT_ENABLED));
        this.output.setSideMask(compound.hasKey(KEY_OUTPUT_SIDES, 3)
                ? compound.getInteger(KEY_OUTPUT_SIDES) : SourceOutputSettings.ALL_SIDES);
        if (compound.hasKey(KEY_OUTPUT_RATE_MB, 4)) {
            this.output.setRate(compound.getLong(KEY_OUTPUT_RATE_MB));
        } else if (compound.hasKey(KEY_OUTPUT_RATE_INDEX, 3)) {
            // Migrate worlds saved by the previous preset-based release.
            long[] legacyRates = {1L, 10L, 100L, 1000L, 10000L, 100000L, 1000000L,
                    10000000L, 100000000L, 1000000000L};
            int index = Math.max(0, Math.min(legacyRates.length - 1,
                    compound.getInteger(KEY_OUTPUT_RATE_INDEX)));
            this.output.setRate(legacyRates[index]);
        } else {
            this.output.setRate(1000L);
        }
    }

    // ------------------------------------------------------------------
    // Client sync
    // ------------------------------------------------------------------

    @Override
    public NBTTagCompound getUpdateTag() {
        return this.writeToNBT(new NBTTagCompound());
    }

    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(this.pos, 3, this.getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet) {
        this.readFromNBT(packet.getNbtCompound());
    }

    private void markUpdated() {
        this.markDirty();
        if (this.world != null && !this.world.isRemote) {
            this.world.notifyBlockUpdate(this.pos, this.world.getBlockState(this.pos),
                    this.world.getBlockState(this.pos), 3);
        }
    }

    private boolean isDisallowed(FluidStack stack) {
        if (stack == null) {
            return false;
        }
        return !InfinityWhatConfig.isFluidAllowed(fluidId(stack));
    }

    /** Returns the fully qualified fluid id (vanilla fluids are registered without a namespace). */
    private static String fluidId(FluidStack stack) {
        if (stack == null || stack.getFluid() == null) {
            return null;
        }
        String name = net.minecraftforge.fluids.FluidRegistry.getFluidName(stack.getFluid());
        if (name == null) {
            return null;
        }
        return name.contains(":") ? name : "minecraft:" + name;
    }

    // ------------------------------------------------------------------
    // Server tick: active output into selected adjacent fluid handlers
    // ------------------------------------------------------------------

    @Override
    public void update() {
        if (this.world == null || this.world.isRemote) {
            return;
        }
        if (this.output.isEnabled() && this.tank != null) {
            this.pushToNeighbors();
        }
    }

    private void pushToNeighbors() {
        if (this.world == null || this.world.isRemote || this.tank == null) {
            return;
        }

        boolean infiniteSource = effectivelyInfinite();
        boolean changed = false;
        long rate = this.getOutputAmountPerTick();
        for (EnumFacing direction : EnumFacing.values()) {
            if (!isOutputSideEnabled(direction)) {
                continue;
            }
            BlockPos neighborPos = this.pos.offset(direction);
            if (!this.world.isBlockLoaded(neighborPos)) {
                continue;
            }
            TileEntity neighbor = this.world.getTileEntity(neighborPos);
            if (neighbor == null || neighbor == this) {
                continue;
            }
            IFluidHandler target = neighbor.getCapability(
                    CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, direction.getOpposite());
            if (target == null) {
                continue;
            }

            long remaining = infiniteSource ? rate : Math.min(rate, this.tank.amount);
            while (remaining > 0) {
                int offered = (int) Math.min((long) Integer.MAX_VALUE, remaining);
                FluidStack offer = new FluidStack(this.tank, offered);
                int accepted = Math.max(0, Math.min(offered, target.fill(offer, true)));
                if (accepted <= 0) {
                    break;
                }
                remaining -= accepted;
                if (!infiniteSource) {
                    this.tank.amount -= accepted;
                    changed = true;
                    if (this.tank.amount <= 0) {
                        this.tank = null;
                        this.infinite = false;
                        break;
                    }
                }
                if (accepted < offered) {
                    break;
                }
            }
            if (this.tank == null && !infiniteSource) {
                break;
            }
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
        public IFluidTankProperties[] getTankProperties() {
            return new IFluidTankProperties[]{new DynamicTankProperties()};
        }

        @Override
        public int fill(FluidStack resource, boolean doFill) {
            if (resource == null || resource.amount <= 0 || isDisallowed(resource)) {
                return 0;
            }
            FluidStack stored = InfFluidGeneratorTileEntity.this.tank;
            // fluid_type_limit = 1: only one fluid type may be stored at a time.
            if (stored != null && !stored.isFluidEqual(resource)) {
                return 0;
            }

            if (InfinityWhatConfig.infiniteEnabled()) {
                if (doFill) {
                    FluidStack single = resource.copy();
                    single.amount = 1;
                    InfFluidGeneratorTileEntity.this.tank = single;
                    InfFluidGeneratorTileEntity.this.infinite = true;
                    markUpdated();
                }
                // The spec says the amount jumps to the max immediately, so accept it all.
                return resource.amount;
            }

            int capacity = InfinityWhatConfig.finiteCapacity();
            int current = stored == null ? 0 : stored.amount;
            int accepted = Math.min(capacity - current, resource.amount);
            if (accepted <= 0) {
                return 0;
            }
            if (doFill) {
                if (stored == null) {
                    InfFluidGeneratorTileEntity.this.tank = new FluidStack(resource, accepted);
                } else {
                    stored.amount += accepted;
                }
                markUpdated();
            }
            return accepted;
        }

        @Nullable
        @Override
        public FluidStack drain(FluidStack resource, boolean doDrain) {
            if (resource == null || resource.amount <= 0
                    || InfFluidGeneratorTileEntity.this.tank == null
                    || !InfFluidGeneratorTileEntity.this.tank.isFluidEqual(resource)) {
                return null;
            }
            return drain(resource.amount, doDrain);
        }

        @Nullable
        @Override
        public FluidStack drain(int maxDrain, boolean doDrain) {
            FluidStack stored = InfFluidGeneratorTileEntity.this.tank;
            if (stored == null || maxDrain <= 0) {
                return null;
            }
            if (effectivelyInfinite()) {
                // Unlimited source: never depletes, always hands out the requested amount.
                return new FluidStack(stored, maxDrain);
            }
            int drained = Math.min(maxDrain, stored.amount);
            FluidStack result = new FluidStack(stored, drained);
            if (doDrain) {
                stored.amount -= drained;
                if (stored.amount <= 0) {
                    InfFluidGeneratorTileEntity.this.tank = null;
                    InfFluidGeneratorTileEntity.this.infinite = false;
                }
                markUpdated();
            }
            return result;
        }
    }

    /** Live tank view used by {@link IFluidHandler#getTankProperties()}. */
    private final class DynamicTankProperties implements IFluidTankProperties {

        @Nullable
        @Override
        public FluidStack getContents() {
            FluidStack stored = InfFluidGeneratorTileEntity.this.tank;
            if (stored == null) {
                return null;
            }
            int amount = effectivelyInfinite() ? Integer.MAX_VALUE : stored.amount;
            return new FluidStack(stored, amount);
        }

        @Override
        public int getCapacity() {
            return effectivelyInfinite() ? Integer.MAX_VALUE : InfinityWhatConfig.finiteCapacity();
        }

        @Override
        public boolean canFill() {
            return true;
        }

        @Override
        public boolean canDrain() {
            return true;
        }

        @Override
        public boolean canFillFluidType(FluidStack fluidStack) {
            if (fluidStack == null || isDisallowed(fluidStack)) {
                return false;
            }
            FluidStack stored = InfFluidGeneratorTileEntity.this.tank;
            return stored == null || stored.isFluidEqual(fluidStack);
        }

        @Override
        public boolean canDrainFluidType(FluidStack fluidStack) {
            return fluidStack != null && InfFluidGeneratorTileEntity.this.tank != null
                    && InfFluidGeneratorTileEntity.this.tank.isFluidEqual(fluidStack);
        }
    }

    /** Whether this tile entity is still attached to a loaded world at its position. */
    public boolean isUsable() {
        return this.world != null && this.world.getTileEntity(this.pos) == this;
    }
}

package com.bandybus.infwhat.block.entity;

import com.bandybus.infwhat.config.InfinityWhatConfig;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

/**
 * Stores a single fluid type. When the infinite source switch is enabled, inserting any allowed
 * fluid immediately turns it into an unlimited source. 1.8.9 has no fluid capability, so this
 * tile entity implements the legacy {@link IFluidHandler} interface directly; pipes that use the
 * Forge fluid API detect it with an {@code instanceof} check.
 *
 * <p>ENGINE LIMIT: {@link FluidStack} stores its amount as an {@code int} in millibuckets, so the
 * handler reports {@link Integer#MAX_VALUE} mB while infinite. The GUI presents the conceptual
 * amount as {@code Long.MAX_VALUE B}.
 */
public class InfFluidGeneratorTileEntity extends TileEntity implements ITickable, IFluidHandler {

    private static final String KEY_TANK = "Tank";
    private static final String KEY_INFINITE = "Infinite";
    private static final String KEY_OUTPUT_ENABLED = "OutputEnabled";
    private static final String KEY_OUTPUT_SIDES = "OutputSides";
    private static final String KEY_OUTPUT_RATE_MB = "OutputRateMb";
    private static final String KEY_OUTPUT_RATE_INDEX = "OutputRateIndex";
    public static final long MAX_OUTPUT_RATE_MB = (long) Integer.MAX_VALUE * 1000L;

    private FluidStack tank;
    private boolean infinite;
    private boolean outputEnabled;
    private int outputSideMask = (1 << EnumFacing.values().length) - 1;
    private long outputAmountPerTickMb = 1000L;

    public InfFluidGeneratorTileEntity() {
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

    private boolean effectivelyInfinite() {
        return this.infinite && this.tank != null && InfinityWhatConfig.infiniteEnabled();
    }

    public long getStoredAmount() {
        return effectivelyInfinite() ? Long.MAX_VALUE : (this.tank == null ? 0L : this.tank.amount);
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

    public boolean isOutputSideEnabled(EnumFacing direction) {
        return (this.outputSideMask & (1 << direction.ordinal())) != 0;
    }

    public void toggleOutputSide(EnumFacing direction) {
        this.outputSideMask ^= 1 << direction.ordinal();
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
        this.outputSideMask = sideMask & ((1 << EnumFacing.values().length) - 1);
        this.outputAmountPerTickMb = Math.max(1L, Math.min(MAX_OUTPUT_RATE_MB, rateMb));
        markUpdated();
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

    // ------------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------------

    @Override
    public void writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        if (this.tank != null) {
            NBTTagCompound tankTag = new NBTTagCompound();
            this.tank.writeToNBT(tankTag);
            compound.setTag(KEY_TANK, tankTag);
        }
        compound.setBoolean(KEY_INFINITE, this.infinite);
        compound.setBoolean(KEY_OUTPUT_ENABLED, this.outputEnabled);
        compound.setInteger(KEY_OUTPUT_SIDES, this.outputSideMask);
        compound.setLong(KEY_OUTPUT_RATE_MB, this.outputAmountPerTickMb);
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
        this.outputEnabled = compound.getBoolean(KEY_OUTPUT_ENABLED);
        this.outputSideMask = compound.hasKey(KEY_OUTPUT_SIDES, 3)
                ? compound.getInteger(KEY_OUTPUT_SIDES)
                : (1 << EnumFacing.values().length) - 1;
        if (compound.hasKey(KEY_OUTPUT_RATE_MB, 4)) {
            this.outputAmountPerTickMb = Math.max(1L, Math.min(MAX_OUTPUT_RATE_MB,
                    compound.getLong(KEY_OUTPUT_RATE_MB)));
        } else if (compound.hasKey(KEY_OUTPUT_RATE_INDEX, 3)) {
            long[] legacyRates = {1L, 10L, 100L, 1000L, 10000L, 100000L, 1000000L,
                    10000000L, 100000000L, 1000000000L};
            int index = Math.max(0, Math.min(legacyRates.length - 1,
                    compound.getInteger(KEY_OUTPUT_RATE_INDEX)));
            this.outputAmountPerTickMb = legacyRates[index];
        } else {
            this.outputAmountPerTickMb = 1000L;
        }
    }

    // ------------------------------------------------------------------
    // Client sync
    // ------------------------------------------------------------------

    @Override
    public net.minecraft.network.Packet getDescriptionPacket() {
        return new S35PacketUpdateTileEntity(this.pos, 3, this.serializeNBT());
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity packet) {
        this.readFromNBT(packet.getNbtCompound());
    }

    private void markUpdated() {
        this.markDirty();
        if (this.worldObj != null && !this.worldObj.isRemote) {
            this.worldObj.markBlockForUpdate(this.pos);
        }
    }

    private static String fluidId(FluidStack stack) {
        if (stack == null || stack.getFluid() == null) {
            return null;
        }
        String name = FluidRegistry.getFluidName(stack.getFluid());
        if (name == null) {
            return null;
        }
        return name.contains(":") ? name : "minecraft:" + name;
    }

    private boolean isDisallowed(FluidStack stack) {
        return stack != null && !InfinityWhatConfig.isFluidAllowed(fluidId(stack));
    }

    // ------------------------------------------------------------------
    // Server tick: active output into selected adjacent fluid handlers
    // ------------------------------------------------------------------

    @Override
    public void update() {
        if (this.worldObj == null || this.worldObj.isRemote) {
            return;
        }
        if (this.outputEnabled && this.tank != null) {
            this.pushToNeighbors();
        }
    }

    private void pushToNeighbors() {
        if (this.worldObj == null || this.worldObj.isRemote || this.tank == null) {
            return;
        }

        boolean infiniteSource = effectivelyInfinite();
        boolean changed = false;
        long rate = getOutputAmountPerTick();
        for (EnumFacing direction : EnumFacing.values()) {
            if (!isOutputSideEnabled(direction)) {
                continue;
            }
            net.minecraft.util.BlockPos neighborPos = this.pos.offset(direction);
            if (!this.worldObj.isBlockLoaded(neighborPos)) {
                continue;
            }
            TileEntity neighbor = this.worldObj.getTileEntity(neighborPos);
            if (!(neighbor instanceof IFluidHandler) || neighbor == this) {
                continue;
            }
            IFluidHandler target = (IFluidHandler) neighbor;

            long remaining = infiniteSource ? rate : Math.min(rate, this.tank.amount);
            while (remaining > 0) {
                int offered = (int) Math.min((long) Integer.MAX_VALUE, remaining);
                FluidStack offer = new FluidStack(this.tank, offered);
                int accepted = Math.max(0,
                        Math.min(offered, target.fill(direction.getOpposite(), offer, true)));
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
    // Legacy IFluidHandler (1.8.9 has no fluid capability)
    // ------------------------------------------------------------------

    @Override
    public int fill(EnumFacing from, FluidStack resource, boolean doFill) {
        if (resource == null || resource.amount <= 0 || isDisallowed(resource)) {
            return 0;
        }
        FluidStack stored = this.tank;
        if (stored != null && !stored.isFluidEqual(resource)) {
            return 0;
        }

        if (InfinityWhatConfig.infiniteEnabled()) {
            if (doFill) {
                FluidStack single = resource.copy();
                single.amount = 1;
                this.tank = single;
                this.infinite = true;
                markUpdated();
            }
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
                this.tank = new FluidStack(resource, accepted);
            } else {
                stored.amount += accepted;
            }
            markUpdated();
        }
        return accepted;
    }

    @Override
    public FluidStack drain(EnumFacing from, FluidStack resource, boolean doDrain) {
        if (resource == null || resource.amount <= 0 || this.tank == null
                || !this.tank.isFluidEqual(resource)) {
            return null;
        }
        return drain(from, resource.amount, doDrain);
    }

    @Override
    public FluidStack drain(EnumFacing from, int maxDrain, boolean doDrain) {
        FluidStack stored = this.tank;
        if (stored == null || maxDrain <= 0) {
            return null;
        }
        if (effectivelyInfinite()) {
            return new FluidStack(stored, maxDrain);
        }
        int drained = Math.min(maxDrain, stored.amount);
        FluidStack result = new FluidStack(stored, drained);
        if (doDrain) {
            stored.amount -= drained;
            if (stored.amount <= 0) {
                this.tank = null;
                this.infinite = false;
            }
            markUpdated();
        }
        return result;
    }

    @Override
    public boolean canFill(EnumFacing from, Fluid fluid) {
        if (fluid == null) {
            return false;
        }
        if (this.tank == null) {
            return InfinityWhatConfig.isFluidAllowed(
                    fluidId(new FluidStack(fluid, 1)));
        }
        return this.tank.getFluid() == fluid;
    }

    @Override
    public boolean canDrain(EnumFacing from, Fluid fluid) {
        return this.tank != null && (fluid == null || this.tank.getFluid() == fluid);
    }

    @Override
    public FluidTankInfo[] getTankInfo(EnumFacing from) {
        FluidStack contents = null;
        if (this.tank != null) {
            contents = new FluidStack(this.tank,
                    effectivelyInfinite() ? Integer.MAX_VALUE : this.tank.amount);
        }
        int capacity = effectivelyInfinite()
                ? Integer.MAX_VALUE : InfinityWhatConfig.finiteCapacity();
        return new FluidTankInfo[]{new FluidTankInfo(contents, capacity)};
    }

    /** Whether this tile entity is still attached to a loaded world at its position. */
    public boolean isUsable() {
        return this.worldObj != null && this.worldObj.getTileEntity(this.pos) == this;
    }
}

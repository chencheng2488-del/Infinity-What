package com.bandybus.infwhat.block.entity;

import com.bandybus.infwhat.block.RedstoneMode;
import com.bandybus.infwhat.config.InfinityWhatConfig;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

import java.util.Arrays;

/**
 * Stores a single fluid type. When the infinite source switch is enabled, inserting any allowed
 * fluid immediately turns it into an unlimited source. 1.7.10 has no fluid capability, so this
 * tile entity implements the legacy {@link IFluidHandler} interface directly.
 *
 * <p>Output is configured per face: six independent rates, a redstone gate, a batch size and a
 * shared output switch. The infinite amount is reported as {@link Integer#MAX_VALUE} mB because
 * {@link FluidStack} stores its amount as an {@code int}.
 */
public class InfFluidGeneratorTileEntity extends TileEntity implements IFluidHandler {

    private static final String KEY_TANK = "Tank";
    private static final String KEY_INFINITE = "Infinite";
    private static final String KEY_OUTPUT_ENABLED = "OutputEnabled";
    private static final String KEY_OUTPUT_SIDES = "OutputSides";
    private static final String KEY_REDSTONE_MODE = "RedstoneMode";
    private static final String KEY_PULSE_ON = "PulseOnTicks";
    private static final String KEY_PULSE_OFF = "PulseOffTicks";
    private static final String KEY_BATCH_SIZE = "BatchSize";
    /** Legacy single-rate keys, only read for 2.1.x saves. */
    private static final String KEY_LEGACY_OUTPUT_RATE_MB = "OutputRateMb";
    private static final String KEY_LEGACY_OUTPUT_RATE_INDEX = "OutputRateIndex";
    private static final String KEY_DATA_VERSION = "InfDataVersion";
    private static final int DATA_VERSION = 2;
    private static final int SIDE_COUNT = ForgeDirection.VALID_DIRECTIONS.length;
    private static final int ALL_SIDES = (1 << SIDE_COUNT) - 1;
    private static final String[] SIDE_RATE_KEYS = createSideRateKeys();
    private static final long DEFAULT_OUTPUT_RATE_MB = 1000L;

    /** Highest rate the GUI accepts, expressed in millibuckets per side and tick. */
    public static final long MAX_OUTPUT_RATE_MB = (long) Integer.MAX_VALUE * 1000L;
    public static final int DEFAULT_PULSE_TICKS = 20;
    public static final int MIN_PULSE_TICKS = 1;
    public static final int MAX_PULSE_TICKS = 12000;
    public static final int DEFAULT_BATCH_SIZE = 1;
    public static final int MIN_BATCH_SIZE = 1;
    public static final int MAX_BATCH_SIZE = 64;

    private FluidStack tank;
    private boolean infinite;
    private boolean outputEnabled;
    private int outputSideMask = ALL_SIDES;
    private RedstoneMode redstoneMode = RedstoneMode.ALWAYS;
    private int pulseOnTicks = DEFAULT_PULSE_TICKS;
    private int pulseOffTicks = DEFAULT_PULSE_TICKS;
    private int batchSize = DEFAULT_BATCH_SIZE;
    private int batchCounter;
    private final OutputStatus outputStatus = new OutputStatus();
    private int pushStartSide;
    private boolean wasOutputActive;
    private final long[] sideRates = new long[SIDE_COUNT];
    private final long[] lastNonZeroRates = new long[SIDE_COUNT];

    public InfFluidGeneratorTileEntity() {
        Arrays.fill(this.sideRates, DEFAULT_OUTPUT_RATE_MB);
        Arrays.fill(this.lastNonZeroRates, DEFAULT_OUTPUT_RATE_MB);
    }

    private static String[] createSideRateKeys() {
        String[] keys = new String[SIDE_COUNT];
        for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
            String name = direction.name().toLowerCase(java.util.Locale.ROOT);
            keys[direction.ordinal()] = "SideRate"
                    + Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }
        return keys;
    }

    private static int sideIndex(ForgeDirection direction) {
        return direction.ordinal();
    }

    // ------------------------------------------------------------------
    // Data exposed to the GUI
    // ------------------------------------------------------------------

    /** Stored fluid stack, or null while the tank is empty. */
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

    public boolean isOutputSideEnabled(ForgeDirection direction) {
        return (this.outputSideMask & (1 << direction.ordinal())) != 0;
    }

    public void toggleOutputSide(ForgeDirection direction) {
        int index = sideIndex(direction);
        boolean enabling = !isOutputSideEnabled(direction);
        long restoredRate = this.sideRates[index];
        if (enabling && restoredRate <= 0L) {
            restoredRate = this.lastNonZeroRates[index] > 0L
                    ? this.lastNonZeroRates[index] : DEFAULT_OUTPUT_RATE_MB;
        }
        if (enabling
                && getTotalEffectiveRateMb() + restoredRate > InfinityWhatConfig.maxFluidTotalRateMb()) {
            return;
        }
        this.outputSideMask ^= 1 << index;
        if (enabling && this.sideRates[index] <= 0L) {
            this.sideRates[index] = restoredRate;
        }
        markUpdated();
    }

    public long getSideRateMb(ForgeDirection direction) {
        return this.sideRates[sideIndex(direction)];
    }

    public long getEffectiveSideRateMb(ForgeDirection direction) {
        return isOutputSideEnabled(direction) ? this.sideRates[sideIndex(direction)] : 0L;
    }

    public long getTotalEffectiveRateMb() {
        long total = 0L;
        for (int i = 0; i < this.sideRates.length; i++) {
            if ((this.outputSideMask & (1 << i)) != 0) {
                total += this.sideRates[i];
            }
        }
        return total;
    }

    /** The common rate when all six sides share one value, otherwise -1. */
    public long getUniformSideRateMb() {
        long first = this.sideRates[0];
        for (int i = 1; i < this.sideRates.length; i++) {
            if (this.sideRates[i] != first) {
                return -1L;
            }
        }
        return first;
    }

    public boolean hasMixedSideRates() {
        return getUniformSideRateMb() < 0L;
    }

    public boolean setSideRateMb(ForgeDirection direction, long millibuckets) {
        if (millibuckets < 0L || millibuckets > InfinityWhatConfig.maxFluidRatePerSideMb()) {
            return false;
        }
        int index = sideIndex(direction);
        if (this.sideRates[index] == millibuckets) {
            return true;
        }
        long newTotal = getTotalEffectiveRateMb();
        if (isOutputSideEnabled(direction)) {
            newTotal = newTotal - getEffectiveSideRateMb(direction) + millibuckets;
        }
        if (newTotal > InfinityWhatConfig.maxFluidTotalRateMb()) {
            return false;
        }
        this.sideRates[index] = millibuckets;
        if (millibuckets > 0L) {
            this.lastNonZeroRates[index] = millibuckets;
        }
        markUpdated();
        return true;
    }

    public boolean setUniformSideRateMb(long millibuckets) {
        long[] rates = new long[this.sideRates.length];
        Arrays.fill(rates, millibuckets);
        return applyFullConfiguration(this.outputEnabled, this.outputSideMask, rates);
    }

    public int getOutputSideMask() {
        return this.outputSideMask;
    }

    public RedstoneMode getRedstoneMode() {
        return this.redstoneMode;
    }

    public void setRedstoneMode(RedstoneMode mode) {
        if (mode == null || this.redstoneMode == mode) {
            return;
        }
        this.redstoneMode = mode;
        markUpdated();
    }

    public int getPulseOnTicks() {
        return this.pulseOnTicks;
    }

    public int getPulseOffTicks() {
        return this.pulseOffTicks;
    }

    public boolean setPulseTicks(int onTicks, int offTicks) {
        if (onTicks < MIN_PULSE_TICKS || onTicks > MAX_PULSE_TICKS
                || offTicks < MIN_PULSE_TICKS || offTicks > MAX_PULSE_TICKS) {
            return false;
        }
        if (this.pulseOnTicks == onTicks && this.pulseOffTicks == offTicks) {
            return true;
        }
        this.pulseOnTicks = onTicks;
        this.pulseOffTicks = offTicks;
        markUpdated();
        return true;
    }

    /** Whether the redstone gate currently allows output. */
    public boolean isRedstoneAllowed() {
        if (this.worldObj == null) {
            return this.redstoneMode == RedstoneMode.ALWAYS;
        }
        switch (this.redstoneMode) {
            case HIGH:
                return this.worldObj.isBlockIndirectlyGettingPowered(
                        this.xCoord, this.yCoord, this.zCoord);
            case LOW:
                return !this.worldObj.isBlockIndirectlyGettingPowered(
                        this.xCoord, this.yCoord, this.zCoord);
            case PULSE:
                long period = (long) this.pulseOnTicks + this.pulseOffTicks;
                long phase = Math.floorMod(this.worldObj.getTotalWorldTime()
                        + this.xCoord * 31L + this.yCoord * 961L + this.zCoord * 29791L, period);
                return phase < this.pulseOnTicks;
            case ALWAYS:
            default:
                return true;
        }
    }

    public int getComparatorOutput() {
        if (!this.outputEnabled || !isRedstoneAllowed()) {
            return 0;
        }
        long total = getTotalEffectiveRateMb();
        if (total <= 0L) {
            return 0;
        }
        long buckets = Math.max(1L, total / 1000L);
        return (int) Math.min(15L, buckets);
    }

    public boolean applyFullConfiguration(boolean enabled, int sideMask, long[] rates) {
        if (rates == null || rates.length != this.sideRates.length) {
            return false;
        }
        int mask = sideMask & ALL_SIDES;
        long maxPerSide = InfinityWhatConfig.maxFluidRatePerSideMb();
        long[] validated = new long[rates.length];
        long total = 0L;
        for (int i = 0; i < rates.length; i++) {
            if (rates[i] < 0L || rates[i] > maxPerSide) {
                return false;
            }
            validated[i] = rates[i];
            if ((mask & (1 << i)) != 0) {
                total += rates[i];
            }
        }
        if (total > InfinityWhatConfig.maxFluidTotalRateMb()) {
            return false;
        }
        this.outputEnabled = enabled;
        this.outputSideMask = mask;
        for (int i = 0; i < validated.length; i++) {
            this.sideRates[i] = validated[i];
            if (validated[i] > 0L) {
                this.lastNonZeroRates[i] = validated[i];
            }
        }
        markUpdated();
        return true;
    }

    /** Legacy global-rate entry point kept for old cards; all six sides get the same rate. */
    public boolean applyOutputConfiguration(boolean enabled, int sideMask, long rateMb) {
        long[] rates = new long[this.sideRates.length];
        Arrays.fill(rates, Math.max(1L, rateMb));
        return applyFullConfiguration(enabled, sideMask, rates);
    }

    public boolean applyOutputState(boolean enabled, int sideMask) {
        int mask = sideMask & ALL_SIDES;
        if (this.outputEnabled == enabled && this.outputSideMask == mask) {
            return true;
        }
        this.outputEnabled = enabled;
        this.outputSideMask = mask;
        markUpdated();
        return true;
    }

    public boolean applyRates(long[] rates) {
        if (rates == null || rates.length != this.sideRates.length) {
            return false;
        }
        long maxPerSide = InfinityWhatConfig.maxFluidRatePerSideMb();
        long[] validated = new long[rates.length];
        long total = 0L;
        for (int i = 0; i < rates.length; i++) {
            if (rates[i] < 0L || rates[i] > maxPerSide) {
                return false;
            }
            validated[i] = rates[i];
            if ((this.outputSideMask & (1 << i)) != 0) {
                total += rates[i];
            }
        }
        if (total > InfinityWhatConfig.maxFluidTotalRateMb()) {
            return false;
        }
        for (int i = 0; i < validated.length; i++) {
            this.sideRates[i] = validated[i];
            if (validated[i] > 0L) {
                this.lastNonZeroRates[i] = validated[i];
            }
        }
        markUpdated();
        return true;
    }

    public boolean applyRedstoneConfiguration(RedstoneMode mode, int onTicks, int offTicks) {
        if (mode == null || onTicks < MIN_PULSE_TICKS || onTicks > MAX_PULSE_TICKS
                || offTicks < MIN_PULSE_TICKS || offTicks > MAX_PULSE_TICKS) {
            return false;
        }
        this.redstoneMode = mode;
        this.pulseOnTicks = onTicks;
        this.pulseOffTicks = offTicks;
        markUpdated();
        return true;
    }

    public int getBatchSize() {
        return this.batchSize;
    }

    public boolean setBatchSize(int size) {
        if (size < MIN_BATCH_SIZE || size > MAX_BATCH_SIZE) {
            return false;
        }
        if (this.batchSize == size) {
            return true;
        }
        this.batchSize = size;
        this.batchCounter = 0;
        markUpdated();
        return true;
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
        compound.setInteger(KEY_DATA_VERSION, DATA_VERSION);
        compound.setBoolean(KEY_INFINITE, this.infinite);
        compound.setBoolean(KEY_OUTPUT_ENABLED, this.outputEnabled);
        compound.setInteger(KEY_OUTPUT_SIDES, this.outputSideMask);
        compound.setInteger(KEY_REDSTONE_MODE, this.redstoneMode.ordinal());
        compound.setInteger(KEY_PULSE_ON, this.pulseOnTicks);
        compound.setInteger(KEY_PULSE_OFF, this.pulseOffTicks);
        compound.setInteger(KEY_BATCH_SIZE, this.batchSize);
        for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
            compound.setLong(SIDE_RATE_KEYS[sideIndex(direction)], this.sideRates[sideIndex(direction)]);
        }
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
        this.redstoneMode = RedstoneMode.byId(compound.getInteger(KEY_REDSTONE_MODE));
        this.pulseOnTicks = clampPulseTicks(compound, KEY_PULSE_ON);
        this.pulseOffTicks = clampPulseTicks(compound, KEY_PULSE_OFF);
        this.batchSize = Math.max(MIN_BATCH_SIZE,
                Math.min(MAX_BATCH_SIZE, compound.getInteger(KEY_BATCH_SIZE)));
        this.batchCounter = 0;
        loadOutputRates(compound);
    }

    private static int clampPulseTicks(NBTTagCompound compound, String key) {
        if (!compound.hasKey(key, 3)) {
            return DEFAULT_PULSE_TICKS;
        }
        return Math.max(MIN_PULSE_TICKS, Math.min(MAX_PULSE_TICKS, compound.getInteger(key)));
    }

    private void loadOutputRates(NBTTagCompound compound) {
        int dataVersion = compound.hasKey(KEY_DATA_VERSION, 3)
                ? compound.getInteger(KEY_DATA_VERSION) : 1;
        int legacyMask = (compound.hasKey(KEY_OUTPUT_SIDES, 3)
                ? compound.getInteger(KEY_OUTPUT_SIDES) : ALL_SIDES) & ALL_SIDES;
        long legacyRate = readLegacyRate(compound);

        long[] loaded = new long[this.sideRates.length];
        for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
            int index = sideIndex(direction);
            loaded[index] = dataVersion >= DATA_VERSION
                    ? (compound.hasKey(SIDE_RATE_KEYS[index], 4)
                            ? compound.getLong(SIDE_RATE_KEYS[index]) : legacyRate)
                    : legacyRate;
        }

        this.outputSideMask = (dataVersion >= DATA_VERSION
                ? (compound.hasKey(KEY_OUTPUT_SIDES, 3)
                        ? compound.getInteger(KEY_OUTPUT_SIDES) : ALL_SIDES)
                : legacyMask) & ALL_SIDES;
        long maxPerSide = InfinityWhatConfig.maxFluidRatePerSideMb();
        for (int i = 0; i < loaded.length; i++) {
            loaded[i] = Math.max(0L, Math.min(maxPerSide, loaded[i]));
        }
        clampTotalRate(loaded);
        for (int i = 0; i < loaded.length; i++) {
            this.sideRates[i] = loaded[i];
            this.lastNonZeroRates[i] = loaded[i] > 0L ? loaded[i] : DEFAULT_OUTPUT_RATE_MB;
        }
    }

    private static long readLegacyRate(NBTTagCompound compound) {
        if (compound.hasKey(KEY_LEGACY_OUTPUT_RATE_MB, 4)) {
            return Math.max(1L, Math.min(MAX_OUTPUT_RATE_MB,
                    compound.getLong(KEY_LEGACY_OUTPUT_RATE_MB)));
        }
        if (compound.hasKey(KEY_LEGACY_OUTPUT_RATE_INDEX, 3)) {
            long[] legacyRates = {1L, 10L, 100L, 1000L, 10000L, 100000L, 1000000L,
                    10000000L, 100000000L, 1000000000L};
            int index = Math.max(0, Math.min(legacyRates.length - 1,
                    compound.getInteger(KEY_LEGACY_OUTPUT_RATE_INDEX)));
            return legacyRates[index];
        }
        return DEFAULT_OUTPUT_RATE_MB;
    }

    private void clampTotalRate(long[] rates) {
        long maxTotal = InfinityWhatConfig.maxFluidTotalRateMb();
        long total = 0L;
        for (int i = 0; i < rates.length; i++) {
            if ((this.outputSideMask & (1 << i)) != 0) {
                total += rates[i];
            }
        }
        if (total <= maxTotal) {
            return;
        }
        long overflow = total - maxTotal;
        for (int i = 0; i < rates.length && overflow > 0; i++) {
            if ((this.outputSideMask & (1 << i)) == 0) {
                continue;
            }
            long reduction = Math.min(rates[i], overflow);
            rates[i] -= reduction;
            overflow -= reduction;
        }
    }

    // ------------------------------------------------------------------
    // Client sync
    // ------------------------------------------------------------------

    @Override
    public net.minecraft.network.Packet getDescriptionPacket() {
        NBTTagCompound tag = new NBTTagCompound();
        this.writeToNBT(tag);
        return new S35PacketUpdateTileEntity(this.xCoord, this.yCoord, this.zCoord, 1, tag);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity packet) {
        this.readFromNBT(packet.getNbtCompound());
    }

    private void markUpdated() {
        this.markDirty();
        if (this.worldObj != null && !this.worldObj.isRemote) {
            this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
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
    // Server tick
    // ------------------------------------------------------------------

    @Override
    public void updateEntity() {
        if (this.worldObj == null || this.worldObj.isRemote) {
            return;
        }
        this.outputStatus.tick();
        boolean active = this.outputEnabled && this.tank != null && isRedstoneAllowed();
        if (!active) {
            this.wasOutputActive = false;
            return;
        }
        if (!this.wasOutputActive) {
            this.wasOutputActive = true;
            this.outputStatus.wake();
        }
        if (getTotalEffectiveRateMb() <= 0L) {
            this.outputStatus.notifyViewers(this::isViewingMenu, OutputStatus.NO_SIDES);
            return;
        }
        if (!this.outputStatus.canAttempt()) {
            return;
        }
        if (++this.batchCounter >= this.batchSize) {
            int batch = this.batchSize;
            this.batchCounter = 0;
            int outcome = OutputStatus.NONE;
            for (int i = 0; i < batch; i++) {
                outcome = OutputStatus.merge(outcome, pushToNeighbors());
            }
            this.outputStatus.record(outcome);
            this.outputStatus.notifyViewers(this::isViewingMenu, outcome);
        }
    }

    public void wakeOutput() {
        this.outputStatus.wake();
    }

    private boolean isViewingMenu(net.minecraft.inventory.Container menu) {
        return menu instanceof com.bandybus.infwhat.container.InfFluidGeneratorContainer
                && ((com.bandybus.infwhat.container.InfFluidGeneratorContainer) menu)
                        .getTileEntity() == this;
    }

    private int pushToNeighbors() {
        if (this.worldObj == null || this.worldObj.isRemote || this.tank == null) {
            return OutputStatus.NONE;
        }

        boolean infiniteSource = effectivelyInfinite();
        boolean changed = false;
        int result = OutputStatus.NONE;
        ForgeDirection[] directions = ForgeDirection.VALID_DIRECTIONS;
        for (int offset = 0; offset < directions.length; offset++) {
            ForgeDirection direction = directions[(this.pushStartSide + offset) % directions.length];
            long rate = getEffectiveSideRateMb(direction);
            if (rate <= 0L) {
                continue;
            }
            int neighborX = this.xCoord + direction.offsetX;
            int neighborY = this.yCoord + direction.offsetY;
            int neighborZ = this.zCoord + direction.offsetZ;
            if (!this.worldObj.blockExists(neighborX, neighborY, neighborZ)) {
                continue;
            }
            TileEntity neighbor = this.worldObj.getTileEntity(neighborX, neighborY, neighborZ);
            if (!(neighbor instanceof IFluidHandler) || neighbor == this) {
                continue;
            }
            IFluidHandler target = (IFluidHandler) neighbor;
            boolean validTank = target.canFill(direction.getOpposite(), this.tank.getFluid());

            long acceptedForSide = 0L;
            long remaining = infiniteSource ? rate : Math.min(rate, this.tank.amount);
            while (remaining > 0) {
                int offered = (int) Math.min((long) Integer.MAX_VALUE, remaining);
                FluidStack offer = new FluidStack(this.tank, offered);
                int accepted = Math.max(0,
                        Math.min(offered, target.fill(direction.getOpposite(), offer, true)));
                if (accepted <= 0) {
                    break;
                }
                acceptedForSide += accepted;
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
            if (acceptedForSide > 0) {
                result = OutputStatus.OK;
            } else if (validTank) {
                if (result != OutputStatus.OK) {
                    result = OutputStatus.FULL;
                }
            } else if (result == OutputStatus.NONE) {
                result = OutputStatus.REJECTED;
            }
            if (this.tank == null && !infiniteSource) {
                break;
            }
        }
        this.pushStartSide = (this.pushStartSide + 1) % directions.length;
        if (changed) {
            markUpdated();
        }
        return result;
    }

    // ------------------------------------------------------------------
    // Legacy IFluidHandler
    // ------------------------------------------------------------------

    @Override
    public int fill(ForgeDirection from, FluidStack resource, boolean doFill) {
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
    public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain) {
        if (resource == null || resource.amount <= 0 || this.tank == null
                || !this.tank.isFluidEqual(resource)) {
            return null;
        }
        return drain(from, resource.amount, doDrain);
    }

    @Override
    public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain) {
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
    public boolean canFill(ForgeDirection from, Fluid fluid) {
        if (fluid == null) {
            return false;
        }
        if (this.tank == null) {
            return InfinityWhatConfig.isFluidAllowed(fluidId(new FluidStack(fluid, 1)));
        }
        return this.tank.getFluid() == fluid;
    }

    @Override
    public boolean canDrain(ForgeDirection from, Fluid fluid) {
        return this.tank != null && (fluid == null || this.tank.getFluid() == fluid);
    }

    @Override
    public FluidTankInfo[] getTankInfo(ForgeDirection from) {
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
        return this.worldObj != null
                && this.worldObj.getTileEntity(this.xCoord, this.yCoord, this.zCoord) == this;
    }
}

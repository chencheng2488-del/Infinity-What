package com.bandybus.infwhat.block.entity;

import com.bandybus.infwhat.block.RedstoneMode;
import com.bandybus.infwhat.config.InfinityWhatConfig;
import com.bandybus.infwhat.container.InfFluidGeneratorContainer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

import javax.annotation.Nullable;
import java.util.Arrays;

/**
 * Stores a single fluid type. When the infinite source switch is enabled, inserting any allowed
 * fluid immediately turns it into an unlimited source. Output goes through the standard Forge
 * {@link IFluidHandler} capability, which is what pipes and fluid cables use in 1.12.2.
 *
 * <p>ENGINE LIMIT: {@link FluidStack} stores its amount as an {@code int} in millibuckets, so the
 * capability reports {@link Integer#MAX_VALUE} mB while infinite. The GUI presents the
 * conceptual amount as {@code Long.MAX_VALUE B} (buckets, 1 B = 1000 mB).
 */
public class InfFluidGeneratorTileEntity extends TileEntity implements ITickable {

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
    private static final int SIDE_COUNT = EnumFacing.VALUES.length;
    private static final int ALL_SIDES = (1 << SIDE_COUNT) - 1;
    private static final String[] SIDE_RATE_KEYS = createSideRateKeys();
    private static final long DEFAULT_OUTPUT_RATE_MB = 1_000L;
    public static final long MAX_OUTPUT_RATE_MB = (long) Integer.MAX_VALUE * 1_000L;
    public static final int DEFAULT_PULSE_TICKS = 20;
    public static final int MIN_PULSE_TICKS = 1;
    public static final int MAX_PULSE_TICKS = 12000;
    public static final int DEFAULT_BATCH_SIZE = 1;
    public static final int MIN_BATCH_SIZE = 1;
    public static final int MAX_BATCH_SIZE = 64;

    /** Stored fluid; {@code null} means empty. While infinite the amount is only a placeholder. */
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
    /** Per-side output rate in mB/tick; 0 means the side produces nothing. */
    private final long[] sideRates = new long[SIDE_COUNT];
    /** Last non-zero rate per side, so toggling a side back on restores a useful value. */
    private final long[] lastNonZeroRates = new long[SIDE_COUNT];

    private final IFluidHandler fluidHandler = new InfiniteFluidTank();

    public InfFluidGeneratorTileEntity() {
        Arrays.fill(this.sideRates, DEFAULT_OUTPUT_RATE_MB);
        Arrays.fill(this.lastNonZeroRates, DEFAULT_OUTPUT_RATE_MB);
    }

    private static String[] createSideRateKeys() {
        String[] keys = new String[EnumFacing.VALUES.length];
        for (EnumFacing direction : EnumFacing.VALUES) {
            String name = direction.getName();
            keys[direction.getIndex()] = "SideRate"
                    + Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }
        return keys;
    }

    private static int sideIndex(EnumFacing direction) {
        return direction.getIndex();
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

    /** Display amount: Long.MAX_VALUE buckets when infinite, otherwise the stored mB amount. */
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
        return (this.outputSideMask & (1 << direction.getIndex())) != 0;
    }

    public void toggleOutputSide(EnumFacing direction) {
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

    /** Rate configured for one side, in mB/tick. 0 means the side produces nothing. */
    public long getSideRateMb(EnumFacing direction) {
        return this.sideRates[sideIndex(direction)];
    }

    /** Rate actually used for one side; a disabled side always reports 0. */
    public long getEffectiveSideRateMb(EnumFacing direction) {
        return isOutputSideEnabled(direction) ? this.sideRates[sideIndex(direction)] : 0L;
    }

    /** Sum of the effective per-side rates. */
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

    /**
     * Sets one side's rate. Returns false without changing anything when the value is outside
     * the server limits or would push the total above the configured cap.
     */
    public boolean setSideRateMb(EnumFacing direction, long millibuckets) {
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

    /** Applies one rate to all six sides, as the 2.2 phase-1 GUI does. */
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

    /** Sets both pulse phases; returns false without changing anything when out of range. */
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

    /**
     * Whether the redstone gate currently allows output. {@code PULSE} ignores the incoming
     * signal and runs on its own work/stop cycle; the cycle is offset by the block position so
     * neighbouring generators do not all fire on the same tick.
     */
    public boolean isRedstoneAllowed() {
        if (this.world == null) {
            return this.redstoneMode == RedstoneMode.ALWAYS;
        }
        switch (this.redstoneMode) {
            case HIGH:
                return this.world.isBlockPowered(this.pos);
            case LOW:
                return !this.world.isBlockPowered(this.pos);
            case PULSE:
                long period = (long) this.pulseOnTicks + this.pulseOffTicks;
                long phase = Math.floorMod(
                        this.world.getTotalWorldTime() + this.pos.toLong(), period);
                return phase < this.pulseOnTicks;
            case ALWAYS:
            default:
                return true;
        }
    }

    /**
     * Comparator strength: 0 while disabled or gated, otherwise 1-15 proportional to the
     * configured output in buckets per tick.
     */
    public int getComparatorOutput() {
        if (!this.outputEnabled || !isRedstoneAllowed()) {
            return 0;
        }
        long total = getTotalEffectiveRateMb();
        if (total <= 0L) {
            return 0;
        }
        long buckets = Math.max(1L, total / 1_000L);
        return (int) Math.min(15L, buckets);
    }

    /**
     * Replaces the whole output configuration in one step. Everything is validated before any
     * field is written, so a rejected paste can never leave a half-applied configuration.
     */
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

    /** Applies only the output switch and side mask; rates and redstone stay put. */
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

    /** Applies only per-side rates, validated against the current side mask and server limits. */
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

    /** Applies only the redstone mode and pulse timing. */
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

    /** Sets how many ticks of output are grouped into one burst; 1 keeps per-tick output. */
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

    /**
     * The tank exposed to the bucket interaction in the GUI. This is the same handler that is
     * published through the Forge fluid capability.
     */
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
        compound.setInteger(KEY_DATA_VERSION, DATA_VERSION);
        compound.setBoolean(KEY_INFINITE, this.infinite);
        compound.setBoolean(KEY_OUTPUT_ENABLED, this.outputEnabled);
        compound.setInteger(KEY_OUTPUT_SIDES, this.outputSideMask);
        compound.setInteger(KEY_REDSTONE_MODE, this.redstoneMode.ordinal());
        compound.setInteger(KEY_PULSE_ON, this.pulseOnTicks);
        compound.setInteger(KEY_PULSE_OFF, this.pulseOffTicks);
        compound.setInteger(KEY_BATCH_SIZE, this.batchSize);
        for (EnumFacing direction : EnumFacing.VALUES) {
            int index = sideIndex(direction);
            compound.setLong(SIDE_RATE_KEYS[index], this.sideRates[index]);
        }
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

    /**
     * Reads the six per-side rates, migrating 2.1.x saves that only stored one global rate.
     * This runs on the client as well (update tags reuse readFromNBT), so it must stay side-effect free.
     */
    private void loadOutputRates(NBTTagCompound compound) {
        int dataVersion = compound.hasKey(KEY_DATA_VERSION, 3)
                ? compound.getInteger(KEY_DATA_VERSION) : 1;
        int legacyMask = (compound.hasKey(KEY_OUTPUT_SIDES, 3)
                ? compound.getInteger(KEY_OUTPUT_SIDES) : ALL_SIDES) & ALL_SIDES;
        long legacyRate = readLegacyRate(compound);

        long[] loaded = new long[this.sideRates.length];
        for (EnumFacing direction : EnumFacing.VALUES) {
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

    /** Reads the 2.1.x global rate, including the older preset-index format. */
    private static long readLegacyRate(NBTTagCompound compound) {
        if (compound.hasKey(KEY_LEGACY_OUTPUT_RATE_MB, 4)) {
            return Math.max(1L, Math.min(MAX_OUTPUT_RATE_MB,
                    compound.getLong(KEY_LEGACY_OUTPUT_RATE_MB)));
        }
        if (compound.hasKey(KEY_LEGACY_OUTPUT_RATE_INDEX, 3)) {
            long[] legacyRates = {1L, 10L, 100L, 1_000L, 10_000L, 100_000L, 1_000_000L,
                    10_000_000L, 100_000_000L, 1_000_000_000L};
            int index = Math.max(0, Math.min(legacyRates.length - 1,
                    compound.getInteger(KEY_LEGACY_OUTPUT_RATE_INDEX)));
            return legacyRates[index];
        }
        return DEFAULT_OUTPUT_RATE_MB;
    }

    /** Reduces enabled sides in a fixed order when a lowered config cap is below the saved total. */
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
            this.outputStatus.notifyViewers(this, this::isViewingMenu, OutputStatus.NO_SIDES);
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
            this.outputStatus.notifyViewers(this, this::isViewingMenu, outcome);
        }
    }

    /** Clears the retry backoff so the next tick attempts output again (neighbor/config change). */
    public void wakeOutput() {
        this.outputStatus.wake();
    }

    private boolean isViewingMenu(net.minecraft.inventory.Container menu) {
        return menu instanceof InfFluidGeneratorContainer
                && ((InfFluidGeneratorContainer) menu).getTileEntity() == this;
    }

    /**
     * @return {@link OutputStatus#OK} if anything was accepted, otherwise full/rejected/none.
     */
    private int pushToNeighbors() {
        if (this.world == null || this.world.isRemote || this.tank == null) {
            return OutputStatus.NONE;
        }

        boolean infiniteSource = effectivelyInfinite();
        boolean changed = false;
        int result = OutputStatus.NONE;
        EnumFacing[] directions = EnumFacing.VALUES;
        for (int offset = 0; offset < directions.length; offset++) {
            EnumFacing direction = directions[(this.pushStartSide + offset) % directions.length];
            long rate = getEffectiveSideRateMb(direction);
            if (rate <= 0L) {
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
            boolean validTank = false;
            IFluidTankProperties[] properties = target.getTankProperties();
            if (properties != null) {
                for (int tankIndex = 0; tankIndex < properties.length; tankIndex++) {
                    IFluidTankProperties property = properties[tankIndex];
                    if (property != null && property.canFillFluidType(this.tank)) {
                        validTank = true;
                        break;
                    }
                }
            }

            long acceptedForSide = 0L;
            long remaining = infiniteSource ? rate : Math.min(rate, this.tank.amount);
            while (remaining > 0) {
                int offered = (int) Math.min((long) Integer.MAX_VALUE, remaining);
                FluidStack offer = new FluidStack(this.tank, offered);
                int accepted = Math.max(0, Math.min(offered, target.fill(offer, true)));
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
                    // Keep one unit of the type and flip the source to infinite.
                    FluidStack single = resource.copy();
                    single.amount = 1;
                    InfFluidGeneratorTileEntity.this.tank = single;
                    InfFluidGeneratorTileEntity.this.infinite = true;
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

package com.bandybus.infwhat.block.entity;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.RedstoneMode;
import com.bandybus.infwhat.config.InfinityWhatConfig;
import com.bandybus.infwhat.menu.InfFluidGeneratorMenu;
import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CauldronBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.TickableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.Arrays;

/**
 * Stores a single fluid type. When the infinite source is enabled (config), inserting any
 * allowed fluid immediately turns it into an unlimited source.
 *
 * 1.16.5 has no vanilla {@code FluidStack} and Fabric API 0.42 has no transfer API, so the
 * tank stores the {@link Fluid} plus a millibucket amount directly. This branch cannot expose
 * a pipe-compatible fluid handler; the active output was therefore reduced to a no-op and the
 * machine is filled/drained through its GUI with vanilla water/lava buckets.
 */
public class InfFluidGeneratorBlockEntity extends BlockEntity
        implements TickableBlockEntity, BlockEntityClientSerializable {

    private static final String KEY_FLUID = "Fluid";
    private static final String KEY_TANK_AMOUNT_MB = "TankAmountMb";
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
    private static final int SIDE_COUNT = Direction.values().length;
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

    private boolean infiniteModeEnabled = false;

    /** Stored fluid. When {@link #infinite} is true the amount is only a placeholder (1 mB). */
    private Fluid tankFluid = Fluids.EMPTY;
    private long tankAmountMb;
    private boolean infinite = false;
    private boolean outputEnabled = false;
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

    public InfFluidGeneratorBlockEntity() {
        super(ModBlockEntities.INF_FLUID_GENERATOR);
        Arrays.fill(this.sideRates, DEFAULT_OUTPUT_RATE_MB);
        Arrays.fill(this.lastNonZeroRates, DEFAULT_OUTPUT_RATE_MB);
    }

    private static String[] createSideRateKeys() {
        Direction[] directions = Direction.values();
        String[] keys = new String[directions.length];
        for (Direction direction : directions) {
            String name = direction.getName();
            keys[direction.get3DDataValue()] = "SideRate"
                    + Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }
        return keys;
    }

    private static int sideIndex(Direction direction) {
        return direction.get3DDataValue();
    }

    private static boolean isFluidAllowed(Fluid fluid) {
        return InfinityWhatConfig.isFluidAllowed(Registry.FLUID.getKey(fluid));
    }

    // ------------------------------------------------------------------
    // Tank access (GUI bucket interaction)
    // ------------------------------------------------------------------

    public Fluid getStoredFluid() {
        return this.tankFluid;
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
        return effectivelyInfinite() ? Long.MAX_VALUE : this.tankAmountMb;
    }

    /** Inserts up to {@code amountMb}; returns the accepted amount. */
    public long insertFluid(Fluid fluid, long amountMb) {
        if (fluid == null || fluid == Fluids.EMPTY || amountMb <= 0L || !isFluidAllowed(fluid)) {
            return 0L;
        }
        // fluid_type_limit = 1: only one fluid type may be stored at a time.
        if (this.tankFluid != Fluids.EMPTY && !this.tankFluid.isSame(fluid)) {
            return 0L;
        }
        if (InfinityWhatConfig.infiniteEnabled()) {
            if (!this.infinite || !this.tankFluid.isSame(fluid) || this.tankAmountMb <= 0L) {
                this.tankFluid = fluid;
                this.tankAmountMb = 1L;
                this.infinite = true;
                markUpdated();
            }
            return amountMb;
        }
        long capacity = InfinityWhatConfig.finiteCapacity();
        long accepted = Math.min(amountMb, capacity - this.tankAmountMb);
        if (accepted <= 0L) {
            return 0L;
        }
        this.tankFluid = fluid;
        this.tankAmountMb += accepted;
        this.infinite = false;
        markUpdated();
        return accepted;
    }

    /** Drains up to {@code amountMb}; returns the drained amount. */
    public long extractFluid(long amountMb) {
        if (this.tankFluid == Fluids.EMPTY || amountMb <= 0L) {
            return 0L;
        }
        if (effectivelyInfinite()) {
            return amountMb;
        }
        long drained = Math.min(amountMb, this.tankAmountMb);
        if (drained <= 0L) {
            return 0L;
        }
        this.tankAmountMb -= drained;
        if (this.tankAmountMb <= 0L) {
            this.tankFluid = Fluids.EMPTY;
            this.tankAmountMb = 0L;
            this.infinite = false;
        }
        markUpdated();
        return drained;
    }

    public void clearFluid() {
        if (this.tankFluid == Fluids.EMPTY && !this.infinite) {
            return;
        }
        this.tankFluid = Fluids.EMPTY;
        this.tankAmountMb = 0L;
        this.infinite = false;
        markUpdated();
    }

    public float getFillRatio() {
        if (effectivelyInfinite()) {
            return 1.0F;
        }
        long capacity = InfinityWhatConfig.finiteCapacity();
        if (capacity <= 0 || this.tankAmountMb <= 0L) {
            return 0.0F;
        }
        return Math.min(1.0F, (float) this.tankAmountMb / (float) capacity);
    }

    // ------------------------------------------------------------------
    // Output configuration
    // ------------------------------------------------------------------

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
    public long getSideRateMb(Direction direction) {
        return this.sideRates[sideIndex(direction)];
    }

    /** Rate actually used for one side; a disabled side always reports 0. */
    public long getEffectiveSideRateMb(Direction direction) {
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
    public boolean setSideRateMb(Direction direction, long millibuckets) {
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
        if (this.level == null) {
            return this.redstoneMode == RedstoneMode.ALWAYS;
        }
        switch (this.redstoneMode) {
            case HIGH:
                return this.level.hasNeighborSignal(this.worldPosition);
            case LOW:
                return !this.level.hasNeighborSignal(this.worldPosition);
            case PULSE:
                long period = (long) this.pulseOnTicks + this.pulseOffTicks;
                long phase = Math.floorMod(
                        this.level.getGameTime() + this.worldPosition.asLong(), period);
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

    // ------------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------------

    @Override
    public CompoundTag save(CompoundTag tag) {
        super.save(tag);
        if (this.tankFluid != Fluids.EMPTY && this.tankAmountMb > 0L) {
            ResourceLocation id = Registry.FLUID.getKey(this.tankFluid);
            if (id != null) {
                tag.putString(KEY_FLUID, id.toString());
            }
            tag.putLong(KEY_TANK_AMOUNT_MB, this.tankAmountMb);
        }
        tag.putInt(KEY_DATA_VERSION, DATA_VERSION);
        tag.putBoolean(KEY_INFINITE, this.infinite);
        tag.putBoolean(KEY_OUTPUT_ENABLED, this.outputEnabled);
        tag.putInt(KEY_OUTPUT_SIDES, this.outputSideMask);
        tag.putInt(KEY_REDSTONE_MODE, this.redstoneMode.ordinal());
        tag.putInt(KEY_PULSE_ON, this.pulseOnTicks);
        tag.putInt(KEY_PULSE_OFF, this.pulseOffTicks);
        tag.putInt(KEY_BATCH_SIZE, this.batchSize);
        for (Direction direction : Direction.values()) {
            int index = sideIndex(direction);
            tag.putLong(SIDE_RATE_KEYS[index], this.sideRates[index]);
        }
        return tag;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.tankFluid = Fluids.EMPTY;
        this.tankAmountMb = 0L;
        this.infinite = false;
        if (tag.contains(KEY_FLUID)) {
            ResourceLocation id = ResourceLocation.tryParse(tag.getString(KEY_FLUID));
            Fluid loaded = id == null ? Fluids.EMPTY : Registry.FLUID.get(id);
            long loadedAmount = tag.getLong(KEY_TANK_AMOUNT_MB);
            if (loaded != Fluids.EMPTY && loadedAmount > 0L) {
                this.tankFluid = loaded;
                this.tankAmountMb = loadedAmount;
            }
        }
        this.infinite = tag.getBoolean(KEY_INFINITE) && this.tankFluid != Fluids.EMPTY;
        this.outputEnabled = tag.getBoolean(KEY_OUTPUT_ENABLED);
        this.redstoneMode = RedstoneMode.byId(tag.getInt(KEY_REDSTONE_MODE));
        this.pulseOnTicks = clampPulseTicks(tag, KEY_PULSE_ON);
        this.pulseOffTicks = clampPulseTicks(tag, KEY_PULSE_OFF);
        this.batchSize = Math.max(MIN_BATCH_SIZE,
                Math.min(MAX_BATCH_SIZE, tag.getInt(KEY_BATCH_SIZE)));
        this.batchCounter = 0;
        loadOutputRates(tag);
    }

    private static int clampPulseTicks(CompoundTag tag, String key) {
        if (!tag.contains(key)) {
            return DEFAULT_PULSE_TICKS;
        }
        return Math.max(MIN_PULSE_TICKS, Math.min(MAX_PULSE_TICKS, tag.getInt(key)));
    }

    /**
     * Reads the six per-side rates, migrating 2.1.x saves that only stored one global rate.
     * This runs on the client as well (update tags reuse load), so it must stay side-effect free.
     */
    private void loadOutputRates(CompoundTag tag) {
        int dataVersion = tag.contains(KEY_DATA_VERSION)
                ? tag.getInt(KEY_DATA_VERSION) : 1;
        int legacyMask = (tag.contains(KEY_OUTPUT_SIDES)
                ? tag.getInt(KEY_OUTPUT_SIDES) : ALL_SIDES) & ALL_SIDES;
        long legacyRate = readLegacyRate(tag);

        long[] loaded = new long[this.sideRates.length];
        for (Direction direction : Direction.values()) {
            int index = sideIndex(direction);
            loaded[index] = dataVersion >= DATA_VERSION
                    ? (tag.contains(SIDE_RATE_KEYS[index])
                            ? tag.getLong(SIDE_RATE_KEYS[index]) : legacyRate)
                    : legacyRate;
        }

        this.outputSideMask = (dataVersion >= DATA_VERSION
                ? (tag.contains(KEY_OUTPUT_SIDES)
                        ? tag.getInt(KEY_OUTPUT_SIDES) : ALL_SIDES)
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
    private static long readLegacyRate(CompoundTag tag) {
        if (tag.contains(KEY_LEGACY_OUTPUT_RATE_MB)) {
            return Math.max(1L, Math.min(MAX_OUTPUT_RATE_MB,
                    tag.getLong(KEY_LEGACY_OUTPUT_RATE_MB)));
        }
        if (tag.contains(KEY_LEGACY_OUTPUT_RATE_INDEX)) {
            int[] legacyRates = {1, 10, 100, 1_000, 10_000, 100_000, 1_000_000,
                    10_000_000, 100_000_000, 1_000_000_000};
            int index = Math.max(0, Math.min(legacyRates.length - 1,
                    tag.getInt(KEY_LEGACY_OUTPUT_RATE_INDEX)));
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
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        save(tag);
        return tag;
    }

    // 1.14–1.16 的客户端只对原版方块实体处理 ClientboundBlockEntityDataPacket，自定义方块
    // 实体必须走 Fabric 的客户端可序列化接口，否则按钮/储罐状态永远收不到更新。
    @Override
    public CompoundTag toClientTag(CompoundTag tag) {
        return save(tag);
    }

    @Override
    public void fromClientTag(CompoundTag tag) {
        load(tag);
    }

    private void markUpdated() {
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(),
                    3 /* UPDATE_CLIENTS */);
        }
    }

    // ------------------------------------------------------------------
    // Server tick
    // ------------------------------------------------------------------

    @Override
    public void tick() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        this.outputStatus.tick();
        boolean active = this.outputEnabled && this.tankFluid != Fluids.EMPTY
                && this.isRedstoneAllowed();
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

    private boolean isViewingMenu(AbstractContainerMenu menu) {
        return menu instanceof InfFluidGeneratorMenu
                && ((InfFluidGeneratorMenu) menu).getBlockEntity() == this;
    }

    /**
     * 1.14.4/1.15.x 的 API 0.28.5 没带 fabric-transfer-api，无法推给其他 mod 的储罐；
     * 唯一的原版目标是灌满相邻炼药锅（每次一级，仅水）。
     */
    private int pushToNeighbors() {
        this.pushStartSide = (this.pushStartSide + 1) % Direction.values().length;
        if (this.level == null || this.level.isClientSide
                || !this.tankFluid.isSame(Fluids.WATER)) {
            return OutputStatus.NONE;
        }
        if (!effectivelyInfinite() && this.tankAmountMb < 1_000L) {
            return OutputStatus.NONE;
        }
        Direction[] directions = Direction.values();
        int result = OutputStatus.NONE;
        for (int i = 0; i < directions.length; i++) {
            Direction direction = directions[(this.pushStartSide + i) % directions.length];
            BlockPos target = this.worldPosition.relative(direction);
            BlockState state = this.level.getBlockState(target);
            if (!(state.getBlock() instanceof CauldronBlock)) {
                continue;
            }
            int level = state.getValue(CauldronBlock.LEVEL);
            if (level >= 3) {
                result = OutputStatus.merge(result, OutputStatus.FULL);
                continue;
            }
            this.level.setBlock(target, state.setValue(CauldronBlock.LEVEL, level + 1), 3);
            this.extractFluid(1_000L);
            return OutputStatus.OK;
        }
        return result;
    }
}

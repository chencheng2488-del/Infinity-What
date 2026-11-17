package com.bandybus.infwhat.block.entity;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.RedstoneMode;
import com.bandybus.infwhat.config.InfinityWhatConfig;
import com.bandybus.infwhat.menu.InfFluidGeneratorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;

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

    /** Stored fluid. When {@link #infinite} is true the amount is only a placeholder (1). */
    private FluidStack tank = FluidStack.EMPTY;
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

    private final IFluidHandler fluidHandler = new InfiniteFluidTank();
    private LazyOptional<IFluidHandler> fluidOptional = LazyOptional.of(() -> this.fluidHandler);

    public InfFluidGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INF_FLUID_GENERATOR.get(), pos, state);
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

    // ------------------------------------------------------------------
    // Capability
    // ------------------------------------------------------------------

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.FLUID_HANDLER) {
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
        return switch (this.redstoneMode) {
            case ALWAYS -> true;
            case HIGH -> this.level.hasNeighborSignal(this.worldPosition);
            case LOW -> !this.level.hasNeighborSignal(this.worldPosition);
            case PULSE -> {
                long period = (long) this.pulseOnTicks + this.pulseOffTicks;
                long phase = Math.floorMod(
                        this.level.getGameTime() + this.worldPosition.asLong(), period);
                yield phase < this.pulseOnTicks;
            }
        };
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
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!this.tank.isEmpty()) {
            output.store(KEY_TANK, FluidStack.CODEC, this.tank);
        }
        output.putInt(KEY_DATA_VERSION, DATA_VERSION);
        output.putBoolean(KEY_INFINITE, this.infinite);
        output.putBoolean(KEY_OUTPUT_ENABLED, this.outputEnabled);
        output.putInt(KEY_OUTPUT_SIDES, this.outputSideMask);
        output.putInt(KEY_REDSTONE_MODE, this.redstoneMode.ordinal());
        output.putInt(KEY_PULSE_ON, this.pulseOnTicks);
        output.putInt(KEY_PULSE_OFF, this.pulseOffTicks);
        output.putInt(KEY_BATCH_SIZE, this.batchSize);
        for (Direction direction : Direction.values()) {
            int index = sideIndex(direction);
            output.putLong(SIDE_RATE_KEYS[index], this.sideRates[index]);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.tank = input.read(KEY_TANK, FluidStack.CODEC).orElse(FluidStack.EMPTY);
        this.infinite = input.getBooleanOr(KEY_INFINITE, false) && !this.tank.isEmpty();
        this.outputEnabled = input.getBooleanOr(KEY_OUTPUT_ENABLED, false);
        this.redstoneMode = RedstoneMode.byId(input.getIntOr(KEY_REDSTONE_MODE, 0));
        this.pulseOnTicks = clampPulseTicks(input, KEY_PULSE_ON);
        this.pulseOffTicks = clampPulseTicks(input, KEY_PULSE_OFF);
        this.batchSize = Math.max(MIN_BATCH_SIZE,
                Math.min(MAX_BATCH_SIZE, input.getIntOr(KEY_BATCH_SIZE, DEFAULT_BATCH_SIZE)));
        this.batchCounter = 0;
        loadOutputRates(input);
    }

    private static int clampPulseTicks(ValueInput input, String key) {
        return Math.max(MIN_PULSE_TICKS,
                Math.min(MAX_PULSE_TICKS, input.getIntOr(key, DEFAULT_PULSE_TICKS)));
    }

    /**
     * Reads the six per-side rates, migrating 2.1.x saves that only stored one global rate.
     * This runs on the client as well (update tags reuse load), so it must stay side-effect free.
     */
    private void loadOutputRates(ValueInput input) {
        int dataVersion = input.getIntOr(KEY_DATA_VERSION, 1);
        int legacyMask = input.getIntOr(KEY_OUTPUT_SIDES, ALL_SIDES) & ALL_SIDES;
        long legacyRate = readLegacyRate(input);

        long[] loaded = new long[this.sideRates.length];
        for (Direction direction : Direction.values()) {
            int index = sideIndex(direction);
            loaded[index] = dataVersion >= DATA_VERSION
                    ? input.getLongOr(SIDE_RATE_KEYS[index], legacyRate)
                    : legacyRate;
        }

        this.outputSideMask = (dataVersion >= DATA_VERSION
                ? input.getIntOr(KEY_OUTPUT_SIDES, ALL_SIDES) : legacyMask) & ALL_SIDES;
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
    private static long readLegacyRate(ValueInput input) {
        long legacyMb = input.getLongOr(KEY_LEGACY_OUTPUT_RATE_MB, -1L);
        if (legacyMb >= 0L) {
            return Math.max(1L, Math.min(MAX_OUTPUT_RATE_MB, legacyMb));
        }
        int legacyIndex = input.getIntOr(KEY_LEGACY_OUTPUT_RATE_INDEX, -1);
        if (legacyIndex >= 0) {
            int[] legacyRates = {1, 10, 100, 1_000, 10_000, 100_000, 1_000_000,
                    10_000_000, 100_000_000, 1_000_000_000};
            return legacyRates[Math.min(legacyRates.length - 1, legacyIndex)];
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
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
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
        return !InfinityWhatConfig.isFluidAllowed(ForgeRegistries.FLUIDS.getKey(stack.getFluid()));
    }

    // ------------------------------------------------------------------
    // Server tick: active output into selected adjacent fluid handlers
    // ------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  InfFluidGeneratorBlockEntity blockEntity) {
        if (level.isClientSide) {
            return;
        }
        blockEntity.outputStatus.tick();
        boolean active = blockEntity.outputEnabled && !blockEntity.tank.isEmpty()
                && blockEntity.isRedstoneAllowed();
        if (!active) {
            blockEntity.wasOutputActive = false;
            return;
        }
        if (!blockEntity.wasOutputActive) {
            blockEntity.wasOutputActive = true;
            blockEntity.outputStatus.wake();
        }
        if (blockEntity.getTotalEffectiveRateMb() <= 0L) {
            blockEntity.outputStatus.notifyViewers(blockEntity, blockEntity::isViewingMenu,
                    OutputStatus.NO_SIDES);
            return;
        }
        if (!blockEntity.outputStatus.canAttempt()) {
            return;
        }
        if (++blockEntity.batchCounter >= blockEntity.batchSize) {
            int batch = blockEntity.batchSize;
            blockEntity.batchCounter = 0;
            int outcome = OutputStatus.NONE;
            for (int i = 0; i < batch; i++) {
                outcome = OutputStatus.merge(outcome, blockEntity.pushToNeighbors());
            }
            blockEntity.outputStatus.record(outcome);
            blockEntity.outputStatus.notifyViewers(blockEntity, blockEntity::isViewingMenu, outcome);
        }
    }

    /** Clears the retry backoff so the next tick attempts output again (neighbor/config change). */
    public void wakeOutput() {
        this.outputStatus.wake();
    }

    private boolean isViewingMenu(AbstractContainerMenu menu) {
        return menu instanceof InfFluidGeneratorMenu generatorMenu
                && generatorMenu.getBlockEntity() == this;
    }

    /**
     * @return {@link OutputStatus#OK} if anything was accepted, otherwise full/rejected/none.
     */
    private int pushToNeighbors() {
        if (this.level == null || this.level.isClientSide || this.tank.isEmpty()) {
            return OutputStatus.NONE;
        }

        boolean infiniteSource = effectivelyInfinite();
        boolean changed = false;
        int result = OutputStatus.NONE;
        Direction[] directions = Direction.values();
        for (int offset = 0; offset < directions.length; offset++) {
            Direction direction = directions[(this.pushStartSide + offset) % directions.length];
            long rate = getEffectiveSideRateMb(direction);
            if (rate <= 0L) {
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
                    .getCapability(ForgeCapabilities.FLUID_HANDLER, direction.getOpposite())
                    .orElse(null);
            if (target == null) {
                continue;
            }
            boolean validTank = false;
            for (int tank = 0; tank < target.getTanks(); tank++) {
                if (target.isFluidValid(tank, this.tank)) {
                    validTank = true;
                    break;
                }
            }

            long acceptedForSide = 0L;
            long remaining = infiniteSource ? rate : Math.min(rate, this.tank.getAmount());
            while (remaining > 0) {
                int offered = (int) Math.min((long) Integer.MAX_VALUE, remaining);
                FluidStack offer = new FluidStack(this.tank, offered);
                int accepted = Math.max(0, Math.min(offered,
                        target.fill(offer, IFluidHandler.FluidAction.EXECUTE)));
                if (accepted <= 0) {
                    break;
                }
                acceptedForSide += accepted;
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
            if (acceptedForSide > 0) {
                result = OutputStatus.OK;
            } else if (validTank) {
                if (result != OutputStatus.OK) {
                    result = OutputStatus.FULL;
                }
            } else if (result == OutputStatus.NONE) {
                result = OutputStatus.REJECTED;
            }
            if (this.tank.isEmpty() && !infiniteSource) break;
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

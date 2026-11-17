package com.bandybus.infwhat.block.entity;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.RedstoneMode;
import com.bandybus.infwhat.config.InfinityWhatConfig;
import com.bandybus.infwhat.menu.InfFluidGeneratorMenu;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
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
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.Arrays;

/**
 * Stores a single fluid type. When the infinite source is enabled (config), inserting any
 * allowed fluid immediately turns it into an unlimited source. Output goes through the
 * Fabric transfer API {@link Storage} view.
 *
 * <p>Fabric stores fluid amounts in droplets, so 1 mB = 81 droplets. The GUI, saved rates and
 * the 2.2 rate model still speak millibuckets; conversion happens at this boundary.
 */
public class InfFluidGeneratorBlockEntity extends BlockEntity implements ExtendedMenuProvider<BlockPos> {

    public static final long DROPLETS_PER_MB = FluidConstants.BUCKET / 1_000L;

    private static final String KEY_FLUID = "Fluid";
    private static final String KEY_AMOUNT = "Amount";
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

    /** Stored fluid. When {@link #infinite} is true the amount is only a placeholder (1 mB). */
    private FluidVariant tank = FluidVariant.blank();
    private long tankAmount;
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

    private final InfiniteFluidTank fluidStorage = new InfiniteFluidTank();

    public InfFluidGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INF_FLUID_GENERATOR, pos, state);
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
    // Fluid storage
    // ------------------------------------------------------------------

    public Storage<FluidVariant> getFluidStorage() {
        return this.fluidStorage;
    }

    private static long finiteCapacityDroplets() {
        return (long) InfinityWhatConfig.finiteCapacity() * DROPLETS_PER_MB;
    }

    private static boolean isFluidAllowed(FluidVariant variant) {
        return InfinityWhatConfig.isFluidAllowed(BuiltInRegistries.FLUID.getKey(variant.getFluid()));
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
     * The persisted {@code infinite} flag is only honoured while the balance switch is enabled.
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

    /** Applies one rate to all six sides. */
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
     * signal and runs on its own work/stop cycle, offset by the block position.
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

    /** Comparator strength: 0 while disabled or gated, otherwise 1-15 proportional to rate. */
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

    /** Legacy single-rate getter used by the old 2.1 screen; returns -1 when rates are mixed. */
    public long getOutputAmountPerTick() {
        return getUniformSideRateMb();
    }

    /** Legacy single-rate setter used by the old 2.1 screen; applies to all six sides. */
    public void setOutputAmountPerTick(long millibuckets) {
        setUniformSideRateMb(millibuckets);
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
        return Component.translatable("container." + InfinityWhat.MOD_ID + ".inf_fluid_generator");
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
    // Server tick: active output into selected adjacent fluid storages
    // ------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  InfFluidGeneratorBlockEntity blockEntity) {
        blockEntity.tick();
    }

    private void tick() {
        if (this.level == null || this.level.isClientSide()) {
            return;
        }
        this.outputStatus.tick();

        boolean active = this.outputEnabled && !this.tank.isBlank() && isRedstoneAllowed();
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
        return menu instanceof InfFluidGeneratorMenu generatorMenu
                && generatorMenu.getBlockEntity() == this;
    }

    /**
     * @return {@link OutputStatus#OK} if anything was accepted, otherwise full/rejected/none.
     */
    private int pushToNeighbors() {
        if (this.level == null || this.level.isClientSide() || this.tank.isBlank()) {
            return OutputStatus.NONE;
        }

        boolean infiniteSource = effectivelyInfinite();
        FluidVariant resource = this.tank;
        int result = OutputStatus.NONE;
        Direction[] directions = Direction.values();
        for (int offset = 0; offset < directions.length; offset++) {
            Direction direction = directions[(this.pushStartSide + offset) % directions.length];
            long rateMb = getEffectiveSideRateMb(direction);
            if (rateMb <= 0L) {
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
            boolean validTarget = target.supportsInsertion();
            long rateDroplets = rateMb * DROPLETS_PER_MB;
            long remaining = infiniteSource ? rateDroplets
                    : Math.min(rateDroplets, this.tankAmount);
            if (remaining <= 0L) {
                break;
            }
            long accepted;
            try (Transaction transaction = Transaction.openOuter()) {
                accepted = target.insert(resource, remaining, transaction);
                if (accepted > 0L) {
                    this.fluidStorage.extract(resource, accepted, transaction);
                    transaction.commit();
                }
            }
            if (accepted > 0L) {
                result = OutputStatus.OK;
            } else if (validTarget) {
                if (result != OutputStatus.OK) {
                    result = OutputStatus.FULL;
                }
            } else if (result == OutputStatus.NONE) {
                result = OutputStatus.REJECTED;
            }
            if (this.tank.isBlank() && !infiniteSource) {
                break;
            }
        }
        this.pushStartSide = (this.pushStartSide + 1) % directions.length;
        return result;
    }

    // ------------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!this.tank.isBlank()) {
            Identifier fluidId = BuiltInRegistries.FLUID.getKey(this.tank.getFluid());
            output.putString(KEY_FLUID, fluidId == null ? "" : fluidId.toString());
            output.putLong(KEY_AMOUNT, this.tankAmount);
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
        this.tank = FluidVariant.blank();
        this.tankAmount = 0;
        this.infinite = false;
        String storedId = input.getStringOr(KEY_FLUID, "");
        if (!storedId.isEmpty()) {
            Identifier fluidId = Identifier.tryParse(storedId);
            if (fluidId != null) {
                Fluid fluid = BuiltInRegistries.FLUID.get(fluidId)
                        .map(net.minecraft.core.Holder.Reference::value)
                        .orElse(null);
                if (fluid != null) {
                    FluidVariant loaded = FluidVariant.of(fluid);
                    if (!loaded.isBlank()) {
                        this.tank = loaded;
                        this.tankAmount = Math.max(0L, input.getLongOr(KEY_AMOUNT, 0L));
                    }
                }
            }
        }
        this.infinite = input.getBooleanOr(KEY_INFINITE, false) && !this.tank.isBlank();
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
        return input.getInt(key)
                .map(value -> Math.max(MIN_PULSE_TICKS, Math.min(MAX_PULSE_TICKS, value)))
                .orElse(DEFAULT_PULSE_TICKS);
    }

    /**
     * Reads the six per-side rates, migrating 2.1.x saves that only stored one global rate.
     * This runs on the client as well (update tags reuse load), so it must stay side-effect free.
     */
    private void loadOutputRates(ValueInput input) {
        int dataVersion = input.getIntOr(KEY_DATA_VERSION, 1);
        int legacyMask = input.getIntOr(KEY_OUTPUT_SIDES, ALL_SIDES) & ALL_SIDES;
        long legacyRate = legacyRateMb(input);

        long[] loaded = new long[this.sideRates.length];
        for (Direction direction : Direction.values()) {
            int index = sideIndex(direction);
            if (dataVersion >= DATA_VERSION) {
                loaded[index] = input.getLong(SIDE_RATE_KEYS[index]).orElse(legacyRate);
            } else {
                loaded[index] = legacyRate;
            }
        }

        this.outputSideMask = (dataVersion >= DATA_VERSION
                ? input.getIntOr(KEY_OUTPUT_SIDES, ALL_SIDES)
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

    private static long legacyRateMb(ValueInput input) {
        if (input.getLong(KEY_LEGACY_OUTPUT_RATE_MB).isPresent()) {
            return Math.max(1L, Math.min(MAX_OUTPUT_RATE_MB,
                    input.getLongOr(KEY_LEGACY_OUTPUT_RATE_MB, DEFAULT_OUTPUT_RATE_MB)));
        }
        if (input.getInt(KEY_LEGACY_OUTPUT_RATE_INDEX).isPresent()) {
            // Migrate worlds saved by the previous preset-based release.
            long[] legacyRates = {1, 10, 100, 1_000, 10_000, 100_000, 1_000_000,
                    10_000_000, 100_000_000, 1_000_000_000};
            int index = Math.max(0, Math.min(legacyRates.length - 1,
                    input.getIntOr(KEY_LEGACY_OUTPUT_RATE_INDEX, 9)));
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
        for (int i = rates.length - 1; i >= 0 && total > maxTotal; i--) {
            if ((this.outputSideMask & (1 << i)) == 0) {
                continue;
            }
            long excess = total - maxTotal;
            long reduction = Math.min(rates[i], excess);
            rates[i] -= reduction;
            total -= reduction;
        }
    }

    // ------------------------------------------------------------------
    // Client sync
    // ------------------------------------------------------------------

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void markUpdated() {
        setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(),
                    Block.UPDATE_CLIENTS);
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
                // The amount jumps to the max immediately, so accept it all.
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

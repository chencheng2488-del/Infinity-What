package com.bandybus.infwhat.block.entity;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.RedstoneMode;
import com.bandybus.infwhat.config.InfinityWhatConfig;
import com.bandybus.infwhat.menu.InfItemGeneratorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;

import javax.annotation.Nullable;

/**
 * Infinity Item Generator. Its GUI-controlled infinite-mode switch makes the stored item an
 * unlimited source. Active output is independently configurable and can be limited to selected
 * sides; the standard Forge {@link IItemHandler} capability remains available to pipes/import
 * buses on every side.
 *
 * ENGINE LIMIT: {@link ItemStack} stores its count as an {@code int}, so a true
 * {@code Long.MAX_VALUE} count cannot be stored. The internal slot always holds exactly one
 * template item; while infinite the exposed capability reports {@link Integer#MAX_VALUE} (the
 * largest amount the engine can express) so AE2 storage buses and pipes see an effectively
 * unbounded amount, and extraction never depletes. The GUI shows the item name/id instead of an
 * infinity badge to avoid clashing with the vanilla stack-count overlay.
 */
public class InfItemGeneratorBlockEntity extends BlockEntity implements MenuProvider {

    private static final String KEY_ITEM = "Item";
    private static final String KEY_INFINITE_MODE = "InfiniteMode";
    private static final String KEY_OUTPUT_ENABLED = "OutputEnabled";
    private static final String KEY_OUTPUT_SIDES = "OutputSides";
    private static final String KEY_REDSTONE_MODE = "RedstoneMode";
    private static final String KEY_PULSE_ON = "PulseOnTicks";
    private static final String KEY_PULSE_OFF = "PulseOffTicks";
    private static final String KEY_BATCH_SIZE = "BatchSize";
    /** Legacy single-rate key, only read for 2.1.x saves. */
    private static final String KEY_LEGACY_OUTPUT_RATE = "OutputRate";
    private static final String KEY_DATA_VERSION = "InfDataVersion";
    private static final int DATA_VERSION = 2;
    private static final int SIDE_COUNT = Direction.values().length;
    private static final int ALL_SIDES = (1 << SIDE_COUNT) - 1;
    private static final String[] SIDE_RATE_KEYS = createSideRateKeys();

    public static final int DEFAULT_OUTPUT_RATE = 8;
    public static final int DEFAULT_PULSE_TICKS = 20;
    public static final int MIN_PULSE_TICKS = 1;
    public static final int MAX_PULSE_TICKS = 12000;
    public static final int DEFAULT_BATCH_SIZE = 1;
    public static final int MIN_BATCH_SIZE = 1;
    public static final int MAX_BATCH_SIZE = 64;

    private boolean infiniteModeEnabled = false;
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
    /** Per-side output rate in items/tick; 0 means the side produces nothing. */
    private final int[] sideRates = new int[SIDE_COUNT];
    /** Last non-zero rate per side, so toggling a side back on restores a useful value. */
    private final int[] lastNonZeroRates = new int[SIDE_COUNT];

    private final GeneratorItemHandler itemHandler = new GeneratorItemHandler();
    /**
     * Capability view of the single slot. It reports the huge infinite count and lets pipes/AE2
     * pull from the block, while {@link GeneratorItemHandler} keeps the GUI facing a plain
     * one-item slot.
     */
    private final IItemHandler capabilityHandler = new CapabilityItemHandler();
    private LazyOptional<IItemHandler> itemOptional = LazyOptional.of(() -> this.capabilityHandler);

    public InfItemGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INF_ITEM_GENERATOR.get(), pos, state);
        Arrays.fill(this.sideRates, DEFAULT_OUTPUT_RATE);
        Arrays.fill(this.lastNonZeroRates, DEFAULT_OUTPUT_RATE);
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
        if (capability == ForgeCapabilities.ITEM_HANDLER) {
            return this.itemOptional.cast();
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        this.itemOptional.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        this.itemOptional = LazyOptional.of(() -> this.capabilityHandler);
    }

    // ------------------------------------------------------------------
    // Data exposed to the GUI / other code
    // ------------------------------------------------------------------

    /** The stored item, always a single unit internally. */
    public ItemStack getStoredItem() {
        return this.itemHandler.getInternalStack();
    }

    public boolean isInfinite() {
        return effectivelyInfinite();
    }

    public boolean isInfiniteModeEnabled() {
        return this.infiniteModeEnabled;
    }

    public void setInfiniteModeEnabled(boolean enabled) {
        if (this.infiniteModeEnabled == enabled) {
            return;
        }
        this.infiniteModeEnabled = enabled;
        markUpdated();
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
        int restoredRate = this.sideRates[index];
        if (enabling && restoredRate <= 0) {
            restoredRate = this.lastNonZeroRates[index] > 0
                    ? this.lastNonZeroRates[index] : DEFAULT_OUTPUT_RATE;
        }
        if (enabling
                && getTotalEffectiveRate() + restoredRate > InfinityWhatConfig.maxItemTotalRate()) {
            return;
        }
        this.outputSideMask ^= 1 << index;
        if (enabling && this.sideRates[index] <= 0) {
            this.sideRates[index] = restoredRate;
        }
        markUpdated();
    }

    /** Rate configured for one side, in items/tick. 0 means the side produces nothing. */
    public int getSideRate(Direction direction) {
        return this.sideRates[sideIndex(direction)];
    }

    /** Rate actually used for one side; a disabled side always reports 0. */
    public int getEffectiveSideRate(Direction direction) {
        return isOutputSideEnabled(direction) ? this.sideRates[sideIndex(direction)] : 0;
    }

    /** Sum of the effective per-side rates. */
    public long getTotalEffectiveRate() {
        long total = 0L;
        for (int i = 0; i < this.sideRates.length; i++) {
            if ((this.outputSideMask & (1 << i)) != 0) {
                total += this.sideRates[i];
            }
        }
        return total;
    }

    /** The common rate when all six sides share one value, otherwise -1. */
    public int getUniformSideRate() {
        int first = this.sideRates[0];
        for (int i = 1; i < this.sideRates.length; i++) {
            if (this.sideRates[i] != first) {
                return -1;
            }
        }
        return first;
    }

    public boolean hasMixedSideRates() {
        return getUniformSideRate() < 0;
    }

    /**
     * Sets one side's rate. Returns false without changing anything when the value is outside
     * the server limits or would push the total above the configured cap.
     */
    public boolean setSideRate(Direction direction, int amount) {
        if (amount < 0 || amount > InfinityWhatConfig.maxItemRatePerSide()) {
            return false;
        }
        int index = sideIndex(direction);
        if (this.sideRates[index] == amount) {
            return true;
        }
        long newTotal = getTotalEffectiveRate();
        if (isOutputSideEnabled(direction)) {
            newTotal = newTotal - getEffectiveSideRate(direction) + amount;
        }
        if (newTotal > InfinityWhatConfig.maxItemTotalRate()) {
            return false;
        }
        this.sideRates[index] = amount;
        if (amount > 0) {
            this.lastNonZeroRates[index] = amount;
        }
        markUpdated();
        return true;
    }

    /** Applies one rate to all six sides, as the 2.2 phase-1 GUI does. */
    public boolean setUniformSideRate(int amount) {
        int[] rates = new int[this.sideRates.length];
        Arrays.fill(rates, amount);
        return applyFullConfiguration(this.outputEnabled, this.outputSideMask, rates,
                this.infiniteModeEnabled);
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
     * configured item output per tick.
     */
    public int getComparatorOutput() {
        if (!this.outputEnabled || !isRedstoneAllowed()) {
            return 0;
        }
        long total = getTotalEffectiveRate();
        if (total <= 0L) {
            return 0;
        }
        return (int) Math.min(15L, total);
    }

    /**
     * Replaces the whole output configuration in one step. Everything is validated before any
     * field is written, so a rejected paste can never leave a half-applied configuration.
     */
    public boolean applyFullConfiguration(boolean enabled, int sideMask, int[] rates,
                                          boolean infiniteMode) {
        if (rates == null || rates.length != this.sideRates.length) {
            return false;
        }
        int mask = sideMask & ALL_SIDES;
        int maxPerSide = (int) Math.min(Integer.MAX_VALUE, InfinityWhatConfig.maxItemRatePerSide());
        int[] validated = new int[rates.length];
        long total = 0L;
        for (int i = 0; i < rates.length; i++) {
            if (rates[i] < 0 || rates[i] > maxPerSide) {
                return false;
            }
            validated[i] = rates[i];
            if ((mask & (1 << i)) != 0) {
                total += rates[i];
            }
        }
        if (total > InfinityWhatConfig.maxItemTotalRate()) {
            return false;
        }
        this.outputEnabled = enabled;
        this.outputSideMask = mask;
        this.infiniteModeEnabled = infiniteMode;
        for (int i = 0; i < validated.length; i++) {
            this.sideRates[i] = validated[i];
            if (validated[i] > 0) {
                this.lastNonZeroRates[i] = validated[i];
            }
        }
        markUpdated();
        return true;
    }

    /** Legacy global-rate entry point kept for old cards; all six sides get the same rate. */
    public boolean applyOutputConfiguration(boolean enabled, int sideMask, int amountPerTick,
                                            boolean infiniteMode) {
        int[] rates = new int[this.sideRates.length];
        Arrays.fill(rates, Math.max(1, amountPerTick));
        return applyFullConfiguration(enabled, sideMask, rates, infiniteMode);
    }

    /** Applies only the output switch and side mask; rates, infinite mode and redstone stay put. */
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
    public boolean applyRates(int[] rates) {
        if (rates == null || rates.length != this.sideRates.length) {
            return false;
        }
        int maxPerSide = (int) Math.min(Integer.MAX_VALUE, InfinityWhatConfig.maxItemRatePerSide());
        int[] validated = new int[rates.length];
        long total = 0L;
        for (int i = 0; i < rates.length; i++) {
            if (rates[i] < 0 || rates[i] > maxPerSide) {
                return false;
            }
            validated[i] = rates[i];
            if ((this.outputSideMask & (1 << i)) != 0) {
                total += rates[i];
            }
        }
        if (total > InfinityWhatConfig.maxItemTotalRate()) {
            return false;
        }
        for (int i = 0; i < validated.length; i++) {
            this.sideRates[i] = validated[i];
            if (validated[i] > 0) {
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

    /**
     * The generator is only infinite while its GUI switch is enabled, it holds an item, and the
     * shared infinite-source balance switch is enabled.
     */
    private boolean effectivelyInfinite() {
        return this.infiniteModeEnabled
                && !this.itemHandler.getInternalStack().isEmpty()
                && InfinityWhatConfig.infiniteEnabled();
    }

    public IItemHandler getItemHandler() {
        return this.itemHandler;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container." + InfinityWhat.MOD_ID + ".inf_item_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new InfItemGeneratorMenu(containerId, inventory, this);
    }

    // ------------------------------------------------------------------
    // Server tick: active output into configured adjacent containers
    // ------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, InfItemGeneratorBlockEntity blockEntity) {
        blockEntity.tick();
    }

    private void tick() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        this.outputStatus.tick();

        boolean active = effectivelyInfinite() && this.outputEnabled && isRedstoneAllowed();
        if (!active) {
            this.wasOutputActive = false;
            return;
        }
        if (!this.wasOutputActive) {
            this.wasOutputActive = true;
            this.outputStatus.wake();
        }
        if (getTotalEffectiveRate() <= 0L) {
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
        return menu instanceof InfItemGeneratorMenu generatorMenu
                && generatorMenu.getBlockEntity() == this;
    }

    /**
     * Tries to insert the generated item into selected adjacent blocks that expose a Forge
     * {@link IItemHandler} (chests, hoppers, AE2 interfaces, pipe endpoints, ...).
     *
     * @return {@link OutputStatus#OK} if anything was accepted, otherwise full/rejected/none.
     */
    private int pushToNeighbors() {
        ItemStack stored = this.itemHandler.getInternalStack();
        if (stored.isEmpty() || this.level == null) {
            return OutputStatus.NONE;
        }
        int result = OutputStatus.NONE;
        Direction[] directions = Direction.values();
        for (int offset = 0; offset < directions.length; offset++) {
            Direction direction = directions[(this.pushStartSide + offset) % directions.length];
            int count = getEffectiveSideRate(direction);
            if (count <= 0) {
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
            IItemHandler target = neighbor
                    .getCapability(ForgeCapabilities.ITEM_HANDLER, direction.getOpposite())
                    .orElse(null);
            if (target == null) {
                continue;
            }
            boolean validSlot = false;
            for (int slot = 0; slot < target.getSlots(); slot++) {
                if (target.isItemValid(slot, stored)) {
                    validSlot = true;
                    break;
                }
            }
            // Copy (so NBT/components survive); the helper splits large counts across target slots.
            ItemStack offer = stored.copy();
            offer.setCount(count);
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(target, offer, false);
            int accepted = count - remainder.getCount();
            if (accepted > 0) {
                result = OutputStatus.OK;
            } else if (validSlot) {
                if (result != OutputStatus.OK) {
                    result = OutputStatus.FULL;
                }
            } else if (result == OutputStatus.NONE) {
                result = OutputStatus.REJECTED;
            }
        }
        this.pushStartSide = (this.pushStartSide + 1) % directions.length;
        return result;
    }

    // ------------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ItemStack stored = this.itemHandler.getInternalStack();
        if (!stored.isEmpty()) {
            tag.put(KEY_ITEM, stored.save(registries));
        }
        tag.putInt(KEY_DATA_VERSION, DATA_VERSION);
        tag.putBoolean(KEY_INFINITE_MODE, this.infiniteModeEnabled);
        tag.putBoolean(KEY_OUTPUT_ENABLED, this.outputEnabled);
        tag.putInt(KEY_OUTPUT_SIDES, this.outputSideMask);
        tag.putInt(KEY_REDSTONE_MODE, this.redstoneMode.ordinal());
        tag.putInt(KEY_PULSE_ON, this.pulseOnTicks);
        tag.putInt(KEY_PULSE_OFF, this.pulseOffTicks);
        tag.putInt(KEY_BATCH_SIZE, this.batchSize);
        for (Direction direction : Direction.values()) {
            int index = sideIndex(direction);
            tag.putInt(SIDE_RATE_KEYS[index], this.sideRates[index]);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.itemHandler.setInternalEmpty();
        if (tag.contains(KEY_ITEM, Tag.TAG_COMPOUND)) {
            ItemStack loaded = ItemStack.parseOptional(registries, tag.getCompound(KEY_ITEM));
            if (!loaded.isEmpty()) {
                // The slot always holds exactly one template item.
                this.itemHandler.setInternalStack(loaded.copyWithCount(1));
            }
        }
        this.infiniteModeEnabled = tag.getBoolean(KEY_INFINITE_MODE);
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
        if (!tag.contains(key, Tag.TAG_INT)) {
            return DEFAULT_PULSE_TICKS;
        }
        return Math.max(MIN_PULSE_TICKS, Math.min(MAX_PULSE_TICKS, tag.getInt(key)));
    }

    /**
     * Reads the six per-side rates, migrating 2.1.x saves that only stored one global rate.
     * This runs on the client as well (update tags reuse load), so it must stay side-effect free.
     */
    private void loadOutputRates(CompoundTag tag) {
        int dataVersion = tag.contains(KEY_DATA_VERSION, Tag.TAG_INT)
                ? tag.getInt(KEY_DATA_VERSION) : 1;
        int legacyMask = (tag.contains(KEY_OUTPUT_SIDES, Tag.TAG_INT)
                ? tag.getInt(KEY_OUTPUT_SIDES) : ALL_SIDES) & ALL_SIDES;
        boolean hasLegacyRate = tag.contains(KEY_LEGACY_OUTPUT_RATE, Tag.TAG_INT);
        int legacyRate = Math.max(1, hasLegacyRate
                ? tag.getInt(KEY_LEGACY_OUTPUT_RATE) : DEFAULT_OUTPUT_RATE);

        int[] loaded = new int[this.sideRates.length];
        for (Direction direction : Direction.values()) {
            int index = sideIndex(direction);
            if (dataVersion >= DATA_VERSION) {
                loaded[index] = tag.contains(SIDE_RATE_KEYS[index], Tag.TAG_INT)
                        ? tag.getInt(SIDE_RATE_KEYS[index])
                        : (hasLegacyRate ? legacyRate : DEFAULT_OUTPUT_RATE);
            } else {
                loaded[index] = legacyRate;
            }
        }

        this.outputSideMask = (dataVersion >= DATA_VERSION
                ? (tag.contains(KEY_OUTPUT_SIDES, Tag.TAG_INT)
                        ? tag.getInt(KEY_OUTPUT_SIDES) : ALL_SIDES)
                : legacyMask) & ALL_SIDES;
        int maxPerSide = (int) Math.min(Integer.MAX_VALUE, InfinityWhatConfig.maxItemRatePerSide());
        for (int i = 0; i < loaded.length; i++) {
            loaded[i] = Math.max(0, Math.min(maxPerSide, loaded[i]));
        }
        clampTotalRate(loaded);
        for (int i = 0; i < loaded.length; i++) {
            this.sideRates[i] = loaded[i];
            this.lastNonZeroRates[i] = loaded[i] > 0 ? loaded[i] : DEFAULT_OUTPUT_RATE;
        }
    }

    /** Reduces enabled sides in a fixed order when a lowered config cap is below the saved total. */
    private void clampTotalRate(int[] rates) {
        long maxTotal = InfinityWhatConfig.maxItemTotalRate();
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
            int reduction = (int) Math.min((long) rates[i], overflow);
            rates[i] -= reduction;
            overflow -= reduction;
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

    // ------------------------------------------------------------------
    // Capability item handler (huge count while infinite)
    // ------------------------------------------------------------------

    /**
     * The {@link IItemHandler} given to pipes and AE2 storage buses. While the generator is
     * infinite it reports {@link Integer#MAX_VALUE} for the single slot and never depletes on
     * extraction, so the network sees an effectively unbounded amount instead of one item.
     * Insertion is always limited to one template item.
     */
    private final class CapabilityItemHandler implements IItemHandler {

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            if (slot != 0) {
                return ItemStack.EMPTY;
            }
            ItemStack stored = itemHandler.getInternalStack();
            if (stored.isEmpty()) {
                return ItemStack.EMPTY;
            }
            if (effectivelyInfinite()) {
                // ItemStack#count is an int, so Integer.MAX_VALUE is the largest amount the
                // engine can express; AE2 reads this count and shows the slot as unbounded.
                return ItemHandlerHelper.copyStackWithSize(stored, Integer.MAX_VALUE);
            }
            return stored.copy();
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot != 0 || stack.isEmpty()) {
                return stack;
            }
            // Only one item is ever accepted, and never while the infinite source is active.
            if (effectivelyInfinite() || !itemHandler.getInternalStack().isEmpty()) {
                return stack;
            }
            return itemHandler.insertItem(0, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot != 0 || amount <= 0) {
                return ItemStack.EMPTY;
            }
            ItemStack stored = itemHandler.getInternalStack();
            if (stored.isEmpty()) {
                return ItemStack.EMPTY;
            }
            if (effectivelyInfinite()) {
                // Unlimited source: never deplete, hand out up to one normal stack per call.
                return ItemHandlerHelper.copyStackWithSize(stored, Math.min(amount, stored.getMaxStackSize()));
            }
            return itemHandler.extractItem(0, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return effectivelyInfinite() ? Integer.MAX_VALUE : 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && !effectivelyInfinite() && itemHandler.getInternalStack().isEmpty()
                    && InfinityWhatConfig.isItemAllowed(ForgeRegistries.ITEMS.getKey(stack.getItem()));
        }
    }

    // ------------------------------------------------------------------
    // Item handler
    // ------------------------------------------------------------------

    private final class GeneratorItemHandler extends ItemStackHandler {

        GeneratorItemHandler() {
            super(1);
        }

        ItemStack getInternalStack() {
            return this.stacks.get(0);
        }

        void setInternalStack(ItemStack stack) {
            this.stacks.set(0, stack);
        }

        void setInternalEmpty() {
            this.stacks.set(0, ItemStack.EMPTY);
        }

        @Override
        public int getSlotLimit(int slot) {
            // The generator always holds exactly one template item.
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            // A single item may be inserted while the slot is empty; the slot is locked otherwise.
            return slot == 0 && this.stacks.get(0).isEmpty()
                    && InfinityWhatConfig.isItemAllowed(ForgeRegistries.ITEMS.getKey(stack.getItem()));
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (effectivelyInfinite() || stack.isEmpty() || !this.stacks.get(0).isEmpty()) {
                return stack;
            }
            // super honours getSlotLimit (1) and isItemValid, so at most one unit is stored,
            // and it fires onContentsChanged (which marks the block entity dirty) itself.
            return super.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot != 0 || amount <= 0) {
                return ItemStack.EMPTY;
            }
            ItemStack existing = this.stacks.get(0);
            if (existing.isEmpty()) {
                return ItemStack.EMPTY;
            }
            if (effectivelyInfinite()) {
                // Unlimited source: hand out up to one normal stack per call, never deplete.
                return ItemHandlerHelper.copyStackWithSize(existing, Math.min(amount, existing.getMaxStackSize()));
            }
            return super.extractItem(slot, amount, simulate);
        }

        @Override
        protected void onContentsChanged(int slot) {
            markUpdated();
        }
    }
}

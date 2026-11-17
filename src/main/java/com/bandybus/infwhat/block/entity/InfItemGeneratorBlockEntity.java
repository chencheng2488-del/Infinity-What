package com.bandybus.infwhat.block.entity;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.RedstoneMode;
import com.bandybus.infwhat.config.InfinityWhatConfig;
import com.bandybus.infwhat.menu.InfItemGeneratorMenu;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;

/**
 * Infinity Item Generator. Its GUI-controlled infinite-mode switch makes the stored item an
 * unlimited source. Active output is independently configurable and can be limited to selected
 * sides; the Fabric item storage view remains available to pipes/import buses on every side.
 *
 * ENGINE LIMIT: a vanilla {@link ItemStack} stores its count as an {@code int}, so a true
 * {@code Long.MAX_VALUE} count cannot be stored. The internal slot always holds exactly one
 * template item; while infinite the exposed storage reports {@link Long#MAX_VALUE} (the largest
 * amount the engine can express) so AE2 storage buses and pipes see an effectively unbounded
 * amount, and extraction never depletes.
 */
public class InfItemGeneratorBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory {

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

    /** The stored item, always a single unit internally. */
    private ItemStack storedItem = ItemStack.EMPTY;

    private final GeneratorItemStorage itemStorage = new GeneratorItemStorage();

    public InfItemGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INF_ITEM_GENERATOR, pos, state);
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
    // Item storage
    // ------------------------------------------------------------------

    public Storage<ItemVariant> getItemStorage() {
        return this.itemStorage;
    }

    // ------------------------------------------------------------------
    // Data exposed to the GUI / other code
    // ------------------------------------------------------------------

    /** The stored item, always a single unit internally. */
    public ItemStack getStoredItem() {
        return this.storedItem;
    }

    /** Replaces the template item; the slot always keeps exactly one unit. */
    public void setStoredItem(ItemStack stack) {
        ItemStack next = ItemStack.EMPTY;
        if (!stack.isEmpty()) {
            next = stack.copy();
            next.setCount(1);
        }
        if (ItemStack.matches(this.storedItem, next)) {
            return;
        }
        this.storedItem = next;
        markUpdated();
    }

    public void removeStored(int amount) {
        if (this.storedItem.isEmpty() || amount <= 0) {
            return;
        }
        if (amount >= this.storedItem.getCount()) {
            this.storedItem = ItemStack.EMPTY;
        } else {
            this.storedItem.shrink(amount);
        }
        markUpdated();
    }

    /**
     * Manual retrieval used by the GUI. While infinite the source is never depleted; otherwise
     * the stored template item is consumed.
     */
    public ItemStack extractStored(int amount) {
        if (this.storedItem.isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        if (effectivelyInfinite()) {
            ItemStack unlimited = this.storedItem.copy();
            unlimited.setCount(Math.min(amount, this.storedItem.getMaxStackSize()));
            return unlimited;
        }
        ItemStack result = this.storedItem.copy();
        result.setCount(Math.min(amount, this.storedItem.getCount()));
        removeStored(result.getCount());
        return result;
    }

    /** Whether the GUI slot may accept the given stack. */
    public boolean canInsertItem(ItemStack stack) {
        return !stack.isEmpty()
                && !effectivelyInfinite()
                && this.storedItem.isEmpty()
                && InfinityWhatConfig.isItemAllowed(Registry.ITEM.getKey(stack.getItem()));
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
                && !this.storedItem.isEmpty()
                && InfinityWhatConfig.infiniteEnabled();
    }

    @Override
    public Component getDisplayName() {
        return new TranslatableComponent("container." + InfinityWhat.MOD_ID + ".inf_item_generator");
    }

    @Override
    public void writeScreenOpeningData(ServerPlayer player, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(this.worldPosition);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new InfItemGeneratorMenu(containerId, inventory, this);
    }

    // ------------------------------------------------------------------
    // Server tick: active output into configured adjacent containers
    // ------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  InfItemGeneratorBlockEntity blockEntity) {
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
        return menu instanceof InfItemGeneratorMenu
                && ((InfItemGeneratorMenu) menu).getBlockEntity() == this;
    }

    /**
     * Tries to insert the generated item into selected adjacent blocks that expose a Fabric item
     * storage (chests, hoppers, AE2 interfaces, pipe endpoints, ...).
     *
     * @return {@link OutputStatus#OK} if anything was accepted, otherwise full/none.
     */
    private int pushToNeighbors() {
        ItemStack stored = this.storedItem;
        if (stored.isEmpty() || this.level == null) {
            return OutputStatus.NONE;
        }
        ItemVariant resource = ItemVariant.of(stored);
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
            Storage<ItemVariant> target;
            if (neighbor instanceof InfItemGeneratorBlockEntity generator) {
                target = generator.getItemStorage();
            } else {
                target = ItemStorage.SIDED.find(this.level, neighborPos,
                        direction.getOpposite());
            }
            if (target == null) {
                continue;
            }
            long accepted;
            try (Transaction transaction = Transaction.openOuter()) {
                accepted = target.insert(resource, count, transaction);
                transaction.commit();
            }
            if (accepted > 0) {
                result = OutputStatus.OK;
            } else if (result != OutputStatus.OK) {
                // The target exists but accepted nothing this attempt: treat it as full and back off.
                result = OutputStatus.FULL;
            }
        }
        this.pushStartSide = (this.pushStartSide + 1) % directions.length;
        return result;
    }

    // ------------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!this.storedItem.isEmpty()) {
            tag.put(KEY_ITEM, this.storedItem.save(new CompoundTag()));
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
    public void load(CompoundTag tag) {
        super.load(tag);
        this.storedItem = ItemStack.EMPTY;
        if (tag.contains(KEY_ITEM, Tag.TAG_COMPOUND)) {
            ItemStack loaded = ItemStack.of(tag.getCompound(KEY_ITEM));
            if (!loaded.isEmpty()) {
                // The slot always holds exactly one template item.
                this.storedItem = loaded.copy();
                this.storedItem.setCount(1);
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
    // Item storage implementation (single slot, transactional)
    // ------------------------------------------------------------------

    private final class GeneratorItemStorage extends SnapshotParticipant<ItemStack>
            implements SingleSlotStorage<ItemVariant> {

        @Override
        protected ItemStack createSnapshot() {
            return InfItemGeneratorBlockEntity.this.storedItem.copy();
        }

        @Override
        protected void readSnapshot(ItemStack snapshot) {
            InfItemGeneratorBlockEntity.this.storedItem = snapshot;
        }

        @Override
        protected void onFinalCommit() {
            markUpdated();
        }

        @Override
        public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
            if (resource.isBlank() || maxAmount < 1) {
                return 0;
            }
            // Only one item is ever accepted, and never while the infinite source is active.
            if (effectivelyInfinite() || !InfItemGeneratorBlockEntity.this.storedItem.isEmpty()) {
                return 0;
            }
            if (!InfinityWhatConfig.isItemAllowed(Registry.ITEM.getKey(resource.getItem()))) {
                return 0;
            }
            updateSnapshots(transaction);
            InfItemGeneratorBlockEntity.this.storedItem = resource.toStack(1);
            return 1;
        }

        @Override
        public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
            if (resource.isBlank() || maxAmount <= 0
                    || InfItemGeneratorBlockEntity.this.storedItem.isEmpty()
                    || !resource.equals(ItemVariant.of(InfItemGeneratorBlockEntity.this.storedItem))) {
                return 0;
            }
            ItemStack stored = InfItemGeneratorBlockEntity.this.storedItem;
            if (effectivelyInfinite()) {
                // Unlimited source: never deplete, hand out up to one normal stack per call.
                return Math.min(maxAmount, stored.getMaxStackSize());
            }
            updateSnapshots(transaction);
            InfItemGeneratorBlockEntity.this.storedItem = ItemStack.EMPTY;
            return 1;
        }

        @Override
        public boolean isResourceBlank() {
            return InfItemGeneratorBlockEntity.this.storedItem.isEmpty();
        }

        @Override
        public ItemVariant getResource() {
            ItemStack stored = InfItemGeneratorBlockEntity.this.storedItem;
            return stored.isEmpty() ? ItemVariant.blank() : ItemVariant.of(stored);
        }

        @Override
        public long getAmount() {
            if (effectivelyInfinite()) {
                return Long.MAX_VALUE;
            }
            return InfItemGeneratorBlockEntity.this.storedItem.isEmpty() ? 0 : 1;
        }

        @Override
        public long getCapacity() {
            return effectivelyInfinite() ? Long.MAX_VALUE : 1;
        }
    }
}

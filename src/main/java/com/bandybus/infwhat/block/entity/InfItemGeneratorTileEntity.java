package com.bandybus.infwhat.block.entity;

import com.bandybus.infwhat.block.RedstoneMode;
import com.bandybus.infwhat.config.InfinityWhatConfig;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

import java.util.Arrays;

/**
 * Infinity Item Generator. Its GUI-controlled infinite-mode switch turns the stored item into an
 * unlimited source. Active output is configurable per side; the standard Forge item handler
 * capability stays available to pipes and hoppers on every side.
 *
 * <p>1.8.9 represents empty item stacks with {@code null}, so every read path is null-checked.
 */
public class InfItemGeneratorTileEntity extends TileEntity implements ITickable {

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
    private static final int SIDE_COUNT = EnumFacing.values().length;
    private static final int ALL_SIDES = (1 << SIDE_COUNT) - 1;
    private static final String[] SIDE_RATE_KEYS = createSideRateKeys();

    public static final int DEFAULT_OUTPUT_RATE = 8;
    public static final int DEFAULT_PULSE_TICKS = 20;
    public static final int MIN_PULSE_TICKS = 1;
    public static final int MAX_PULSE_TICKS = 12000;
    public static final int DEFAULT_BATCH_SIZE = 1;
    public static final int MIN_BATCH_SIZE = 1;
    public static final int MAX_BATCH_SIZE = 64;

    private boolean infiniteModeEnabled;
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
    /** Per-side output rate in items/tick; 0 means the side produces nothing. */
    private final int[] sideRates = new int[SIDE_COUNT];
    /** Last non-zero rate per side, so toggling a side back on restores a useful value. */
    private final int[] lastNonZeroRates = new int[SIDE_COUNT];

    private final GeneratorItemHandler itemHandler = new GeneratorItemHandler();
    private final IItemHandler capabilityHandler = new GeneratorCapabilityHandler();

    public InfItemGeneratorTileEntity() {
        Arrays.fill(this.sideRates, DEFAULT_OUTPUT_RATE);
        Arrays.fill(this.lastNonZeroRates, DEFAULT_OUTPUT_RATE);
    }

    private static String[] createSideRateKeys() {
        EnumFacing[] directions = EnumFacing.values();
        String[] keys = new String[directions.length];
        for (EnumFacing direction : directions) {
            String name = direction.getName();
            keys[direction.ordinal()] = "SideRate"
                    + Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }
        return keys;
    }

    private static int sideIndex(EnumFacing direction) {
        return direction.ordinal();
    }

    // ------------------------------------------------------------------
    // Capability
    // ------------------------------------------------------------------

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY
                || super.hasCapability(capability, facing);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return (T) this.capabilityHandler;
        }
        return super.getCapability(capability, facing);
    }

    // ------------------------------------------------------------------
    // Data exposed to the GUI / other code
    // ------------------------------------------------------------------

    public ItemStack getStoredItem() {
        return this.itemHandler.getStackInSlot(0);
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

    public boolean isOutputSideEnabled(EnumFacing direction) {
        return (this.outputSideMask & (1 << direction.ordinal())) != 0;
    }

    public void toggleOutputSide(EnumFacing direction) {
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
    public int getSideRate(EnumFacing direction) {
        return this.sideRates[sideIndex(direction)];
    }

    /** Rate actually used for one side; a disabled side always reports 0. */
    public int getEffectiveSideRate(EnumFacing direction) {
        return isOutputSideEnabled(direction) ? this.sideRates[sideIndex(direction)] : 0;
    }

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

    public boolean setSideRate(EnumFacing direction, int amount) {
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
                return this.worldObj.isBlockIndirectlyGettingPowered(this.pos) > 0;
            case LOW:
                return this.worldObj.isBlockIndirectlyGettingPowered(this.pos) <= 0;
            case PULSE:
                long period = (long) this.pulseOnTicks + this.pulseOffTicks;
                long phase = Math.floorMod(
                        this.worldObj.getTotalWorldTime() + this.pos.toLong(), period);
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

    /** Replaces the whole output configuration in one validated step. */
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

    /** Applies only per-side rates, validated against the current side mask and limits. */
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

    /** Infinite mode is active only while the switch is on, an item is stored and balance allows it. */
    private boolean effectivelyInfinite() {
        ItemStack stored = this.itemHandler.getStackInSlot(0);
        return this.infiniteModeEnabled && stored != null && stored.stackSize > 0
                && InfinityWhatConfig.infiniteEnabled();
    }

    public IItemHandler getItemHandler() {
        return this.itemHandler;
    }

    // ------------------------------------------------------------------
    // Server tick: active output into configured adjacent containers
    // ------------------------------------------------------------------

    @Override
    public void update() {
        if (this.worldObj == null || this.worldObj.isRemote) {
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

    /** Clears the retry backoff so the next tick attempts output again. */
    public void wakeOutput() {
        this.outputStatus.wake();
    }

    private boolean isViewingMenu(net.minecraft.inventory.Container menu) {
        return menu instanceof com.bandybus.infwhat.container.InfItemGeneratorContainer
                && ((com.bandybus.infwhat.container.InfItemGeneratorContainer) menu)
                        .getTileEntity() == this;
    }

    /**
     * Tries to insert the generated item into selected adjacent blocks that expose a Forge
     * {@link IItemHandler}.
     *
     * @return {@link OutputStatus#OK} if anything was accepted, otherwise full/rejected/none.
     */
    private int pushToNeighbors() {
        ItemStack stored = this.itemHandler.getStackInSlot(0);
        if (stored == null || stored.stackSize <= 0 || this.worldObj == null) {
            return OutputStatus.NONE;
        }
        int result = OutputStatus.NONE;
        EnumFacing[] directions = EnumFacing.values();
        for (int offset = 0; offset < directions.length; offset++) {
            EnumFacing direction = directions[(this.pushStartSide + offset) % directions.length];
            int count = getEffectiveSideRate(direction);
            if (count <= 0) {
                continue;
            }
            BlockPos neighborPos = this.pos.offset(direction);
            if (!this.worldObj.isBlockLoaded(neighborPos)) {
                continue;
            }
            TileEntity neighbor = this.worldObj.getTileEntity(neighborPos);
            if (neighbor == null || neighbor == this) {
                continue;
            }
            IItemHandler target = neighbor.getCapability(
                    CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, direction.getOpposite());
            if (target == null) {
                continue;
            }
            // 1.8.9's IItemHandler has no isItemValid; a handler with slots counts as a target.
            boolean validSlot = target.getSlots() > 0;
            ItemStack offer = stored.copy();
            offer.stackSize = count;
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(target, offer, false);
            int accepted = count - (remainder == null ? 0 : remainder.stackSize);
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
    public void writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        ItemStack stored = this.itemHandler.getStackInSlot(0);
        if (stored != null && stored.stackSize > 0) {
            compound.setTag(KEY_ITEM, stored.writeToNBT(new NBTTagCompound()));
        }
        compound.setInteger(KEY_DATA_VERSION, DATA_VERSION);
        compound.setBoolean(KEY_INFINITE_MODE, this.infiniteModeEnabled);
        compound.setBoolean(KEY_OUTPUT_ENABLED, this.outputEnabled);
        compound.setInteger(KEY_OUTPUT_SIDES, this.outputSideMask);
        compound.setInteger(KEY_REDSTONE_MODE, this.redstoneMode.ordinal());
        compound.setInteger(KEY_PULSE_ON, this.pulseOnTicks);
        compound.setInteger(KEY_PULSE_OFF, this.pulseOffTicks);
        compound.setInteger(KEY_BATCH_SIZE, this.batchSize);
        for (EnumFacing direction : EnumFacing.values()) {
            compound.setInteger(SIDE_RATE_KEYS[sideIndex(direction)],
                    this.sideRates[sideIndex(direction)]);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        this.itemHandler.setStackInSlot(0, null);
        if (compound.hasKey(KEY_ITEM, 10)) {
            ItemStack loaded = ItemStack.loadItemStackFromNBT(compound.getCompoundTag(KEY_ITEM));
            if (loaded != null && loaded.stackSize > 0) {
                loaded.stackSize = 1;
                this.itemHandler.setStackInSlot(0, loaded);
            }
        }
        this.infiniteModeEnabled = compound.getBoolean(KEY_INFINITE_MODE);
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

    /** Reads the six per-side rates, migrating 2.1.x saves that only stored one global rate. */
    private void loadOutputRates(NBTTagCompound compound) {
        int dataVersion = compound.hasKey(KEY_DATA_VERSION, 3)
                ? compound.getInteger(KEY_DATA_VERSION) : 1;
        int legacyMask = (compound.hasKey(KEY_OUTPUT_SIDES, 3)
                ? compound.getInteger(KEY_OUTPUT_SIDES) : ALL_SIDES) & ALL_SIDES;
        boolean hasLegacyRate = compound.hasKey(KEY_LEGACY_OUTPUT_RATE, 3);
        int legacyRate = Math.max(1, hasLegacyRate
                ? compound.getInteger(KEY_LEGACY_OUTPUT_RATE) : DEFAULT_OUTPUT_RATE);

        int[] loaded = new int[this.sideRates.length];
        for (EnumFacing direction : EnumFacing.values()) {
            int index = sideIndex(direction);
            if (dataVersion >= DATA_VERSION) {
                loaded[index] = compound.hasKey(SIDE_RATE_KEYS[index], 3)
                        ? compound.getInteger(SIDE_RATE_KEYS[index])
                        : (hasLegacyRate ? legacyRate : DEFAULT_OUTPUT_RATE);
            } else {
                loaded[index] = legacyRate;
            }
        }

        this.outputSideMask = (dataVersion >= DATA_VERSION
                ? (compound.hasKey(KEY_OUTPUT_SIDES, 3)
                        ? compound.getInteger(KEY_OUTPUT_SIDES) : ALL_SIDES)
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

    private static String itemId(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return null;
        }
        ResourceLocation id = (ResourceLocation) Item.itemRegistry.getNameForObject(stack.getItem());
        return id == null ? null : id.toString();
    }

    // ------------------------------------------------------------------
    // Capability item handler (huge count while infinite)
    // ------------------------------------------------------------------

    private final class GeneratorCapabilityHandler implements IItemHandler {

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            if (slot != 0) {
                return null;
            }
            ItemStack stored = itemHandler.getStackInSlot(0);
            if (stored == null || stored.stackSize <= 0) {
                return null;
            }
            if (effectivelyInfinite()) {
                return ItemHandlerHelper.copyStackWithSize(stored, Integer.MAX_VALUE);
            }
            return stored.copy();
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot != 0 || stack == null || stack.stackSize <= 0) {
                return stack;
            }
            ItemStack stored = itemHandler.getStackInSlot(0);
            if (effectivelyInfinite() || (stored != null && stored.stackSize > 0)) {
                return stack;
            }
            return itemHandler.insertItem(0, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot != 0 || amount <= 0) {
                return null;
            }
            ItemStack stored = itemHandler.getStackInSlot(0);
            if (stored == null || stored.stackSize <= 0) {
                return null;
            }
            if (effectivelyInfinite()) {
                return ItemHandlerHelper.copyStackWithSize(stored,
                        Math.min(amount, stored.getMaxStackSize()));
            }
            return itemHandler.extractItem(0, amount, simulate);
        }
    }

    // ------------------------------------------------------------------
    // Internal item handler used by the GUI slot
    // ------------------------------------------------------------------

    private final class GeneratorItemHandler extends ItemStackHandler {

        GeneratorItemHandler() {
            super(1);
        }

        @Override
        protected int getStackLimit(int slot, ItemStack stack) {
            return 1;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (effectivelyInfinite() || stack == null || stack.stackSize <= 0) {
                return stack;
            }
            ItemStack stored = this.getStackInSlot(0);
            if (stored != null && stored.stackSize > 0) {
                return stack;
            }
            return super.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot != 0 || amount <= 0) {
                return null;
            }
            ItemStack existing = this.getStackInSlot(0);
            if (existing == null || existing.stackSize <= 0) {
                return null;
            }
            if (effectivelyInfinite()) {
                return ItemHandlerHelper.copyStackWithSize(existing,
                        Math.min(amount, existing.getMaxStackSize()));
            }
            return super.extractItem(slot, amount, simulate);
        }

        @Override
        protected void onContentsChanged(int slot) {
            markUpdated();
        }
    }

    /** Checks the configured item filter for a stack (used by the container slot). */
    public static boolean isAllowed(ItemStack stack) {
        return InfinityWhatConfig.isItemAllowed(itemId(stack));
    }

    /** Whether this tile entity is still attached to a loaded world at its position. */
    public boolean isUsable() {
        return this.worldObj != null && this.worldObj.getTileEntity(this.pos) == this;
    }
}

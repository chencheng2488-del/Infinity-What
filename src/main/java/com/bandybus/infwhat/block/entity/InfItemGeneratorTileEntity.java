package com.bandybus.infwhat.block.entity;

import com.bandybus.infwhat.block.RedstoneMode;
import com.bandybus.infwhat.config.InfinityWhatConfig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.Arrays;

/**
 * Infinity Item Generator. Its GUI-controlled infinite-mode switch turns the stored item into an
 * unlimited source. Active output is configurable per side, and the single-slot {@link IInventory}
 * that every machine face exposes lets hoppers and pipes pull from it.
 *
 * <p>1.7.10 has no item handler capability, so transport uses the plain {@code IInventory} API.
 * While infinite mode is active {@link #decrStackSize(int, int)} returns fresh copies instead of
 * consuming the stored item.
 */
public class InfItemGeneratorTileEntity extends TileEntity implements IInventory {

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
    private static final int SIDE_COUNT = ForgeDirection.VALID_DIRECTIONS.length;
    private static final int ALL_SIDES = (1 << SIDE_COUNT) - 1;
    private static final String[] SIDE_RATE_KEYS = createSideRateKeys();

    /** Items pushed per enabled side and tick when nothing else is configured. */
    public static final int DEFAULT_OUTPUT_RATE = 8;
    public static final int DEFAULT_PULSE_TICKS = 20;
    public static final int MIN_PULSE_TICKS = 1;
    public static final int MAX_PULSE_TICKS = 12000;
    public static final int DEFAULT_BATCH_SIZE = 1;
    public static final int MIN_BATCH_SIZE = 1;
    public static final int MAX_BATCH_SIZE = 64;

    private ItemStack storedItem;
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
    private final int[] sideRates = new int[SIDE_COUNT];
    private final int[] lastNonZeroRates = new int[SIDE_COUNT];

    public InfItemGeneratorTileEntity() {
        Arrays.fill(this.sideRates, DEFAULT_OUTPUT_RATE);
        Arrays.fill(this.lastNonZeroRates, DEFAULT_OUTPUT_RATE);
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
    // Data exposed to the GUI / other code
    // ------------------------------------------------------------------

    /** The stored item, or null while the slot is empty. */
    public ItemStack getStoredItem() {
        return this.storedItem;
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

    public boolean isOutputSideEnabled(ForgeDirection direction) {
        return (this.outputSideMask & (1 << direction.ordinal())) != 0;
    }

    public void toggleOutputSide(ForgeDirection direction) {
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

    public int getSideRate(ForgeDirection direction) {
        return this.sideRates[sideIndex(direction)];
    }

    public int getEffectiveSideRate(ForgeDirection direction) {
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

    public boolean setSideRate(ForgeDirection direction, int amount) {
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
        long total = getTotalEffectiveRate();
        if (total <= 0L) {
            return 0;
        }
        return (int) Math.min(15L, total);
    }

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

    private boolean effectivelyInfinite() {
        return this.infiniteModeEnabled && this.storedItem != null && this.storedItem.stackSize > 0
                && InfinityWhatConfig.infiniteEnabled();
    }

    /** Item id used by the configuration filter, or null when unknown. */
    private static String itemId(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return null;
        }
        return Item.itemRegistry.getNameForObject(stack.getItem());
    }

    /** Checks the configured item filter for a stack (used by the container slot). */
    public static boolean isAllowed(ItemStack stack) {
        return InfinityWhatConfig.isItemAllowed(itemId(stack));
    }

    /** Whether this tile entity is still attached to a loaded world at its position. */
    public boolean isUsable() {
        return this.worldObj != null
                && this.worldObj.getTileEntity(this.xCoord, this.yCoord, this.zCoord) == this;
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

    public void wakeOutput() {
        this.outputStatus.wake();
    }

    private boolean isViewingMenu(net.minecraft.inventory.Container menu) {
        return menu instanceof com.bandybus.infwhat.container.InfItemGeneratorContainer
                && ((com.bandybus.infwhat.container.InfItemGeneratorContainer) menu)
                        .getTileEntity() == this;
    }

    private int pushToNeighbors() {
        ItemStack stored = this.storedItem;
        if (stored == null || stored.stackSize <= 0 || this.worldObj == null) {
            return OutputStatus.NONE;
        }
        int result = OutputStatus.NONE;
        ForgeDirection[] directions = ForgeDirection.VALID_DIRECTIONS;
        for (int offset = 0; offset < directions.length; offset++) {
            ForgeDirection direction = directions[(this.pushStartSide + offset) % directions.length];
            int count = getEffectiveSideRate(direction);
            if (count <= 0) {
                continue;
            }
            int neighborX = this.xCoord + direction.offsetX;
            int neighborY = this.yCoord + direction.offsetY;
            int neighborZ = this.zCoord + direction.offsetZ;
            if (!this.worldObj.blockExists(neighborX, neighborY, neighborZ)) {
                continue;
            }
            TileEntity neighbor = this.worldObj.getTileEntity(neighborX, neighborY, neighborZ);
            if (!(neighbor instanceof IInventory) || neighbor == this) {
                continue;
            }
            IInventory target = (IInventory) neighbor;
            boolean validTarget = target.getSizeInventory() > 0;
            ItemStack offer = stored.copy();
            offer.stackSize = Math.min(count, offer.getMaxStackSize());
            int accepted = insertStack(target, offer);
            if (accepted > 0) {
                result = OutputStatus.OK;
            } else if (validTarget) {
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

    /**
     * Merges as much of {@code stack} into {@code target} as it accepts and returns the accepted
     * count. The source stack is never consumed.
     */
    private static int insertStack(IInventory target, ItemStack stack) {
        int limit = Math.min(target.getInventoryStackLimit(), stack.getMaxStackSize());
        int remaining = stack.stackSize;
        int accepted = 0;
        for (int slot = 0; slot < target.getSizeInventory() && remaining > 0; slot++) {
            if (!target.isItemValidForSlot(slot, stack)) {
                continue;
            }
            ItemStack current = target.getStackInSlot(slot);
            if (current == null) {
                int amount = Math.min(remaining, limit);
                ItemStack placed = stack.copy();
                placed.stackSize = amount;
                target.setInventorySlotContents(slot, placed);
                remaining -= amount;
                accepted += amount;
            } else if (current.isItemEqual(stack)
                    && ItemStack.areItemStackTagsEqual(current, stack)) {
                int space = Math.min(limit, current.getMaxStackSize()) - current.stackSize;
                if (space <= 0) {
                    continue;
                }
                int amount = Math.min(remaining, space);
                current.stackSize += amount;
                remaining -= amount;
                accepted += amount;
                target.setInventorySlotContents(slot, current);
            }
        }
        return accepted;
    }

    // ------------------------------------------------------------------
    // IInventory (transport API of 1.7.10)
    // ------------------------------------------------------------------

    @Override
    public int getSizeInventory() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        if (index != 0 || this.storedItem == null || this.storedItem.stackSize <= 0) {
            return null;
        }
        return this.storedItem.copy();
    }

    @Override
    public ItemStack decrStackSize(int index, int amount) {
        if (index != 0 || amount <= 0 || this.storedItem == null) {
            return null;
        }
        if (effectivelyInfinite()) {
            ItemStack copy = this.storedItem.copy();
            copy.stackSize = Math.min(amount, copy.getMaxStackSize());
            return copy;
        }
        int taken = Math.min(amount, this.storedItem.stackSize);
        ItemStack result = this.storedItem.copy();
        result.stackSize = taken;
        this.storedItem.stackSize -= taken;
        if (this.storedItem.stackSize <= 0) {
            this.storedItem = null;
        }
        markUpdated();
        return result;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int index) {
        return null;
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        if (index != 0) {
            return;
        }
        if (effectivelyInfinite()) {
            return;
        }
        if (stack == null) {
            this.storedItem = null;
        } else {
            ItemStack single = stack.copy();
            single.stackSize = 1;
            this.storedItem = single;
        }
        markUpdated();
    }

    @Override
    public String getInventoryName() {
        return "container.infwhat.inf_item_generator";
    }

    @Override
    public boolean isCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 1;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return isUsable() && player.getDistanceSq(this.xCoord + 0.5D, this.yCoord + 0.5D,
                this.zCoord + 0.5D) <= 64.0D;
    }

    @Override
    public void openChest() {
    }

    @Override
    public void closeChest() {
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return index == 0 && isAllowed(stack) && !effectivelyInfinite()
                && (this.storedItem == null || this.storedItem.stackSize <= 0);
    }

    // ------------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------------

    @Override
    public void writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        if (this.storedItem != null && this.storedItem.stackSize > 0) {
            compound.setTag(KEY_ITEM, this.storedItem.writeToNBT(new NBTTagCompound()));
        }
        compound.setInteger(KEY_DATA_VERSION, DATA_VERSION);
        compound.setBoolean(KEY_INFINITE_MODE, this.infiniteModeEnabled);
        compound.setBoolean(KEY_OUTPUT_ENABLED, this.outputEnabled);
        compound.setInteger(KEY_OUTPUT_SIDES, this.outputSideMask);
        compound.setInteger(KEY_REDSTONE_MODE, this.redstoneMode.ordinal());
        compound.setInteger(KEY_PULSE_ON, this.pulseOnTicks);
        compound.setInteger(KEY_PULSE_OFF, this.pulseOffTicks);
        compound.setInteger(KEY_BATCH_SIZE, this.batchSize);
        for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
            compound.setInteger(SIDE_RATE_KEYS[sideIndex(direction)],
                    this.sideRates[sideIndex(direction)]);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        this.storedItem = null;
        if (compound.hasKey(KEY_ITEM, 10)) {
            ItemStack loaded = ItemStack.loadItemStackFromNBT(compound.getCompoundTag(KEY_ITEM));
            if (loaded != null && loaded.stackSize > 0) {
                loaded.stackSize = 1;
                this.storedItem = loaded;
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

    private void loadOutputRates(NBTTagCompound compound) {
        int dataVersion = compound.hasKey(KEY_DATA_VERSION, 3)
                ? compound.getInteger(KEY_DATA_VERSION) : 1;
        int legacyMask = (compound.hasKey(KEY_OUTPUT_SIDES, 3)
                ? compound.getInteger(KEY_OUTPUT_SIDES) : ALL_SIDES) & ALL_SIDES;
        boolean hasLegacyRate = compound.hasKey(KEY_LEGACY_OUTPUT_RATE, 3);
        int legacyRate = Math.max(1, hasLegacyRate
                ? compound.getInteger(KEY_LEGACY_OUTPUT_RATE) : DEFAULT_OUTPUT_RATE);

        int[] loaded = new int[this.sideRates.length];
        for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
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
}

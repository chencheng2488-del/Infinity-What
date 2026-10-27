package com.bandybus.infwhat.block.entity;

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

/**
 * Infinity Item Generator. Its GUI-controlled infinite-mode switch turns the stored item into an
 * unlimited source. Active output is configurable per side, and the single-slot {@link IInventory}
 * that every machine face exposes lets hoppers and pipes pull from it.
 *
 * <p>1.7.10 has no item handler capability, so the transport hook is the plain {@code IInventory}
 * API. While infinite mode is active {@link #decrStackSize(int, int)} returns fresh copies instead
 * of consuming the stored item, which is how the 1.8.9 release emulated the same behaviour through
 * {@code IItemHandler}.
 *
 * <p>1.7.10 represents empty item stacks with {@code null}, so every read path is null-checked.
 */
public class InfItemGeneratorTileEntity extends TileEntity implements IInventory {

    private static final String KEY_ITEM = "Item";
    private static final String KEY_INFINITE_MODE = "InfiniteMode";
    private static final String KEY_OUTPUT_ENABLED = "OutputEnabled";
    private static final String KEY_OUTPUT_SIDES = "OutputSides";
    private static final String KEY_OUTPUT_RATE = "OutputRate";

    /** Items pushed per enabled side and tick when nothing else is configured. */
    public static final int DEFAULT_OUTPUT_RATE = 8;

    /** Number of faces a machine can push into (DOWN, UP, NORTH, SOUTH, WEST, EAST). */
    private static final int SIDE_COUNT = ForgeDirection.VALID_DIRECTIONS.length;

    /** Single stored item stack; the inventory limit keeps its size at one. */
    private ItemStack storedItem;

    private boolean infiniteModeEnabled;
    private boolean outputEnabled;
    private int outputSideMask = (1 << SIDE_COUNT) - 1;
    private int outputItemsPerTick = DEFAULT_OUTPUT_RATE;

    public InfItemGeneratorTileEntity() {
    }

    // ------------------------------------------------------------------
    // Data exposed to the GUI / other code
    // ------------------------------------------------------------------

    /** The stored item, or null while the slot is empty. */
    public ItemStack getStoredItem() {
        return this.storedItem;
    }

    /** Whether the generator currently behaves as an unlimited item source. */
    public boolean isInfinite() {
        return effectivelyInfinite();
    }

    /** Whether the infinite-mode switch itself is enabled. */
    public boolean isInfiniteModeEnabled() {
        return this.infiniteModeEnabled;
    }

    /** Turns the infinite-mode switch on or off and syncs the change to clients. */
    public void setInfiniteModeEnabled(boolean enabled) {
        if (this.infiniteModeEnabled == enabled) {
            return;
        }
        this.infiniteModeEnabled = enabled;
        markUpdated();
    }

    /** Whether active output is currently switched on. */
    public boolean isOutputEnabled() {
        return this.outputEnabled;
    }

    /** Turns active output on or off and syncs the change to clients. */
    public void setOutputEnabled(boolean enabled) {
        if (this.outputEnabled == enabled) {
            return;
        }
        this.outputEnabled = enabled;
        markUpdated();
    }

    /** Whether active output pushes toward the given face. */
    public boolean isOutputSideEnabled(ForgeDirection direction) {
        return (this.outputSideMask & (1 << direction.ordinal())) != 0;
    }

    /** Flips active output for one face. */
    public void toggleOutputSide(ForgeDirection direction) {
        this.outputSideMask ^= 1 << direction.ordinal();
        markUpdated();
    }

    /** Configured push amount per enabled face and tick. */
    public int getOutputItemsPerTick() {
        return this.outputItemsPerTick;
    }

    /** Clamps and stores a new push amount per enabled face and tick. */
    public void setOutputItemsPerTick(int amount) {
        int clamped = Math.max(1, amount);
        if (this.outputItemsPerTick == clamped) {
            return;
        }
        this.outputItemsPerTick = clamped;
        markUpdated();
    }

    /** Raw six-bit mask of enabled output faces. */
    public int getOutputSideMask() {
        return this.outputSideMask;
    }

    /** Applies a configuration copied from another item generator. */
    public void applyOutputConfiguration(boolean enabled, int sideMask, int amountPerTick,
                                         boolean infiniteMode) {
        this.outputEnabled = enabled;
        this.outputSideMask = sideMask & ((1 << SIDE_COUNT) - 1);
        this.outputItemsPerTick = Math.max(1, amountPerTick);
        this.infiniteModeEnabled = infiniteMode;
        markUpdated();
    }

    /** Infinite mode combined with a stored item and the configuration master switch. */
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
    // Server tick: active output into configured adjacent containers
    // ------------------------------------------------------------------

    @Override
    public void updateEntity() {
        if (this.worldObj == null || this.worldObj.isRemote) {
            return;
        }
        if (effectivelyInfinite() && this.outputEnabled) {
            this.pushToNeighbors();
        }
    }

    /** Pushes the configured amount into every enabled neighbouring container. */
    private void pushToNeighbors() {
        ItemStack stored = this.storedItem;
        if (stored == null || stored.stackSize <= 0 || this.worldObj == null) {
            return;
        }
        int count = this.outputItemsPerTick;
        for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
            if (!isOutputSideEnabled(direction)) {
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
            ItemStack offer = stored.copy();
            offer.stackSize = Math.min(count, offer.getMaxStackSize());
            insertStack((IInventory) neighbor, offer);
        }
    }

    /**
     * Merges as much of {@code stack} into {@code target} as the target accepts. The target slot
     * limit and the item stack limit are both respected; leftovers are simply discarded because the
     * generator keeps its source stack untouched.
     */
    private static void insertStack(IInventory target, ItemStack stack) {
        int limit = Math.min(target.getInventoryStackLimit(), stack.getMaxStackSize());
        int remaining = stack.stackSize;
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
            } else if (current.isItemEqual(stack)
                    && ItemStack.areItemStackTagsEqual(current, stack)) {
                int space = Math.min(limit, current.getMaxStackSize()) - current.stackSize;
                if (space <= 0) {
                    continue;
                }
                int amount = Math.min(remaining, space);
                current.stackSize += amount;
                remaining -= amount;
                target.setInventorySlotContents(slot, current);
            }
        }
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
            // The stored item is the source of the infinite supply; it must not be replaced.
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
        compound.setBoolean(KEY_INFINITE_MODE, this.infiniteModeEnabled);
        compound.setBoolean(KEY_OUTPUT_ENABLED, this.outputEnabled);
        compound.setInteger(KEY_OUTPUT_SIDES, this.outputSideMask);
        compound.setInteger(KEY_OUTPUT_RATE, this.outputItemsPerTick);
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
        this.outputSideMask = compound.hasKey(KEY_OUTPUT_SIDES, 3)
                ? compound.getInteger(KEY_OUTPUT_SIDES)
                : (1 << SIDE_COUNT) - 1;
        this.outputItemsPerTick = compound.hasKey(KEY_OUTPUT_RATE, 3)
                ? Math.max(1, compound.getInteger(KEY_OUTPUT_RATE))
                : DEFAULT_OUTPUT_RATE;
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

    /** Marks the chunk dirty and pushes a block update to nearby clients. */
    private void markUpdated() {
        this.markDirty();
        if (this.worldObj != null && !this.worldObj.isRemote) {
            this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
        }
    }
}

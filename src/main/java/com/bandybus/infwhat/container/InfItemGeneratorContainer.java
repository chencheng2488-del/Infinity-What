package com.bandybus.infwhat.container;

import com.bandybus.infwhat.block.entity.InfItemGeneratorTileEntity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * Container menu for the Infinity Item Generator: player inventory plus a single generator slot.
 * While infinite mode is enabled, left-click pulls one item and shift+left-click pulls a full
 * stack without ever depleting the source.
 */
public class InfItemGeneratorContainer extends Container {

    /** Generator slot origin on the GUI texture. */
    public static final int SLOT_X = 84;
    /** Generator slot origin on the GUI texture. */
    public static final int SLOT_Y = 48;

    private static final int PLAYER_SLOTS = 27;
    private static final int HOTBAR_SLOTS = 9;
    /** Index of the generator slot inside the container slot list. */
    public static final int GENERATOR_SLOT_INDEX = PLAYER_SLOTS + HOTBAR_SLOTS;

    /** Button id that toggles infinite mode. */
    public static final int BUTTON_TOGGLE_INFINITE = 0;
    /** Button id that toggles active output. */
    public static final int BUTTON_TOGGLE_OUTPUT = 1;
    /** Button id of the first face toggle; the six faces follow in ForgeDirection order. */
    public static final int BUTTON_TOGGLE_SIDE_BASE = 2;

    private final InfItemGeneratorTileEntity blockEntity;
    private final net.minecraft.world.World world;
    private final GeneratorSlot generatorSlot;

    public InfItemGeneratorContainer(InventoryPlayer playerInventory,
                                     InfItemGeneratorTileEntity tile) {
        this.blockEntity = tile;
        this.world = playerInventory.player.worldObj;

        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlotToContainer(new Slot(playerInventory, col + row * 9 + 9,
                        11 + col * 18, 99 + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col) {
            this.addSlotToContainer(new Slot(playerInventory, col, 11 + col * 18, 153));
        }

        this.generatorSlot = new GeneratorSlot(this.blockEntity, this.blockEntity, 0,
                SLOT_X, SLOT_Y);
        this.addSlotToContainer(this.generatorSlot);
    }

    /** Machine tile entity backing this menu. */
    public InfItemGeneratorTileEntity getTileEntity() {
        return this.blockEntity;
    }

    /** Whether the generator currently acts as an unlimited source. */
    public boolean isInfinite() {
        return this.blockEntity.isInfinite();
    }

    /** Whether the infinite-mode switch is on. */
    public boolean isInfiniteModeEnabled() {
        return this.blockEntity.isInfiniteModeEnabled();
    }

    /** Whether active output is switched on. */
    public boolean isOutputEnabled() {
        return this.blockEntity.isOutputEnabled();
    }

    /** Configured push amount per enabled face and tick. */
    public int getOutputItemsPerTick() {
        return this.blockEntity.getOutputItemsPerTick();
    }

    /** Whether active output pushes toward the given face. */
    public boolean isOutputSideEnabled(ForgeDirection direction) {
        return this.blockEntity.isOutputSideEnabled(direction);
    }

    /** The stored source item, or null while empty. */
    public ItemStack getStoredItem() {
        return this.blockEntity.getStoredItem();
    }

    /** Hides the generator slot while the I/O page of the GUI is open. */
    public void setGeneratorSlotVisible(boolean visible) {
        this.generatorSlot.setVisible(visible);
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return this.blockEntity.isUsable()
                && playerIn.getDistanceSq(this.blockEntity.xCoord + 0.5D,
                this.blockEntity.yCoord + 0.5D,
                this.blockEntity.zCoord + 0.5D) <= 64.0D;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        Slot slot = (Slot) this.inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) {
            return null;
        }
        ItemStack stack = slot.getStack();
        ItemStack copy = stack.copy();
        boolean moved;

        if (index == GENERATOR_SLOT_INDEX) {
            if (this.blockEntity.isInfinite()) {
                return null;
            }
            moved = this.mergeItemStack(stack, 0, PLAYER_SLOTS + HOTBAR_SLOTS, false);
        } else if (this.blockEntity.isInfinite()) {
            // While infinite the source slot is locked, so shift-clicking only shuffles the inventory.
            if (index < PLAYER_SLOTS) {
                moved = this.mergeItemStack(stack, PLAYER_SLOTS, PLAYER_SLOTS + HOTBAR_SLOTS, false);
            } else {
                moved = this.mergeItemStack(stack, 0, PLAYER_SLOTS, false);
            }
        } else {
            moved = this.mergeItemStack(stack, GENERATOR_SLOT_INDEX, GENERATOR_SLOT_INDEX + 1, false);
            if (!moved && index < PLAYER_SLOTS) {
                moved = this.mergeItemStack(stack, PLAYER_SLOTS, PLAYER_SLOTS + HOTBAR_SLOTS, false);
            } else if (!moved) {
                moved = this.mergeItemStack(stack, 0, PLAYER_SLOTS, false);
            }
        }
        if (!moved) {
            return null;
        }

        if (stack.stackSize <= 0) {
            slot.putStack(null);
        } else {
            slot.onSlotChanged();
        }
        return copy;
    }

    @Override
    public ItemStack slotClick(int slotId, int clickedButton, int mode, EntityPlayer player) {
        if (slotId == GENERATOR_SLOT_INDEX && this.blockEntity.isInfinite()) {
            if (!this.world.isRemote) {
                if (mode == 0 && clickedButton == 0) {
                    takeFromInfinite(player, 1, false);
                } else if (mode == 1 && clickedButton == 0) {
                    takeFromInfinite(player, getInfiniteStackSize(), true);
                }
                if (player instanceof EntityPlayerMP) {
                    ((EntityPlayerMP) player).sendContainerToPlayer(this);
                }
            }
            return player.inventory.getItemStack();
        }
        return super.slotClick(slotId, clickedButton, mode, player);
    }

    /** Full stack size the infinite source can hand out in one go. */
    private int getInfiniteStackSize() {
        ItemStack stored = this.blockEntity.getStoredItem();
        return stored == null || stored.stackSize <= 0 ? 1 : stored.getMaxStackSize();
    }

    /** Hands out copies of the stored item without consuming the source stack. */
    private void takeFromInfinite(EntityPlayer player, int amount, boolean toInventory) {
        ItemStack stored = this.blockEntity.getStoredItem();
        if (stored == null || stored.stackSize <= 0 || amount <= 0) {
            return;
        }
        ItemStack taken = stored.copy();
        taken.stackSize = Math.min(amount, taken.getMaxStackSize());

        if (toInventory) {
            if (!player.inventory.addItemStackToInventory(taken)) {
                player.dropPlayerItemWithRandomChoice(taken, false);
            }
            return;
        }
        ItemStack carried = player.inventory.getItemStack();
        if (carried == null || carried.stackSize <= 0) {
            player.inventory.setItemStack(taken);
        } else if (carried.isItemEqual(taken)
                && ItemStack.areItemStackTagsEqual(carried, taken)) {
            int space = carried.getMaxStackSize() - carried.stackSize;
            int moved = Math.min(space, taken.stackSize);
            if (moved > 0) {
                carried.stackSize += moved;
            }
            int leftover = taken.stackSize - moved;
            if (leftover > 0) {
                ItemStack rest = taken.copy();
                rest.stackSize = leftover;
                if (!player.inventory.addItemStackToInventory(rest)) {
                    player.dropPlayerItemWithRandomChoice(rest, false);
                }
            }
            player.inventory.setItemStack(carried);
        } else if (!player.inventory.addItemStackToInventory(taken)) {
            player.dropPlayerItemWithRandomChoice(taken, false);
        }
    }

    @Override
    public boolean enchantItem(EntityPlayer player, int id) {
        if (this.world.isRemote) {
            return true;
        }
        if (id == BUTTON_TOGGLE_INFINITE) {
            this.blockEntity.setInfiniteModeEnabled(!this.blockEntity.isInfiniteModeEnabled());
            return true;
        }
        if (id == BUTTON_TOGGLE_OUTPUT) {
            this.blockEntity.setOutputEnabled(!this.blockEntity.isOutputEnabled());
            return true;
        }
        int sideDataValue = id - BUTTON_TOGGLE_SIDE_BASE;
        if (sideDataValue >= 0 && sideDataValue < ForgeDirection.VALID_DIRECTIONS.length) {
            this.blockEntity.toggleOutputSide(ForgeDirection.VALID_DIRECTIONS[sideDataValue]);
            return true;
        }
        return false;
    }

    /**
     * Generator slot backed by the machine inventory. It only accepts allowed items, hands nothing
     * out while infinite mode is active, and can be moved off-screen by the GUI.
     */
    private static final class GeneratorSlot extends Slot {

        private final InfItemGeneratorTileEntity blockEntity;

        GeneratorSlot(InfItemGeneratorTileEntity blockEntity, IInventory inventory,
                      int index, int x, int y) {
            super(inventory, index, x, y);
            this.blockEntity = blockEntity;
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return InfItemGeneratorTileEntity.isAllowed(stack) && super.isItemValid(stack);
        }

        @Override
        public boolean canTakeStack(EntityPlayer playerIn) {
            return !this.blockEntity.isInfinite() && super.canTakeStack(playerIn);
        }

        @Override
        public void putStack(ItemStack stack) {
            if (this.blockEntity.isInfinite()) {
                return;
            }
            super.putStack(stack);
        }

        @Override
        public int getSlotStackLimit() {
            return 1;
        }

        /** Moves the click target off-screen while the I/O page is displayed. */
        void setVisible(boolean visible) {
            this.xDisplayPosition = visible ? SLOT_X : -1000;
            this.yDisplayPosition = visible ? SLOT_Y : -1000;
        }
    }
}

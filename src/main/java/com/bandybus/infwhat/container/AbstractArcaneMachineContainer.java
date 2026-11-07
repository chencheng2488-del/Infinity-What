package com.bandybus.infwhat.container;

import com.bandybus.infwhat.integration.thaumcraft4.AbstractArcaneMachineTileEntity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

/** Shared container layout and button handling for the two Thaumcraft 4 arcane machines. */
public abstract class AbstractArcaneMachineContainer extends Container {

    public static final int INPUT_SLOT_X = 57;
    public static final int INPUT_SLOT_Y = 26;
    public static final int TARGET_SLOT_X = 111;
    public static final int TARGET_SLOT_Y = 26;

    public static final int BUTTON_TOGGLE_INFINITE = 0;
    /** Button id that toggles active output. */
    public static final int BUTTON_TOGGLE_OUTPUT = 1;
    /** Button id of the first face toggle; the six faces follow in ForgeDirection order. */
    public static final int BUTTON_TOGGLE_SIDE_BASE = 2;
    /** Rate edits are encoded as this base plus the requested amount. */
    public static final int BUTTON_SET_RATE_BASE = 1_000_000;
    /** Radius edits are encoded as this base plus the requested size. */
    public static final int BUTTON_SET_RANGE_BASE = 2_000_000;
    /** Button id that toggles the persistent world-space range box. */
    public static final int BUTTON_TOGGLE_PREVIEW = 100;

    private static final int PLAYER_SLOTS = 27;
    private static final int HOTBAR_SLOTS = 9;
    public static final int INPUT_SLOT_INDEX = PLAYER_SLOTS + HOTBAR_SLOTS;
    public static final int TARGET_SLOT_INDEX = INPUT_SLOT_INDEX + 1;

    protected final AbstractArcaneMachineTileEntity tile;
    private final SampleSlot sampleSlot;
    private final TargetSlot targetSlot;

    protected AbstractArcaneMachineContainer(InventoryPlayer playerInventory,
                                             AbstractArcaneMachineTileEntity tile) {
        this.tile = tile;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlotToContainer(new Slot(playerInventory, column + row * 9 + 9,
                        11 + column * 18, 117 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            this.addSlotToContainer(new Slot(playerInventory, column, 11 + column * 18, 175));
        }
        this.sampleSlot = new SampleSlot(tile, tile, 0, INPUT_SLOT_X, INPUT_SLOT_Y);
        this.targetSlot = new TargetSlot(tile, tile, 1, TARGET_SLOT_X, TARGET_SLOT_Y);
        this.addSlotToContainer(this.sampleSlot);
        this.addSlotToContainer(this.targetSlot);
    }

    public AbstractArcaneMachineTileEntity getTileEntity() {
        return this.tile;
    }

    public boolean isInfinite() {
        return this.tile.isInfinite();
    }

    /** Whether active output is switched on. */
    public boolean isOutputEnabled() {
        return this.tile.isOutputEnabled();
    }

    /** Configured push amount per enabled face and tick. */
    public int getOutputItemsPerTick() {
        return this.tile.getOutputItemsPerTick();
    }

    /** Whether active output pushes toward the given face. */
    public boolean isOutputSideEnabled(ForgeDirection direction) {
        return this.tile.isOutputSideEnabled(direction);
    }

    /** Jar refill radius used by the emanator. */
    public int getRefillRadius() {
        return this.tile.getRefillRadius();
    }

    /** Whether the emanator keeps its range box visible in the world. */
    public boolean isRangePreviewEnabled() {
        return this.tile.isRangePreviewEnabled();
    }

    /** Hides the machine slots while the I/O page of the GUI is open. */
    public void setMachineSlotsVisible(boolean visible) {
        this.sampleSlot.setVisible(visible);
        this.targetSlot.setVisible(visible);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return this.tile.isUsable() && player.getDistanceSq(this.tile.xCoord + 0.5D,
                this.tile.yCoord + 0.5D, this.tile.zCoord + 0.5D) <= 64.0D;
    }

    @Override
    public boolean enchantItem(EntityPlayer player, int id) {
        if (player.worldObj.isRemote) {
            return true;
        }
        if (id == BUTTON_TOGGLE_INFINITE) {
            this.tile.setInfiniteEnabled(!this.tile.isInfinite());
            return true;
        }
        if (id == BUTTON_TOGGLE_OUTPUT) {
            this.tile.setOutputEnabled(!this.tile.isOutputEnabled());
            return true;
        }
        if (id == BUTTON_TOGGLE_PREVIEW) {
            return this.tile.setRangePreviewEnabled(!this.tile.isRangePreviewEnabled());
        }
        int sideIndex = id - BUTTON_TOGGLE_SIDE_BASE;
        if (sideIndex >= 0 && sideIndex < ForgeDirection.VALID_DIRECTIONS.length) {
            this.tile.toggleOutputSide(ForgeDirection.VALID_DIRECTIONS[sideIndex]);
            return true;
        }
        int amount = id - BUTTON_SET_RATE_BASE;
        if (amount >= 1 && amount <= AbstractArcaneMachineTileEntity.MAX_OUTPUT_RATE) {
            this.tile.setOutputItemsPerTick(amount);
            return true;
        }
        int radius = id - BUTTON_SET_RANGE_BASE;
        if (radius >= 1) {
            return this.tile.setRefillRadius(radius);
        }
        return false;
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
        if (index == INPUT_SLOT_INDEX || index == TARGET_SLOT_INDEX) {
            moved = this.mergeItemStack(stack, 0, PLAYER_SLOTS + HOTBAR_SLOTS, false);
        } else if (index < PLAYER_SLOTS) {
            moved = this.mergeItemStack(stack, INPUT_SLOT_INDEX, TARGET_SLOT_INDEX + 1, false);
            if (!moved) {
                moved = this.mergeItemStack(stack, PLAYER_SLOTS, PLAYER_SLOTS + HOTBAR_SLOTS,
                        false);
            }
        } else {
            moved = this.mergeItemStack(stack, INPUT_SLOT_INDEX, TARGET_SLOT_INDEX + 1, false);
            if (!moved) {
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

    /** Sample slot: freely editable while the machine is not providing, locked while it is. */
    private static final class SampleSlot extends Slot {

        private final AbstractArcaneMachineTileEntity tile;

        SampleSlot(AbstractArcaneMachineTileEntity tile, IInventory inventory,
                   int index, int x, int y) {
            super(inventory, index, x, y);
            this.tile = tile;
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return this.tile.isItemValidForSlot(0, stack);
        }

        @Override
        public boolean canTakeStack(EntityPlayer player) {
            return !this.tile.isProviding();
        }

        @Override
        public void putStack(ItemStack stack) {
            if (this.tile.isProviding()) {
                return;
            }
            super.putStack(stack);
        }

        void setVisible(boolean visible) {
            this.xDisplayPosition = visible ? INPUT_SLOT_X : -1000;
            this.yDisplayPosition = visible ? INPUT_SLOT_Y : -1000;
        }
    }

    /** Target slot: contents are defined by the machine subclass. */
    private static final class TargetSlot extends Slot {

        private final AbstractArcaneMachineTileEntity tile;

        TargetSlot(AbstractArcaneMachineTileEntity tile, IInventory inventory,
                   int index, int x, int y) {
            super(inventory, index, x, y);
            this.tile = tile;
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return this.tile.isItemValidForSlot(1, stack);
        }

        void setVisible(boolean visible) {
            this.xDisplayPosition = visible ? TARGET_SLOT_X : -1000;
            this.yDisplayPosition = visible ? TARGET_SLOT_Y : -1000;
        }
    }
}

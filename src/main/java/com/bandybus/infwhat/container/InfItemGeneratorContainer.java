package com.bandybus.infwhat.container;

import com.bandybus.infwhat.block.entity.InfItemGeneratorTileEntity;
import com.bandybus.infwhat.config.InfinityWhatConfig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/**
 * Container menu for the Infinity Item Generator: the player inventory plus a single generator
 * slot. The slot always holds exactly one item; while infinite mode is enabled the item is an
 * unlimited source. Manual retrieval is handled in {@link #slotClick}: left-click gives one item,
 * shift+left-click gives a full stack, and the source is never depleted.
 */
public class InfItemGeneratorContainer extends Container {

    public static final int SLOT_X = 84;
    public static final int SLOT_Y = 48;

    private static final int PLAYER_SLOTS = 27;
    private static final int HOTBAR_SLOTS = 9;
    public static final int GENERATOR_SLOT_INDEX = PLAYER_SLOTS + HOTBAR_SLOTS;

    public static final int BUTTON_TOGGLE_INFINITE = 0;
    public static final int BUTTON_TOGGLE_OUTPUT = 1;
    public static final int BUTTON_TOGGLE_SIDE_BASE = 2;

    private final InfItemGeneratorTileEntity blockEntity;
    private final net.minecraft.world.World world;
    private final GeneratorSlot generatorSlot;

    public InfItemGeneratorContainer(InventoryPlayer playerInventory,
                                     InfItemGeneratorTileEntity tile) {
        this.blockEntity = tile;
        this.world = playerInventory.player.world;

        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlotToContainer(new Slot(playerInventory, col + row * 9 + 9,
                        11 + col * 18, 99 + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col) {
            this.addSlotToContainer(new Slot(playerInventory, col, 11 + col * 18, 153));
        }

        this.generatorSlot = new GeneratorSlot(this.blockEntity,
                this.blockEntity.getItemHandler(), 0, SLOT_X, SLOT_Y);
        this.addSlotToContainer(this.generatorSlot);
    }

    public InfItemGeneratorTileEntity getTileEntity() {
        return this.blockEntity;
    }

    public boolean isInfinite() {
        return this.blockEntity.isInfinite();
    }

    public boolean isInfiniteModeEnabled() {
        return this.blockEntity.isInfiniteModeEnabled();
    }

    public boolean isOutputEnabled() {
        return this.blockEntity.isOutputEnabled();
    }

    public int getOutputItemsPerTick() {
        return this.blockEntity.getOutputItemsPerTick();
    }

    public boolean isOutputSideEnabled(EnumFacing direction) {
        return this.blockEntity.isOutputSideEnabled(direction);
    }

    public ItemStack getStoredItem() {
        return this.blockEntity.getStoredItem();
    }

    /** Hides the stored-item slot while the screen shows the separate I/O settings page. */
    public void setGeneratorSlotVisible(boolean visible) {
        this.generatorSlot.setVisible(visible);
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return this.blockEntity.isUsable()
                && playerIn.getDistanceSq(this.blockEntity.getPos().getX() + 0.5D,
                this.blockEntity.getPos().getY() + 0.5D,
                this.blockEntity.getPos().getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        Slot slot = this.inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getStack();
        ItemStack copy = stack.copy();
        boolean moved;

        if (index == GENERATOR_SLOT_INDEX) {
            if (this.blockEntity.isInfinite()) {
                return ItemStack.EMPTY;
            }
            moved = this.mergeItemStack(stack, 0, PLAYER_SLOTS + HOTBAR_SLOTS, false);
        } else {
            moved = this.mergeItemStack(stack, GENERATOR_SLOT_INDEX, GENERATOR_SLOT_INDEX + 1, false);
            if (!moved && index < PLAYER_SLOTS) {
                moved = this.mergeItemStack(stack, PLAYER_SLOTS, PLAYER_SLOTS + HOTBAR_SLOTS, false);
            } else if (!moved) {
                moved = this.mergeItemStack(stack, 0, PLAYER_SLOTS, false);
            }
        }
        if (!moved) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.putStack(ItemStack.EMPTY);
        } else {
            slot.onSlotChanged();
        }
        return copy;
    }

    @Override
    public ItemStack slotClick(int slotId, int dragType, ClickType clickType, EntityPlayer player) {
        if (slotId == GENERATOR_SLOT_INDEX && this.blockEntity.isInfinite()) {
            if (!this.world.isRemote) {
                if (clickType == ClickType.PICKUP && dragType == 0) {
                    takeFromInfinite(player, 1, false);
                } else if (clickType == ClickType.QUICK_MOVE && dragType == 0) {
                    takeFromInfinite(player, getInfiniteStackSize(), true);
                }
                if (player instanceof EntityPlayerMP) {
                    ((EntityPlayerMP) player).sendContainerToPlayer(this);
                }
            }
            return player.inventory.getItemStack();
        }
        return super.slotClick(slotId, dragType, clickType, player);
    }

    private int getInfiniteStackSize() {
        ItemStack stored = this.blockEntity.getStoredItem();
        return stored.isEmpty() ? 1 : stored.getMaxStackSize();
    }

    /**
     * Pulls items out of the infinite source and gives them to the player, either onto the cursor
     * or straight into the inventory (shift-click). The source itself is never depleted.
     */
    private void takeFromInfinite(EntityPlayer player, int amount, boolean toInventory) {
        ItemStack taken = this.blockEntity.getItemHandler().extractItem(0, amount, false);
        if (taken.isEmpty()) {
            return;
        }
        if (toInventory) {
            if (!player.inventory.addItemStackToInventory(taken)) {
                player.dropItem(taken, false);
            }
            return;
        }
        ItemStack carried = player.inventory.getItemStack();
        if (carried.isEmpty()) {
            player.inventory.setItemStack(taken);
        } else if (ItemStack.areItemsEqual(carried, taken)
                && ItemStack.areItemStackTagsEqual(carried, taken)) {
            int space = carried.getMaxStackSize() - carried.getCount();
            int moved = Math.min(space, taken.getCount());
            if (moved > 0) {
                carried.grow(moved);
            }
            int leftover = taken.getCount() - moved;
            if (leftover > 0) {
                ItemStack rest = taken.copy();
                rest.setCount(leftover);
                if (!player.inventory.addItemStackToInventory(rest)) {
                    player.dropItem(rest, false);
                }
            }
            player.inventory.setItemStack(carried);
        } else if (!player.inventory.addItemStackToInventory(taken)) {
            player.dropItem(taken, false);
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
        if (sideDataValue >= 0 && sideDataValue < EnumFacing.values().length) {
            this.blockEntity.toggleOutputSide(EnumFacing.byIndex(sideDataValue));
            return true;
        }
        return false;
    }

    /** Slot backed by the generator's item handler. */
    private static final class GeneratorSlot extends SlotItemHandler {

        private final InfItemGeneratorTileEntity blockEntity;
        private boolean visible = true;

        GeneratorSlot(InfItemGeneratorTileEntity blockEntity, IItemHandler itemHandler,
                      int index, int x, int y) {
            super(itemHandler, index, x, y);
            this.blockEntity = blockEntity;
        }

        @Override
        public boolean canTakeStack(EntityPlayer playerIn) {
            return !this.blockEntity.isInfinite() && super.canTakeStack(playerIn);
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            String id = stack.isEmpty() || stack.getItem() == null ? null
                    : ForgeRegistries.ITEMS.getKey(stack.getItem()) == null ? null
                    : ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
            return InfinityWhatConfig.isItemAllowed(id) && super.isItemValid(stack);
        }

        @Override
        public void putStack(ItemStack stack) {
            // The infinite template is locked: vanilla slot updates must not replace it.
            if (this.blockEntity.isInfinite()) {
                return;
            }
            super.putStack(stack);
        }

        @Override
        public boolean isEnabled() {
            return this.visible;
        }

        void setVisible(boolean visible) {
            this.visible = visible;
            this.xPos = visible ? SLOT_X : -1000;
            this.yPos = visible ? SLOT_Y : -1000;
        }
    }
}

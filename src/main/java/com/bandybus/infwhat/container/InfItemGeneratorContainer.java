package com.bandybus.infwhat.container;

import com.bandybus.infwhat.block.RedstoneMode;
import com.bandybus.infwhat.block.entity.InfItemGeneratorTileEntity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.SlotItemHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Container menu for the Infinity Item Generator: player inventory plus a single generator slot.
 * While infinite mode is enabled, left-click pulls one item and shift+left-click pulls a full
 * stack without ever depleting the source.
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
    public static final int BUTTON_REDSTONE_MODE_BASE = 8;

    private final InfItemGeneratorTileEntity blockEntity;
    private final net.minecraft.world.World world;
    private final GeneratorSlot generatorSlot;
    private final List<VisibilitySlot> playerSlots = new ArrayList<VisibilitySlot>();

    public InfItemGeneratorContainer(InventoryPlayer playerInventory,
                                     InfItemGeneratorTileEntity tile) {
        this.blockEntity = tile;
        this.world = playerInventory.player.worldObj;

        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                VisibilitySlot slot = new VisibilitySlot(playerInventory, col + row * 9 + 9,
                        11 + col * 18, 99 + row * 18);
                this.playerSlots.add(slot);
                this.addSlotToContainer(slot);
            }
        }
        for (int col = 0; col < 9; ++col) {
            VisibilitySlot slot = new VisibilitySlot(playerInventory, col, 11 + col * 18, 153);
            this.playerSlots.add(slot);
            this.addSlotToContainer(slot);
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

    public int getUniformSideRate() {
        return this.blockEntity.getUniformSideRate();
    }

    public boolean hasMixedSideRates() {
        return this.blockEntity.hasMixedSideRates();
    }

    public int getSideRate(EnumFacing direction) {
        return this.blockEntity.getSideRate(direction);
    }

    public long getTotalEffectiveRate() {
        return this.blockEntity.getTotalEffectiveRate();
    }

    public boolean isOutputSideEnabled(EnumFacing direction) {
        return this.blockEntity.isOutputSideEnabled(direction);
    }

    public RedstoneMode getRedstoneMode() {
        return this.blockEntity.getRedstoneMode();
    }

    public int getPulseOnTicks() {
        return this.blockEntity.getPulseOnTicks();
    }

    public int getPulseOffTicks() {
        return this.blockEntity.getPulseOffTicks();
    }

    public int getBatchSize() {
        return this.blockEntity.getBatchSize();
    }

    public ItemStack getStoredItem() {
        return this.blockEntity.getStoredItem();
    }

    public void setGeneratorSlotVisible(boolean visible) {
        this.generatorSlot.setVisible(visible);
    }

    /** Hides the player inventory while a face detail page is open. */
    public void setPlayerSlotsVisible(boolean visible) {
        for (VisibilitySlot slot : this.playerSlots) {
            slot.setVisible(visible);
        }
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
    public ItemStack slotClick(int slotId, int clickedButton, ClickType mode, EntityPlayer player) {
        if (slotId == GENERATOR_SLOT_INDEX && this.blockEntity.isInfinite()) {
            if (!this.world.isRemote) {
                if (mode == ClickType.PICKUP && clickedButton == 0) {
                    takeFromInfinite(player, 1, false);
                } else if (mode == ClickType.QUICK_MOVE && clickedButton == 0) {
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

    private int getInfiniteStackSize() {
        ItemStack stored = this.blockEntity.getStoredItem();
        return stored == null || stored.stackSize <= 0 ? 1 : stored.getMaxStackSize();
    }

    private void takeFromInfinite(EntityPlayer player, int amount, boolean toInventory) {
        ItemStack taken = this.blockEntity.getItemHandler().extractItem(0, amount, false);
        if (taken == null || taken.stackSize <= 0) {
            return;
        }
        if (toInventory) {
            if (!player.inventory.addItemStackToInventory(taken)) {
                player.dropItem(taken, false);
            }
            return;
        }
        ItemStack carried = player.inventory.getItemStack();
        if (carried == null || carried.stackSize <= 0) {
            player.inventory.setItemStack(taken);
        } else if (ItemHandlerHelper.canItemStacksStack(carried, taken)) {
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
            this.blockEntity.toggleOutputSide(EnumFacing.values()[sideDataValue]);
            return true;
        }
        int redstoneMode = id - BUTTON_REDSTONE_MODE_BASE;
        if (redstoneMode >= 0 && redstoneMode < RedstoneMode.values().length) {
            this.blockEntity.setRedstoneMode(RedstoneMode.byId(redstoneMode));
            return true;
        }
        return false;
    }

    private static final class GeneratorSlot extends SlotItemHandler {

        private final InfItemGeneratorTileEntity blockEntity;
        private boolean visible = true;

        GeneratorSlot(InfItemGeneratorTileEntity blockEntity, IItemHandler itemHandler,
                      int index, int x, int y) {
            super(itemHandler, index, x, y);
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

        void setVisible(boolean visible) {
            this.visible = visible;
            this.xDisplayPosition = visible ? SLOT_X : -10000;
            this.yDisplayPosition = visible ? SLOT_Y : -10000;
        }
    }
}

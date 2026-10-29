package com.bandybus.infwhat.container;

import com.bandybus.infwhat.block.entity.InfFluidGeneratorTileEntity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.InvWrapper;

/**
 * Container menu for the Infinity Fluid Generator. The machine itself has no item slots; a
 * synthetic off-screen slot is used only as the click target for bucket interactions with the
 * tank, mirroring the modern release.
 */
public class InfFluidGeneratorContainer extends Container {

    public static final int TANK_X = 26;
    public static final int TANK_Y = 23;
    public static final int TANK_WIDTH = 16;
    public static final int TANK_HEIGHT = 40;

    public static final int BUTTON_TOGGLE_OUTPUT = 0;
    public static final int BUTTON_TOGGLE_SIDE_BASE = 1;
    public static final int BUTTON_CLEAR_FLUID = 7;

    private static final int PLAYER_SLOTS = 27;
    private static final int HOTBAR_SLOTS = 9;
    public static final int FLUID_SLOT_INDEX = PLAYER_SLOTS + HOTBAR_SLOTS;

    private final InfFluidGeneratorTileEntity blockEntity;
    private final net.minecraft.world.World world;

    public InfFluidGeneratorContainer(InventoryPlayer playerInventory,
                                      InfFluidGeneratorTileEntity tile) {
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

        // Synthetic tank slot: inactive and off-screen, only reachable from explicit clicks
        // routed by the screen.
        this.addSlotToContainer(new FluidSyntheticSlot(-100, -100));
    }

    public InfFluidGeneratorTileEntity getTileEntity() {
        return this.blockEntity;
    }

    public boolean isOutputSideEnabled(EnumFacing direction) {
        return this.blockEntity.isOutputSideEnabled(direction);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        Slot slot = this.inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getStack();
        ItemStack copy = stack.copy();

        if (index < PLAYER_SLOTS) {
            if (!this.mergeItemStack(stack, PLAYER_SLOTS, PLAYER_SLOTS + HOTBAR_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.mergeItemStack(stack, 0, PLAYER_SLOTS, false)) {
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
    public boolean canInteractWith(EntityPlayer playerIn) {
        return this.blockEntity.isUsable()
                && playerIn.getDistanceSq(this.blockEntity.getPos().getX() + 0.5D,
                this.blockEntity.getPos().getY() + 0.5D,
                this.blockEntity.getPos().getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public ItemStack slotClick(int slotId, int dragType, ClickType clickType, EntityPlayer player) {
        if (slotId == FLUID_SLOT_INDEX && clickType == ClickType.PICKUP
                && (dragType == 0 || dragType == 1)) {
            // The client also sends this click, but only the server mutates the tank.
            if (!this.world.isRemote) {
                handleFluidContainer(player);
            }
            return ItemStack.EMPTY;
        }
        return super.slotClick(slotId, dragType, clickType, player);
    }

    /**
     * Direct bucket interaction with the tank: the held container is filled from the tank, or poured into it as a fallback.
     */
    private void handleFluidContainer(EntityPlayer player) {
        ItemStack carried = player.inventory.getItemStack();
        if (carried.isEmpty()) {
            return;
        }
        IFluidHandler tank = this.blockEntity.getFluidHandler();
        FluidActionResult result = FluidUtil.tryFillContainer(carried, tank, Integer.MAX_VALUE, player, true);
        if (!result.isSuccess()) {
            result = FluidUtil.tryEmptyContainer(carried, tank, Integer.MAX_VALUE, player, true);
        }
        if (!result.isSuccess()) {
            return;
        }

        ItemStack resultStack = result.getResult();
        if (carried.getCount() <= 1) {
            player.inventory.setItemStack(resultStack);
        } else {
            ItemStack remaining = carried.copy();
            remaining.shrink(1);
            ItemStack leftover = ItemHandlerHelper.insertItemStacked(
                    new InvWrapper(player.inventory), resultStack, false);
            if (!leftover.isEmpty()) {
                player.dropItem(leftover, false);
            }
            player.inventory.setItemStack(remaining);
        }
        if (player instanceof EntityPlayerMP) {
            // Cursor stacks are not tracked by the slot list, so resend the whole window.
            ((EntityPlayerMP) player).sendContainerToPlayer(this);
        }
    }

    @Override
    public boolean enchantItem(EntityPlayer player, int id) {
        if (this.world.isRemote) {
            return true;
        }
        if (id == BUTTON_TOGGLE_OUTPUT) {
            this.blockEntity.setOutputEnabled(!this.blockEntity.isOutputEnabled());
            return true;
        }
        if (id == BUTTON_CLEAR_FLUID) {
            this.blockEntity.clearFluid();
            return true;
        }
        int sideDataValue = id - BUTTON_TOGGLE_SIDE_BASE;
        if (sideDataValue >= 0 && sideDataValue < EnumFacing.values().length) {
            this.blockEntity.toggleOutputSide(EnumFacing.byIndex(sideDataValue));
            return true;
        }
        return false;
    }

    private static final class FluidSyntheticSlot extends Slot {

        FluidSyntheticSlot(int x, int y) {
            super(new InventoryBasic("", false, 1), 0, x, y);
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return false;
        }

        @Override
        public boolean canTakeStack(EntityPlayer playerIn) {
            return false;
        }

        @Override
        public boolean isEnabled() {
            return false;
        }
    }
}

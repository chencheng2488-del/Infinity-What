package com.bandybus.infwhat.container;

import com.bandybus.infwhat.block.entity.InfFluidGeneratorTileEntity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.IFluidHandler;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * Container menu for the Infinity Fluid Generator. The machine has no item slots; a synthetic
 * off-screen slot is only the click target for bucket interactions with the tank.
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
            return null;
        }
        ItemStack stack = slot.getStack();
        ItemStack copy = stack.copy();

        if (index < PLAYER_SLOTS) {
            if (!this.mergeItemStack(stack, PLAYER_SLOTS, PLAYER_SLOTS + HOTBAR_SLOTS, false)) {
                return null;
            }
        } else if (!this.mergeItemStack(stack, 0, PLAYER_SLOTS, false)) {
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
    public boolean canInteractWith(EntityPlayer playerIn) {
        return this.blockEntity.isUsable()
                && playerIn.getDistanceSq(this.blockEntity.getPos().getX() + 0.5D,
                this.blockEntity.getPos().getY() + 0.5D,
                this.blockEntity.getPos().getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public ItemStack slotClick(int slotId, int clickedButton, int mode, EntityPlayer player) {
        if (slotId == FLUID_SLOT_INDEX && mode == 0 && (clickedButton == 0 || clickedButton == 1)) {
            if (!this.world.isRemote) {
                handleFluidContainer(player);
            }
            return player.inventory.getItemStack();
        }
        return super.slotClick(slotId, clickedButton, mode, player);
    }

    /** Fills the held container from the tank first; pouring it into the tank is the fallback. */
    private void handleFluidContainer(EntityPlayer player) {
        ItemStack carried = player.inventory.getItemStack();
        if (carried == null || carried.stackSize <= 0) {
            return;
        }
        IFluidHandler tank = this.blockEntity;
        ItemStack result = FluidUtil.tryFillBucket(carried, tank, EnumFacing.UP);
        if (result == null) {
            result = FluidUtil.tryEmptyBucket(carried, tank, EnumFacing.UP);
        }
        if (result == null) {
            return;
        }

        if (carried.stackSize <= 1) {
            player.inventory.setItemStack(result);
        } else {
            ItemStack remaining = carried.copy();
            remaining.stackSize -= 1;
            if (!player.inventory.addItemStackToInventory(result)) {
                player.dropPlayerItemWithRandomChoice(result, false);
            }
            player.inventory.setItemStack(remaining);
        }
        if (player instanceof EntityPlayerMP) {
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
            this.blockEntity.toggleOutputSide(EnumFacing.values()[sideDataValue]);
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
        public int getSlotStackLimit() {
            return 0;
        }
    }
}

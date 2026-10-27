package com.bandybus.infwhat.container;

import com.bandybus.infwhat.block.entity.InfFluidGeneratorTileEntity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemBucket;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;

/**
 * Container menu for the Infinity Fluid Generator. The machine has no item slots; a synthetic
 * off-screen slot is only the click target for bucket interactions with the tank.
 *
 * <p>1.7.10 has no {@code FluidUtil}, so bucket handling goes through
 * {@link FluidContainerRegistry}, the {@link IFluidContainerItem} interface and a direct vanilla
 * bucket special case.
 */
public class InfFluidGeneratorContainer extends Container {

    /** Tank overlay origin on the GUI texture. */
    public static final int TANK_X = 26;
    /** Tank overlay origin on the GUI texture. */
    public static final int TANK_Y = 23;
    /** Tank overlay width in pixels. */
    public static final int TANK_WIDTH = 16;
    /** Tank overlay height in pixels. */
    public static final int TANK_HEIGHT = 40;

    /** Button id that toggles active output. */
    public static final int BUTTON_TOGGLE_OUTPUT = 0;
    /** Button id of the first face toggle; the six faces follow in ForgeDirection order. */
    public static final int BUTTON_TOGGLE_SIDE_BASE = 1;
    /** Button id that empties the tank immediately. */
    public static final int BUTTON_CLEAR_FLUID = 7;

    private static final int PLAYER_SLOTS = 27;
    private static final int HOTBAR_SLOTS = 9;
    /** Index of the synthetic off-screen slot used for tank clicks. */
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

    /** Machine tile entity backing this menu. */
    public InfFluidGeneratorTileEntity getTileEntity() {
        return this.blockEntity;
    }

    /** Whether active output pushes toward the given face. */
    public boolean isOutputSideEnabled(ForgeDirection direction) {
        return this.blockEntity.isOutputSideEnabled(direction);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        Slot slot = (Slot) this.inventorySlots.get(index);
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
                && playerIn.getDistanceSq(this.blockEntity.xCoord + 0.5D,
                this.blockEntity.yCoord + 0.5D,
                this.blockEntity.zCoord + 0.5D) <= 64.0D;
    }

    @Override
    public ItemStack slotClick(int slotId, int clickedButton, int mode, EntityPlayer player) {
        if (slotId == FLUID_SLOT_INDEX && mode == 0 && (clickedButton == 0 || clickedButton == 1)) {
            if (!this.world.isRemote) {
                handleFluidContainer(player, clickedButton == 1);
            }
            return player.inventory.getItemStack();
        }
        return super.slotClick(slotId, clickedButton, mode, player);
    }

    /** Right-click empties the held container into the tank; left-click fills it from the tank. */
    private void handleFluidContainer(EntityPlayer player, boolean emptyContainer) {
        ItemStack carried = player.inventory.getItemStack();
        if (carried == null || carried.stackSize <= 0) {
            return;
        }
        ItemStack result = emptyContainer
                ? fillTankFromContainer(carried)
                : fillContainerFromTank(carried);
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

    /** Moves a filled container into the tank and returns the emptied container. */
    private ItemStack fillTankFromContainer(ItemStack carried) {
        FluidStack contained = FluidContainerRegistry.getFluidForFilledItem(carried);
        if (contained == null || contained.amount <= 0) {
            return null;
        }
        if (this.blockEntity.fill(ForgeDirection.UP, contained, false) < contained.amount) {
            return null;
        }
        ItemStack emptied = emptyContainerOf(carried);
        if (emptied == null) {
            return null;
        }
        this.blockEntity.fill(ForgeDirection.UP, contained, true);
        return emptied;
    }

    /** Resolves the empty container that matches a filled one. */
    private static ItemStack emptyContainerOf(ItemStack filled) {
        if (filled.getItem() instanceof ItemBucket) {
            return new ItemStack(Items.bucket);
        }
        ItemStack registered = FluidContainerRegistry.drainFluidContainer(filled);
        if (registered != null) {
            return registered;
        }
        if (filled.getItem() instanceof IFluidContainerItem) {
            ItemStack copy = filled.copy();
            ((IFluidContainerItem) copy.getItem()).drain(copy, Integer.MAX_VALUE, true);
            return copy;
        }
        return null;
    }

    /** Moves fluid from the tank into an empty container and returns the filled container. */
    private ItemStack fillContainerFromTank(ItemStack carried) {
        FluidStack available = this.blockEntity.drain(ForgeDirection.UP, Integer.MAX_VALUE, false);
        if (available == null || available.amount <= 0) {
            return null;
        }
        if (carried.getItem() instanceof IFluidContainerItem) {
            ItemStack copy = carried.copy();
            IFluidContainerItem container = (IFluidContainerItem) copy.getItem();
            int accepted = container.fill(copy, available, false);
            if (accepted <= 0 || this.blockEntity.drain(ForgeDirection.UP, accepted, true) == null) {
                return null;
            }
            container.fill(copy, new FluidStack(available, accepted), true);
            return copy;
        }
        ItemStack filledStack = FluidContainerRegistry.fillFluidContainer(available, carried);
        if (filledStack == null) {
            return null;
        }
        FluidStack content = FluidContainerRegistry.getFluidForFilledItem(filledStack);
        if (content == null || content.amount <= 0
                || this.blockEntity.drain(ForgeDirection.UP, content.amount, true) == null) {
            return null;
        }
        return filledStack;
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
        if (sideDataValue >= 0 && sideDataValue < ForgeDirection.VALID_DIRECTIONS.length) {
            this.blockEntity.toggleOutputSide(ForgeDirection.VALID_DIRECTIONS[sideDataValue]);
            return true;
        }
        return false;
    }

    /**
     * Off-screen slot that only serves as a click target for tank interactions; it never stores or
     * hands out items.
     */
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

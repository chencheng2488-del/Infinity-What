package com.bandybus.infwhat.container;

import com.bandybus.infwhat.block.RedstoneMode;
import com.bandybus.infwhat.block.entity.InfFluidGeneratorTileEntity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBucket;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Container menu for the Infinity Fluid Generator. The machine has no item slots; a synthetic
 * off-screen slot is only the click target for bucket interactions with the tank.
 *
 * <p>1.6.4 has no {@code FluidUtil}, so bucket handling goes through
 * {@link FluidContainerRegistry}, the {@link IFluidContainerItem} interface and a direct vanilla
 * bucket special case.
 */
public class InfFluidGeneratorContainer extends Container {

    public static final int TANK_X = 26;
    public static final int TANK_Y = 23;
    public static final int TANK_WIDTH = 16;
    public static final int TANK_HEIGHT = 40;

    public static final int BUTTON_TOGGLE_OUTPUT = 0;
    public static final int BUTTON_TOGGLE_SIDE_BASE = 1;
    public static final int BUTTON_CLEAR_FLUID = 7;
    public static final int BUTTON_REDSTONE_MODE_BASE = 8;

    private static final int PLAYER_SLOTS = 27;
    private static final int HOTBAR_SLOTS = 9;
    public static final int FLUID_SLOT_INDEX = PLAYER_SLOTS + HOTBAR_SLOTS;

    private final InfFluidGeneratorTileEntity blockEntity;
    private final net.minecraft.world.World world;
    private final List<VisibilitySlot> playerSlots = new ArrayList<VisibilitySlot>();

    public InfFluidGeneratorContainer(InventoryPlayer playerInventory,
                                      InfFluidGeneratorTileEntity tile) {
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

        this.addSlotToContainer(new FluidSyntheticSlot(-100, -100));
    }

    public InfFluidGeneratorTileEntity getTileEntity() {
        return this.blockEntity;
    }

    public void setPlayerSlotsVisible(boolean visible) {
        for (VisibilitySlot slot : this.playerSlots) {
            slot.setVisible(visible);
        }
    }

    public FluidStack getFluidStack() {
        return this.blockEntity.getStoredFluid();
    }

    public boolean isInfinite() {
        return this.blockEntity.isInfinite();
    }

    public long getAmount() {
        return this.blockEntity.getStoredAmount();
    }

    public boolean isOutputEnabled() {
        return this.blockEntity.isOutputEnabled();
    }

    public long getSideRateMb(ForgeDirection direction) {
        return this.blockEntity.getSideRateMb(direction);
    }

    public long getTotalEffectiveRateMb() {
        return this.blockEntity.getTotalEffectiveRateMb();
    }

    public boolean isOutputSideEnabled(ForgeDirection direction) {
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

    public long getUniformSideRateMb() {
        return this.blockEntity.getUniformSideRateMb();
    }

    public boolean hasMixedSideRates() {
        return this.blockEntity.hasMixedSideRates();
    }

    public float getFillRatio() {
        return this.blockEntity.getFillRatio();
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
        ItemStack result = fillContainerFromTank(carried);
        if (result == null) {
            result = fillTankFromContainer(carried);
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

    /**
     * 1.6.4 has no {@code drainFluidContainer}; the registered container data is searched for the
     * matching filled stack instead.
     */
    private static ItemStack emptyContainerOf(ItemStack filled) {
        if (filled.getItem() instanceof ItemBucket) {
            return new ItemStack(Item.bucketEmpty);
        }
        if (filled.getItem() instanceof IFluidContainerItem) {
            ItemStack copy = filled.copy();
            ((IFluidContainerItem) copy.getItem()).drain(copy, Integer.MAX_VALUE, true);
            return copy;
        }
        FluidContainerRegistry.FluidContainerData[] data =
                FluidContainerRegistry.getRegisteredFluidContainerData();
        for (FluidContainerRegistry.FluidContainerData entry : data) {
            if (entry != null && entry.filledContainer != null && entry.emptyContainer != null
                    && entry.filledContainer.isItemEqual(filled)) {
                ItemStack empty = entry.emptyContainer.copy();
                empty.stackSize = 1;
                return empty;
            }
        }
        return null;
    }

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
        int redstoneMode = id - BUTTON_REDSTONE_MODE_BASE;
        if (redstoneMode >= 0 && redstoneMode < RedstoneMode.values().length) {
            this.blockEntity.setRedstoneMode(RedstoneMode.byId(redstoneMode));
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

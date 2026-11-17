package com.bandybus.infwhat.menu;

import com.bandybus.infwhat.block.ModBlocks;
import com.bandybus.infwhat.block.RedstoneMode;
import com.bandybus.infwhat.block.entity.InfFluidGeneratorBlockEntity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SPacketSetSlot;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import com.bandybus.infwhat.fluid.InfFluidKind;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Container menu for the Infinity Fluid Generator. There are no item slots for the machine
 * itself, only the player inventory; the fluid is read straight from the block entity, which
 * is kept in sync with the client through the block entity update packet.
 */
public class InfFluidGeneratorMenu extends Container {

    // Fluid tank geometry inside the GUI, also used by the screen.
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

    /** Index of the tank slot inside {@link #inventorySlots}; it is added after the 36 player slots. */
    public static final int FLUID_SLOT_INDEX = PLAYER_SLOTS + HOTBAR_SLOTS;

    private final InfFluidGeneratorBlockEntity blockEntity;
    private final World world;
    private final List<VisibilitySlot> playerSlots = new ArrayList<VisibilitySlot>();

    /** Client and server constructor; the tile entity is looked up by the caller. */
    public InfFluidGeneratorMenu(InventoryPlayer playerInventory, TileEntity blockEntity) {
        this.blockEntity = (InfFluidGeneratorBlockEntity) blockEntity;
        this.world = playerInventory.player.world;

        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                VisibilitySlot slot = new VisibilitySlot(playerInventory, col + row * 9 + 9,
                        11 + col * 18, 99 + row * 18);
                this.playerSlots.add(slot);
                this.addSlot(slot);
            }
        }
        for (int col = 0; col < 9; ++col) {
            VisibilitySlot slot = new VisibilitySlot(playerInventory, col, 11 + col * 18, 153);
            this.playerSlots.add(slot);
            this.addSlot(slot);
        }

        // Display-only slot over the tank. It never stores items; bucket interactions are
        // handled in slotClick(...) so a fluid container on the cursor can be used directly.
        this.addSlot(new FluidSlot(-100, -100));
    }

    /** Hides the player inventory while a face detail page is open. */
    public void setPlayerSlotsVisible(boolean visible) {
        for (VisibilitySlot slot : this.playerSlots) {
            slot.setVisible(visible);
        }
    }

    /**
     * Direct bucket interaction with the tank slot: while the GUI is open, the container on the
     * cursor is filled from the tank first, and poured into the tank when filling is not possible.
     * Without an open GUI the block only exposes its fluid capability.
     */
    @Override
    public ItemStack slotClick(int slotId, int dragType, ClickType clickTypeIn, EntityPlayer player) {
        if (slotId == FLUID_SLOT_INDEX && clickTypeIn == ClickType.PICKUP
                && (dragType == 0 || dragType == 1)) {
            // The client also calls slotClick() locally, so only the authoritative server side
            // performs the transfer; the result reaches the client through normal sync.
            if (!player.world.isRemote) {
                handleFluidContainer(player);
            }
            return ItemStack.EMPTY;
        }
        return super.slotClick(slotId, dragType, clickTypeIn, player);
    }

    private void handleFluidContainer(EntityPlayer player) {
        ItemStack carried = player.inventory.getItemStack();
        if (carried.isEmpty()) {
            return;
        }
        Item item = carried.getItem();
        InfFluidKind bucketKind = InfFluidKind.byBucket(item);
        ItemStack resultStack;
        if (bucketKind != null) {
            if (this.blockEntity.fillTank(bucketKind, 1000, false) < 1000) {
                return;
            }
            this.blockEntity.fillTank(bucketKind, 1000, true);
            resultStack = new ItemStack(Items.BUCKET);
        } else if (item == Items.BUCKET) {
            InfFluidKind stored = this.blockEntity.getStoredKind();
            if (stored == null || this.blockEntity.drainTank(1000, false) < 1000) {
                return;
            }
            this.blockEntity.drainTank(1000, true);
            resultStack = new ItemStack(stored.getFilledBucket());
        } else {
            return;
        }
        if (carried.getCount() <= 1) {
            player.inventory.setItemStack(resultStack);
        } else {
            // Only one container is converted; the rest stay on the cursor and the produced
            // container goes to the inventory (buckets do not stack once they hold a fluid).
            ItemStack remaining = carried.copy();
            remaining.shrink(1);
            ItemStack leftover = ItemHandlerHelper.insertItemStacked(
                    new PlayerMainInvWrapper(player.inventory), resultStack, false);
            if (!leftover.isEmpty() && !player.addItemStackToInventory(leftover)) {
                player.entityDropItem(leftover, 0.0F);
            }
            player.inventory.setItemStack(remaining);
        }
        syncCarried(player);
        this.detectAndSendChanges();
    }

    /** Container id -1 is the vanilla carriage slot; this mirrors the server-side cursor. */
    private static void syncCarried(EntityPlayer player) {
        if (player instanceof EntityPlayerMP) {
            ((EntityPlayerMP) player).connection.sendPacket(
                    new SPacketSetSlot(-1, -1, player.inventory.getItemStack()));
        }
    }

    /** Inactive menu target used only by explicit bucket clicks routed from the screen. */
    private static class FluidSlot extends Slot {
        FluidSlot(int x, int y) {
            super(new InventoryBasic(new TextComponentString("infwhat.tank"), 1), 0, x, y);
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return false;
        }

        @Override
        public boolean canTakeStack(EntityPlayer player) {
            return false;
        }

        @Override
        public boolean isEnabled() {
            return false;
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        if (this.world.getBlockState(this.blockEntity.getPos()).getBlock()
                != ModBlocks.INF_FLUID_GENERATOR) {
            return false;
        }
        return player.getDistanceSq(
                this.blockEntity.getPos().getX() + 0.5D,
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

        if (index < PLAYER_SLOTS) {
            if (!this.mergeItemStack(stack, PLAYER_SLOTS, PLAYER_SLOTS + HOTBAR_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!this.mergeItemStack(stack, 0, PLAYER_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.putStack(ItemStack.EMPTY);
        } else {
            slot.onSlotChanged();
        }
        return copy;
    }

    public InfFluidGeneratorBlockEntity getBlockEntity() {
        return this.blockEntity;
    }

    /** The stored fluid kind, or {@code null} when the tank is empty. */
    public InfFluidKind getFluidKind() {
        return this.blockEntity.getStoredKind();
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

    public long getSideRateMb(EnumFacing direction) {
        return this.blockEntity.getSideRateMb(direction);
    }

    public long getTotalEffectiveRateMb() {
        return this.blockEntity.getTotalEffectiveRateMb();
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
    public boolean enchantItem(EntityPlayer player, int id) {
        if (player.world.isRemote) {
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
        int redstoneMode = id - BUTTON_REDSTONE_MODE_BASE;
        if (redstoneMode >= 0 && redstoneMode < RedstoneMode.values().length) {
            this.blockEntity.setRedstoneMode(RedstoneMode.byId(redstoneMode));
            return true;
        }
        return false;
    }
}

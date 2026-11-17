package com.bandybus.infwhat.menu;

import com.bandybus.infwhat.block.ModBlocks;
import com.bandybus.infwhat.block.RedstoneMode;
import com.bandybus.infwhat.block.entity.InfItemGeneratorBlockEntity;
import com.bandybus.infwhat.config.InfinityWhatConfig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SPacketSetSlot;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * Container menu for the Infinity Item Generator: the player inventory plus a single generator
 * slot. The slot always holds exactly one item; while infinite mode is enabled the item becomes
 * an infinite source. Manual retrieval is handled in {@link #slotClick}: left-click gives one
 * item, shift+left-click gives a full stack, and the source is never depleted (it is the
 * capability view, not this slot, that reports the huge count to pipes/AE2).
 */
public class InfItemGeneratorMenu extends Container {

    // Generator slot position inside the 184x178 GUI design canvas.
    public static final int SLOT_X = 84;
    public static final int SLOT_Y = 48;

    private static final int PLAYER_SLOTS = 27;
    private static final int HOTBAR_SLOTS = 9;

    /** Index of the generator slot; it is added after the 36 player slots. */
    public static final int GENERATOR_SLOT_INDEX = PLAYER_SLOTS + HOTBAR_SLOTS;

    public static final int BUTTON_TOGGLE_INFINITE = 0;
    public static final int BUTTON_TOGGLE_OUTPUT = 1;
    public static final int BUTTON_TOGGLE_SIDE_BASE = 2;
    public static final int BUTTON_REDSTONE_MODE_BASE = 8;

    private final InfItemGeneratorBlockEntity blockEntity;
    private final World world;
    private final GeneratorSlot generatorSlot;
    private final List<VisibilitySlot> playerSlots = new ArrayList<VisibilitySlot>();

    /** Client and server constructor; the tile entity is looked up by the caller. */
    public InfItemGeneratorMenu(InventoryPlayer playerInventory, TileEntity blockEntity) {
        this.blockEntity = (InfItemGeneratorBlockEntity) blockEntity;
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

        this.generatorSlot = new GeneratorSlot(this.blockEntity, this.blockEntity.getItemHandler(),
                0, SLOT_X, SLOT_Y);
        this.addSlot(this.generatorSlot);
    }

    /** Slot backed by the generator's item handler; picking the item up is blocked while infinite. */
    private static class GeneratorSlot extends SlotItemHandler {
        private final InfItemGeneratorBlockEntity blockEntity;
        private boolean visible = true;

        GeneratorSlot(InfItemGeneratorBlockEntity blockEntity, IItemHandler itemHandler,
                      int index, int x, int y) {
            super(itemHandler, index, x, y);
            this.blockEntity = blockEntity;
        }

        @Override
        public boolean canTakeStack(EntityPlayer player) {
            return !this.blockEntity.isInfinite() && super.canTakeStack(player);
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            // Honour the item filter even if the backing handler is bypassed by click routing.
            return InfinityWhatConfig.isItemAllowed(ForgeRegistries.ITEMS.getKey(stack.getItem()))
                    && super.isItemValid(stack);
        }

        @Override
        public boolean isEnabled() {
            return this.visible;
        }

        void setVisible(boolean visible) {
            this.visible = visible;
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        if (this.world.getBlockState(this.blockEntity.getPos()).getBlock()
                != ModBlocks.INF_ITEM_GENERATOR) {
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

        if (index == GENERATOR_SLOT_INDEX) {
            // Never let a locked (infinite) slot be shifted out.
            if (this.blockEntity.isInfinite()
                    || !this.mergeItemStack(stack, 0, PLAYER_SLOTS + HOTBAR_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            // mergeItemStack merges by the item's stack limit, not the slot limit, so move the
            // template item explicitly one unit at a time.
            boolean moved = false;
            Slot generatorSlot = this.inventorySlots.get(GENERATOR_SLOT_INDEX);
            if (!stack.isEmpty() && generatorSlot.isItemValid(stack)
                    && generatorSlot.getStack().isEmpty()) {
                ItemStack single = stack.copy();
                single.setCount(1);
                generatorSlot.putStack(single);
                generatorSlot.onSlotChanged();
                stack.shrink(1);
                moved = true;
            }
            if (!moved && index < PLAYER_SLOTS) {
                moved = this.mergeItemStack(stack, PLAYER_SLOTS, PLAYER_SLOTS + HOTBAR_SLOTS, false);
            } else if (!moved) {
                moved = this.mergeItemStack(stack, 0, PLAYER_SLOTS, false);
            }
            if (!moved) {
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

    public InfItemGeneratorBlockEntity getBlockEntity() {
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

    /** Hides the stored item slot while the screen displays its separate I/O settings page. */
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
    public boolean enchantItem(EntityPlayer player, int id) {
        // Vanilla invokes this on the client when a GUI button is pressed and again on the
        // server. Only the server changes saved data.
        if (player.world.isRemote) {
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
        int redstoneMode = id - BUTTON_REDSTONE_MODE_BASE;
        if (redstoneMode >= 0 && redstoneMode < RedstoneMode.values().length) {
            this.blockEntity.setRedstoneMode(RedstoneMode.byId(redstoneMode));
            return true;
        }
        return false;
    }

    /** The single template item held by the generator (used by the screen to label the slot). */
    public ItemStack getStoredItem() {
        return this.blockEntity.getStoredItem();
    }

    /**
     * Manual infinite retrieval while infinite mode is enabled. Vanilla would try to move the
     * whole stack, but the stored stack is only one unit, so intercept the click here:
     * left-click hands out one item, shift+left-click hands out a full stack into the inventory.
     * All other click types are ignored while the slot is locked in infinite mode.
     */
    @Override
    public ItemStack slotClick(int slotId, int dragType, ClickType clickTypeIn, EntityPlayer player) {
        if (slotId == GENERATOR_SLOT_INDEX && this.blockEntity.isInfinite()) {
            // The client also runs slotClick locally; only the authoritative server side may
            // create items. The server result reaches the client through normal container sync.
            if (!player.world.isRemote) {
                if (clickTypeIn == ClickType.PICKUP && dragType == 0) {
                    takeFromInfinite(player, 1, false);
                } else if (clickTypeIn == ClickType.QUICK_MOVE && dragType == 0) {
                    takeFromInfinite(player, getInfiniteStackSize(), true);
                }
            }
            return ItemStack.EMPTY;
        }
        return super.slotClick(slotId, dragType, clickTypeIn, player);
    }

    private int getInfiniteStackSize() {
        ItemStack stored = this.blockEntity.getStoredItem();
        return stored.isEmpty() ? 1 : stored.getMaxStackSize();
    }

    /**
     * Pulls {@code amount} items out of the infinite source and gives them to the player, either
     * into the carried stack or straight into the inventory (shift-click). The source is never
     * depleted.
     */
    private void takeFromInfinite(EntityPlayer player, int amount, boolean toInventory) {
        ItemStack taken = this.blockEntity.getItemHandler().extractItem(0, amount, false);
        if (taken.isEmpty()) {
            return;
        }
        if (toInventory) {
            if (!player.addItemStackToInventory(taken)) {
                player.entityDropItem(taken, 0.0F);
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
                if (!player.addItemStackToInventory(rest)) {
                    player.entityDropItem(rest, 0.0F);
                }
            }
            player.inventory.setItemStack(carried);
        } else if (!player.addItemStackToInventory(taken)) {
            player.entityDropItem(taken, 0.0F);
        }
        syncCarried(player);
    }

    /** Container id -1 is the vanilla carriage slot; this mirrors the server-side cursor. */
    private static void syncCarried(EntityPlayer player) {
        if (player instanceof EntityPlayerMP) {
            ((EntityPlayerMP) player).connection.sendPacket(
                    new SPacketSetSlot(-1, -1, player.inventory.getItemStack()));
        }
    }
}

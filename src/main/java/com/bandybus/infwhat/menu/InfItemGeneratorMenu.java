package com.bandybus.infwhat.menu;

import com.bandybus.infwhat.block.ModBlocks;
import com.bandybus.infwhat.block.RedstoneMode;
import com.bandybus.infwhat.block.entity.InfItemGeneratorBlockEntity;
import net.minecraft.util.math.Direction;
import net.minecraft.util.PacketByteBuf;
import net.minecraft.inventory.BasicInventory;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.container.Container;
import net.minecraft.container.SlotActionType;
import net.minecraft.container.BlockContext;
import net.minecraft.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Container menu for the Infinity Item Generator: the player inventory plus a single generator
 * slot. The slot always holds exactly one item; while infinite mode is enabled the item becomes
 * an infinite source. Manual retrieval is handled in {@link #clicked}: left-click gives one item,
 * shift+left-click gives a full stack, and the source is never depleted.
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
    private final List<VisibilitySlot> playerSlots = new ArrayList<>();

    // Client constructor: receives the block pos written by writeScreenOpeningData.
    public InfItemGeneratorMenu(int containerId, PlayerInventory playerInventory, PacketByteBuf data) {
        this(containerId, playerInventory,
                playerInventory.player.world.getBlockEntity(data.readBlockPos()));
    }

    // Server constructor.
    public InfItemGeneratorMenu(int containerId, PlayerInventory playerInventory, BlockEntity blockEntity) {
        // 1.15.2 Fabric registers containers by id, not MenuType; null is the legacy convention.
        super(null, containerId);
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

        this.generatorSlot = new GeneratorSlot(this.blockEntity, SLOT_X, SLOT_Y);
        this.addSlot(this.generatorSlot);
    }

    /**
     * Slot backed directly by the generator block entity. Reads and writes are proxied onto the
     * block entity's internal template item.
     */
    private static class GeneratorSlot extends Slot {
        private final InfItemGeneratorBlockEntity blockEntity;
        private boolean visible = true;

        GeneratorSlot(InfItemGeneratorBlockEntity blockEntity, int x, int y) {
            super(new BasicInventory(1), 0, x, y);
            this.blockEntity = blockEntity;
        }

        @Override
        public ItemStack getStack() {
            return this.blockEntity.getStoredItem();
        }

        @Override
        public boolean hasStack() {
            return !this.blockEntity.getStoredItem().isEmpty();
        }

        @Override
        public void setStack(ItemStack stack) {
            this.blockEntity.setStoredItem(stack);
        }

        @Override
        public void markDirty() {
            this.blockEntity.markDirty();
        }

        @Override
        public ItemStack takeStack(int amount) {
            ItemStack stored = this.blockEntity.getStoredItem();
            if (stored.isEmpty() || amount <= 0) {
                return ItemStack.EMPTY;
            }
            ItemStack removed = stored.copy();
            removed.setAmount(Math.min(amount, stored.getAmount()));
            this.blockEntity.removeStored(removed.getAmount());
            return removed;
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return this.blockEntity.canInsertItem(stack);
        }

        @Override
        public boolean canTakeItems(PlayerEntity player) {
            return !this.blockEntity.isInfinite();
        }

        @Override
        public int getMaxStackAmount() {
            return 1;
        }

        @Override
        public boolean doDrawHoveringEffect() {
            return this.visible;
        }

        void setVisible(boolean visible) {
            this.visible = visible;
        }
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return canUse(
                BlockContext.create(this.world, this.blockEntity.getPos()),
                player,
                ModBlocks.INF_ITEM_GENERATOR);
    }

    @Override
    public ItemStack transferSlot(PlayerEntity player, int index) {
        Slot slot = this.slotList.get(index);
        if (!slot.hasStack()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getStack();
        ItemStack copy = stack.copy();

        if (index == GENERATOR_SLOT_INDEX) {
            // Never let a locked (infinite) slot be shifted out.
            if (this.blockEntity.isInfinite()
                    || !this.insertItem(stack, 0, PLAYER_SLOTS + HOTBAR_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            // 1.16.5's insertItem merges with the item's stack limit, not the slot limit;
            // shift-click would merge the whole pile into the template slot. Move one only.
            boolean moved = false;
            Slot templateSlot = this.slotList.get(GENERATOR_SLOT_INDEX);
            if (!stack.isEmpty() && templateSlot.canInsert(stack) && templateSlot.getStack().isEmpty()) {
                ItemStack single = stack.copy();
                single.setAmount(1);
                templateSlot.setStack(single);
                templateSlot.markDirty();
                stack.subtractAmount(1);
                moved = true;
            }
            if (!moved && index < PLAYER_SLOTS) {
                moved = this.insertItem(stack, PLAYER_SLOTS, PLAYER_SLOTS + HOTBAR_SLOTS, false);
            } else if (!moved) {
                moved = this.insertItem(stack, 0, PLAYER_SLOTS, false);
            }
            if (!moved) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.setStack(ItemStack.EMPTY);
        } else {
            slot.markDirty();
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

    public int getSideRate(Direction direction) {
        return this.blockEntity.getSideRate(direction);
    }

    public long getTotalEffectiveRate() {
        return this.blockEntity.getTotalEffectiveRate();
    }

    public boolean isOutputSideEnabled(Direction direction) {
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

    /** Hides the player inventory while a face detail / redstone / batch page is open. */
    public void setPlayerSlotsVisible(boolean visible) {
        for (VisibilitySlot slot : this.playerSlots) {
            slot.setVisible(visible);
        }
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int buttonId) {
        // Vanilla invokes this on the client when a GUI button is pressed and again on the
        // server from ServerboundContainerButtonClickPacket. Only the server changes saved data.
        if (player.world.isClient) {
            return true;
        }
        if (buttonId == BUTTON_TOGGLE_INFINITE) {
            this.blockEntity.setInfiniteModeEnabled(!this.blockEntity.isInfiniteModeEnabled());
            return true;
        }
        if (buttonId == BUTTON_TOGGLE_OUTPUT) {
            this.blockEntity.setOutputEnabled(!this.blockEntity.isOutputEnabled());
            return true;
        }
        int sideDataValue = buttonId - BUTTON_TOGGLE_SIDE_BASE;
        if (sideDataValue >= 0 && sideDataValue < Direction.values().length) {
            this.blockEntity.toggleOutputSide(Direction.byId(sideDataValue));
            return true;
        }
        int redstoneMode = buttonId - BUTTON_REDSTONE_MODE_BASE;
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
    public ItemStack onSlotClick(int slotId, int button, SlotActionType clickType, PlayerEntity player) {
        if (slotId == GENERATOR_SLOT_INDEX && this.blockEntity.isInfinite()) {
            if (clickType == SlotActionType.PICKUP && button == 0) {
                takeFromInfinite(player, 1, false);
            } else if (clickType == SlotActionType.QUICK_MOVE && button == 0) {
                takeFromInfinite(player, getInfiniteStackSize(), true);
            }
            return ItemStack.EMPTY;
        }
        return super.onSlotClick(slotId, button, clickType, player);
    }

    private int getInfiniteStackSize() {
        ItemStack stored = this.blockEntity.getStoredItem();
        return stored.isEmpty() ? 1 : stored.getMaxAmount();
    }

    /**
     * Pulls {@code amount} items out of the infinite source and gives them to the player, either
     * into the carried stack or straight into the inventory (shift-click). The source is never
     * depleted.
     */
    private void takeFromInfinite(PlayerEntity player, int amount, boolean toInventory) {
        ItemStack taken = this.blockEntity.extractStored(amount);
        if (taken.isEmpty()) {
            return;
        }
        if (toInventory) {
            if (!player.inventory.insertStack(taken)) {
                player.dropItem(taken, false);
            }
            return;
        }
        ItemStack carried = player.inventory.getCursorStack();
        if (carried.isEmpty()) {
            player.inventory.setCursorStack(taken);
        } else if (ItemStack.areEqual(carried, taken)) {
            int space = carried.getMaxAmount() - carried.getAmount();
            int moved = Math.min(space, taken.getAmount());
            if (moved > 0) {
                carried.addAmount(moved);
            }
            int leftover = taken.getAmount() - moved;
            if (leftover > 0) {
                ItemStack rest = taken.copy();
                rest.setAmount(leftover);
                if (!player.inventory.insertStack(rest)) {
                    player.dropItem(rest, false);
                }
            }
            player.inventory.setCursorStack(carried);
        } else if (!player.inventory.insertStack(taken)) {
            player.dropItem(taken, false);
        }
    }
}

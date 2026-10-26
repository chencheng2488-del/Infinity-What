package com.bandybus.infwhat.menu;

import com.bandybus.infwhat.block.ModBlocks;
import com.bandybus.infwhat.block.entity.InfItemGeneratorBlockEntity;
import com.bandybus.infwhat.config.InfinityWhatConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Container menu for the Infinity Item Generator: the player inventory plus a single generator
 * slot. The slot always holds exactly one item; while infinite mode is enabled the item becomes
 * an infinite source. Manual retrieval is handled in {@link #clicked}: left-click gives one item,
 * shift+left-click gives a full stack, and the source is never depleted (it is the storage view,
 * not this slot, that reports the huge amount to pipes).
 */
public class InfItemGeneratorMenu extends AbstractContainerMenu {

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

    private final InfItemGeneratorBlockEntity blockEntity;
    private final Level level;
    private final GeneratorSlot generatorSlot;

    public InfItemGeneratorMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolveBlockEntity(playerInventory, pos));
    }

    private static InfItemGeneratorBlockEntity resolveBlockEntity(Inventory inventory, BlockPos pos) {
        return (InfItemGeneratorBlockEntity) inventory.player.level().getBlockEntity(pos);
    }

    // Server constructor.
    public InfItemGeneratorMenu(int containerId, Inventory playerInventory,
                                InfItemGeneratorBlockEntity blockEntity) {
        super(ModMenuTypes.INF_ITEM_GENERATOR, containerId);
        this.blockEntity = blockEntity;
        this.level = playerInventory.player.level();

        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        11 + col * 18, 99 + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 11 + col * 18, 153));
        }

        this.generatorSlot = new GeneratorSlot(this.blockEntity, new TemplateContainer(this.blockEntity),
                SLOT_X, SLOT_Y);
        this.addSlot(this.generatorSlot);
    }

    /**
     * Slot backed by the generator's template item; picking the item up is blocked while
     * infinite. Vanilla clicks always pass through the container proxy below, so the generator
     * keeps holding at most one template unit.
     */
    private static class GeneratorSlot extends Slot {
        private final InfItemGeneratorBlockEntity blockEntity;
        private boolean visible = true;

        GeneratorSlot(InfItemGeneratorBlockEntity blockEntity, Container container, int x, int y) {
            super(container, 0, x, y);
            this.blockEntity = blockEntity;
        }

        @Override
        public boolean mayPickup(Player player) {
            return !this.blockEntity.isInfinite() && super.mayPickup(player);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            // Honour the item filter even if the backing container is bypassed by click routing.
            return !this.blockEntity.isInfinite()
                    && InfinityWhatConfig.isItemAllowed(BuiltInRegistries.ITEM.getKey(stack.getItem()))
                    && super.mayPlace(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public boolean isActive() {
            return this.visible;
        }

        void setVisible(boolean visible) {
            this.visible = visible;
        }
    }

    /** One-slot container view over the block entity's template item. */
    private static class TemplateContainer implements Container {
        private final InfItemGeneratorBlockEntity blockEntity;

        TemplateContainer(InfItemGeneratorBlockEntity blockEntity) {
            this.blockEntity = blockEntity;
        }

        @Override
        public int getContainerSize() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return this.blockEntity.getStoredItem().isEmpty();
        }

        @Override
        public ItemStack getItem(int slot) {
            return this.blockEntity.getStoredItem();
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            return this.blockEntity.extractStored(amount);
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return this.blockEntity.extractStored(1);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            this.blockEntity.setStoredItem(stack);
        }

        @Override
        public void setChanged() {
            this.blockEntity.markUpdated();
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public void clearContent() {
            this.blockEntity.clearStoredItem();
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(
                ContainerLevelAccess.create(this.level, this.blockEntity.getBlockPos()),
                player,
                ModBlocks.INF_ITEM_GENERATOR);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        if (index == GENERATOR_SLOT_INDEX) {
            // Never let a locked (infinite) slot be shifted out.
            if (this.blockEntity.isInfinite()
                    || !this.moveItemStackTo(stack, 0, PLAYER_SLOTS + HOTBAR_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            boolean moved = this.moveItemStackTo(stack, GENERATOR_SLOT_INDEX, GENERATOR_SLOT_INDEX + 1, false);
            if (!moved && index < PLAYER_SLOTS) {
                moved = this.moveItemStackTo(stack, PLAYER_SLOTS, PLAYER_SLOTS + HOTBAR_SLOTS, false);
            } else if (!moved) {
                moved = this.moveItemStackTo(stack, 0, PLAYER_SLOTS, false);
            }
            if (!moved) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
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

    public int getOutputItemsPerTick() {
        return this.blockEntity.getOutputItemsPerTick();
    }

    public boolean isOutputSideEnabled(Direction direction) {
        return this.blockEntity.isOutputSideEnabled(direction);
    }

    /** Hides the stored item slot while the screen displays its separate I/O settings page. */
    public void setGeneratorSlotVisible(boolean visible) {
        this.generatorSlot.setVisible(visible);
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        // Vanilla invokes this on the client when a GUI button is pressed and again on the
        // server from ServerboundContainerButtonClickPacket. Only the server changes saved data.
        if (player.level().isClientSide) {
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
            this.blockEntity.toggleOutputSide(Direction.from3DDataValue(sideDataValue));
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
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId == GENERATOR_SLOT_INDEX && this.blockEntity.isInfinite()) {
            if (clickType == ClickType.PICKUP && button == 0) {
                takeFromInfinite(player, 1, false);
            } else if (clickType == ClickType.QUICK_MOVE && button == 0) {
                takeFromInfinite(player, getInfiniteStackSize(), true);
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
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
    private void takeFromInfinite(Player player, int amount, boolean toInventory) {
        ItemStack taken = this.blockEntity.extractStored(amount);
        if (taken.isEmpty()) {
            return;
        }
        if (toInventory) {
            if (!player.getInventory().add(taken)) {
                player.drop(taken, false);
            }
            return;
        }
        ItemStack carried = this.getCarried();
        if (carried.isEmpty()) {
            this.setCarried(taken);
        } else if (ItemStack.isSameItemSameComponents(carried, taken)) {
            int space = carried.getMaxStackSize() - carried.getCount();
            int moved = Math.min(space, taken.getCount());
            if (moved > 0) {
                carried.grow(moved);
            }
            int leftover = taken.getCount() - moved;
            if (leftover > 0) {
                ItemStack rest = taken.copyWithCount(leftover);
                if (!player.getInventory().add(rest)) {
                    player.drop(rest, false);
                }
            }
            this.setCarried(carried);
        } else if (!player.getInventory().add(taken)) {
            player.drop(taken, false);
        }
    }
}

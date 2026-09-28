package cn.autoforged.infinity_what_1790519020.menu;

import cn.autoforged.infinity_what_1790519020.block.ModBlocks;
import cn.autoforged.infinity_what_1790519020.block.entity.InfItemGeneratorBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * Container menu for the Infinity Item Generator: the player inventory plus a single generator
 * slot. The slot always holds exactly one item; while the block is powered the item becomes an
 * infinite source. Manual retrieval is handled in {@link #clicked}: left-click gives one item,
 * shift+left-click gives a full stack, and the source is never depleted (it is the capability
 * view, not this slot, that reports the huge count to pipes/AE2).
 */
public class InfItemGeneratorMenu extends AbstractContainerMenu {

    // Generator slot position inside the 176x166 GUI.
    public static final int SLOT_X = 80;
    public static final int SLOT_Y = 35;

    private static final int PLAYER_SLOTS = 27;
    private static final int HOTBAR_SLOTS = 9;

    /** Index of the generator slot; it is added after the 36 player slots. */
    public static final int GENERATOR_SLOT_INDEX = PLAYER_SLOTS + HOTBAR_SLOTS;

    private final InfItemGeneratorBlockEntity blockEntity;
    private final Level level;

    // Client constructor: receives the block pos written by NetworkHooks#openScreen.
    public InfItemGeneratorMenu(int containerId, Inventory playerInventory, FriendlyByteBuf data) {
        this(containerId, playerInventory,
                playerInventory.player.level().getBlockEntity(data.readBlockPos()));
    }

    // Server constructor.
    public InfItemGeneratorMenu(int containerId, Inventory playerInventory, BlockEntity blockEntity) {
        super(ModMenuTypes.INF_ITEM_GENERATOR.get(), containerId);
        this.blockEntity = (InfItemGeneratorBlockEntity) blockEntity;
        this.level = playerInventory.player.level();

        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }

        this.addSlot(new GeneratorSlot(this.blockEntity, this.blockEntity.getItemHandler(), 0, SLOT_X, SLOT_Y));
    }

    /** Slot backed by the generator's item handler; picking the item up is blocked while infinite. */
    private static class GeneratorSlot extends SlotItemHandler {
        private final InfItemGeneratorBlockEntity blockEntity;

        GeneratorSlot(InfItemGeneratorBlockEntity blockEntity, IItemHandler itemHandler, int index, int x, int y) {
            super(itemHandler, index, x, y);
            this.blockEntity = blockEntity;
        }

        @Override
        public boolean mayPickup(Player player) {
            return !this.blockEntity.isInfinite() && super.mayPickup(player);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(
                ContainerLevelAccess.create(this.level, this.blockEntity.getBlockPos()),
                player,
                ModBlocks.INF_ITEM_GENERATOR.get());
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

    public boolean isPowered() {
        return this.blockEntity.isPowered();
    }

    /** The single template item held by the generator (used by the screen to label the slot). */
    public ItemStack getStoredItem() {
        return this.blockEntity.getStoredItem();
    }

    /**
     * Manual infinite retrieval while the generator is charged. Vanilla would try to move the
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
        ItemStack taken = this.blockEntity.getItemHandler().extractItem(0, amount, false);
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
        } else if (ItemStack.isSameItemSameTags(carried, taken)) {
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

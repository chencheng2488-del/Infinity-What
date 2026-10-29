package com.bandybus.infwhat.menu;

import com.bandybus.infwhat.block.ModBlocks;
import com.bandybus.infwhat.block.entity.InfItemGeneratorBlockEntity;
import com.bandybus.infwhat.config.InfinityWhatConfig;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SSetSlotPacket;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.IWorldPosCallable;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Container menu for the Infinity Item Generator: the player inventory plus a single generator
 * slot. The slot always holds exactly one item; while infinite mode is enabled the item becomes
 * an infinite source. Manual retrieval is handled in {@link #clicked}: left-click gives one item,
 * shift+left-click gives a full stack, and the source is never depleted (it is the capability
 * view, not this slot, that reports the huge count to pipes/AE2).
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

    private final InfItemGeneratorBlockEntity blockEntity;
    private final World level;
    private final GeneratorSlot generatorSlot;

    // Client constructor: receives the block pos written by NetworkHooks#openGui.
    public InfItemGeneratorMenu(int containerId, PlayerInventory playerInventory, PacketBuffer data) {
        this(containerId, playerInventory,
                playerInventory.player.level.getBlockEntity(data.readBlockPos()));
    }

    // Server constructor.
    public InfItemGeneratorMenu(int containerId, PlayerInventory playerInventory, TileEntity blockEntity) {
        super(ModMenuTypes.INF_ITEM_GENERATOR.get(), containerId);
        this.blockEntity = (InfItemGeneratorBlockEntity) blockEntity;
        this.level = playerInventory.player.level;

        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        11 + col * 18, 99 + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 11 + col * 18, 153));
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
        public boolean mayPickup(PlayerEntity player) {
            return !this.blockEntity.isInfinite() && super.mayPickup(player);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            // Honour the item filter even if the backing handler is bypassed by click routing.
            return InfinityWhatConfig.isItemAllowed(ForgeRegistries.ITEMS.getKey(stack.getItem()))
                    && super.mayPlace(stack);
        }

        @Override
        public boolean isActive() {
            return this.visible;
        }

        void setVisible(boolean visible) {
            this.visible = visible;
        }
    }

    @Override
    public boolean stillValid(PlayerEntity player) {
        return stillValid(
                IWorldPosCallable.create(this.level, this.blockEntity.getBlockPos()),
                player,
                ModBlocks.INF_ITEM_GENERATOR.get());
    }

    @Override
    public ItemStack quickMoveStack(PlayerEntity player, int index) {
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
            // 1.18.2–1.20.4 的 moveItemStackTo 在“同物品合并”时用的是物品堆叠上限而不是
            // 槽位上限，shift-click 会把整摞并进模板槽；这里显式只放 1 个。
            boolean moved = false;
            Slot generatorSlot = this.slots.get(GENERATOR_SLOT_INDEX);
            if (!stack.isEmpty() && generatorSlot.mayPlace(stack) && generatorSlot.getItem().isEmpty()) {
                ItemStack single = stack.copy();
                single.setCount(1);
                generatorSlot.set(single);
                generatorSlot.setChanged();
                stack.shrink(1);
                moved = true;
            }
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
    public boolean clickMenuButton(PlayerEntity player, int buttonId) {
        // Vanilla invokes this on the client when a GUI button is pressed and again on the
        // server from ServerboundContainerButtonClickPacket. Only the server changes saved data.
        if (player.level.isClientSide) {
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
    public ItemStack clicked(int slotId, int button, ClickType clickType, PlayerEntity player) {
        if (slotId == GENERATOR_SLOT_INDEX && this.blockEntity.isInfinite()) {
            if (clickType == ClickType.PICKUP && button == 0) {
                takeFromInfinite(player, 1, false);
            } else if (clickType == ClickType.QUICK_MOVE && button == 0) {
                takeFromInfinite(player, getInfiniteStackSize(), true);
            }
            return ItemStack.EMPTY;
        }
        return super.clicked(slotId, button, clickType, player);
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
    private void takeFromInfinite(PlayerEntity player, int amount, boolean toInventory) {
        ItemStack taken = this.blockEntity.getItemHandler().extractItem(0, amount, false);
        if (taken.isEmpty()) {
            return;
        }
        if (toInventory) {
            if (!player.inventory.add(taken)) {
                player.drop(taken, false);
            }
            return;
        }
        ItemStack carried = player.inventory.getCarried();
        if (carried.isEmpty()) {
            player.inventory.setCarried(taken);
        } else if (ItemStack.matches(carried, taken)) {
            int space = carried.getMaxStackSize() - carried.getCount();
            int moved = Math.min(space, taken.getCount());
            if (moved > 0) {
                carried.grow(moved);
            }
            int leftover = taken.getCount() - moved;
            if (leftover > 0) {
                ItemStack rest = taken.copy();
                rest.setCount(leftover);
                if (!player.inventory.add(rest)) {
                    player.drop(rest, false);
                }
            }
            player.inventory.setCarried(carried);
        } else if (!player.inventory.add(taken)) {
            player.drop(taken, false);
        }
        syncCarried(player);
    }

    /**
     * 1.16.5's Container has no carried-stack synchronisation helper (1.18+ added
     * synchronizeCarriedToRemote). Container id -1 is the vanilla carriage slot, so sending
     * SSetSlotPacket(-1, ...) makes the client mirror the server-side cursor.
     */
    private static void syncCarried(PlayerEntity player) {
        if (player instanceof ServerPlayerEntity) {
            ((ServerPlayerEntity) player).connection.connection.send(
                    new SSetSlotPacket(-1, -1, player.inventory.getCarried()));
        }
    }
}

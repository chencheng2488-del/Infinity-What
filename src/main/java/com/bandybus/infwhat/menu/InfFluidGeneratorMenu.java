package com.bandybus.infwhat.menu;

import com.bandybus.infwhat.block.ModBlocks;
import com.bandybus.infwhat.block.entity.InfFluidGeneratorBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Container menu for the Infinity Fluid Generator. There are no item slots for the machine
 * itself, only the player inventory; the fluid is read straight from the block entity, which
 * is kept in sync with the client through the block entity update packet.
 */
public class InfFluidGeneratorMenu extends AbstractContainerMenu {

    // Fluid tank geometry inside the GUI, also used by the screen.
    public static final int TANK_X = 26;
    public static final int TANK_Y = 23;
    public static final int TANK_WIDTH = 16;
    public static final int TANK_HEIGHT = 40;

    public static final int BUTTON_TOGGLE_OUTPUT = 0;
    public static final int BUTTON_TOGGLE_SIDE_BASE = 1;
    public static final int BUTTON_CLEAR_FLUID = 7;

    private static final int PLAYER_SLOTS = 27;
    private static final int HOTBAR_SLOTS = 9;

    /** Index of the tank slot inside {@link #slots}; it is added after the 36 player slots. */
    public static final int FLUID_SLOT_INDEX = PLAYER_SLOTS + HOTBAR_SLOTS;

    private final InfFluidGeneratorBlockEntity blockEntity;
    private final Level level;

    // Client constructor: receives the block pos written by writeScreenOpeningData.
    public InfFluidGeneratorMenu(int containerId, Inventory playerInventory, FriendlyByteBuf data) {
        this(containerId, playerInventory,
                playerInventory.player.level().getBlockEntity(data.readBlockPos()));
    }

    // Server constructor.
    public InfFluidGeneratorMenu(int containerId, Inventory playerInventory, BlockEntity blockEntity) {
        super(ModMenuTypes.INF_FLUID_GENERATOR, containerId);
        this.blockEntity = (InfFluidGeneratorBlockEntity) blockEntity;
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

        // Display-only slot over the tank. It never stores items; bucket interactions are
        // handled in clicked(...) so a fluid container on the cursor can be used directly.
        // This synthetic slot is deliberately inactive and offscreen: the screen routes clicks
        // over the larger visible tank area here, but vanilla must not draw a 16x16 hover overlay.
        this.addSlot(new FluidSlot(-100, -100));
    }

    /**
     * Direct bucket interaction with the tank slot, mirroring AE2: while the GUI is open, a
     * fluid container on the cursor can be exchanged with the tank. Without an open GUI the
     * block only exposes its fluid storage.
     */
    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId == FLUID_SLOT_INDEX && clickType == ClickType.PICKUP && (button == 0 || button == 1)) {
            // The client also calls clicked() locally (MultiPlayerGameMode#handleInventoryMouseClick),
            // so only the authoritative server side performs the transfer; the result reaches the
            // client through the normal container/block-entity sync.
            if (!player.level().isClientSide) {
                handleFluidContainer(player);
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    /**
     * Exchanges fluid between the cursor stack and the tank. The direction is decided by the
     * contents: the tank first fills the held container, and if that moves nothing the held
     * container is emptied into the tank. Mouse buttons do not influence the direction.
     */
    private void handleFluidContainer(Player player) {
        if (this.getCarried().isEmpty()) {
            return;
        }
        ContainerItemContext context = ContainerItemContext.ofPlayerCursor(player, this);
        Storage<FluidVariant> itemStorage = context.find(FluidStorage.ITEM);
        if (itemStorage == null) {
            return;
        }
        Storage<FluidVariant> tank = this.blockEntity.getFluidStorage();
        long moved;
        try (Transaction transaction = Transaction.openOuter()) {
            moved = StorageUtil.move(tank, itemStorage, variant -> true, Long.MAX_VALUE, transaction);
            if (moved == 0L) {
                moved = StorageUtil.move(itemStorage, tank, variant -> true, Long.MAX_VALUE, transaction);
            }
            if (moved > 0L) {
                transaction.commit();
            }
        }
        if (moved > 0L) {
            this.broadcastChanges();
        }
    }

    /** Inactive menu target used only by explicit bucket clicks routed from the screen. */
    private static class FluidSlot extends Slot {
        FluidSlot(int x, int y) {
            super(new SimpleContainer(1), 0, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        @Override
        public boolean isActive() {
            return false;
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(
                ContainerLevelAccess.create(this.level, this.blockEntity.getBlockPos()),
                player,
                ModBlocks.INF_FLUID_GENERATOR);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        if (index < PLAYER_SLOTS) {
            if (!this.moveItemStackTo(stack, PLAYER_SLOTS, PLAYER_SLOTS + HOTBAR_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!this.moveItemStackTo(stack, 0, PLAYER_SLOTS, false)) {
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

    public InfFluidGeneratorBlockEntity getBlockEntity() {
        return this.blockEntity;
    }

    public FluidVariant getFluidVariant() {
        return this.blockEntity.getStoredFluid();
    }

    public boolean isInfinite() {
        return this.blockEntity.isInfinite();
    }

    /** Display amount in millibuckets; {@link Long#MAX_VALUE} while infinite. */
    public long getAmount() {
        return this.blockEntity.getStoredAmount();
    }

    public boolean isOutputEnabled() {
        return this.blockEntity.isOutputEnabled();
    }

    public boolean isOutputSideEnabled(Direction direction) {
        return this.blockEntity.isOutputSideEnabled(direction);
    }

    public long getOutputAmountPerTick() {
        return this.blockEntity.getOutputAmountPerTick();
    }

    public float getFillRatio() {
        return this.blockEntity.getFillRatio();
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (player.level().isClientSide) {
            return true;
        }
        if (buttonId == BUTTON_TOGGLE_OUTPUT) {
            this.blockEntity.setOutputEnabled(!this.blockEntity.isOutputEnabled());
            return true;
        }
        if (buttonId == BUTTON_CLEAR_FLUID) {
            this.blockEntity.clearFluid();
            return true;
        }
        int sideDataValue = buttonId - BUTTON_TOGGLE_SIDE_BASE;
        if (sideDataValue >= 0 && sideDataValue < Direction.values().length) {
            this.blockEntity.toggleOutputSide(Direction.from3DDataValue(sideDataValue));
            return true;
        }
        return false;
    }
}

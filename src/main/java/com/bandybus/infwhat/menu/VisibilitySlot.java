package com.bandybus.infwhat.menu;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.container.Slot;

/** A player-inventory slot that can be hidden without rebuilding the menu. */
public class VisibilitySlot extends Slot {
    private boolean visible = true;

    public VisibilitySlot(Inventory container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public boolean doDrawHoveringEffect() {
        return this.visible;
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        return this.visible && super.canInsert(stack);
    }

    @Override
    public boolean canTakeItems(PlayerEntity player) {
        return this.visible && super.canTakeItems(player);
    }
}

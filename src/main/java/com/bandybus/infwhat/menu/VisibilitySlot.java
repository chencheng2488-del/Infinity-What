package com.bandybus.infwhat.menu;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

/** A player-inventory slot that can be hidden without rebuilding the menu. */
public class VisibilitySlot extends Slot {
    private boolean visible = true;

    public VisibilitySlot(IInventory container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public boolean isEnabled() {
        return this.visible;
    }
}

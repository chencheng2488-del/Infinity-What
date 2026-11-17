package com.bandybus.infwhat.menu;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;

/** A player-inventory slot that can be hidden without rebuilding the menu. */
public class VisibilitySlot extends Slot {
    private boolean visible = true;

    public VisibilitySlot(Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public boolean isActive() {
        return this.visible;
    }
}

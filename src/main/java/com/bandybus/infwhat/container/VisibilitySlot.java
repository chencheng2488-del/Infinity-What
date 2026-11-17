package com.bandybus.infwhat.container;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

/** A player-inventory slot that can be hidden without rebuilding the container. */
public class VisibilitySlot extends Slot {
    private final int visibleX;
    private final int visibleY;
    private boolean visible = true;

    public VisibilitySlot(IInventory inventory, int index, int x, int y) {
        super(inventory, index, x, y);
        this.visibleX = x;
        this.visibleY = y;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
        this.xPos = visible ? this.visibleX : -1000;
        this.yPos = visible ? this.visibleY : -1000;
    }

    @Override
    public boolean isEnabled() {
        return this.visible;
    }
}

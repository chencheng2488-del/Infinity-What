package com.bandybus.infwhat.block;

import net.minecraft.item.ItemBlock;

/**
 * Block item used by the Infinity What machines. Both machines are limited to one item per stack,
 * matching the newer releases.
 */
public class ItemBlockMachine extends ItemBlock {

    public ItemBlockMachine(int itemId) {
        super(itemId);
        // Spec: stack_size = 1 for machine blocks.
        this.setMaxStackSize(1);
    }
}

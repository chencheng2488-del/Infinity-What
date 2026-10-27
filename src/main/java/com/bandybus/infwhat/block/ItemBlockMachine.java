package com.bandybus.infwhat.block;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;

/**
 * Block item used by the Infinity What machines. The 1.8.9 release limits both machines to one
 * item per stack, so the 1.7.10 port applies the same limit in code.
 */
public class ItemBlockMachine extends ItemBlock {

    public ItemBlockMachine(Block block) {
        super(block);
        // Spec: stack_size = 1 for machine blocks.
        this.setMaxStackSize(1);
    }
}

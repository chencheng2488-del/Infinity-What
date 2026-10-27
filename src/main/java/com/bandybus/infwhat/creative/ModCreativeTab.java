package com.bandybus.infwhat.creative;

import com.bandybus.infwhat.block.ModBlocks;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Dedicated creative tab for all Infinity What items. The label intentionally contains the mod id
 * so the shared {@code itemGroup.infwhat.infinity_what} translation key is used.
 */
public final class ModCreativeTab extends CreativeTabs {

    /** Single tab instance shared by every block and item of the mod. */
    public static final ModCreativeTab INSTANCE = new ModCreativeTab();

    private ModCreativeTab() {
        super("infwhat.infinity_what");
    }

    @Override
    public Item getTabIconItem() {
        // 1.7.10 has no Item#getItemFromBlock helper, so the block item is taken from a stack.
        return new ItemStack(ModBlocks.INF_ITEM_GENERATOR).getItem();
    }

    @Override
    public ItemStack getIconItemStack() {
        return new ItemStack(ModBlocks.INF_ITEM_GENERATOR);
    }
}

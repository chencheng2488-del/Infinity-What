package com.bandybus.infwhat.creative;

import com.bandybus.infwhat.block.ModBlocks;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Dedicated creative tab for all Infinity What items. The label intentionally contains the mod
 * id so the shared {@code itemGroup.infwhat.infinity_what} translation key is used.
 */
public final class ModCreativeTab extends CreativeTabs {

    public static final ModCreativeTab INSTANCE = new ModCreativeTab();

    private ModCreativeTab() {
        super("infwhat.infinity_what");
    }

    @Override
    public Item getTabIconItem() {
        return Item.getItemFromBlock(ModBlocks.INF_ITEM_GENERATOR);
    }

    @Override
    public ItemStack getIconItemStack() {
        return new ItemStack(ModBlocks.INF_ITEM_GENERATOR);
    }
}

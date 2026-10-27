package com.bandybus.infwhat.creative;

import com.bandybus.infwhat.block.ModBlocks;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;

/**
 * Dedicated creative tab for all Infinity What items. The label intentionally contains the mod
 * id ({@code infwhat.infinity_what}) so the existing {@code itemGroup.infwhat.infinity_what}
 * translation key is shared with the modern releases.
 */
public final class ModCreativeTab extends CreativeTabs {

    public static final ModCreativeTab INSTANCE = new ModCreativeTab();

    private ModCreativeTab() {
        super("infwhat.infinity_what");
    }

    @Override
    public ItemStack createIcon() {
        return new ItemStack(ModBlocks.INF_ITEM_GENERATOR);
    }
}

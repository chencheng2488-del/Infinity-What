package com.bandybus.infwhat.creative;

import com.bandybus.infwhat.block.ModBlocks;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

/**
 * Dedicated creative tab for all Infinity What items.
 *
 * <p>1.6.4 Forge grows {@code CreativeTabs.creativeTabArray} when a tab is constructed with an
 * index at or beyond the current length. Using the current length (Forge's own
 * {@code getNextID()} logic) keeps this tab from overwriting Thaumcraft's tab at index 12.
 */
public final class ModCreativeTab extends CreativeTabs {

    /** Single tab instance shared by every block and item of the mod. */
    public static final ModCreativeTab INSTANCE = new ModCreativeTab();

    private ModCreativeTab() {
        super(nextTabIndex(), "infwhat.infinity_what");
    }

    /** Same value as Forge's {@code CreativeTabs.getNextID()}, which is absent from the MCP jar. */
    private static int nextTabIndex() {
        return CreativeTabs.creativeTabArray.length;
    }

    @Override
    public Item getTabIconItem() {
        // 1.6.4 maps block ids to block items through the item list.
        return Item.itemsList[ModBlocks.INF_ITEM_GENERATOR.blockID];
    }
}

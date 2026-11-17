package com.bandybus.infwhat.creative;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.ModBlocks;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/** Dedicated creative tab for all Infinity What items. */
public final class ModCreativeTabs {

    /**
     * 1.18.2 has no {@code CreativeModeTabEvent}: Forge's {@link CreativeModeTab} string
     * constructor appends the new tab to {@code CreativeModeTab.TABS}, so constructing it
     * registers the tab. Items are attached to it at registration time through
     * {@code Item.Properties#tab(...)} (see {@code ModItems} and {@code ModBlocks}).
     */
    public static final CreativeModeTab INFINITY_WHAT = new CreativeModeTab(
            InfinityWhat.MOD_ID + ".infinity_what") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(ModBlocks.INF_ITEM_GENERATOR.get());
        }
    // 1.18.2 has no default tab background for modded tabs; reuse the vanilla generic items
    // background so the tab does not render a missing texture.
    }.setBackgroundSuffix("items.png");

    private ModCreativeTabs() {}

    /**
     * Forces this class to initialise during mod construction so the tab exists before the
     * creative menu is built.
     */
    public static void init() {
        INFINITY_WHAT.getClass();
    }
}

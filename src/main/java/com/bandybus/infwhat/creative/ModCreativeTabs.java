package com.bandybus.infwhat.creative;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.ModBlocks;
import com.bandybus.infwhat.item.ModItems;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/** Dedicated creative tab for all Infinity What items. */
public final class ModCreativeTabs {

    /**
     * 1.19.2 creative tabs are not a registry and Forge provides no registration event for them:
     * the {@link CreativeModeTab} constructor itself appends the new tab to
     * {@code CreativeModeTab.TABS}, so simply constructing it registers the tab.
     */
    public static final CreativeModeTab INFINITY_WHAT = new CreativeModeTab(
            InfinityWhat.MOD_ID + ".infinity_what") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(ModBlocks.INF_ITEM_GENERATOR.get());
        }

        @Override
        public void fillItemList(NonNullList<ItemStack> items) {
            super.fillItemList(items);
            items.add(new ItemStack(ModBlocks.INF_FLUID_GENERATOR.get()));
            items.add(new ItemStack(ModBlocks.INF_ITEM_GENERATOR.get()));
            items.add(new ItemStack(ModItems.INFINITE_SOURCE_CONFIG_REPLICATOR.get()));
        }
    // 1.19.2 has no default tab background for modded tabs; reuse the vanilla generic items
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

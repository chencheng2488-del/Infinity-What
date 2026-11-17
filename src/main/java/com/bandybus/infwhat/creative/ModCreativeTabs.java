package com.bandybus.infwhat.creative;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.ModBlocks;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;

/** Dedicated creative tab for all Infinity What items. */
public final class ModCreativeTabs {

    /**
     * 1.14.3 has no creative-tab registry: Forge's {@link ItemGroup} string constructor appends
     * the new tab to {@code ItemGroup.GROUPS}, so constructing it registers the tab. Items are
     * attached to it at registration time through {@code Item.Properties#group(...)}
     * (see {@code ModItems}).
     */
    public static final ItemGroup INFINITY_WHAT = new ItemGroup(
            InfinityWhat.MOD_ID + ".infinity_what") {
        @Override
        public ItemStack createIcon() {
            return new ItemStack(ModBlocks.INF_ITEM_GENERATOR);
        }
    // 1.14.3 has no default tab background for modded tabs; reuse the vanilla generic items
    // background so the tab does not render a missing texture.
    }.setBackgroundImageName("items.png");

    private ModCreativeTabs() {}

    /**
     * Forces this class to initialise during mod construction so the tab exists before the
     * creative menu is built.
     */
    public static void init() {
        INFINITY_WHAT.getClass();
    }
}

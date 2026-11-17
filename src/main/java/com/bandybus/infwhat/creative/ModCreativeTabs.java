package com.bandybus.infwhat.creative;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.ModBlocks;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;

/** Dedicated creative tab for all Infinity What items. */
public final class ModCreativeTabs {

    /**
     * 1.13.2 has no creative-tab registry: constructing an {@link ItemGroup} appends it to
     * {@code ItemGroup.GROUPS}. Items are attached through {@code Item.Properties#group(...)}.
     */
    public static final ItemGroup INFINITY_WHAT = new ItemGroup(
            InfinityWhat.MOD_ID + ".infinity_what") {
        @Override
        public ItemStack createIcon() {
            return new ItemStack(ModBlocks.INF_ITEM_GENERATOR);
        }
    }.setBackgroundImageName("items.png");

    private ModCreativeTabs() {
    }

    /** Forces this class to initialise so the tab exists before the creative menu is built. */
    public static void init() {
        INFINITY_WHAT.getClass();
    }
}

package com.bandybus.infwhat;

import com.bandybus.infwhat.block.ModBlocks;
import com.bandybus.infwhat.item.ModItems;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/** Crafting recipes for the 1.6.4 release (registered in code; 1.6.4 has no data recipes). */
public final class ModRecipes {

    private ModRecipes() {
    }

    /** Registers the three shaped recipes of the mod. */
    public static void register() {
        GameRegistry.addRecipe(new ItemStack(ModBlocks.INF_FLUID_GENERATOR),
                "IDI", "DBD", "IDI",
                'I', Item.ingotIron, 'D', Item.diamond, 'B', Item.bucketEmpty);

        GameRegistry.addRecipe(new ItemStack(ModBlocks.INF_ITEM_GENERATOR),
                "IDI", "DCD", "IDI",
                'I', Item.ingotIron, 'D', Item.diamond, 'C', Block.chest);

        GameRegistry.addRecipe(new ItemStack(ModItems.INFINITE_SOURCE_CONFIG_REPLICATOR),
                " P ", " R ", " I ",
                'P', Item.paper, 'R', Item.redstone, 'I', Item.ingotIron);
    }
}

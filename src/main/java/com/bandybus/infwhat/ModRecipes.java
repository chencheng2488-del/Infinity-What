package com.bandybus.infwhat;

import com.bandybus.infwhat.block.ModBlocks;
import com.bandybus.infwhat.item.ModItems;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

/** Crafting recipes for the 1.7.10 release (registered in code, as 1.7.10 has no data recipes). */
public final class ModRecipes {

    private ModRecipes() {
    }

    /** Registers the three shaped recipes of the mod. */
    public static void register() {
        GameRegistry.addRecipe(new ItemStack(ModBlocks.INF_FLUID_GENERATOR),
                "IDI", "DBD", "IDI",
                'I', Items.iron_ingot, 'D', Items.diamond, 'B', Items.bucket);

        GameRegistry.addRecipe(new ItemStack(ModBlocks.INF_ITEM_GENERATOR),
                "IDI", "DCD", "IDI",
                'I', Items.iron_ingot, 'D', Items.diamond, 'C', Blocks.chest);

        GameRegistry.addRecipe(new ItemStack(ModItems.INFINITE_SOURCE_CONFIG_REPLICATOR),
                " P ", " R ", " I ",
                'P', Items.paper, 'R', Items.redstone, 'I', Items.iron_ingot);
    }
}

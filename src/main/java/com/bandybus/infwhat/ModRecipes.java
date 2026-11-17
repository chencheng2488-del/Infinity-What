package com.bandybus.infwhat;

import com.bandybus.infwhat.block.ModBlocks;
import com.bandybus.infwhat.item.ModItems;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.registry.GameRegistry;

/** Crafting recipes for the 1.9 release (registered in code). */
public final class ModRecipes {

    private ModRecipes() {
    }

    public static void register() {
        GameRegistry.addRecipe(new ItemStack(ModBlocks.INF_FLUID_GENERATOR),
                "IDI", "DBD", "IDI",
                'I', Items.IRON_INGOT, 'D', Items.DIAMOND, 'B', Items.BUCKET);

        GameRegistry.addRecipe(new ItemStack(ModBlocks.INF_ITEM_GENERATOR),
                "IDI", "DCD", "IDI",
                'I', Items.IRON_INGOT, 'D', Items.DIAMOND,
                'C', Item.getItemFromBlock(Blocks.CHEST));

        GameRegistry.addRecipe(new ItemStack(ModItems.INFINITE_SOURCE_CONFIG_REPLICATOR),
                " P ", " R ", " I ",
                'P', Items.PAPER, 'R', Items.REDSTONE, 'I', Items.IRON_INGOT);
    }
}

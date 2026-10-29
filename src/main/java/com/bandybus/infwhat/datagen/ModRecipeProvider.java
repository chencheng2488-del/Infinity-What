package com.bandybus.infwhat.datagen;

import com.bandybus.infwhat.block.ModBlocks;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.IFinishedRecipe;
import net.minecraft.data.RecipeProvider;
import net.minecraft.data.ShapedRecipeBuilder;
import net.minecraft.item.Items;

import java.util.function.Consumer;

public class ModRecipeProvider extends RecipeProvider {

    public ModRecipeProvider(DataGenerator generator) {
        super(generator);
    }

    @Override
    protected void buildShapelessRecipes(Consumer<IFinishedRecipe> writer) {
        ShapedRecipeBuilder.shaped(ModBlocks.INF_FLUID_GENERATOR.get())
                .pattern("IDI")
                .pattern("DBD")
                .pattern("IDI")
                .define('I', Items.IRON_INGOT)
                .define('D', Items.DIAMOND)
                .define('B', Items.BUCKET)
                .unlockedBy("has_bucket", has(Items.BUCKET))
                .save(writer);

        ShapedRecipeBuilder.shaped(ModBlocks.INF_ITEM_GENERATOR.get())
                .pattern("IDI")
                .pattern("DCD")
                .pattern("IDI")
                .define('I', Items.IRON_INGOT)
                .define('D', Items.DIAMOND)
                .define('C', Items.CHEST)
                .unlockedBy("has_chest", has(Items.CHEST))
                .save(writer);
    }
}

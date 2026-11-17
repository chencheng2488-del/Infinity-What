package com.bandybus.infwhat.datagen;

import com.bandybus.infwhat.block.ModBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider.Runner {

    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        return new RecipeProvider(registries, output) {
            @Override
            protected void buildRecipes() {
                ShapedRecipeBuilder.shaped(registries.lookupOrThrow(Registries.ITEM),
                                RecipeCategory.MISC, ModBlocks.INF_FLUID_GENERATOR.get())
                        .pattern("IDI")
                        .pattern("DBD")
                        .pattern("IDI")
                        .define('I', Items.IRON_INGOT)
                        .define('D', Items.DIAMOND)
                        .define('B', Items.BUCKET)
                        .unlockedBy("has_bucket", has(Items.BUCKET))
                        .save(output);

                ShapedRecipeBuilder.shaped(registries.lookupOrThrow(Registries.ITEM),
                                RecipeCategory.MISC, ModBlocks.INF_ITEM_GENERATOR.get())
                        .pattern("IDI")
                        .pattern("DCD")
                        .pattern("IDI")
                        .define('I', Items.IRON_INGOT)
                        .define('D', Items.DIAMOND)
                        .define('C', Items.CHEST)
                        .unlockedBy("has_chest", has(Items.CHEST))
                        .save(output);
            }
        };
    }

    @Override
    public String getName() {
        return "Infinity What Recipes";
    }
}

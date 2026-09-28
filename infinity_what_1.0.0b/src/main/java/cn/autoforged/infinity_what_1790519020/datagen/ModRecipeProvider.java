package cn.autoforged.infinity_what_1790519020.datagen;

import cn.autoforged.infinity_what_1790519020.block.ModBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;

import java.util.function.Consumer;

public class ModRecipeProvider extends RecipeProvider {

    public ModRecipeProvider(PackOutput output) {
        super(output);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> writer) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.INF_FLUID_GENERATOR.get())
                .pattern("IDI")
                .pattern("DBD")
                .pattern("IDI")
                .define('I', Items.IRON_INGOT)
                .define('D', Items.DIAMOND)
                .define('B', Items.BUCKET)
                .unlockedBy("has_bucket", has(Items.BUCKET))
                .save(writer);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.INF_ITEM_GENERATOR.get())
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

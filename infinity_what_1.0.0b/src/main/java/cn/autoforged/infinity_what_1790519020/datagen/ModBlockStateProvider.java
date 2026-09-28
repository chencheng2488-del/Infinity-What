package cn.autoforged.infinity_what_1790519020.datagen;

import cn.autoforged.infinity_what_1790519020.InfinityWhat;
import cn.autoforged.infinity_what_1790519020.block.ModBlocks;
import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

public class ModBlockStateProvider extends BlockStateProvider {

    public ModBlockStateProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, InfinityWhat.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        simpleBlockWithItem(ModBlocks.INF_FLUID_GENERATOR.get(),
                cubeAll(ModBlocks.INF_FLUID_GENERATOR.get()));
        simpleBlockWithItem(ModBlocks.INF_ITEM_GENERATOR.get(),
                cubeAll(ModBlocks.INF_ITEM_GENERATOR.get()));
    }
}

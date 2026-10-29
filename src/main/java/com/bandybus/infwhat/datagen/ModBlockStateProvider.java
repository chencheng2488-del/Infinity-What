package com.bandybus.infwhat.datagen;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.ModBlocks;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;

public class ModBlockStateProvider extends BlockStateProvider {

    public ModBlockStateProvider(DataGenerator generator, ExistingFileHelper existingFileHelper) {
        super(generator, InfinityWhat.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        // 1.18.2 has no simpleBlockWithItem helper, so the block model and the matching
        // block item model are generated explicitly.
        ModelFile fluidModel = cubeAll(ModBlocks.INF_FLUID_GENERATOR.get());
        simpleBlock(ModBlocks.INF_FLUID_GENERATOR.get(), fluidModel);
        simpleBlockItem(ModBlocks.INF_FLUID_GENERATOR.get(), fluidModel);

        ModelFile itemModel = cubeAll(ModBlocks.INF_ITEM_GENERATOR.get());
        simpleBlock(ModBlocks.INF_ITEM_GENERATOR.get(), itemModel);
        simpleBlockItem(ModBlocks.INF_ITEM_GENERATOR.get(), itemModel);
    }
}

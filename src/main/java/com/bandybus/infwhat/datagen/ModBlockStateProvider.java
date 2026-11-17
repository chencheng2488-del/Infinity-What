package com.bandybus.infwhat.datagen;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.ModBlocks;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

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

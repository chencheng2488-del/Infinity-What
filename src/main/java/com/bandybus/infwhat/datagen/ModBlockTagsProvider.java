package com.bandybus.infwhat.datagen;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.ModBlocks;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.tags.BlockTagsProvider;
import net.minecraft.tags.BlockTags;
import net.minecraftforge.common.data.ExistingFileHelper;

import javax.annotation.Nullable;

public class ModBlockTagsProvider extends BlockTagsProvider {

    public ModBlockTagsProvider(DataGenerator generator, @Nullable ExistingFileHelper existingFileHelper) {
        super(generator, InfinityWhat.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags() {
        // wooden pickaxe or higher: no tier tag required means any pickaxe works.
        this.tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.INF_FLUID_GENERATOR.get());
        this.tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.INF_ITEM_GENERATOR.get());
    }
}

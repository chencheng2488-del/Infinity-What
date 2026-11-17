package com.bandybus.infwhat.datagen;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.ModBlocks;
import net.minecraft.data.BlockTagsProvider;
import net.minecraft.data.DataGenerator;
import net.minecraft.tags.BlockTags;
import net.minecraftforge.common.data.ExistingFileHelper;

import javax.annotation.Nullable;

public class ModBlockTagsProvider extends BlockTagsProvider {

    public ModBlockTagsProvider(DataGenerator generator, @Nullable ExistingFileHelper existingFileHelper) {
        super(generator, InfinityWhat.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags() {
        // wooden pickaxe or higher: 1.16.5 has no BlockTags constant for the modern
        // mineable/pickaxe tag, so the named tag is bound directly.
        this.tag(BlockTags.bind("mineable/pickaxe")).add(ModBlocks.INF_FLUID_GENERATOR.get());
        this.tag(BlockTags.bind("mineable/pickaxe")).add(ModBlocks.INF_ITEM_GENERATOR.get());
    }
}

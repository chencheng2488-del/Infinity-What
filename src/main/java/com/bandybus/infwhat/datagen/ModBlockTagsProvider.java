package com.bandybus.infwhat.datagen;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.ModBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.ExistingFileHelper;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

public class ModBlockTagsProvider extends TagsProvider<Block> {

    public ModBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                                @Nullable ExistingFileHelper existingFileHelper) {
        super(output, Registries.BLOCK, lookupProvider, InfinityWhat.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        // wooden pickaxe or higher: no tier tag required means any pickaxe works.
        this.tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(BuiltInRegistries.BLOCK.getResourceKey(ModBlocks.INF_FLUID_GENERATOR.get()).orElseThrow());
        this.tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(BuiltInRegistries.BLOCK.getResourceKey(ModBlocks.INF_ITEM_GENERATOR.get()).orElseThrow());
    }
}

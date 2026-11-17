package com.bandybus.infwhat.datagen;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.ModBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

public class ModBlockTagsProvider extends BlockTagsProvider {

    public ModBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                                @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, InfinityWhat.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        // wooden pickaxe or higher: no tier tag required means any pickaxe works.
        this.tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.INF_FLUID_GENERATOR.get());
        this.tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.INF_ITEM_GENERATOR.get());
    }
}

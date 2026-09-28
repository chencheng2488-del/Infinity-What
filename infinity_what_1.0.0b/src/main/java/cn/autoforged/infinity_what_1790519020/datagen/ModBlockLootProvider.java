package cn.autoforged.infinity_what_1790519020.datagen;

import cn.autoforged.infinity_what_1790519020.block.ModBlocks;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.RegistryObject;

import java.util.Collections;

public class ModBlockLootProvider extends BlockLootSubProvider {

    protected ModBlockLootProvider() {
        super(Collections.emptySet(), FeatureFlags.REGISTRY.allFlags());
    }

    @Override
    protected void generate() {
        dropSelf(ModBlocks.INF_FLUID_GENERATOR.get());
        dropSelf(ModBlocks.INF_ITEM_GENERATOR.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream()
                .flatMap(RegistryObject::stream)::iterator;
    }
}

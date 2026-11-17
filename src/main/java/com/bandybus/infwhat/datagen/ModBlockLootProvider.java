package com.bandybus.infwhat.datagen;

import com.bandybus.infwhat.block.ModBlocks;
import com.mojang.datafixers.util.Pair;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.loot.BlockLoot;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ModBlockLootProvider extends LootTableProvider {

    public ModBlockLootProvider(DataGenerator generator) {
        super(generator);
    }

    @Override
    protected List<Pair<Supplier<Consumer<BiConsumer<ResourceLocation, LootTable.Builder>>>, LootContextParamSet>> getTables() {
        return List.of(Pair.of(ModBlockLootTables::new, LootContextParamSets.BLOCK));
    }

    private static class ModBlockLootTables extends BlockLoot {

        @Override
        protected void addTables() {
            dropSelf(ModBlocks.INF_FLUID_GENERATOR.get());
            dropSelf(ModBlocks.INF_ITEM_GENERATOR.get());
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return ModBlocks.BLOCKS.getEntries().stream()
                    .flatMap(RegistryObject::stream)::iterator;
        }
    }
}

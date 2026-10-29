package com.bandybus.infwhat.datagen;

import com.bandybus.infwhat.block.ModBlocks;
import com.mojang.datafixers.util.Pair;
import net.minecraft.block.Block;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.loot.BlockLootTables;
import net.minecraft.data.LootTableProvider;
import net.minecraft.loot.LootParameterSet;
import net.minecraft.loot.LootParameterSets;
import net.minecraft.loot.LootTable;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.RegistryObject;

import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ModBlockLootProvider extends LootTableProvider {

    public ModBlockLootProvider(DataGenerator generator) {
        super(generator);
    }

    @Override
    protected List<Pair<Supplier<Consumer<BiConsumer<ResourceLocation, LootTable.Builder>>>, LootParameterSet>> getTables() {
        return Collections.singletonList(Pair.of(ModBlockLootTables::new, LootParameterSets.BLOCK));
    }

    private static class ModBlockLootTables extends BlockLootTables {

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

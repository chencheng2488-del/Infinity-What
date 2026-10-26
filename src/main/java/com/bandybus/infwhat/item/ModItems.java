package com.bandybus.infwhat.item;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.ModBlocks;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

public class ModItems {
    public static Item INFINITE_SOURCE_CONFIG_REPLICATOR;
    public static Item INF_FLUID_GENERATOR_ITEM;
    public static Item INF_ITEM_GENERATOR_ITEM;

    public static void register() {
        INFINITE_SOURCE_CONFIG_REPLICATOR = Registry.register(BuiltInRegistries.ITEM,
                ResourceLocation.fromNamespaceAndPath(InfinityWhat.MOD_ID, "infinite_source_config_replicator"),
                new InfiniteSourceConfigureReplicatorItem(new Item.Properties().stacksTo(1)));

        // Spec: stack_size = 1
        INF_FLUID_GENERATOR_ITEM = registerBlockItem("inf_fluid_generator", ModBlocks.INF_FLUID_GENERATOR);
        INF_ITEM_GENERATOR_ITEM = registerBlockItem("inf_item_generator", ModBlocks.INF_ITEM_GENERATOR);
    }

    private static Item registerBlockItem(String name, net.minecraft.world.level.block.Block block) {
        BlockItem blockItem = new BlockItem(block, new Item.Properties().stacksTo(1));
        Item registered = Registry.register(BuiltInRegistries.ITEM,
                ResourceLocation.fromNamespaceAndPath(InfinityWhat.MOD_ID, name), blockItem);
        // Vanilla links block items to their blocks through this map (creative pick block, etc.).
        blockItem.registerBlocks(Item.BY_BLOCK, blockItem);
        return registered;
    }
}

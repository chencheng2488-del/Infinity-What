package com.bandybus.infwhat.item;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.ModBlocks;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

public class ModItems {
    public static Item INFINITE_SOURCE_CONFIG_REPLICATOR;
    public static Item INF_FLUID_GENERATOR_ITEM;
    public static Item INF_ITEM_GENERATOR_ITEM;

    public static void register() {
        INFINITE_SOURCE_CONFIG_REPLICATOR = Registry.register(BuiltInRegistries.ITEM,
                Identifier.fromNamespaceAndPath(InfinityWhat.MOD_ID, "infinite_source_config_replicator"),
                new InfiniteSourceConfigureReplicatorItem(new Item.Properties().stacksTo(1)
                        .setId(itemKey("infinite_source_config_replicator"))));

        // Spec: stack_size = 1
        INF_FLUID_GENERATOR_ITEM = registerBlockItem("inf_fluid_generator", ModBlocks.INF_FLUID_GENERATOR);
        INF_ITEM_GENERATOR_ITEM = registerBlockItem("inf_item_generator", ModBlocks.INF_ITEM_GENERATOR);
    }

    private static Item registerBlockItem(String name, net.minecraft.world.level.block.Block block) {
        // 1.21.2+ requires the item id up front, and block items must opt into the block
        // description prefix or they display their raw registry id instead of the block name.
        BlockItem blockItem = new BlockItem(block, new Item.Properties().stacksTo(1)
                .setId(itemKey(name))
                .useBlockDescriptionPrefix());
        Item registered = Registry.register(BuiltInRegistries.ITEM,
                Identifier.fromNamespaceAndPath(InfinityWhat.MOD_ID, name), blockItem);
        // Vanilla links block items to their blocks through this map (creative pick block, etc.).
        blockItem.registerBlocks(Item.BY_BLOCK, blockItem);
        return registered;
    }

    private static ResourceKey<Item> itemKey(String name) {
        return ResourceKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath(InfinityWhat.MOD_ID, name));
    }
}

package com.bandybus.infwhat.item;

import com.bandybus.infwhat.InfinityWhat;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ModItems {
    public static final Item INFINITE_SOURCE_CONFIG_REPLICATOR = register(
            "infinite_source_config_replicator",
            new InfiniteSourceConfigureReplicatorItem(new Item.Properties().stacksTo(1)));

    public static Item registerBlockItem(String name, Block block) {
        return register(name, new BlockItem(block, new Item.Properties().stacksTo(1)));
    }

    private static Item register(String name, Item item) {
        return Registry.register(BuiltInRegistries.ITEM, InfinityWhat.id(name), item);
    }

    public static void register() {
    }
}

package com.bandybus.infwhat.item;

import com.bandybus.infwhat.InfinityWhat;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

public class ModItems {

    public static Item INFINITE_SOURCE_CONFIG_REPLICATOR;

    public static void register() {
        INFINITE_SOURCE_CONFIG_REPLICATOR = Registry.register(Registry.ITEM,
                new ResourceLocation(InfinityWhat.MOD_ID, "infinite_source_config_replicator"),
                new InfiniteSourceConfigureReplicatorItem(new Item.Properties().stacksTo(1)));
    }

    private ModItems() {
    }
}

package com.bandybus.infwhat.item;

import com.bandybus.infwhat.InfinityWhat;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.item.Item;

public class ModItems {

    public static Item INFINITE_SOURCE_CONFIG_REPLICATOR;

    public static void register() {
        INFINITE_SOURCE_CONFIG_REPLICATOR = Registry.register(Registry.ITEM,
                new Identifier(InfinityWhat.MOD_ID, "infinite_source_config_replicator"),
                new InfiniteSourceConfigureReplicatorItem(new Item.Settings().stackSize(1)));
    }

    private ModItems() {
    }
}

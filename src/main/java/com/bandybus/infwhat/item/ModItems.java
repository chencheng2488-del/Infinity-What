package com.bandybus.infwhat.item;

import com.bandybus.infwhat.InfinityWhat;
import net.minecraft.world.item.Item;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, InfinityWhat.MOD_ID);

    public static final DeferredHolder<Item, Item> INFINITE_SOURCE_CONFIG_REPLICATOR = ITEMS.register(
            "infinite_source_config_replicator",
            () -> new InfiniteSourceConfigureReplicatorItem(new Item.Properties().stacksTo(1)
                    .setId(ResourceKey.create(Registries.ITEM,
                            ResourceLocation.fromNamespaceAndPath(InfinityWhat.MOD_ID, "infinite_source_config_replicator")))));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}

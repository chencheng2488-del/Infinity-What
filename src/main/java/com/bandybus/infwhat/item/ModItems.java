package com.bandybus.infwhat.item;

import com.bandybus.infwhat.InfinityWhat;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, InfinityWhat.MOD_ID);

    public static final RegistryObject<Item> INFINITE_SOURCE_CONFIG_REPLICATOR = ITEMS.register(
            "infinite_source_config_replicator",
            () -> new InfiniteSourceConfigureReplicatorItem(new Item.Properties().stacksTo(1)));

    public static void register(BusGroup eventBus) {
        ITEMS.register(eventBus);
    }
}

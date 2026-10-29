package com.bandybus.infwhat.item;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.creative.ModCreativeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, InfinityWhat.MOD_ID);

    public static final RegistryObject<Item> INFINITE_SOURCE_CONFIG_REPLICATOR = ITEMS.register(
            "infinite_source_config_replicator",
            () -> new InfiniteSourceConfigureReplicatorItem(new Item.Properties()
                    .stacksTo(1)
                    // 1.18.2 attaches items to a creative tab through the item properties.
                    .tab(ModCreativeTabs.INFINITY_WHAT)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}

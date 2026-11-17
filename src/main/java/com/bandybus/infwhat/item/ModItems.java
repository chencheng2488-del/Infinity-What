package com.bandybus.infwhat.item;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.ModBlocks;
import com.bandybus.infwhat.creative.ModCreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.IForgeRegistry;

@Mod.EventBusSubscriber(modid = InfinityWhat.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModItems {

    public static final Item INFINITE_SOURCE_CONFIG_REPLICATOR = new InfiniteSourceConfigureReplicatorItem(
            new Item.Properties()
                    .maxStackSize(1)
                    .group(ModCreativeTabs.INFINITY_WHAT))
            .setRegistryName(InfinityWhat.MOD_ID, "infinite_source_config_replicator");

    public static final Item INF_FLUID_GENERATOR = new ItemBlock(
            ModBlocks.INF_FLUID_GENERATOR,
            new Item.Properties()
                    .maxStackSize(1)
                    .group(ModCreativeTabs.INFINITY_WHAT))
            .setRegistryName(InfinityWhat.MOD_ID, "inf_fluid_generator");

    public static final Item INF_ITEM_GENERATOR = new ItemBlock(
            ModBlocks.INF_ITEM_GENERATOR,
            new Item.Properties()
                    .maxStackSize(1)
                    .group(ModCreativeTabs.INFINITY_WHAT))
            .setRegistryName(InfinityWhat.MOD_ID, "inf_item_generator");

    private ModItems() {
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        IForgeRegistry<Item> registry = event.getRegistry();
        registry.register(INFINITE_SOURCE_CONFIG_REPLICATOR);
        registry.register(INF_FLUID_GENERATOR);
        registry.register(INF_ITEM_GENERATOR);
    }
}

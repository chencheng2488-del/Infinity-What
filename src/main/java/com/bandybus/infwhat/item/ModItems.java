package com.bandybus.infwhat.item;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.ModBlocks;
import com.bandybus.infwhat.creative.ModCreativeTabs;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Items are plain static fields on 1.14.3: Forge 27 predates {@code DeferredRegister}, so they
 * are registered from the {@link RegistryEvent.Register} callback instead.
 */
@Mod.EventBusSubscriber(modid = InfinityWhat.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModItems {

    // Spec: stack_size = 1 for every Infinity What item.
    public static final Item INF_FLUID_GENERATOR = new BlockItem(ModBlocks.INF_FLUID_GENERATOR,
            new Item.Properties().maxStackSize(1).group(ModCreativeTabs.INFINITY_WHAT))
            .setRegistryName(ModBlocks.INF_FLUID_GENERATOR.getRegistryName());

    public static final Item INF_ITEM_GENERATOR = new BlockItem(ModBlocks.INF_ITEM_GENERATOR,
            new Item.Properties().maxStackSize(1).group(ModCreativeTabs.INFINITY_WHAT))
            .setRegistryName(ModBlocks.INF_ITEM_GENERATOR.getRegistryName());

    public static final Item INFINITE_SOURCE_CONFIG_REPLICATOR = new InfiniteSourceConfigureReplicatorItem(
            new Item.Properties().maxStackSize(1).group(ModCreativeTabs.INFINITY_WHAT))
            .setRegistryName(InfinityWhat.MOD_ID, "infinite_source_config_replicator");

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(INF_FLUID_GENERATOR, INF_ITEM_GENERATOR,
                INFINITE_SOURCE_CONFIG_REPLICATOR);
    }
}

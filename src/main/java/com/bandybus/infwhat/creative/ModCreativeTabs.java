package com.bandybus.infwhat.creative;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.ModBlocks;
import com.bandybus.infwhat.item.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.CreativeModeTabEvent;

/** Dedicated creative tab for all Infinity What items. */
public final class ModCreativeTabs {

    /**
     * 1.19.4 creative tabs are not a registry: Forge hands every mod a
     * {@link CreativeModeTabEvent.Register} on the mod event bus, and the returned tab is kept
     * here. The tab itself is created by Forge so it is placed and ordered correctly.
     */
    public static CreativeModeTab INFINITY_WHAT;

    private ModCreativeTabs() {}

    public static void register(CreativeModeTabEvent.Register event) {
        INFINITY_WHAT = event.registerCreativeModeTab(
                ResourceLocation.fromNamespaceAndPath(InfinityWhat.MOD_ID, "infinity_what"),
                builder -> builder
                        .title(Component.translatable("itemGroup.infwhat.infinity_what"))
                        .icon(() -> new ItemStack(ModBlocks.INF_ITEM_GENERATOR.get()))
                        .displayItems((parameters, output) -> {
                            output.accept(ModBlocks.INF_FLUID_GENERATOR.get());
                            output.accept(ModBlocks.INF_ITEM_GENERATOR.get());
                            output.accept(ModItems.INFINITE_SOURCE_CONFIG_REPLICATOR.get());
                        }));
    }
}

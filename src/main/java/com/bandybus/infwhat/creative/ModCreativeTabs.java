package com.bandybus.infwhat.creative;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.ModBlocks;
import com.bandybus.infwhat.item.ModItems;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/** Dedicated creative tab for all Infinity What items. */
public final class ModCreativeTabs {
    public static CreativeModeTab INFINITY_WHAT;

    public static void register() {
        INFINITY_WHAT = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
                Identifier.fromNamespaceAndPath(InfinityWhat.MOD_ID, "infinity_what"),
                FabricCreativeModeTab.builder()
                        .title(Component.translatable("itemGroup.infwhat.infinity_what"))
                        .icon(() -> new ItemStack(ModBlocks.INF_ITEM_GENERATOR))
                        .displayItems((parameters, output) -> {
                            output.accept(ModBlocks.INF_FLUID_GENERATOR);
                            output.accept(ModBlocks.INF_ITEM_GENERATOR);
                            output.accept(ModItems.INFINITE_SOURCE_CONFIG_REPLICATOR);
                        })
                        .build());
    }

    private ModCreativeTabs() {}
}

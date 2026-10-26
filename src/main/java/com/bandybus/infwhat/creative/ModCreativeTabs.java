package com.bandybus.infwhat.creative;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.ModBlocks;
import com.bandybus.infwhat.item.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** Dedicated creative tab for all Infinity What items. */
public final class ModCreativeTabs {
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, InfinityWhat.MOD_ID);

    public static final RegistryObject<CreativeModeTab> INFINITY_WHAT = TABS.register(
            "infinity_what", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.infwhat.infinity_what"))
                    .icon(() -> new ItemStack(ModBlocks.INF_ITEM_GENERATOR.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModBlocks.INF_FLUID_GENERATOR.get());
                        output.accept(ModBlocks.INF_ITEM_GENERATOR.get());
                        output.accept(ModItems.INFINITE_SOURCE_CONFIG_REPLICATOR.get());
                    })
                    .build());

    private ModCreativeTabs() {}

    public static void register(IEventBus eventBus) {
        TABS.register(eventBus);
    }
}

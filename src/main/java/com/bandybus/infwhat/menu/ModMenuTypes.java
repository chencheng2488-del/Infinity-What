package com.bandybus.infwhat.menu;

import com.bandybus.infwhat.InfinityWhat;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, InfinityWhat.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<InfFluidGeneratorMenu>> INF_FLUID_GENERATOR =
            MENUS.register("inf_fluid_generator",
                    () -> IMenuTypeExtension.create(InfFluidGeneratorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<InfItemGeneratorMenu>> INF_ITEM_GENERATOR =
            MENUS.register("inf_item_generator",
                    () -> IMenuTypeExtension.create(InfItemGeneratorMenu::new));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}

package com.bandybus.infwhat.menu;

import com.bandybus.infwhat.InfinityWhat;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.fmllegacy.RegistryObject;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.CONTAINERS, InfinityWhat.MOD_ID);

    public static final RegistryObject<MenuType<InfFluidGeneratorMenu>> INF_FLUID_GENERATOR =
            MENUS.register("inf_fluid_generator",
                    () -> IForgeContainerType.create(InfFluidGeneratorMenu::new));

    public static final RegistryObject<MenuType<InfItemGeneratorMenu>> INF_ITEM_GENERATOR =
            MENUS.register("inf_item_generator",
                    () -> IForgeContainerType.create(InfItemGeneratorMenu::new));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}

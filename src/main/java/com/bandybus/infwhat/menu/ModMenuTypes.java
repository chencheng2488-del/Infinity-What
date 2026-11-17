package com.bandybus.infwhat.menu;

import com.bandybus.infwhat.InfinityWhat;
import net.minecraft.inventory.container.ContainerType;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModMenuTypes {
    public static final DeferredRegister<ContainerType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.CONTAINERS, InfinityWhat.MOD_ID);

    public static final RegistryObject<ContainerType<InfFluidGeneratorMenu>> INF_FLUID_GENERATOR =
            MENUS.register("inf_fluid_generator",
                    () -> IForgeContainerType.create(InfFluidGeneratorMenu::new));

    public static final RegistryObject<ContainerType<InfItemGeneratorMenu>> INF_ITEM_GENERATOR =
            MENUS.register("inf_item_generator",
                    () -> IForgeContainerType.create(InfItemGeneratorMenu::new));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}

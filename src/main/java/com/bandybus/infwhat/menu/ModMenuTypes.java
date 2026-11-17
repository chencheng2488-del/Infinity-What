package com.bandybus.infwhat.menu;

import com.bandybus.infwhat.InfinityWhat;
import net.minecraft.inventory.container.ContainerType;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = InfinityWhat.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModMenuTypes {

    public static final ContainerType<InfFluidGeneratorMenu> INF_FLUID_GENERATOR =
            IForgeContainerType.create(InfFluidGeneratorMenu::new);

    public static final ContainerType<InfItemGeneratorMenu> INF_ITEM_GENERATOR =
            IForgeContainerType.create(InfItemGeneratorMenu::new);

    static {
        INF_FLUID_GENERATOR.setRegistryName(InfinityWhat.MOD_ID, "inf_fluid_generator");
        INF_ITEM_GENERATOR.setRegistryName(InfinityWhat.MOD_ID, "inf_item_generator");
    }

    @SubscribeEvent
    public static void registerContainerTypes(RegistryEvent.Register<ContainerType<?>> event) {
        event.getRegistry().registerAll(INF_FLUID_GENERATOR, INF_ITEM_GENERATOR);
    }
}

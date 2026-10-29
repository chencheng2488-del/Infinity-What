package com.bandybus.infwhat.menu;

import com.bandybus.infwhat.InfinityWhat;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;

public class ModMenuTypes {

    public static MenuType<InfFluidGeneratorMenu> INF_FLUID_GENERATOR;
    public static MenuType<InfItemGeneratorMenu> INF_ITEM_GENERATOR;

    public static void register() {
        INF_FLUID_GENERATOR = Registry.register(Registry.MENU,
                new ResourceLocation(InfinityWhat.MOD_ID, "inf_fluid_generator"),
                new ExtendedScreenHandlerType<>(
                        (containerId, inventory, data) -> new InfFluidGeneratorMenu(containerId, inventory, data)));

        INF_ITEM_GENERATOR = Registry.register(Registry.MENU,
                new ResourceLocation(InfinityWhat.MOD_ID, "inf_item_generator"),
                new ExtendedScreenHandlerType<>(
                        (containerId, inventory, data) -> new InfItemGeneratorMenu(containerId, inventory, data)));
    }

    private ModMenuTypes() {
    }
}

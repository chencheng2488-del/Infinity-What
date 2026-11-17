package com.bandybus.infwhat.menu;

import com.bandybus.infwhat.InfinityWhat;
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;

public class ModMenuTypes {

    public static MenuType<InfFluidGeneratorMenu> INF_FLUID_GENERATOR;
    public static MenuType<InfItemGeneratorMenu> INF_ITEM_GENERATOR;

    public static void register() {
        // 1.18 / 1.18.1 的 Fabric API 0.46.6 还没有公开的 ExtendedScreenHandlerType。
        INF_FLUID_GENERATOR = ScreenHandlerRegistry.registerExtended(
                new ResourceLocation(InfinityWhat.MOD_ID, "inf_fluid_generator"),
                (containerId, inventory, data) -> new InfFluidGeneratorMenu(containerId, inventory, data));

        INF_ITEM_GENERATOR = ScreenHandlerRegistry.registerExtended(
                new ResourceLocation(InfinityWhat.MOD_ID, "inf_item_generator"),
                (containerId, inventory, data) -> new InfItemGeneratorMenu(containerId, inventory, data));
    }

    private ModMenuTypes() {
    }
}

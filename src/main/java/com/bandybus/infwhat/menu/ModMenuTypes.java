package com.bandybus.infwhat.menu;

import com.bandybus.infwhat.InfinityWhat;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.MenuType;

public class ModMenuTypes {
    public static MenuType<InfFluidGeneratorMenu> INF_FLUID_GENERATOR;
    public static MenuType<InfItemGeneratorMenu> INF_ITEM_GENERATOR;

    public static void register() {
        INF_FLUID_GENERATOR = Registry.register(BuiltInRegistries.MENU,
                Identifier.fromNamespaceAndPath(InfinityWhat.MOD_ID, "inf_fluid_generator"),
                new ExtendedScreenHandlerType<>(InfFluidGeneratorMenu::new, BlockPos.STREAM_CODEC));

        INF_ITEM_GENERATOR = Registry.register(BuiltInRegistries.MENU,
                Identifier.fromNamespaceAndPath(InfinityWhat.MOD_ID, "inf_item_generator"),
                new ExtendedScreenHandlerType<>(InfItemGeneratorMenu::new, BlockPos.STREAM_CODEC));
    }
}

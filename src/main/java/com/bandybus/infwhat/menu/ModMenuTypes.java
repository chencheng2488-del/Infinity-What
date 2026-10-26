package com.bandybus.infwhat.menu;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.entity.InfFluidGeneratorBlockEntity;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;

public class ModMenuTypes {
    public static final MenuType<InfFluidGeneratorMenu> INF_FLUID_GENERATOR =
            Registry.register(BuiltInRegistries.MENU, InfinityWhat.id("inf_fluid_generator"),
                    new ExtendedScreenHandlerType<>(InfFluidGeneratorMenu::new, BlockPos.STREAM_CODEC));

    public static final MenuType<InfItemGeneratorMenu> INF_ITEM_GENERATOR =
            Registry.register(BuiltInRegistries.MENU, InfinityWhat.id("inf_item_generator"),
                    new ExtendedScreenHandlerType<>(InfItemGeneratorMenu::new, BlockPos.STREAM_CODEC));

    public static void register() {
    }

    private ModMenuTypes() {
    }
}

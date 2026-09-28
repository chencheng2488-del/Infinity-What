package cn.autoforged.infinity_what_1790519020.menu;

import cn.autoforged.infinity_what_1790519020.InfinityWhat;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, InfinityWhat.MOD_ID);

    public static final RegistryObject<MenuType<InfFluidGeneratorMenu>> INF_FLUID_GENERATOR =
            MENUS.register("inf_fluid_generator",
                    () -> IForgeMenuType.create(InfFluidGeneratorMenu::new));

    public static final RegistryObject<MenuType<InfItemGeneratorMenu>> INF_ITEM_GENERATOR =
            MENUS.register("inf_item_generator",
                    () -> IForgeMenuType.create(InfItemGeneratorMenu::new));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}

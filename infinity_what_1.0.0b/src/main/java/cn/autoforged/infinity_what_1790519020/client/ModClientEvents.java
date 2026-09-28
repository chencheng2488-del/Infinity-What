package cn.autoforged.infinity_what_1790519020.client;

import cn.autoforged.infinity_what_1790519020.InfinityWhat;
import cn.autoforged.infinity_what_1790519020.client.screen.InfFluidGeneratorScreen;
import cn.autoforged.infinity_what_1790519020.client.screen.InfItemGeneratorScreen;
import cn.autoforged.infinity_what_1790519020.menu.ModMenuTypes;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = InfinityWhat.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModClientEvents {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // MenuScreens.register is not thread safe, so it must run on the main thread.
        event.enqueueWork(() -> {
            MenuScreens.register(
                    ModMenuTypes.INF_FLUID_GENERATOR.get(),
                    InfFluidGeneratorScreen::new);
            MenuScreens.register(
                    ModMenuTypes.INF_ITEM_GENERATOR.get(),
                    InfItemGeneratorScreen::new);
        });
    }
}

package com.bandybus.infwhat.client;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.client.screen.InfFluidGeneratorScreen;
import com.bandybus.infwhat.client.screen.InfItemGeneratorScreen;
import com.bandybus.infwhat.menu.ModMenuTypes;
import net.minecraft.client.gui.screens.MenuScreens;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

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

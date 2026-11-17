package com.bandybus.infwhat.client;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.client.screen.InfFluidGeneratorScreen;
import com.bandybus.infwhat.client.screen.InfItemGeneratorScreen;
import com.bandybus.infwhat.menu.ModMenuTypes;
import net.minecraft.client.gui.ScreenManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DeferredWorkQueue;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = InfinityWhat.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModClientEvents {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // ScreenManager.register is not thread safe, so it must run on the main thread.
        // Forge 32 (1.16.1) lifecycle events have no enqueueWork; DeferredWorkQueue is the
        // equivalent API on this version.
        DeferredWorkQueue.runLater(() -> {
            ScreenManager.register(
                    ModMenuTypes.INF_FLUID_GENERATOR.get(),
                    InfFluidGeneratorScreen::new);
            ScreenManager.register(
                    ModMenuTypes.INF_ITEM_GENERATOR.get(),
                    InfItemGeneratorScreen::new);
        });
    }
}

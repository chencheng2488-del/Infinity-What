package com.bandybus.infwhat.client;

import com.bandybus.infwhat.InfinityWhat;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Client-only setup. 1.13.2 has no ScreenManager: the container GUI is created by the
 * {@link ExtensionPoint#GUIFACTORY} registered on the mod container, which receives the
 * {@code OpenContainer} packet sent by {@code NetworkHooks.openGui}.
 */
@Mod.EventBusSubscriber(modid = InfinityWhat.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT)
public final class ModClientEvents {

    private ModClientEvents() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.GUIFACTORY,
                () -> ClientGuiFactory::open);
    }
}

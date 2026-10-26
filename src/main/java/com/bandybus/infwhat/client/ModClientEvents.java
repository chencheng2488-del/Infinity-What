package com.bandybus.infwhat.client;

import com.bandybus.infwhat.client.network.ClientRateResultHandler;
import com.bandybus.infwhat.client.screen.InfFluidGeneratorScreen;
import com.bandybus.infwhat.client.screen.InfItemGeneratorScreen;
import com.bandybus.infwhat.menu.ModMenuTypes;
import com.bandybus.infwhat.network.ModNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.MenuScreens;

public class ModClientEvents implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        MenuScreens.register(ModMenuTypes.INF_FLUID_GENERATOR, InfFluidGeneratorScreen::new);
        MenuScreens.register(ModMenuTypes.INF_ITEM_GENERATOR, InfItemGeneratorScreen::new);

        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.OutputRateResultPayload.TYPE,
                (payload, context) -> context.client().execute(() -> ClientRateResultHandler.handle(
                        payload.containerId(), payload.fluidGenerator(), payload.success(), payload.amount())));
    }
}

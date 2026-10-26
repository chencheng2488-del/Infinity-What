package com.bandybus.infwhat.client;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.client.screen.InfFluidGeneratorScreen;
import com.bandybus.infwhat.client.screen.InfItemGeneratorScreen;
import com.bandybus.infwhat.client.network.ClientRateResultHandler;
import com.bandybus.infwhat.menu.ModMenuTypes;
import com.bandybus.infwhat.network.ModNetworking;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = InfinityWhat.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModClientEvents {

    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.INF_FLUID_GENERATOR.get(), InfFluidGeneratorScreen::new);
        event.register(ModMenuTypes.INF_ITEM_GENERATOR.get(), InfItemGeneratorScreen::new);
    }

    @SubscribeEvent
    public static void registerClientPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(ModNetworking.OutputRateResultPayload.TYPE,
                ModNetworking.OutputRateResultPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientRateResultHandler.handle(
                        payload.containerId(), payload.fluidGenerator(), payload.success(), payload.amount())));
    }
}

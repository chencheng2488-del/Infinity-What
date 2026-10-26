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

        // 服务端保存回执：先在网络线程读完数据，再切到客户端主线程通知当前屏幕。
        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.OUTPUT_RATE_RESULT,
                (client, handler, buffer, responseSender) -> {
                    int containerId = buffer.readVarInt();
                    boolean fluidGenerator = buffer.readBoolean();
                    boolean success = buffer.readBoolean();
                    long amount = buffer.readLong();
                    client.execute(() -> ClientRateResultHandler.handle(containerId,
                            fluidGenerator, success, amount));
                });
    }
}

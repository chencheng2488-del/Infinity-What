package com.bandybus.infwhat.client;

import com.bandybus.infwhat.client.network.ClientRateResultHandler;
import com.bandybus.infwhat.client.screen.InfFluidGeneratorScreen;
import com.bandybus.infwhat.client.screen.InfItemGeneratorScreen;
import com.bandybus.infwhat.menu.InfFluidGeneratorMenu;
import com.bandybus.infwhat.menu.InfItemGeneratorMenu;
import com.bandybus.infwhat.menu.ModMenuTypes;
import com.bandybus.infwhat.network.ModNetworking;
import com.bandybus.infwhat.network.RateRejectReason;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.ScreenProviderRegistry;

public class ModClientEvents implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // 1.15.2 Fabric API registers client screens by container id, not by MenuType.
        ScreenProviderRegistry.INSTANCE.registerFactory(ModMenuTypes.INF_FLUID_GENERATOR,
                menu -> new InfFluidGeneratorScreen((InfFluidGeneratorMenu) menu));
        ScreenProviderRegistry.INSTANCE.registerFactory(ModMenuTypes.INF_ITEM_GENERATOR,
                menu -> new InfItemGeneratorScreen((InfItemGeneratorMenu) menu));

        // 服务端保存回执：先在网络线程读完数据，再切到客户端主线程通知当前屏幕。
        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.OUTPUT_RATE_RESULT,
                (client, handler, buffer, responseSender) -> {
                    int containerId = buffer.readVarInt();
                    boolean fluidGenerator = buffer.readBoolean();
                    int side = buffer.readByte();
                    boolean success = buffer.readBoolean();
                    long amount = buffer.readLong();
                    int reason = buffer.readByte();
                    client.execute(() -> ClientRateResultHandler.handle(containerId,
                            fluidGenerator, side, success, amount,
                            RateRejectReason.byId(reason)));
                });
    }
}

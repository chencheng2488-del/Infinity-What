package com.bandybus.infwhat.client.network;

import com.bandybus.infwhat.client.screen.InfFluidGeneratorScreen;
import com.bandybus.infwhat.client.screen.InfItemGeneratorScreen;
import com.bandybus.infwhat.network.RateRejectReason;
import net.minecraft.client.MinecraftClient;

/** Routes output-rate save acknowledgements to the screen that initiated the request. */
public final class ClientRateResultHandler {
    private ClientRateResultHandler() {}

    public static void handle(int containerId, boolean fluidGenerator, int side,
                              boolean success, long amount, RateRejectReason reason) {
        MinecraftClient minecraft = MinecraftClient.getInstance();
        if (minecraft.currentScreen instanceof InfFluidGeneratorScreen
                && fluidGenerator && ((InfFluidGeneratorScreen) minecraft.currentScreen)
                        .getContainerId() == containerId) {
            ((InfFluidGeneratorScreen) minecraft.currentScreen).onOutputRateResult(
                    side, success, amount, reason);
        } else if (minecraft.currentScreen instanceof InfItemGeneratorScreen
                && !fluidGenerator && ((InfItemGeneratorScreen) minecraft.currentScreen)
                        .getContainerId() == containerId) {
            ((InfItemGeneratorScreen) minecraft.currentScreen).onOutputRateResult(
                    side, success, amount, reason);
        }
    }
}

package com.bandybus.infwhat.client.network;

import com.bandybus.infwhat.client.screen.InfFluidGeneratorScreen;
import com.bandybus.infwhat.client.screen.InfItemGeneratorScreen;
import com.bandybus.infwhat.network.RateRejectReason;
import net.minecraft.client.Minecraft;

/** Routes output-rate save acknowledgements to the screen that initiated the request. */
public final class ClientRateResultHandler {
    private ClientRateResultHandler() {}

    public static void handle(int containerId, boolean fluidGenerator, int side,
                              boolean success, long amount, RateRejectReason reason) {
        Minecraft minecraft = Minecraft.getInstance();
        if (fluidGenerator && minecraft.currentScreen instanceof InfFluidGeneratorScreen) {
            InfFluidGeneratorScreen screen = (InfFluidGeneratorScreen) minecraft.currentScreen;
            if (screen.getContainerId() == containerId) {
                screen.onOutputRateResult(side, success, amount, reason);
            }
        } else if (!fluidGenerator && minecraft.currentScreen instanceof InfItemGeneratorScreen) {
            InfItemGeneratorScreen screen = (InfItemGeneratorScreen) minecraft.currentScreen;
            if (screen.getContainerId() == containerId) {
                screen.onOutputRateResult(side, success, amount, reason);
            }
        }
    }
}

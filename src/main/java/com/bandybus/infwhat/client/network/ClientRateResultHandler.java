package com.bandybus.infwhat.client.network;

import com.bandybus.infwhat.client.screen.InfFluidGeneratorScreen;
import com.bandybus.infwhat.client.screen.InfItemGeneratorScreen;
import net.minecraft.client.Minecraft;

/** Routes output-rate save acknowledgements to the screen that initiated the request. */
public final class ClientRateResultHandler {
    private ClientRateResultHandler() {}

    public static void handle(int containerId, boolean fluidGenerator, boolean success, long amount) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof InfFluidGeneratorScreen screen
                && fluidGenerator && screen.getContainerId() == containerId) {
            screen.onOutputRateResult(success, amount);
        } else if (minecraft.screen instanceof InfItemGeneratorScreen screen
                && !fluidGenerator && screen.getContainerId() == containerId) {
            screen.onOutputRateResult(success, amount);
        }
    }
}

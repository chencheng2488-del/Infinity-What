package com.bandybus.infwhat.client.network;

import com.bandybus.infwhat.client.gui.GuiInfFluidGenerator;
import com.bandybus.infwhat.client.gui.GuiInfItemGenerator;
import com.bandybus.infwhat.network.RateRejectReason;
import net.minecraft.client.Minecraft;

/** Routes output-rate save acknowledgements to the screen that initiated the request. */
public final class ClientRateResultHandler {
    private ClientRateResultHandler() {
    }

    public static void handle(final int windowId, final boolean fluidGenerator, final int side,
                              final boolean success, final long amount,
                              final RateRejectReason reason) {
        // The packet arrives on the network thread; GUI changes must run on the client thread.
        Minecraft.getMinecraft().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                Minecraft minecraft = Minecraft.getMinecraft();
                if (minecraft.currentScreen instanceof GuiInfFluidGenerator && fluidGenerator) {
                    GuiInfFluidGenerator screen = (GuiInfFluidGenerator) minecraft.currentScreen;
                    if (screen.getContainerId() == windowId) {
                        screen.onOutputRateResult(side, success, amount, reason);
                    }
                } else if (minecraft.currentScreen instanceof GuiInfItemGenerator
                        && !fluidGenerator) {
                    GuiInfItemGenerator screen = (GuiInfItemGenerator) minecraft.currentScreen;
                    if (screen.getContainerId() == windowId) {
                        screen.onOutputRateResult(side, success, amount, reason);
                    }
                }
            }
        });
    }
}

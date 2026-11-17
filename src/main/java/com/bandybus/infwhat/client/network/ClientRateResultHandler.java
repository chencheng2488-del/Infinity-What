package com.bandybus.infwhat.client.network;

import com.bandybus.infwhat.client.gui.GuiInfFluidGenerator;
import com.bandybus.infwhat.client.gui.GuiInfItemGenerator;
import com.bandybus.infwhat.network.RateRejectReason;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;

import java.util.LinkedList;
import java.util.List;

/**
 * Routes output-rate save acknowledgements to the screen that initiated the request.
 *
 * <p>1.6.4 has no scheduled-task hook on {@link Minecraft}, and packets arrive on the network
 * thread, so the result is queued and dispatched from the screen's own update loop.
 */
@SideOnly(Side.CLIENT)
public final class ClientRateResultHandler {

    private static final List<Result> PENDING = new LinkedList<Result>();

    private ClientRateResultHandler() {
    }

    public static synchronized void handle(final int windowId, final boolean fluidGenerator,
                                           final int side, final boolean success,
                                           final long amount, final RateRejectReason reason) {
        PENDING.add(new Result(windowId, fluidGenerator, side, success, amount, reason));
    }

    /** Applies every queued acknowledgement; call once per client tick from the active screen. */
    public static synchronized void dispatch() {
        if (PENDING.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        for (Result result : PENDING) {
            if (minecraft.currentScreen instanceof GuiInfFluidGenerator
                    && result.fluidGenerator) {
                GuiInfFluidGenerator screen = (GuiInfFluidGenerator) minecraft.currentScreen;
                if (screen.getContainerId() == result.windowId) {
                    screen.onOutputRateResult(result.side, result.success, result.amount,
                            result.reason);
                }
            } else if (minecraft.currentScreen instanceof GuiInfItemGenerator
                    && !result.fluidGenerator) {
                GuiInfItemGenerator screen = (GuiInfItemGenerator) minecraft.currentScreen;
                if (screen.getContainerId() == result.windowId) {
                    screen.onOutputRateResult(result.side, result.success, result.amount,
                            result.reason);
                }
            }
        }
        PENDING.clear();
    }

    private static final class Result {
        private final int windowId;
        private final boolean fluidGenerator;
        private final int side;
        private final boolean success;
        private final long amount;
        private final RateRejectReason reason;

        private Result(int windowId, boolean fluidGenerator, int side, boolean success,
                       long amount, RateRejectReason reason) {
            this.windowId = windowId;
            this.fluidGenerator = fluidGenerator;
            this.side = side;
            this.success = success;
            this.amount = amount;
            this.reason = reason;
        }
    }
}

package com.bandybus.infwhat.network;

import com.bandybus.infwhat.InfinityWhat;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

/**
 * Network channel for GUI output-rate edits. The rate may exceed a container button id range, so
 * it travels as a dedicated packet.
 */
public final class ModNetworking {

    private static final SimpleNetworkWrapper INSTANCE =
            NetworkRegistry.INSTANCE.newSimpleChannel(InfinityWhat.MODID);

    private ModNetworking() {
    }

    /** Registers both output-rate messages with the channel. */
    public static void register() {
        INSTANCE.registerMessage(SetOutputRateMessage.Handler.class,
                SetOutputRateMessage.class, 0, Side.SERVER);
        INSTANCE.registerMessage(OutputRateResultMessage.Handler.class,
                OutputRateResultMessage.class, 1, Side.CLIENT);
    }

    /** Sends a new output rate request from the client to the server. */
    public static void sendOutputRate(int windowId, boolean fluidGenerator, long amount) {
        INSTANCE.sendToServer(new SetOutputRateMessage(windowId, fluidGenerator, amount));
    }

    /** Shared channel instance used by the message handlers to send their reply. */
    static SimpleNetworkWrapper channel() {
        return INSTANCE;
    }
}

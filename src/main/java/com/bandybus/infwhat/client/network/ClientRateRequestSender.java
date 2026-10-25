package com.bandybus.infwhat.client.network;

import com.bandybus.infwhat.network.ModNetworking;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Client-only sender for server-authoritative generator output-rate changes. */
public final class ClientRateRequestSender {
    private ClientRateRequestSender() {}

    public static void send(int containerId, boolean fluidGenerator, long amount) {
        ClientPacketDistributor.sendToServer(
                new ModNetworking.SetOutputRatePayload(containerId, fluidGenerator, amount));
    }
}

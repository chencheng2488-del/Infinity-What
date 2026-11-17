package com.bandybus.infwhat.client.network;

import com.bandybus.infwhat.network.ModNetworking;
import net.neoforged.neoforge.network.PacketDistributor;

/** Client-only sender for server-authoritative generator output-rate changes. */
public final class ClientRateRequestSender {
    private ClientRateRequestSender() {}

    public static void send(int containerId, boolean fluidGenerator, long amount) {
        send(containerId, fluidGenerator, -1, amount);
    }

    public static void send(int containerId, boolean fluidGenerator, int side, long amount) {
        PacketDistributor.sendToServer(
                new ModNetworking.SetOutputRatePayload(containerId, fluidGenerator, side, amount));
    }

    public static void sendRedstone(int containerId, boolean fluidGenerator, int mode,
                                    int pulseOnTicks, int pulseOffTicks) {
        PacketDistributor.sendToServer(new ModNetworking.SetRedstonePayload(
                containerId, fluidGenerator, mode, pulseOnTicks, pulseOffTicks));
    }

    public static void sendBatch(int containerId, boolean fluidGenerator, int size) {
        PacketDistributor.sendToServer(
                new ModNetworking.SetBatchPayload(containerId, fluidGenerator, size));
    }
}

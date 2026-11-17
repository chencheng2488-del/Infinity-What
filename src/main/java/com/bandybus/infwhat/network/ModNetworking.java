package com.bandybus.infwhat.network;

import com.bandybus.infwhat.InfinityWhat;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Network channel for the GUI output-rate, redstone and batch edits. Values may exceed the range
 * of a container button id, so they travel as dedicated packets.
 */
public final class ModNetworking {

    private static final SimpleNetworkWrapper INSTANCE =
            NetworkRegistry.INSTANCE.newSimpleChannel(InfinityWhat.MODID);

    private ModNetworking() {
    }

    public static void register() {
        INSTANCE.registerMessage(SetOutputRateMessage.Handler.class,
                SetOutputRateMessage.class, 0, Side.SERVER);
        INSTANCE.registerMessage(OutputRateResultMessage.Handler.class,
                OutputRateResultMessage.class, 1, Side.CLIENT);
        INSTANCE.registerMessage(SetRedstoneMessage.Handler.class,
                SetRedstoneMessage.class, 2, Side.SERVER);
        INSTANCE.registerMessage(SetBatchMessage.Handler.class,
                SetBatchMessage.class, 3, Side.SERVER);
        INSTANCE.registerMessage(SetOrdoSettingsMessage.Handler.class,
                SetOrdoSettingsMessage.class, 4, Side.SERVER);
    }

    public static void sendOutputRate(int windowId, boolean fluidGenerator, long amount) {
        sendOutputRate(windowId, fluidGenerator, -1, amount);
    }

    public static void sendOutputRate(int windowId, boolean fluidGenerator, int side, long amount) {
        INSTANCE.sendToServer(new SetOutputRateMessage(windowId, fluidGenerator, side, amount));
    }

    public static void sendRedstone(int windowId, boolean fluidGenerator, int mode,
                                    int pulseOnTicks, int pulseOffTicks) {
        INSTANCE.sendToServer(new SetRedstoneMessage(
                windowId, fluidGenerator, mode, pulseOnTicks, pulseOffTicks));
    }

    public static void sendBatch(int windowId, boolean fluidGenerator, int size) {
        INSTANCE.sendToServer(new SetBatchMessage(windowId, fluidGenerator, size));
    }

    public static void sendOrdoSettings(net.minecraft.util.math.BlockPos pos,
                                        float speedMultiplier, int stabilityMode,
                                        float stabilityValue) {
        INSTANCE.sendToServer(new SetOrdoSettingsMessage(pos, speedMultiplier, stabilityMode,
                stabilityValue));
    }

    static SimpleNetworkWrapper channel() {
        return INSTANCE;
    }
}

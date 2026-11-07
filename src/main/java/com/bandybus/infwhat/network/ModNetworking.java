package com.bandybus.infwhat.network;

import com.bandybus.infwhat.InfinityWhat;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Network channel for the GUI output-rate edits. The rate may exceed the range of a container
 * button id, so it travels as a dedicated packet.
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
        INSTANCE.registerMessage(SetOrdoSettingsMessage.Handler.class,
                SetOrdoSettingsMessage.class, 2, Side.SERVER);
    }

    public static void sendOutputRate(int windowId, boolean fluidGenerator, long amount) {
        INSTANCE.sendToServer(new SetOutputRateMessage(windowId, fluidGenerator, amount));
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

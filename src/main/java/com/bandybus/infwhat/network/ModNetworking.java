package com.bandybus.infwhat.network;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.entity.InfFluidGeneratorBlockEntity;
import com.bandybus.infwhat.block.entity.InfItemGeneratorBlockEntity;
import com.bandybus.infwhat.menu.InfFluidGeneratorMenu;
import com.bandybus.infwhat.menu.InfItemGeneratorMenu;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Server-authoritative rate updates and acknowledgements. */
public final class ModNetworking {
    private ModNetworking() {}

    /** Registers both payload types; called once from the common initializer. */
    public static void registerPayloads() {
        PayloadTypeRegistry.serverboundPlay().register(SetOutputRatePayload.TYPE, SetOutputRatePayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(OutputRateResultPayload.TYPE, OutputRateResultPayload.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(SetOutputRatePayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> handleSetRate(payload, player));
        });
    }

    public static void sendOutputRate(int containerId, boolean fluidGenerator, long amount) {
        ClientNetworkingHook.send(new SetOutputRatePayload(containerId, fluidGenerator, amount));
    }

    private static void handleSetRate(SetOutputRatePayload payload, ServerPlayer player) {
        AbstractContainerMenu menu = player.containerMenu;
        boolean success = false;
        long acceptedAmount = 0L;
        if (menu.containerId == payload.containerId && menu.stillValid(player)) {
            if (payload.fluidGenerator && menu instanceof InfFluidGeneratorMenu fluidMenu) {
                if (payload.amount >= 1L && payload.amount <= InfFluidGeneratorBlockEntity.MAX_OUTPUT_RATE_MB) {
                    InfFluidGeneratorBlockEntity blockEntity = fluidMenu.getBlockEntity();
                    blockEntity.setOutputAmountPerTick(payload.amount);
                    acceptedAmount = blockEntity.getOutputAmountPerTick();
                    success = true;
                }
            } else if (!payload.fluidGenerator && menu instanceof InfItemGeneratorMenu itemMenu) {
                if (payload.amount >= 1L && payload.amount <= Integer.MAX_VALUE) {
                    InfItemGeneratorBlockEntity blockEntity = itemMenu.getBlockEntity();
                    blockEntity.setOutputItemsPerTick((int) payload.amount);
                    acceptedAmount = blockEntity.getOutputItemsPerTick();
                    success = true;
                }
            }
        }
        ServerPlayNetworking.send(player, new OutputRateResultPayload(
                payload.containerId, payload.fluidGenerator, success, acceptedAmount));
    }

    /** Indirection so the server-only class never touches client networking directly. */
    private static final class ClientNetworkingHook {
        private static void send(CustomPacketPayload payload) {
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(payload);
        }
    }

    public record SetOutputRatePayload(int containerId, boolean fluidGenerator, long amount)
            implements CustomPacketPayload {
        public static final Type<SetOutputRatePayload> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath(InfinityWhat.MOD_ID, "set_output_rate"));
        public static final StreamCodec<FriendlyByteBuf, SetOutputRatePayload> STREAM_CODEC =
                StreamCodec.of((buffer, payload) -> {
                    buffer.writeVarInt(payload.containerId);
                    buffer.writeBoolean(payload.fluidGenerator);
                    buffer.writeLong(payload.amount);
                }, buffer -> new SetOutputRatePayload(buffer.readVarInt(), buffer.readBoolean(), buffer.readLong()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record OutputRateResultPayload(int containerId, boolean fluidGenerator,
                                          boolean success, long amount) implements CustomPacketPayload {
        public static final Type<OutputRateResultPayload> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath(InfinityWhat.MOD_ID, "output_rate_result"));
        public static final StreamCodec<FriendlyByteBuf, OutputRateResultPayload> STREAM_CODEC =
                StreamCodec.of((buffer, payload) -> {
                    buffer.writeVarInt(payload.containerId);
                    buffer.writeBoolean(payload.fluidGenerator);
                    buffer.writeBoolean(payload.success);
                    buffer.writeLong(payload.amount);
                }, buffer -> new OutputRateResultPayload(buffer.readVarInt(), buffer.readBoolean(),
                        buffer.readBoolean(), buffer.readLong()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}

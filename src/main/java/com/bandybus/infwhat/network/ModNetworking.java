package com.bandybus.infwhat.network;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.entity.InfFluidGeneratorBlockEntity;
import com.bandybus.infwhat.block.entity.InfItemGeneratorBlockEntity;
import com.bandybus.infwhat.menu.InfFluidGeneratorMenu;
import com.bandybus.infwhat.menu.InfItemGeneratorMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Server-authoritative rate updates and acknowledgements. */
public final class ModNetworking {
    private ModNetworking() {}

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(SetOutputRatePayload.TYPE, SetOutputRatePayload.STREAM_CODEC,
                ModNetworking::handleSetRate);
    }

    public static void sendOutputRate(int containerId, boolean fluidGenerator, long amount) {
        PacketDistributor.sendToServer(new SetOutputRatePayload(containerId, fluidGenerator, amount));
    }

    private static void handleSetRate(SetOutputRatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

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
            PacketDistributor.sendToPlayer(player, new OutputRateResultPayload(
                    payload.containerId, payload.fluidGenerator, success, acceptedAmount));
        });
    }

    public record SetOutputRatePayload(int containerId, boolean fluidGenerator, long amount)
            implements CustomPacketPayload {
        public static final Type<SetOutputRatePayload> TYPE = new Type<>(
                new ResourceLocation(InfinityWhat.MOD_ID, "set_output_rate"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SetOutputRatePayload> STREAM_CODEC =
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
                new ResourceLocation(InfinityWhat.MOD_ID, "output_rate_result"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OutputRateResultPayload> STREAM_CODEC =
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

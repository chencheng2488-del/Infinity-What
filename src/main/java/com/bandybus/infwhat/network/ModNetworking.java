package com.bandybus.infwhat.network;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.entity.InfFluidGeneratorBlockEntity;
import com.bandybus.infwhat.block.entity.InfItemGeneratorBlockEntity;
import com.bandybus.infwhat.client.network.ClientRateResultHandler;
import com.bandybus.infwhat.menu.InfFluidGeneratorMenu;
import com.bandybus.infwhat.menu.InfItemGeneratorMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;
import net.neoforged.neoforge.network.registration.IPayloadRegistrar;

/** Server-authoritative rate updates and acknowledgements. */
public final class ModNetworking {
    private static final ResourceLocation SET_RATE_ID = new ResourceLocation(InfinityWhat.MOD_ID, "set_output_rate");
    private static final ResourceLocation RATE_RESULT_ID = new ResourceLocation(InfinityWhat.MOD_ID, "output_rate_result");

    private ModNetworking() {}

    public static void registerPayloads(RegisterPayloadHandlerEvent event) {
        IPayloadRegistrar registrar = event.registrar(InfinityWhat.MOD_ID).versioned("1");
        registrar.play(SET_RATE_ID, SetOutputRatePayload::new,
                handlers -> handlers.server(ModNetworking::handleSetRate));
        registrar.play(RATE_RESULT_ID, OutputRateResultPayload::new,
                handlers -> handlers.client(ModNetworking::handleRateResult));
    }

    public static void sendOutputRate(int containerId, boolean fluidGenerator, long amount) {
        PacketDistributor.SERVER.noArg().send(new SetOutputRatePayload(containerId, fluidGenerator, amount));
    }

    private static void handleSetRate(SetOutputRatePayload payload, PlayPayloadContext context) {
        context.workHandler().submitAsync(() -> {
            ServerPlayer player = context.player().filter(ServerPlayer.class::isInstance)
                    .map(ServerPlayer.class::cast).orElse(null);
            if (player == null) return;

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
            PacketDistributor.PLAYER.with(player).send(new OutputRateResultPayload(
                    payload.containerId, payload.fluidGenerator, success, acceptedAmount));
        });
    }

    private static void handleRateResult(OutputRateResultPayload payload, PlayPayloadContext context) {
        context.workHandler().submitAsync(() -> ClientRateResultHandler.handle(
                payload.containerId, payload.fluidGenerator, payload.success, payload.amount));
    }

    private record SetOutputRatePayload(int containerId, boolean fluidGenerator, long amount)
            implements CustomPacketPayload {
        private SetOutputRatePayload(FriendlyByteBuf buffer) {
            this(buffer.readVarInt(), buffer.readBoolean(), buffer.readLong());
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeVarInt(containerId);
            buffer.writeBoolean(fluidGenerator);
            buffer.writeLong(amount);
        }

        @Override
        public ResourceLocation id() {
            return SET_RATE_ID;
        }
    }

    private record OutputRateResultPayload(int containerId, boolean fluidGenerator,
                                           boolean success, long amount) implements CustomPacketPayload {
        private OutputRateResultPayload(FriendlyByteBuf buffer) {
            this(buffer.readVarInt(), buffer.readBoolean(), buffer.readBoolean(), buffer.readLong());
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeVarInt(containerId);
            buffer.writeBoolean(fluidGenerator);
            buffer.writeBoolean(success);
            buffer.writeLong(amount);
        }

        @Override
        public ResourceLocation id() {
            return RATE_RESULT_ID;
        }
    }
}

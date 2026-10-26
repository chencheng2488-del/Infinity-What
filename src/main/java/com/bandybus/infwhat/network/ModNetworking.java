package com.bandybus.infwhat.network;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.entity.InfFluidGeneratorBlockEntity;
import com.bandybus.infwhat.block.entity.InfItemGeneratorBlockEntity;
import com.bandybus.infwhat.client.network.ClientRateResultHandler;
import com.bandybus.infwhat.menu.InfFluidGeneratorMenu;
import com.bandybus.infwhat.menu.InfItemGeneratorMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.DistExecutor;
import net.neoforged.neoforge.network.NetworkEvent;
import net.neoforged.neoforge.network.NetworkRegistry;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.simple.SimpleChannel;


/** Server-authoritative packets for GUI rates, including their save acknowledgements. */
public final class ModNetworking {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(InfinityWhat.MOD_ID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private ModNetworking() {}

    public static void register() {
        CHANNEL.registerMessage(0, SetOutputRatePacket.class,
                SetOutputRatePacket::encode, SetOutputRatePacket::decode,
                SetOutputRatePacket::handle);
        CHANNEL.registerMessage(1, OutputRateResultPacket.class,
                OutputRateResultPacket::encode, OutputRateResultPacket::decode,
                OutputRateResultPacket::handle);
    }

    public static void sendOutputRate(int containerId, boolean fluidGenerator, long amount) {
        CHANNEL.sendToServer(new SetOutputRatePacket(containerId, fluidGenerator, amount));
    }

    private record SetOutputRatePacket(int containerId, boolean fluidGenerator, long amount) {
        private static void encode(SetOutputRatePacket packet, FriendlyByteBuf buffer) {
            buffer.writeVarInt(packet.containerId);
            buffer.writeBoolean(packet.fluidGenerator);
            buffer.writeLong(packet.amount);
        }

        private static SetOutputRatePacket decode(FriendlyByteBuf buffer) {
            return new SetOutputRatePacket(buffer.readVarInt(), buffer.readBoolean(), buffer.readLong());
        }

        private static void handle(SetOutputRatePacket packet, NetworkEvent.Context context) {
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) return;
                AbstractContainerMenu menu = player.containerMenu;
                boolean success = false;
                long acceptedAmount = 0L;

                if (menu.containerId == packet.containerId && menu.stillValid(player)) {
                    if (packet.fluidGenerator && menu instanceof InfFluidGeneratorMenu fluidMenu) {
                        if (packet.amount >= 1L
                                && packet.amount <= InfFluidGeneratorBlockEntity.MAX_OUTPUT_RATE_MB) {
                            InfFluidGeneratorBlockEntity blockEntity = fluidMenu.getBlockEntity();
                            blockEntity.setOutputAmountPerTick(packet.amount);
                            acceptedAmount = blockEntity.getOutputAmountPerTick();
                            success = true;
                        }
                    } else if (!packet.fluidGenerator && menu instanceof InfItemGeneratorMenu itemMenu) {
                        if (packet.amount >= 1L && packet.amount <= Integer.MAX_VALUE) {
                            InfItemGeneratorBlockEntity blockEntity = itemMenu.getBlockEntity();
                            blockEntity.setOutputItemsPerTick((int) packet.amount);
                            acceptedAmount = blockEntity.getOutputItemsPerTick();
                            success = true;
                        }
                    }
                }

                CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                        new OutputRateResultPacket(packet.containerId, packet.fluidGenerator,
                                success, acceptedAmount));
            });
            context.setPacketHandled(true);
        }
    }

    private record OutputRateResultPacket(int containerId, boolean fluidGenerator,
                                          boolean success, long amount) {
        private static void encode(OutputRateResultPacket packet, FriendlyByteBuf buffer) {
            buffer.writeVarInt(packet.containerId);
            buffer.writeBoolean(packet.fluidGenerator);
            buffer.writeBoolean(packet.success);
            buffer.writeLong(packet.amount);
        }

        private static OutputRateResultPacket decode(FriendlyByteBuf buffer) {
            return new OutputRateResultPacket(buffer.readVarInt(), buffer.readBoolean(),
                    buffer.readBoolean(), buffer.readLong());
        }

        private static void handle(OutputRateResultPacket packet, NetworkEvent.Context context) {
            context.enqueueWork(() -> net.neoforged.fml.DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ClientRateResultHandler.handle(packet.containerId,
                            packet.fluidGenerator, packet.success, packet.amount)));
            context.setPacketHandled(true);
        }
    }
}

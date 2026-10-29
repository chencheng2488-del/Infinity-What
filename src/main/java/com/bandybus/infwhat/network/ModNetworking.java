package com.bandybus.infwhat.network;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.entity.InfFluidGeneratorBlockEntity;
import com.bandybus.infwhat.block.entity.InfItemGeneratorBlockEntity;
import com.bandybus.infwhat.client.network.ClientRateResultHandler;
import com.bandybus.infwhat.menu.InfFluidGeneratorMenu;
import com.bandybus.infwhat.menu.InfItemGeneratorMenu;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import java.util.function.Supplier;

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

    private static final class SetOutputRatePacket {
        private final int containerId;
        private final boolean fluidGenerator;
        private final long amount;

        private SetOutputRatePacket(int containerId, boolean fluidGenerator, long amount) {
            this.containerId = containerId;
            this.fluidGenerator = fluidGenerator;
            this.amount = amount;
        }

        private static void encode(SetOutputRatePacket packet, PacketBuffer buffer) {
            buffer.writeVarInt(packet.containerId);
            buffer.writeBoolean(packet.fluidGenerator);
            buffer.writeLong(packet.amount);
        }

        private static SetOutputRatePacket decode(PacketBuffer buffer) {
            return new SetOutputRatePacket(buffer.readVarInt(), buffer.readBoolean(), buffer.readLong());
        }

        private static void handle(SetOutputRatePacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayerEntity player = context.getSender();
                if (player == null) {
                    return;
                }
                Container menu = player.containerMenu;
                boolean success = false;
                long acceptedAmount = 0L;

                if (menu.containerId == packet.containerId && menu.stillValid(player)) {
                    if (packet.fluidGenerator && menu instanceof InfFluidGeneratorMenu) {
                        if (packet.amount >= 1L
                                && packet.amount <= InfFluidGeneratorBlockEntity.MAX_OUTPUT_RATE_MB) {
                            InfFluidGeneratorBlockEntity blockEntity =
                                    ((InfFluidGeneratorMenu) menu).getBlockEntity();
                            blockEntity.setOutputAmountPerTick(packet.amount);
                            acceptedAmount = blockEntity.getOutputAmountPerTick();
                            success = true;
                        }
                    } else if (!packet.fluidGenerator && menu instanceof InfItemGeneratorMenu) {
                        if (packet.amount >= 1L && packet.amount <= Integer.MAX_VALUE) {
                            InfItemGeneratorBlockEntity blockEntity =
                                    ((InfItemGeneratorMenu) menu).getBlockEntity();
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

    private static final class OutputRateResultPacket {
        private final int containerId;
        private final boolean fluidGenerator;
        private final boolean success;
        private final long amount;

        private OutputRateResultPacket(int containerId, boolean fluidGenerator,
                                      boolean success, long amount) {
            this.containerId = containerId;
            this.fluidGenerator = fluidGenerator;
            this.success = success;
            this.amount = amount;
        }

        private static void encode(OutputRateResultPacket packet, PacketBuffer buffer) {
            buffer.writeVarInt(packet.containerId);
            buffer.writeBoolean(packet.fluidGenerator);
            buffer.writeBoolean(packet.success);
            buffer.writeLong(packet.amount);
        }

        private static OutputRateResultPacket decode(PacketBuffer buffer) {
            return new OutputRateResultPacket(buffer.readVarInt(), buffer.readBoolean(),
                    buffer.readBoolean(), buffer.readLong());
        }

        private static void handle(OutputRateResultPacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ClientRateResultHandler.handle(packet.containerId,
                            packet.fluidGenerator, packet.success, packet.amount)));
            context.setPacketHandled(true);
        }
    }
}

package com.bandybus.infwhat.network;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.RedstoneMode;
import com.bandybus.infwhat.block.entity.InfFluidGeneratorBlockEntity;
import com.bandybus.infwhat.block.entity.InfItemGeneratorBlockEntity;
import com.bandybus.infwhat.client.network.ClientRateResultHandler;
import com.bandybus.infwhat.config.InfinityWhatConfig;
import com.bandybus.infwhat.menu.InfFluidGeneratorMenu;
import com.bandybus.infwhat.menu.InfItemGeneratorMenu;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

/** Server-authoritative packets for GUI rates, including their save acknowledgements. */
public final class ModNetworking {
    private static final int PROTOCOL_VERSION = 5;
    private static final SimpleChannel CHANNEL = ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(InfinityWhat.MOD_ID, "main"))
            .networkProtocolVersion(PROTOCOL_VERSION)
            .clientAcceptedVersions(Channel.VersionTest.exact(PROTOCOL_VERSION))
            .serverAcceptedVersions(Channel.VersionTest.exact(PROTOCOL_VERSION))
            .simpleChannel();

    private ModNetworking() {
    }

    public static void register() {
        CHANNEL.messageBuilder(SetOutputRatePacket.class)
                .encoder(SetOutputRatePacket::encode)
                .decoder(SetOutputRatePacket::decode)
                .consumerMainThread(SetOutputRatePacket::handle)
                .add();
        CHANNEL.messageBuilder(OutputRateResultPacket.class)
                .encoder(OutputRateResultPacket::encode)
                .decoder(OutputRateResultPacket::decode)
                .consumerMainThread(OutputRateResultPacket::handle)
                .add();
        CHANNEL.messageBuilder(SetRedstonePacket.class)
                .encoder(SetRedstonePacket::encode)
                .decoder(SetRedstonePacket::decode)
                .consumerMainThread(SetRedstonePacket::handle)
                .add();
        CHANNEL.messageBuilder(SetBatchPacket.class)
                .encoder(SetBatchPacket::encode)
                .decoder(SetBatchPacket::decode)
                .consumerMainThread(SetBatchPacket::handle)
                .add();
    }

    public static void sendOutputRate(int containerId, boolean fluidGenerator, long amount) {
        sendOutputRate(containerId, fluidGenerator, -1, amount);
    }

    public static void sendOutputRate(int containerId, boolean fluidGenerator, int side, long amount) {
        CHANNEL.send(new SetOutputRatePacket(containerId, fluidGenerator, side, amount),
                PacketDistributor.SERVER.noArg());
    }

    public static void sendRedstone(int containerId, boolean fluidGenerator, int mode,
                                    int pulseOnTicks, int pulseOffTicks) {
        CHANNEL.send(new SetRedstonePacket(
                        containerId, fluidGenerator, mode, pulseOnTicks, pulseOffTicks),
                PacketDistributor.SERVER.noArg());
    }

    public static void sendBatch(int containerId, boolean fluidGenerator, int size) {
        CHANNEL.send(new SetBatchPacket(containerId, fluidGenerator, size),
                PacketDistributor.SERVER.noArg());
    }

    /** {@code side} is -1 for "all six sides", otherwise a {@link Direction} data value. */
    private record SetOutputRatePacket(int containerId, boolean fluidGenerator, int side, long amount) {
        private static void encode(SetOutputRatePacket packet, FriendlyByteBuf buffer) {
            buffer.writeVarInt(packet.containerId);
            buffer.writeBoolean(packet.fluidGenerator);
            buffer.writeByte(packet.side);
            buffer.writeLong(packet.amount);
        }

        private static SetOutputRatePacket decode(FriendlyByteBuf buffer) {
            return new SetOutputRatePacket(buffer.readVarInt(), buffer.readBoolean(),
                    buffer.readByte(), buffer.readLong());
        }

        private static void handle(SetOutputRatePacket packet, CustomPayloadEvent.Context context) {
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) return;
                AbstractContainerMenu menu = player.containerMenu;
                boolean success = false;
                long acceptedAmount = 0L;
                RateRejectReason reason = RateRejectReason.REJECTED;
                boolean validSide = packet.side >= -1 && packet.side < Direction.values().length;

                if (validSide && menu.containerId == packet.containerId && menu.stillValid(player)) {
                    if (packet.fluidGenerator && menu instanceof InfFluidGeneratorMenu fluidMenu) {
                        InfFluidGeneratorBlockEntity blockEntity = fluidMenu.getBlockEntity();
                        reason = fluidRejectReason(blockEntity, packet.side, packet.amount);
                        if (reason == RateRejectReason.NONE) {
                            if (packet.side < 0) {
                                success = blockEntity.setUniformSideRateMb(packet.amount);
                            } else {
                                success = blockEntity.setSideRateMb(
                                        Direction.from3DDataValue(packet.side), packet.amount);
                            }
                            if (success) {
                                acceptedAmount = packet.side < 0
                                        ? blockEntity.getUniformSideRateMb()
                                        : blockEntity.getSideRateMb(Direction.from3DDataValue(packet.side));
                            } else {
                                reason = RateRejectReason.REJECTED;
                            }
                        }
                    } else if (!packet.fluidGenerator && menu instanceof InfItemGeneratorMenu itemMenu) {
                        InfItemGeneratorBlockEntity blockEntity = itemMenu.getBlockEntity();
                        reason = itemRejectReason(blockEntity, packet.side, packet.amount);
                        if (reason == RateRejectReason.NONE) {
                            if (packet.side < 0) {
                                success = blockEntity.setUniformSideRate((int) packet.amount);
                            } else {
                                success = blockEntity.setSideRate(
                                        Direction.from3DDataValue(packet.side), (int) packet.amount);
                            }
                            if (success) {
                                acceptedAmount = packet.side < 0
                                        ? blockEntity.getUniformSideRate()
                                        : blockEntity.getSideRate(Direction.from3DDataValue(packet.side));
                            } else {
                                reason = RateRejectReason.REJECTED;
                            }
                        }
                    }
                }

                CHANNEL.send(new OutputRateResultPacket(packet.containerId, packet.fluidGenerator,
                                packet.side, success, acceptedAmount, reason.ordinal()),
                        PacketDistributor.PLAYER.with(player));
            });
            context.setPacketHandled(true);
        }
    }

    private static RateRejectReason itemRejectReason(InfItemGeneratorBlockEntity blockEntity,
                                                     int side, long amount) {
        if (amount < 0L || amount > Integer.MAX_VALUE) {
            return RateRejectReason.INVALID;
        }
        if (amount > InfinityWhatConfig.maxItemRatePerSide()) {
            return RateRejectReason.LIMIT_SIDE;
        }
        long newTotal;
        if (side < 0) {
            newTotal = amount * enabledSideCount(blockEntity);
        } else {
            Direction direction = Direction.from3DDataValue(side);
            newTotal = blockEntity.getTotalEffectiveRate();
            if (blockEntity.isOutputSideEnabled(direction)) {
                newTotal = newTotal - blockEntity.getEffectiveSideRate(direction) + amount;
            }
        }
        return newTotal > InfinityWhatConfig.maxItemTotalRate()
                ? RateRejectReason.LIMIT_TOTAL : RateRejectReason.NONE;
    }

    private static int enabledSideCount(InfItemGeneratorBlockEntity blockEntity) {
        int count = 0;
        for (Direction direction : Direction.values()) {
            if (blockEntity.isOutputSideEnabled(direction)) {
                count++;
            }
        }
        return count;
    }

    private static RateRejectReason fluidRejectReason(InfFluidGeneratorBlockEntity blockEntity,
                                                      int side, long amount) {
        if (amount < 0L || amount > InfFluidGeneratorBlockEntity.MAX_OUTPUT_RATE_MB) {
            return RateRejectReason.INVALID;
        }
        if (amount > InfinityWhatConfig.maxFluidRatePerSideMb()) {
            return RateRejectReason.LIMIT_SIDE;
        }
        long newTotal;
        if (side < 0) {
            newTotal = amount * enabledFluidSideCount(blockEntity);
        } else {
            Direction direction = Direction.from3DDataValue(side);
            newTotal = blockEntity.getTotalEffectiveRateMb();
            if (blockEntity.isOutputSideEnabled(direction)) {
                newTotal = newTotal - blockEntity.getEffectiveSideRateMb(direction) + amount;
            }
        }
        return newTotal > InfinityWhatConfig.maxFluidTotalRateMb()
                ? RateRejectReason.LIMIT_TOTAL : RateRejectReason.NONE;
    }

    private static int enabledFluidSideCount(InfFluidGeneratorBlockEntity blockEntity) {
        int count = 0;
        for (Direction direction : Direction.values()) {
            if (blockEntity.isOutputSideEnabled(direction)) {
                count++;
            }
        }
        return count;
    }

    /** {@code side} is -1 for "all six sides", otherwise a {@link Direction} data value. */
    private record SetBatchPacket(int containerId, boolean fluidGenerator, int size) {
        private static void encode(SetBatchPacket packet, FriendlyByteBuf buffer) {
            buffer.writeVarInt(packet.containerId);
            buffer.writeBoolean(packet.fluidGenerator);
            buffer.writeVarInt(packet.size);
        }

        private static SetBatchPacket decode(FriendlyByteBuf buffer) {
            return new SetBatchPacket(buffer.readVarInt(), buffer.readBoolean(),
                    buffer.readVarInt());
        }

        private static void handle(SetBatchPacket packet, CustomPayloadEvent.Context context) {
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) return;
                AbstractContainerMenu menu = player.containerMenu;
                if (menu.containerId != packet.containerId || !menu.stillValid(player)) return;
                if (packet.fluidGenerator && menu instanceof InfFluidGeneratorMenu fluidMenu) {
                    fluidMenu.getBlockEntity().setBatchSize(packet.size);
                } else if (!packet.fluidGenerator && menu instanceof InfItemGeneratorMenu itemMenu) {
                    itemMenu.getBlockEntity().setBatchSize(packet.size);
                }
            });
            context.setPacketHandled(true);
        }
    }

    /** {@code side} is -1 for "all six sides", otherwise a {@link Direction} data value. */
    private record SetRedstonePacket(int containerId, boolean fluidGenerator, int mode,
                                     int pulseOnTicks, int pulseOffTicks) {
        private static void encode(SetRedstonePacket packet, FriendlyByteBuf buffer) {
            buffer.writeVarInt(packet.containerId);
            buffer.writeBoolean(packet.fluidGenerator);
            buffer.writeByte(packet.mode);
            buffer.writeVarInt(packet.pulseOnTicks);
            buffer.writeVarInt(packet.pulseOffTicks);
        }

        private static SetRedstonePacket decode(FriendlyByteBuf buffer) {
            return new SetRedstonePacket(buffer.readVarInt(), buffer.readBoolean(),
                    buffer.readByte(), buffer.readVarInt(), buffer.readVarInt());
        }

        private static void handle(SetRedstonePacket packet, CustomPayloadEvent.Context context) {
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) return;
                AbstractContainerMenu menu = player.containerMenu;
                if (menu.containerId != packet.containerId || !menu.stillValid(player)) return;
                if (packet.mode < 0 || packet.mode >= RedstoneMode.values().length) return;
                RedstoneMode mode = RedstoneMode.byId(packet.mode);
                if (packet.fluidGenerator && menu instanceof InfFluidGeneratorMenu fluidMenu) {
                    InfFluidGeneratorBlockEntity blockEntity = fluidMenu.getBlockEntity();
                    if (blockEntity.setPulseTicks(packet.pulseOnTicks, packet.pulseOffTicks)) {
                        blockEntity.setRedstoneMode(mode);
                    }
                } else if (!packet.fluidGenerator && menu instanceof InfItemGeneratorMenu itemMenu) {
                    InfItemGeneratorBlockEntity blockEntity = itemMenu.getBlockEntity();
                    if (blockEntity.setPulseTicks(packet.pulseOnTicks, packet.pulseOffTicks)) {
                        blockEntity.setRedstoneMode(mode);
                    }
                }
            });
            context.setPacketHandled(true);
        }
    }

    /** {@code side} is -1 for "all six sides", otherwise a {@link Direction} data value. */
    private record OutputRateResultPacket(int containerId, boolean fluidGenerator, int side,
                                          boolean success, long amount, int reason) {
        private static void encode(OutputRateResultPacket packet, FriendlyByteBuf buffer) {
            buffer.writeVarInt(packet.containerId);
            buffer.writeBoolean(packet.fluidGenerator);
            buffer.writeByte(packet.side);
            buffer.writeBoolean(packet.success);
            buffer.writeLong(packet.amount);
            buffer.writeByte(packet.reason);
        }

        private static OutputRateResultPacket decode(FriendlyByteBuf buffer) {
            return new OutputRateResultPacket(buffer.readVarInt(), buffer.readBoolean(),
                    buffer.readByte(), buffer.readBoolean(), buffer.readLong(), buffer.readByte());
        }

        private static void handle(OutputRateResultPacket packet, CustomPayloadEvent.Context context) {
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ClientRateResultHandler.handle(packet.containerId,
                            packet.fluidGenerator, packet.side, packet.success, packet.amount,
                            RateRejectReason.byId(packet.reason))));
            context.setPacketHandled(true);
        }
    }
}

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
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

/** Server-authoritative packets for GUI rates, including their save acknowledgements. */
public final class ModNetworking {
    private static final String PROTOCOL = "5";
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
        CHANNEL.registerMessage(2, SetRedstonePacket.class,
                SetRedstonePacket::encode, SetRedstonePacket::decode,
                SetRedstonePacket::handle);
        CHANNEL.registerMessage(3, SetBatchPacket.class,
                SetBatchPacket::encode, SetBatchPacket::decode,
                SetBatchPacket::handle);
    }

    public static void sendOutputRate(int containerId, boolean fluidGenerator, long amount) {
        sendOutputRate(containerId, fluidGenerator, -1, amount);
    }

    public static void sendOutputRate(int containerId, boolean fluidGenerator, int side, long amount) {
        CHANNEL.sendToServer(new SetOutputRatePacket(containerId, fluidGenerator, side, amount));
    }

    public static void sendRedstone(int containerId, boolean fluidGenerator, int mode,
                                    int pulseOnTicks, int pulseOffTicks) {
        CHANNEL.sendToServer(new SetRedstonePacket(
                containerId, fluidGenerator, mode, pulseOnTicks, pulseOffTicks));
    }

    public static void sendBatch(int containerId, boolean fluidGenerator, int size) {
        CHANNEL.sendToServer(new SetBatchPacket(containerId, fluidGenerator, size));
    }

    /** {@code side} is -1 for "all six sides", otherwise a {@link Direction} data value. */
    private static final class SetOutputRatePacket {
        private final int containerId;
        private final boolean fluidGenerator;
        private final int side;
        private final long amount;

        private SetOutputRatePacket(int containerId, boolean fluidGenerator, int side, long amount) {
            this.containerId = containerId;
            this.fluidGenerator = fluidGenerator;
            this.side = side;
            this.amount = amount;
        }

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

        private static void handle(SetOutputRatePacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) return;
                AbstractContainerMenu menu = player.containerMenu;
                boolean success = false;
                long acceptedAmount = 0L;
                RateRejectReason reason = RateRejectReason.REJECTED;
                boolean validSide = packet.side >= -1 && packet.side < Direction.values().length;

                if (validSide && menu.containerId == packet.containerId && menu.stillValid(player)) {
                    if (packet.fluidGenerator && menu instanceof InfFluidGeneratorMenu) {
                        InfFluidGeneratorBlockEntity blockEntity =
                                ((InfFluidGeneratorMenu) menu).getBlockEntity();
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
                    } else if (!packet.fluidGenerator && menu instanceof InfItemGeneratorMenu) {
                        InfItemGeneratorBlockEntity blockEntity =
                                ((InfItemGeneratorMenu) menu).getBlockEntity();
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

                CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                        new OutputRateResultPacket(packet.containerId, packet.fluidGenerator,
                                packet.side, success, acceptedAmount, reason.ordinal()));
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

    private static final class SetBatchPacket {
        private final int containerId;
        private final boolean fluidGenerator;
        private final int size;

        private SetBatchPacket(int containerId, boolean fluidGenerator, int size) {
            this.containerId = containerId;
            this.fluidGenerator = fluidGenerator;
            this.size = size;
        }

        private static void encode(SetBatchPacket packet, FriendlyByteBuf buffer) {
            buffer.writeVarInt(packet.containerId);
            buffer.writeBoolean(packet.fluidGenerator);
            buffer.writeVarInt(packet.size);
        }

        private static SetBatchPacket decode(FriendlyByteBuf buffer) {
            return new SetBatchPacket(buffer.readVarInt(), buffer.readBoolean(),
                    buffer.readVarInt());
        }

        private static void handle(SetBatchPacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) return;
                AbstractContainerMenu menu = player.containerMenu;
                if (menu.containerId != packet.containerId || !menu.stillValid(player)) return;
                if (packet.fluidGenerator && menu instanceof InfFluidGeneratorMenu) {
                    ((InfFluidGeneratorMenu) menu).getBlockEntity().setBatchSize(packet.size);
                } else if (!packet.fluidGenerator && menu instanceof InfItemGeneratorMenu) {
                    ((InfItemGeneratorMenu) menu).getBlockEntity().setBatchSize(packet.size);
                }
            });
            context.setPacketHandled(true);
        }
    }

    private static final class SetRedstonePacket {
        private final int containerId;
        private final boolean fluidGenerator;
        private final int mode;
        private final int pulseOnTicks;
        private final int pulseOffTicks;

        private SetRedstonePacket(int containerId, boolean fluidGenerator, int mode,
                                  int pulseOnTicks, int pulseOffTicks) {
            this.containerId = containerId;
            this.fluidGenerator = fluidGenerator;
            this.mode = mode;
            this.pulseOnTicks = pulseOnTicks;
            this.pulseOffTicks = pulseOffTicks;
        }

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

        private static void handle(SetRedstonePacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) return;
                AbstractContainerMenu menu = player.containerMenu;
                if (menu.containerId != packet.containerId || !menu.stillValid(player)) return;
                if (packet.mode < 0 || packet.mode >= RedstoneMode.values().length) return;
                RedstoneMode mode = RedstoneMode.byId(packet.mode);
                if (packet.fluidGenerator && menu instanceof InfFluidGeneratorMenu) {
                    InfFluidGeneratorBlockEntity blockEntity =
                            ((InfFluidGeneratorMenu) menu).getBlockEntity();
                    if (blockEntity.setPulseTicks(packet.pulseOnTicks, packet.pulseOffTicks)) {
                        blockEntity.setRedstoneMode(mode);
                    }
                } else if (!packet.fluidGenerator && menu instanceof InfItemGeneratorMenu) {
                    InfItemGeneratorBlockEntity blockEntity =
                            ((InfItemGeneratorMenu) menu).getBlockEntity();
                    if (blockEntity.setPulseTicks(packet.pulseOnTicks, packet.pulseOffTicks)) {
                        blockEntity.setRedstoneMode(mode);
                    }
                }
            });
            context.setPacketHandled(true);
        }
    }

    private static final class OutputRateResultPacket {
        private final int containerId;
        private final boolean fluidGenerator;
        private final int side;
        private final boolean success;
        private final long amount;
        private final int reason;

        private OutputRateResultPacket(int containerId, boolean fluidGenerator, int side,
                                       boolean success, long amount, int reason) {
            this.containerId = containerId;
            this.fluidGenerator = fluidGenerator;
            this.side = side;
            this.success = success;
            this.amount = amount;
            this.reason = reason;
        }

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

        private static void handle(OutputRateResultPacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ClientRateResultHandler.handle(packet.containerId,
                            packet.fluidGenerator, packet.side, packet.success, packet.amount,
                            RateRejectReason.byId(packet.reason))));
            context.setPacketHandled(true);
        }
    }
}

package com.bandybus.infwhat.network;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.RedstoneMode;
import com.bandybus.infwhat.block.entity.InfFluidGeneratorBlockEntity;
import com.bandybus.infwhat.block.entity.InfItemGeneratorBlockEntity;
import com.bandybus.infwhat.config.InfinityWhatConfig;
import com.bandybus.infwhat.menu.InfFluidGeneratorMenu;
import com.bandybus.infwhat.menu.InfItemGeneratorMenu;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
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
        PayloadRegistrar registrar = event.registrar("5");
        registrar.playToServer(SetOutputRatePayload.TYPE, SetOutputRatePayload.STREAM_CODEC,
                ModNetworking::handleSetRate);
        registrar.playToServer(SetRedstonePayload.TYPE, SetRedstonePayload.STREAM_CODEC,
                ModNetworking::handleSetRedstone);
        registrar.playToServer(SetBatchPayload.TYPE, SetBatchPayload.STREAM_CODEC,
                ModNetworking::handleSetBatch);
    }

    private static void handleSetBatch(SetBatchPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            AbstractContainerMenu menu = player.containerMenu;
            if (menu.containerId != payload.containerId || !menu.stillValid(player)) return;
            if (payload.fluidGenerator && menu instanceof InfFluidGeneratorMenu fluidMenu) {
                fluidMenu.getBlockEntity().setBatchSize(payload.size);
            } else if (!payload.fluidGenerator && menu instanceof InfItemGeneratorMenu itemMenu) {
                itemMenu.getBlockEntity().setBatchSize(payload.size);
            }
        });
    }

    private static void handleSetRedstone(SetRedstonePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            AbstractContainerMenu menu = player.containerMenu;
            if (menu.containerId != payload.containerId || !menu.stillValid(player)) return;
            if (payload.mode < 0 || payload.mode >= RedstoneMode.values().length) return;
            RedstoneMode mode = RedstoneMode.byId(payload.mode);
            if (payload.fluidGenerator && menu instanceof InfFluidGeneratorMenu fluidMenu) {
                InfFluidGeneratorBlockEntity blockEntity = fluidMenu.getBlockEntity();
                if (blockEntity.setPulseTicks(payload.pulseOnTicks, payload.pulseOffTicks)) {
                    blockEntity.setRedstoneMode(mode);
                }
            } else if (!payload.fluidGenerator && menu instanceof InfItemGeneratorMenu itemMenu) {
                InfItemGeneratorBlockEntity blockEntity = itemMenu.getBlockEntity();
                if (blockEntity.setPulseTicks(payload.pulseOnTicks, payload.pulseOffTicks)) {
                    blockEntity.setRedstoneMode(mode);
                }
            }
        });
    }

    private static void handleSetRate(SetOutputRatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            AbstractContainerMenu menu = player.containerMenu;
            boolean success = false;
            long acceptedAmount = 0L;
            RateRejectReason reason = RateRejectReason.REJECTED;
            boolean validSide = payload.side >= -1 && payload.side < Direction.values().length;
            if (validSide && menu.containerId == payload.containerId && menu.stillValid(player)) {
                if (payload.fluidGenerator && menu instanceof InfFluidGeneratorMenu fluidMenu) {
                    InfFluidGeneratorBlockEntity blockEntity = fluidMenu.getBlockEntity();
                    reason = fluidRejectReason(blockEntity, payload.side, payload.amount);
                    if (reason == RateRejectReason.NONE) {
                        if (payload.side < 0) {
                            success = blockEntity.setUniformSideRateMb(payload.amount);
                            acceptedAmount = blockEntity.getUniformSideRateMb();
                        } else {
                            Direction direction = Direction.from3DDataValue(payload.side);
                            success = blockEntity.setSideRateMb(direction, payload.amount);
                            acceptedAmount = blockEntity.getSideRateMb(direction);
                        }
                        if (!success) {
                            reason = RateRejectReason.REJECTED;
                        }
                    }
                } else if (!payload.fluidGenerator && menu instanceof InfItemGeneratorMenu itemMenu) {
                    InfItemGeneratorBlockEntity blockEntity = itemMenu.getBlockEntity();
                    reason = itemRejectReason(blockEntity, payload.side, payload.amount);
                    if (reason == RateRejectReason.NONE) {
                        if (payload.side < 0) {
                            success = blockEntity.setUniformSideRate((int) payload.amount);
                            acceptedAmount = blockEntity.getUniformSideRate();
                        } else {
                            Direction direction = Direction.from3DDataValue(payload.side);
                            success = blockEntity.setSideRate(direction, (int) payload.amount);
                            acceptedAmount = blockEntity.getSideRate(direction);
                        }
                        if (!success) {
                            reason = RateRejectReason.REJECTED;
                        }
                    }
                }
            }
            PacketDistributor.sendToPlayer(player, new OutputRateResultPayload(
                    payload.containerId, payload.fluidGenerator, payload.side,
                    success, acceptedAmount, reason.ordinal()));
        });
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
    public record SetOutputRatePayload(int containerId, boolean fluidGenerator, int side, long amount)
            implements CustomPacketPayload {
        public static final Type<SetOutputRatePayload> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath(InfinityWhat.MOD_ID, "set_output_rate"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SetOutputRatePayload> STREAM_CODEC =
                StreamCodec.of((buffer, payload) -> {
                    buffer.writeVarInt(payload.containerId);
                    buffer.writeBoolean(payload.fluidGenerator);
                    buffer.writeByte(payload.side);
                    buffer.writeLong(payload.amount);
                }, buffer -> new SetOutputRatePayload(buffer.readVarInt(), buffer.readBoolean(),
                        buffer.readByte(), buffer.readLong()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** {@code side} is -1 for "all six sides", otherwise a {@link Direction} data value. */
    public record SetBatchPayload(int containerId, boolean fluidGenerator, int size)
            implements CustomPacketPayload {
        public static final Type<SetBatchPayload> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath(InfinityWhat.MOD_ID, "set_batch"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SetBatchPayload> STREAM_CODEC =
                StreamCodec.of((buffer, payload) -> {
                    buffer.writeVarInt(payload.containerId);
                    buffer.writeBoolean(payload.fluidGenerator);
                    buffer.writeVarInt(payload.size);
                }, buffer -> new SetBatchPayload(buffer.readVarInt(), buffer.readBoolean(),
                        buffer.readVarInt()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** {@code side} is -1 for "all six sides", otherwise a {@link Direction} data value. */
    public record SetRedstonePayload(int containerId, boolean fluidGenerator, int mode,
                                     int pulseOnTicks, int pulseOffTicks)
            implements CustomPacketPayload {
        public static final Type<SetRedstonePayload> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath(InfinityWhat.MOD_ID, "set_redstone"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SetRedstonePayload> STREAM_CODEC =
                StreamCodec.of((buffer, payload) -> {
                    buffer.writeVarInt(payload.containerId);
                    buffer.writeBoolean(payload.fluidGenerator);
                    buffer.writeByte(payload.mode);
                    buffer.writeVarInt(payload.pulseOnTicks);
                    buffer.writeVarInt(payload.pulseOffTicks);
                }, buffer -> new SetRedstonePayload(buffer.readVarInt(), buffer.readBoolean(),
                        buffer.readByte(), buffer.readVarInt(), buffer.readVarInt()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** {@code side} is -1 for "all six sides", otherwise a {@link Direction} data value. */
    public record OutputRateResultPayload(int containerId, boolean fluidGenerator, int side,
                                          boolean success, long amount,
                                          int reason) implements CustomPacketPayload {
        public static final Type<OutputRateResultPayload> TYPE = new Type<>(
                Identifier.fromNamespaceAndPath(InfinityWhat.MOD_ID, "output_rate_result"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OutputRateResultPayload> STREAM_CODEC =
                StreamCodec.of((buffer, payload) -> {
                    buffer.writeVarInt(payload.containerId);
                    buffer.writeBoolean(payload.fluidGenerator);
                    buffer.writeByte(payload.side);
                    buffer.writeBoolean(payload.success);
                    buffer.writeLong(payload.amount);
                    buffer.writeByte(payload.reason);
                }, buffer -> new OutputRateResultPayload(buffer.readVarInt(), buffer.readBoolean(),
                        buffer.readByte(), buffer.readBoolean(), buffer.readLong(), buffer.readByte()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}

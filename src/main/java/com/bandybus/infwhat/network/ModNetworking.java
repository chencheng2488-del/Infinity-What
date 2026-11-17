package com.bandybus.infwhat.network;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.RedstoneMode;
import com.bandybus.infwhat.block.entity.InfFluidGeneratorBlockEntity;
import com.bandybus.infwhat.block.entity.InfItemGeneratorBlockEntity;
import com.bandybus.infwhat.config.InfinityWhatConfig;
import com.bandybus.infwhat.menu.InfFluidGeneratorMenu;
import com.bandybus.infwhat.menu.InfItemGeneratorMenu;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Server-authoritative packets for GUI rates, redstone, batch and save acknowledgements. */
public final class ModNetworking {

    private ModNetworking() {}

    /** Registers both payload directions; called once from the common initializer. */
    public static void registerPayloads() {
        PayloadTypeRegistry.playC2S().register(SetOutputRatePayload.TYPE, SetOutputRatePayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(SetRedstonePayload.TYPE, SetRedstonePayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(SetBatchPayload.TYPE, SetBatchPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(OutputRateResultPayload.TYPE,
                OutputRateResultPayload.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(SetOutputRatePayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> handleSetOutputRate(player, payload.containerId(),
                    payload.fluidGenerator(), payload.side(), payload.amount()));
        });
        ServerPlayNetworking.registerGlobalReceiver(SetRedstonePayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> handleSetRedstone(player, payload.containerId(),
                    payload.fluidGenerator(), payload.mode(), payload.onTicks(), payload.offTicks()));
        });
        ServerPlayNetworking.registerGlobalReceiver(SetBatchPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> handleSetBatch(player, payload.containerId(),
                    payload.fluidGenerator(), payload.size()));
        });
    }

    private static void handleSetOutputRate(ServerPlayer player, int containerId,
                                            boolean fluidGenerator, int side, long amount) {
        AbstractContainerMenu menu = player.containerMenu;
        boolean success = false;
        long acceptedAmount = 0L;
        RateRejectReason reason = RateRejectReason.REJECTED;
        boolean validSide = side >= -1 && side < Direction.values().length;

        if (validSide && menu != null && menu.containerId == containerId && menu.stillValid(player)) {
            if (fluidGenerator && menu instanceof InfFluidGeneratorMenu fluidMenu) {
                InfFluidGeneratorBlockEntity blockEntity = fluidMenu.getBlockEntity();
                reason = fluidRejectReason(blockEntity, side, amount);
                if (reason == RateRejectReason.NONE) {
                    success = side < 0
                            ? blockEntity.setUniformSideRateMb(amount)
                            : blockEntity.setSideRateMb(Direction.from3DDataValue(side), amount);
                    if (success) {
                        acceptedAmount = side < 0
                                ? blockEntity.getUniformSideRateMb()
                                : blockEntity.getSideRateMb(Direction.from3DDataValue(side));
                    } else {
                        reason = RateRejectReason.REJECTED;
                    }
                }
            } else if (!fluidGenerator && menu instanceof InfItemGeneratorMenu itemMenu) {
                InfItemGeneratorBlockEntity blockEntity = itemMenu.getBlockEntity();
                reason = itemRejectReason(blockEntity, side, amount);
                if (reason == RateRejectReason.NONE) {
                    success = side < 0
                            ? blockEntity.setUniformSideRate((int) amount)
                            : blockEntity.setSideRate(Direction.from3DDataValue(side), (int) amount);
                    if (success) {
                        acceptedAmount = side < 0
                                ? blockEntity.getUniformSideRate()
                                : blockEntity.getSideRate(Direction.from3DDataValue(side));
                    } else {
                        reason = RateRejectReason.REJECTED;
                    }
                }
            }
        }

        ServerPlayNetworking.send(player, new OutputRateResultPayload(
                containerId, fluidGenerator, side, success, acceptedAmount, reason.ordinal()));
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

    private static void handleSetRedstone(ServerPlayer player, int containerId,
                                          boolean fluidGenerator, int mode,
                                          int onTicks, int offTicks) {
        AbstractContainerMenu menu = player.containerMenu;
        if (menu == null || menu.containerId != containerId || !menu.stillValid(player)) {
            return;
        }
        if (mode < 0 || mode >= RedstoneMode.values().length) {
            return;
        }
        RedstoneMode selected = RedstoneMode.byId(mode);
        if (fluidGenerator && menu instanceof InfFluidGeneratorMenu fluidMenu) {
            InfFluidGeneratorBlockEntity blockEntity = fluidMenu.getBlockEntity();
            if (blockEntity.setPulseTicks(onTicks, offTicks)) {
                blockEntity.setRedstoneMode(selected);
            }
        } else if (!fluidGenerator && menu instanceof InfItemGeneratorMenu itemMenu) {
            InfItemGeneratorBlockEntity blockEntity = itemMenu.getBlockEntity();
            if (blockEntity.setPulseTicks(onTicks, offTicks)) {
                blockEntity.setRedstoneMode(selected);
            }
        }
    }

    private static void handleSetBatch(ServerPlayer player, int containerId,
                                       boolean fluidGenerator, int size) {
        AbstractContainerMenu menu = player.containerMenu;
        if (menu == null || menu.containerId != containerId || !menu.stillValid(player)) {
            return;
        }
        if (fluidGenerator && menu instanceof InfFluidGeneratorMenu fluidMenu) {
            fluidMenu.getBlockEntity().setBatchSize(size);
        } else if (!fluidGenerator && menu instanceof InfItemGeneratorMenu itemMenu) {
            itemMenu.getBlockEntity().setBatchSize(size);
        }
    }

    // ------------------------------------------------------------------
    // C2S payloads
    // ------------------------------------------------------------------

    public record SetOutputRatePayload(int containerId, boolean fluidGenerator, int side, long amount)
            implements CustomPacketPayload {
        public static final Type<SetOutputRatePayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(InfinityWhat.MOD_ID, "set_output_rate"));
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

    public record SetRedstonePayload(int containerId, boolean fluidGenerator, int mode,
                                     int onTicks, int offTicks) implements CustomPacketPayload {
        public static final Type<SetRedstonePayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(InfinityWhat.MOD_ID, "set_redstone"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SetRedstonePayload> STREAM_CODEC =
                StreamCodec.of((buffer, payload) -> {
                    buffer.writeVarInt(payload.containerId);
                    buffer.writeBoolean(payload.fluidGenerator);
                    buffer.writeByte(payload.mode);
                    buffer.writeVarInt(payload.onTicks);
                    buffer.writeVarInt(payload.offTicks);
                }, buffer -> new SetRedstonePayload(buffer.readVarInt(), buffer.readBoolean(),
                        buffer.readByte(), buffer.readVarInt(), buffer.readVarInt()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record SetBatchPayload(int containerId, boolean fluidGenerator, int size)
            implements CustomPacketPayload {
        public static final Type<SetBatchPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(InfinityWhat.MOD_ID, "set_batch"));
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

    // ------------------------------------------------------------------
    // S2C payload
    // ------------------------------------------------------------------

    public record OutputRateResultPayload(int containerId, boolean fluidGenerator, int side,
                                          boolean success, long amount, int reasonOrdinal)
            implements CustomPacketPayload {
        public static final Type<OutputRateResultPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(InfinityWhat.MOD_ID, "output_rate_result"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OutputRateResultPayload> STREAM_CODEC =
                StreamCodec.of((buffer, payload) -> {
                    buffer.writeVarInt(payload.containerId);
                    buffer.writeBoolean(payload.fluidGenerator);
                    buffer.writeByte(payload.side);
                    buffer.writeBoolean(payload.success);
                    buffer.writeLong(payload.amount);
                    buffer.writeByte(payload.reasonOrdinal);
                }, buffer -> new OutputRateResultPayload(buffer.readVarInt(), buffer.readBoolean(),
                        buffer.readByte(), buffer.readBoolean(), buffer.readLong(), buffer.readByte()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}

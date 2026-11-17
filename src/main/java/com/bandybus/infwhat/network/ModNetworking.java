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
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;
import net.neoforged.neoforge.network.registration.IPayloadRegistrar;

/** Server-authoritative packets for GUI rates, including their save acknowledgements. */
public final class ModNetworking {
    private static final ResourceLocation SET_RATE_ID =
            new ResourceLocation(InfinityWhat.MOD_ID, "set_output_rate");
    private static final ResourceLocation RATE_RESULT_ID =
            new ResourceLocation(InfinityWhat.MOD_ID, "output_rate_result");
    private static final ResourceLocation SET_REDSTONE_ID =
            new ResourceLocation(InfinityWhat.MOD_ID, "set_redstone");
    private static final ResourceLocation SET_BATCH_ID =
            new ResourceLocation(InfinityWhat.MOD_ID, "set_batch");

    private ModNetworking() {}

    public static void registerPayloads(RegisterPayloadHandlerEvent event) {
        IPayloadRegistrar registrar = event.registrar(InfinityWhat.MOD_ID).versioned("1");
        registrar.play(SET_RATE_ID, SetOutputRatePayload::new,
                handlers -> handlers.server(ModNetworking::handleSetRate));
        registrar.play(RATE_RESULT_ID, OutputRateResultPayload::new,
                handlers -> handlers.client(ModNetworking::handleRateResult));
        registrar.play(SET_REDSTONE_ID, SetRedstonePayload::new,
                handlers -> handlers.server(ModNetworking::handleSetRedstone));
        registrar.play(SET_BATCH_ID, SetBatchPayload::new,
                handlers -> handlers.server(ModNetworking::handleSetBatch));
    }

    public static void sendOutputRate(int containerId, boolean fluidGenerator, long amount) {
        sendOutputRate(containerId, fluidGenerator, -1, amount);
    }

    public static void sendOutputRate(int containerId, boolean fluidGenerator, int side, long amount) {
        PacketDistributor.SERVER.noArg().send(
                new SetOutputRatePayload(containerId, fluidGenerator, side, amount));
    }

    public static void sendRedstone(int containerId, boolean fluidGenerator, int mode,
                                    int pulseOnTicks, int pulseOffTicks) {
        PacketDistributor.SERVER.noArg().send(
                new SetRedstonePayload(containerId, fluidGenerator, mode, pulseOnTicks, pulseOffTicks));
    }

    public static void sendBatch(int containerId, boolean fluidGenerator, int size) {
        PacketDistributor.SERVER.noArg().send(new SetBatchPayload(containerId, fluidGenerator, size));
    }

    private static void handleSetRate(SetOutputRatePayload payload, PlayPayloadContext context) {
        context.workHandler().submitAsync(() -> {
            ServerPlayer player = context.player().filter(ServerPlayer.class::isInstance)
                    .map(ServerPlayer.class::cast).orElse(null);
            if (player == null) return;

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
                        } else {
                            success = blockEntity.setSideRateMb(
                                    Direction.from3DDataValue(payload.side), payload.amount);
                        }
                        if (success) {
                            acceptedAmount = payload.side < 0
                                    ? blockEntity.getUniformSideRateMb()
                                    : blockEntity.getSideRateMb(Direction.from3DDataValue(payload.side));
                        } else {
                            reason = RateRejectReason.REJECTED;
                        }
                    }
                } else if (!payload.fluidGenerator && menu instanceof InfItemGeneratorMenu itemMenu) {
                    InfItemGeneratorBlockEntity blockEntity = itemMenu.getBlockEntity();
                    reason = itemRejectReason(blockEntity, payload.side, payload.amount);
                    if (reason == RateRejectReason.NONE) {
                        if (payload.side < 0) {
                            success = blockEntity.setUniformSideRate((int) payload.amount);
                        } else {
                            success = blockEntity.setSideRate(
                                    Direction.from3DDataValue(payload.side), (int) payload.amount);
                        }
                        if (success) {
                            acceptedAmount = payload.side < 0
                                    ? blockEntity.getUniformSideRate()
                                    : blockEntity.getSideRate(Direction.from3DDataValue(payload.side));
                        } else {
                            reason = RateRejectReason.REJECTED;
                        }
                    }
                }
            }

            PacketDistributor.PLAYER.with(player).send(new OutputRateResultPayload(
                    payload.containerId, payload.fluidGenerator, payload.side, success,
                    acceptedAmount, reason.ordinal()));
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

    private static void handleSetBatch(SetBatchPayload payload, PlayPayloadContext context) {
        context.workHandler().submitAsync(() -> {
            ServerPlayer player = context.player().filter(ServerPlayer.class::isInstance)
                    .map(ServerPlayer.class::cast).orElse(null);
            if (player == null) return;
            AbstractContainerMenu menu = player.containerMenu;
            if (menu.containerId != payload.containerId || !menu.stillValid(player)) return;
            if (payload.fluidGenerator && menu instanceof InfFluidGeneratorMenu fluidMenu) {
                fluidMenu.getBlockEntity().setBatchSize(payload.size);
            } else if (!payload.fluidGenerator && menu instanceof InfItemGeneratorMenu itemMenu) {
                itemMenu.getBlockEntity().setBatchSize(payload.size);
            }
        });
    }

    private static void handleSetRedstone(SetRedstonePayload payload, PlayPayloadContext context) {
        context.workHandler().submitAsync(() -> {
            ServerPlayer player = context.player().filter(ServerPlayer.class::isInstance)
                    .map(ServerPlayer.class::cast).orElse(null);
            if (player == null) return;
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

    private static void handleRateResult(OutputRateResultPayload payload, PlayPayloadContext context) {
        context.workHandler().submitAsync(() -> ClientRateResultHandler.handle(
                payload.containerId, payload.fluidGenerator, payload.side, payload.success,
                payload.amount, RateRejectReason.byId(payload.reason)));
    }

    /** {@code side} is -1 for "all six sides", otherwise a {@link Direction} data value. */
    private record SetOutputRatePayload(int containerId, boolean fluidGenerator, int side, long amount)
            implements CustomPacketPayload {
        private SetOutputRatePayload(FriendlyByteBuf buffer) {
            this(buffer.readVarInt(), buffer.readBoolean(), buffer.readByte(), buffer.readLong());
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeVarInt(this.containerId);
            buffer.writeBoolean(this.fluidGenerator);
            buffer.writeByte(this.side);
            buffer.writeLong(this.amount);
        }

        @Override
        public ResourceLocation id() {
            return SET_RATE_ID;
        }
    }

    private record SetBatchPayload(int containerId, boolean fluidGenerator, int size)
            implements CustomPacketPayload {
        private SetBatchPayload(FriendlyByteBuf buffer) {
            this(buffer.readVarInt(), buffer.readBoolean(), buffer.readVarInt());
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeVarInt(this.containerId);
            buffer.writeBoolean(this.fluidGenerator);
            buffer.writeVarInt(this.size);
        }

        @Override
        public ResourceLocation id() {
            return SET_BATCH_ID;
        }
    }

    private record SetRedstonePayload(int containerId, boolean fluidGenerator, int mode,
                                      int pulseOnTicks, int pulseOffTicks)
            implements CustomPacketPayload {
        private SetRedstonePayload(FriendlyByteBuf buffer) {
            this(buffer.readVarInt(), buffer.readBoolean(), buffer.readByte(),
                    buffer.readVarInt(), buffer.readVarInt());
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeVarInt(this.containerId);
            buffer.writeBoolean(this.fluidGenerator);
            buffer.writeByte(this.mode);
            buffer.writeVarInt(this.pulseOnTicks);
            buffer.writeVarInt(this.pulseOffTicks);
        }

        @Override
        public ResourceLocation id() {
            return SET_REDSTONE_ID;
        }
    }

    /** {@code side} is -1 for "all six sides", otherwise a {@link Direction} data value. */
    private record OutputRateResultPayload(int containerId, boolean fluidGenerator, int side,
                                           boolean success, long amount, int reason)
            implements CustomPacketPayload {
        private OutputRateResultPayload(FriendlyByteBuf buffer) {
            this(buffer.readVarInt(), buffer.readBoolean(), buffer.readByte(),
                    buffer.readBoolean(), buffer.readLong(), buffer.readByte());
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeVarInt(this.containerId);
            buffer.writeBoolean(this.fluidGenerator);
            buffer.writeByte(this.side);
            buffer.writeBoolean(this.success);
            buffer.writeLong(this.amount);
            buffer.writeByte(this.reason);
        }

        @Override
        public ResourceLocation id() {
            return RATE_RESULT_ID;
        }
    }
}

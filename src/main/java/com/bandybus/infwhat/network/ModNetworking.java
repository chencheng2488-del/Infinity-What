package com.bandybus.infwhat.network;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.RedstoneMode;
import com.bandybus.infwhat.block.entity.InfFluidGeneratorBlockEntity;
import com.bandybus.infwhat.block.entity.InfItemGeneratorBlockEntity;
import com.bandybus.infwhat.config.InfinityWhatConfig;
import com.bandybus.infwhat.menu.InfFluidGeneratorMenu;
import com.bandybus.infwhat.menu.InfItemGeneratorMenu;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.minecraft.util.math.Direction;
import net.minecraft.util.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.container.Container;

/** Server-authoritative packets for GUI rates, including their save acknowledgements. */
public final class ModNetworking {

    public static final Identifier SET_OUTPUT_RATE =
            new Identifier(InfinityWhat.MOD_ID, "set_output_rate");
    public static final Identifier OUTPUT_RATE_RESULT =
            new Identifier(InfinityWhat.MOD_ID, "output_rate_result");
    public static final Identifier SET_REDSTONE =
            new Identifier(InfinityWhat.MOD_ID, "set_redstone");
    public static final Identifier SET_BATCH =
            new Identifier(InfinityWhat.MOD_ID, "set_batch");

    private ModNetworking() {}

    /** 服务端接收器，在公共入口调用一次。 */
    public static void registerServer() {
        // 1.14–1.14.3 只有旧版 fabric.api.network，没有 networking.v1。
        ServerSidePacketRegistry.INSTANCE.register(SET_OUTPUT_RATE, (context, buffer) -> {
            // 先在线程内读完数据，再切到主线程修改世界状态。
            int containerId = buffer.readVarInt();
            boolean fluidGenerator = buffer.readBoolean();
            int side = buffer.readByte();
            long amount = buffer.readLong();
            context.getTaskQueue().execute(() -> handleSetOutputRate(
                    (ServerPlayerEntity) context.getPlayer(), containerId, fluidGenerator, side, amount));
        });

        ServerSidePacketRegistry.INSTANCE.register(SET_REDSTONE, (context, buffer) -> {
            int containerId = buffer.readVarInt();
            boolean fluidGenerator = buffer.readBoolean();
            int mode = buffer.readByte();
            int pulseOnTicks = buffer.readVarInt();
            int pulseOffTicks = buffer.readVarInt();
            context.getTaskQueue().execute(() -> handleSetRedstone(
                    (ServerPlayerEntity) context.getPlayer(), containerId, fluidGenerator,
                    mode, pulseOnTicks, pulseOffTicks));
        });

        ServerSidePacketRegistry.INSTANCE.register(SET_BATCH, (context, buffer) -> {
            int containerId = buffer.readVarInt();
            boolean fluidGenerator = buffer.readBoolean();
            int size = buffer.readVarInt();
            context.getTaskQueue().execute(() -> handleSetBatch(
                    (ServerPlayerEntity) context.getPlayer(), containerId, fluidGenerator, size));
        });
    }

    /** {@code side} is -1 for "all six sides", otherwise a {@link Direction} data value. */
    private static void handleSetOutputRate(ServerPlayerEntity player, int containerId,
                                            boolean fluidGenerator, int side, long amount) {
        Container menu = player.container;
        boolean success = false;
        long acceptedAmount = 0L;
        RateRejectReason reason = RateRejectReason.REJECTED;
        boolean validSide = side >= -1 && side < Direction.values().length;

        if (validSide && menu.syncId == containerId && menu.canUse(player)) {
            if (fluidGenerator && menu instanceof InfFluidGeneratorMenu) {
                InfFluidGeneratorBlockEntity blockEntity =
                        ((InfFluidGeneratorMenu) menu).getBlockEntity();
                reason = fluidRejectReason(blockEntity, side, amount);
                if (reason == RateRejectReason.NONE) {
                    if (side < 0) {
                        success = blockEntity.setUniformSideRateMb(amount);
                    } else {
                        success = blockEntity.setSideRateMb(
                                Direction.byId(side), amount);
                    }
                    if (success) {
                        acceptedAmount = side < 0
                                ? blockEntity.getUniformSideRateMb()
                                : blockEntity.getSideRateMb(Direction.byId(side));
                    } else {
                        reason = RateRejectReason.REJECTED;
                    }
                }
            } else if (!fluidGenerator && menu instanceof InfItemGeneratorMenu) {
                InfItemGeneratorBlockEntity blockEntity =
                        ((InfItemGeneratorMenu) menu).getBlockEntity();
                reason = itemRejectReason(blockEntity, side, amount);
                if (reason == RateRejectReason.NONE) {
                    if (side < 0) {
                        success = blockEntity.setUniformSideRate((int) amount);
                    } else {
                        success = blockEntity.setSideRate(
                                Direction.byId(side), (int) amount);
                    }
                    if (success) {
                        acceptedAmount = side < 0
                                ? blockEntity.getUniformSideRate()
                                : blockEntity.getSideRate(Direction.byId(side));
                    } else {
                        reason = RateRejectReason.REJECTED;
                    }
                }
            }
        }

        PacketByteBuf reply = new PacketByteBuf(Unpooled.buffer());
        reply.writeVarInt(containerId);
        reply.writeBoolean(fluidGenerator);
        reply.writeByte(side);
        reply.writeBoolean(success);
        reply.writeLong(acceptedAmount);
        reply.writeByte(reason.ordinal());
        ServerSidePacketRegistry.INSTANCE.sendToPlayer(player, OUTPUT_RATE_RESULT, reply);
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
            Direction direction = Direction.byId(side);
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
            Direction direction = Direction.byId(side);
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

    private static void handleSetRedstone(ServerPlayerEntity player, int containerId,
                                          boolean fluidGenerator, int mode,
                                          int pulseOnTicks, int pulseOffTicks) {
        Container menu = player.container;
        if (menu.syncId != containerId || !menu.canUse(player)) return;
        if (mode < 0 || mode >= RedstoneMode.values().length) return;
        RedstoneMode redstoneMode = RedstoneMode.byId(mode);
        if (fluidGenerator && menu instanceof InfFluidGeneratorMenu) {
            InfFluidGeneratorBlockEntity blockEntity =
                    ((InfFluidGeneratorMenu) menu).getBlockEntity();
            if (blockEntity.setPulseTicks(pulseOnTicks, pulseOffTicks)) {
                blockEntity.setRedstoneMode(redstoneMode);
            }
        } else if (!fluidGenerator && menu instanceof InfItemGeneratorMenu) {
            InfItemGeneratorBlockEntity blockEntity =
                    ((InfItemGeneratorMenu) menu).getBlockEntity();
            if (blockEntity.setPulseTicks(pulseOnTicks, pulseOffTicks)) {
                blockEntity.setRedstoneMode(redstoneMode);
            }
        }
    }

    private static void handleSetBatch(ServerPlayerEntity player, int containerId,
                                       boolean fluidGenerator, int size) {
        Container menu = player.container;
        if (menu.syncId != containerId || !menu.canUse(player)) return;
        if (fluidGenerator && menu instanceof InfFluidGeneratorMenu) {
            ((InfFluidGeneratorMenu) menu).getBlockEntity().setBatchSize(size);
        } else if (!fluidGenerator && menu instanceof InfItemGeneratorMenu) {
            ((InfItemGeneratorMenu) menu).getBlockEntity().setBatchSize(size);
        }
    }

    /** 客户端把输出速率请求发给服务端，由服务端校验后回执。 */
    public static void sendOutputRate(int containerId, boolean fluidGenerator, long amount) {
        sendOutputRate(containerId, fluidGenerator, -1, amount);
    }

    public static void sendOutputRate(int containerId, boolean fluidGenerator, int side, long amount) {
        PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
        buffer.writeVarInt(containerId);
        buffer.writeBoolean(fluidGenerator);
        buffer.writeByte(side);
        buffer.writeLong(amount);
        ClientSidePacketRegistry.INSTANCE.sendToServer(SET_OUTPUT_RATE, buffer);
    }

    public static void sendRedstone(int containerId, boolean fluidGenerator, int mode,
                                    int pulseOnTicks, int pulseOffTicks) {
        PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
        buffer.writeVarInt(containerId);
        buffer.writeBoolean(fluidGenerator);
        buffer.writeByte(mode);
        buffer.writeVarInt(pulseOnTicks);
        buffer.writeVarInt(pulseOffTicks);
        ClientSidePacketRegistry.INSTANCE.sendToServer(SET_REDSTONE, buffer);
    }

    public static void sendBatch(int containerId, boolean fluidGenerator, int size) {
        PacketByteBuf buffer = new PacketByteBuf(Unpooled.buffer());
        buffer.writeVarInt(containerId);
        buffer.writeBoolean(fluidGenerator);
        buffer.writeVarInt(size);
        ClientSidePacketRegistry.INSTANCE.sendToServer(SET_BATCH, buffer);
    }
}

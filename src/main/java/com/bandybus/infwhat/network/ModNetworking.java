package com.bandybus.infwhat.network;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.RedstoneMode;
import com.bandybus.infwhat.block.entity.InfFluidGeneratorBlockEntity;
import com.bandybus.infwhat.block.entity.InfItemGeneratorBlockEntity;
import com.bandybus.infwhat.config.InfinityWhatConfig;
import com.bandybus.infwhat.menu.InfFluidGeneratorMenu;
import com.bandybus.infwhat.menu.InfItemGeneratorMenu;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Server-authoritative packets for GUI rates, including their save acknowledgements. */
public final class ModNetworking {

    public static final ResourceLocation SET_OUTPUT_RATE =
            new ResourceLocation(InfinityWhat.MOD_ID, "set_output_rate");
    public static final ResourceLocation OUTPUT_RATE_RESULT =
            new ResourceLocation(InfinityWhat.MOD_ID, "output_rate_result");
    public static final ResourceLocation SET_REDSTONE =
            new ResourceLocation(InfinityWhat.MOD_ID, "set_redstone");
    public static final ResourceLocation SET_BATCH =
            new ResourceLocation(InfinityWhat.MOD_ID, "set_batch");

    private ModNetworking() {}

    /** 服务端接收器，在公共入口调用一次。 */
    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(SET_OUTPUT_RATE,
                (server, player, handler, buffer, responseSender) -> {
                    // 先在线程内读完数据，再切到主线程修改世界状态。
                    int containerId = buffer.readVarInt();
                    boolean fluidGenerator = buffer.readBoolean();
                    int side = buffer.readByte();
                    long amount = buffer.readLong();
                    server.execute(() -> handleSetOutputRate(
                            player, containerId, fluidGenerator, side, amount));
                });

        ServerPlayNetworking.registerGlobalReceiver(SET_REDSTONE,
                (server, player, handler, buffer, responseSender) -> {
                    int containerId = buffer.readVarInt();
                    boolean fluidGenerator = buffer.readBoolean();
                    int mode = buffer.readByte();
                    int pulseOnTicks = buffer.readVarInt();
                    int pulseOffTicks = buffer.readVarInt();
                    server.execute(() -> handleSetRedstone(
                            player, containerId, fluidGenerator, mode, pulseOnTicks, pulseOffTicks));
                });

        ServerPlayNetworking.registerGlobalReceiver(SET_BATCH,
                (server, player, handler, buffer, responseSender) -> {
                    int containerId = buffer.readVarInt();
                    boolean fluidGenerator = buffer.readBoolean();
                    int size = buffer.readVarInt();
                    server.execute(() -> handleSetBatch(player, containerId, fluidGenerator, size));
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
                    if (side < 0) {
                        success = blockEntity.setUniformSideRateMb(amount);
                    } else {
                        success = blockEntity.setSideRateMb(
                                Direction.from3DDataValue(side), amount);
                    }
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
                    if (side < 0) {
                        success = blockEntity.setUniformSideRate((int) amount);
                    } else {
                        success = blockEntity.setSideRate(
                                Direction.from3DDataValue(side), (int) amount);
                    }
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

        FriendlyByteBuf reply = PacketByteBufs.create();
        reply.writeVarInt(containerId);
        reply.writeBoolean(fluidGenerator);
        reply.writeByte(side);
        reply.writeBoolean(success);
        reply.writeLong(acceptedAmount);
        reply.writeByte(reason.ordinal());
        ServerPlayNetworking.send(player, OUTPUT_RATE_RESULT, reply);
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
            newTotal = amount * enabledItemSideCount(blockEntity);
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

    private static int enabledItemSideCount(InfItemGeneratorBlockEntity blockEntity) {
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
                                          int pulseOnTicks, int pulseOffTicks) {
        AbstractContainerMenu menu = player.containerMenu;
        if (menu == null || menu.containerId != containerId || !menu.stillValid(player)) {
            return;
        }
        if (mode < 0 || mode >= RedstoneMode.values().length) {
            return;
        }
        RedstoneMode redstoneMode = RedstoneMode.byId(mode);
        if (fluidGenerator && menu instanceof InfFluidGeneratorMenu fluidMenu) {
            InfFluidGeneratorBlockEntity blockEntity = fluidMenu.getBlockEntity();
            if (blockEntity.setPulseTicks(pulseOnTicks, pulseOffTicks)) {
                blockEntity.setRedstoneMode(redstoneMode);
            }
        } else if (!fluidGenerator && menu instanceof InfItemGeneratorMenu itemMenu) {
            InfItemGeneratorBlockEntity blockEntity = itemMenu.getBlockEntity();
            if (blockEntity.setPulseTicks(pulseOnTicks, pulseOffTicks)) {
                blockEntity.setRedstoneMode(redstoneMode);
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

    /** {@code side} is -1 for "all six sides", otherwise a {@link Direction} data value. */
    public static void sendOutputRate(int containerId, boolean fluidGenerator, int side, long amount) {
        FriendlyByteBuf buffer = PacketByteBufs.create();
        buffer.writeVarInt(containerId);
        buffer.writeBoolean(fluidGenerator);
        buffer.writeByte(side);
        buffer.writeLong(amount);
        ClientPlayNetworking.send(SET_OUTPUT_RATE, buffer);
    }

    public static void sendOutputRate(int containerId, boolean fluidGenerator, long amount) {
        sendOutputRate(containerId, fluidGenerator, -1, amount);
    }

    public static void sendRedstone(int containerId, boolean fluidGenerator, int mode,
                                    int pulseOnTicks, int pulseOffTicks) {
        FriendlyByteBuf buffer = PacketByteBufs.create();
        buffer.writeVarInt(containerId);
        buffer.writeBoolean(fluidGenerator);
        buffer.writeByte(mode);
        buffer.writeVarInt(pulseOnTicks);
        buffer.writeVarInt(pulseOffTicks);
        ClientPlayNetworking.send(SET_REDSTONE, buffer);
    }

    public static void sendBatch(int containerId, boolean fluidGenerator, int size) {
        FriendlyByteBuf buffer = PacketByteBufs.create();
        buffer.writeVarInt(containerId);
        buffer.writeBoolean(fluidGenerator);
        buffer.writeVarInt(size);
        ClientPlayNetworking.send(SET_BATCH, buffer);
    }
}

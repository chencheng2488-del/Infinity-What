package com.bandybus.infwhat.network;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.entity.InfFluidGeneratorBlockEntity;
import com.bandybus.infwhat.block.entity.InfItemGeneratorBlockEntity;
import com.bandybus.infwhat.menu.InfFluidGeneratorMenu;
import com.bandybus.infwhat.menu.InfItemGeneratorMenu;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
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

    private ModNetworking() {}

    /** 服务端接收器，在公共入口调用一次。 */
    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(SET_OUTPUT_RATE,
                (server, player, handler, buffer, responseSender) -> {
                    // 先在线程内读完数据，再切到主线程修改世界状态。
                    int containerId = buffer.readVarInt();
                    boolean fluidGenerator = buffer.readBoolean();
                    long amount = buffer.readLong();
                    server.execute(() -> handleSetOutputRate(player, containerId, fluidGenerator, amount));
                });
    }

    private static void handleSetOutputRate(ServerPlayer player, int containerId,
                                            boolean fluidGenerator, long amount) {
        AbstractContainerMenu menu = player.containerMenu;
        boolean success = false;
        long acceptedAmount = 0L;

        if (menu != null && menu.containerId == containerId && menu.stillValid(player)) {
            if (fluidGenerator && menu instanceof InfFluidGeneratorMenu fluidMenu) {
                if (amount >= 1L && amount <= InfFluidGeneratorBlockEntity.MAX_OUTPUT_RATE_MB) {
                    InfFluidGeneratorBlockEntity blockEntity = fluidMenu.getBlockEntity();
                    blockEntity.setOutputAmountPerTick(amount);
                    acceptedAmount = blockEntity.getOutputAmountPerTick();
                    success = true;
                }
            } else if (!fluidGenerator && menu instanceof InfItemGeneratorMenu itemMenu) {
                if (amount >= 1L && amount <= Integer.MAX_VALUE) {
                    InfItemGeneratorBlockEntity blockEntity = itemMenu.getBlockEntity();
                    blockEntity.setOutputItemsPerTick((int) amount);
                    acceptedAmount = blockEntity.getOutputItemsPerTick();
                    success = true;
                }
            }
        }

        FriendlyByteBuf reply = PacketByteBufs.create();
        reply.writeVarInt(containerId);
        reply.writeBoolean(fluidGenerator);
        reply.writeBoolean(success);
        reply.writeLong(acceptedAmount);
        ServerPlayNetworking.send(player, OUTPUT_RATE_RESULT, reply);
    }

    /** 客户端把输出速率请求发给服务端，由服务端校验后回执。 */
    public static void sendOutputRate(int containerId, boolean fluidGenerator, long amount) {
        FriendlyByteBuf buffer = PacketByteBufs.create();
        buffer.writeVarInt(containerId);
        buffer.writeBoolean(fluidGenerator);
        buffer.writeLong(amount);
        ClientPlayNetworking.send(SET_OUTPUT_RATE, buffer);
    }
}

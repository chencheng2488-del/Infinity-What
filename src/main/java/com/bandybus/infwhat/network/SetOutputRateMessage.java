package com.bandybus.infwhat.network;

import com.bandybus.infwhat.block.entity.InfFluidGeneratorTileEntity;
import com.bandybus.infwhat.block.entity.InfItemGeneratorTileEntity;
import com.bandybus.infwhat.container.InfFluidGeneratorContainer;
import com.bandybus.infwhat.container.InfItemGeneratorContainer;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraftforge.common.util.ForgeDirection;

/** Sent from the client to the server to set one side's output rate, or all six with side -1. */
public final class SetOutputRateMessage implements IMessage {

    private int windowId;
    private boolean fluidGenerator;
    private int side;
    private long amount;

    public SetOutputRateMessage() {
    }

    public SetOutputRateMessage(int windowId, boolean fluidGenerator, int side, long amount) {
        this.windowId = windowId;
        this.fluidGenerator = fluidGenerator;
        this.side = side;
        this.amount = amount;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.windowId = buf.readInt();
        this.fluidGenerator = buf.readBoolean();
        this.side = buf.readByte();
        this.amount = buf.readLong();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.windowId);
        buf.writeBoolean(this.fluidGenerator);
        buf.writeByte(this.side);
        buf.writeLong(this.amount);
    }

    /**
     * 1.7.10 has no server-side scheduled-task API, so the handler applies the change immediately
     * and answers with {@link OutputRateResultMessage}.
     */
    public static final class Handler implements IMessageHandler<SetOutputRateMessage, IMessage> {

        @Override
        public IMessage onMessage(SetOutputRateMessage message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            if (player == null) {
                return null;
            }
            boolean success = false;
            long acceptedAmount = 0L;
            RateRejectReason reason = RateRejectReason.REJECTED;
            boolean validSide = message.side >= -1
                    && message.side < ForgeDirection.VALID_DIRECTIONS.length;

            if (validSide && ModNetworking.validMenu(player, message.windowId)) {
                Container menu = player.openContainer;
                if (message.fluidGenerator && menu instanceof InfFluidGeneratorContainer) {
                    InfFluidGeneratorTileEntity tile =
                            ((InfFluidGeneratorContainer) menu).getTileEntity();
                    reason = ModNetworking.fluidRejectReason(tile, message.side, message.amount);
                    if (reason == RateRejectReason.NONE) {
                        if (message.side < 0) {
                            success = tile.setUniformSideRateMb(message.amount);
                        } else {
                            success = tile.setSideRateMb(
                                    ForgeDirection.VALID_DIRECTIONS[message.side], message.amount);
                        }
                        if (success) {
                            acceptedAmount = message.side < 0
                                    ? tile.getUniformSideRateMb()
                                    : tile.getSideRateMb(
                                            ForgeDirection.VALID_DIRECTIONS[message.side]);
                        } else {
                            reason = RateRejectReason.REJECTED;
                        }
                    }
                } else if (!message.fluidGenerator && menu instanceof InfItemGeneratorContainer) {
                    InfItemGeneratorTileEntity tile =
                            ((InfItemGeneratorContainer) menu).getTileEntity();
                    reason = ModNetworking.itemRejectReason(tile, message.side, message.amount);
                    if (reason == RateRejectReason.NONE) {
                        if (message.side < 0) {
                            success = tile.setUniformSideRate((int) message.amount);
                        } else {
                            success = tile.setSideRate(
                                    ForgeDirection.VALID_DIRECTIONS[message.side],
                                    (int) message.amount);
                        }
                        if (success) {
                            acceptedAmount = message.side < 0
                                    ? tile.getUniformSideRate()
                                    : tile.getSideRate(
                                            ForgeDirection.VALID_DIRECTIONS[message.side]);
                        } else {
                            reason = RateRejectReason.REJECTED;
                        }
                    }
                }
            }

            ModNetworking.channel().sendTo(new OutputRateResultMessage(message.windowId,
                    message.fluidGenerator, message.side, success, acceptedAmount,
                    reason.ordinal()), player);
            return null;
        }
    }
}

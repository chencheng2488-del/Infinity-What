package com.bandybus.infwhat.network;

import com.bandybus.infwhat.block.entity.InfFluidGeneratorTileEntity;
import com.bandybus.infwhat.block.entity.InfItemGeneratorTileEntity;
import com.bandybus.infwhat.container.InfFluidGeneratorContainer;
import com.bandybus.infwhat.container.InfItemGeneratorContainer;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

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

    public static final class Handler implements IMessageHandler<SetOutputRateMessage, IMessage> {

        @Override
        public IMessage onMessage(final SetOutputRateMessage message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            if (player == null) {
                return null;
            }
            player.getServerForPlayer().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    boolean success = false;
                    long acceptedAmount = 0L;
                    RateRejectReason reason = RateRejectReason.REJECTED;
                    boolean validSide = message.side >= -1
                            && message.side < EnumFacing.values().length;

                    if (validSide && ModNetworking.validMenu(player, message.windowId)) {
                        Container menu = player.openContainer;
                        if (message.fluidGenerator
                                && menu instanceof InfFluidGeneratorContainer) {
                            InfFluidGeneratorTileEntity tile =
                                    ((InfFluidGeneratorContainer) menu).getTileEntity();
                            reason = ModNetworking.fluidRejectReason(tile, message.side,
                                    message.amount);
                            if (reason == RateRejectReason.NONE) {
                                if (message.side < 0) {
                                    success = tile.setUniformSideRateMb(message.amount);
                                } else {
                                    success = tile.setSideRateMb(
                                            EnumFacing.values()[message.side], message.amount);
                                }
                                if (success) {
                                    acceptedAmount = message.side < 0
                                            ? tile.getUniformSideRateMb()
                                            : tile.getSideRateMb(
                                                    EnumFacing.values()[message.side]);
                                } else {
                                    reason = RateRejectReason.REJECTED;
                                }
                            }
                        } else if (!message.fluidGenerator
                                && menu instanceof InfItemGeneratorContainer) {
                            InfItemGeneratorTileEntity tile =
                                    ((InfItemGeneratorContainer) menu).getTileEntity();
                            reason = ModNetworking.itemRejectReason(tile, message.side,
                                    message.amount);
                            if (reason == RateRejectReason.NONE) {
                                if (message.side < 0) {
                                    success = tile.setUniformSideRate((int) message.amount);
                                } else {
                                    success = tile.setSideRate(
                                            EnumFacing.values()[message.side],
                                            (int) message.amount);
                                }
                                if (success) {
                                    acceptedAmount = message.side < 0
                                            ? tile.getUniformSideRate()
                                            : tile.getSideRate(
                                                    EnumFacing.values()[message.side]);
                                } else {
                                    reason = RateRejectReason.REJECTED;
                                }
                            }
                        }
                    }

                    ModNetworking.channel().sendTo(new OutputRateResultMessage(message.windowId,
                            message.fluidGenerator, message.side, success, acceptedAmount,
                            reason.ordinal()), player);
                }
            });
            return null;
        }
    }
}

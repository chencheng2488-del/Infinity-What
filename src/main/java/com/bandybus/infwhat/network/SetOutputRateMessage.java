package com.bandybus.infwhat.network;

import com.bandybus.infwhat.block.entity.InfFluidGeneratorTileEntity;
import com.bandybus.infwhat.block.entity.InfItemGeneratorTileEntity;
import com.bandybus.infwhat.config.InfinityWhatConfig;
import com.bandybus.infwhat.container.InfFluidGeneratorContainer;
import com.bandybus.infwhat.container.InfItemGeneratorContainer;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Sent from the client to the server to set a new per-side output rate. */
public final class SetOutputRateMessage implements IMessage {

    private int windowId;
    private boolean fluidGenerator;
    /** -1 means "all six sides", otherwise an {@link EnumFacing} index. */
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
            final EntityPlayerMP player = ctx.getServerHandler().player;
            if (player == null) {
                return null;
            }
            player.getServerWorld().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    Container menu = player.openContainer;
                    boolean success = false;
                    long acceptedAmount = 0L;
                    RateRejectReason reason = RateRejectReason.REJECTED;
                    boolean validSide = message.side >= -1
                            && message.side < EnumFacing.VALUES.length;

                    if (validSide && menu != null && message.windowId == menu.windowId
                            && menu.canInteractWith(player)) {
                        if (message.fluidGenerator
                                && menu instanceof InfFluidGeneratorContainer) {
                            InfFluidGeneratorContainer fluidMenu = (InfFluidGeneratorContainer) menu;
                            InfFluidGeneratorTileEntity tile = fluidMenu.getTileEntity();
                            reason = fluidRejectReason(tile, message.side, message.amount);
                            if (reason == RateRejectReason.NONE) {
                                if (message.side < 0) {
                                    success = tile.setUniformSideRateMb(message.amount);
                                } else {
                                    success = tile.setSideRateMb(
                                            EnumFacing.byIndex(message.side), message.amount);
                                }
                                if (success) {
                                    acceptedAmount = message.side < 0
                                            ? tile.getUniformSideRateMb()
                                            : tile.getSideRateMb(EnumFacing.byIndex(message.side));
                                } else {
                                    reason = RateRejectReason.REJECTED;
                                }
                            }
                        } else if (!message.fluidGenerator
                                && menu instanceof InfItemGeneratorContainer) {
                            InfItemGeneratorContainer itemMenu = (InfItemGeneratorContainer) menu;
                            InfItemGeneratorTileEntity tile = itemMenu.getTileEntity();
                            reason = itemRejectReason(tile, message.side, message.amount);
                            if (reason == RateRejectReason.NONE) {
                                if (message.side < 0) {
                                    success = tile.setUniformSideRate((int) message.amount);
                                } else {
                                    success = tile.setSideRate(
                                            EnumFacing.byIndex(message.side), (int) message.amount);
                                }
                                if (success) {
                                    acceptedAmount = message.side < 0
                                            ? tile.getUniformSideRate()
                                            : tile.getSideRate(EnumFacing.byIndex(message.side));
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

    private static RateRejectReason itemRejectReason(InfItemGeneratorTileEntity tile,
                                                     int side, long amount) {
        if (amount < 0L || amount > Integer.MAX_VALUE) {
            return RateRejectReason.INVALID;
        }
        if (amount > InfinityWhatConfig.maxItemRatePerSide()) {
            return RateRejectReason.LIMIT_SIDE;
        }
        long newTotal;
        if (side < 0) {
            newTotal = amount * enabledItemSideCount(tile);
        } else {
            EnumFacing direction = EnumFacing.byIndex(side);
            newTotal = tile.getTotalEffectiveRate();
            if (tile.isOutputSideEnabled(direction)) {
                newTotal = newTotal - tile.getEffectiveSideRate(direction) + amount;
            }
        }
        return newTotal > InfinityWhatConfig.maxItemTotalRate()
                ? RateRejectReason.LIMIT_TOTAL : RateRejectReason.NONE;
    }

    private static int enabledItemSideCount(InfItemGeneratorTileEntity tile) {
        int count = 0;
        for (EnumFacing direction : EnumFacing.VALUES) {
            if (tile.isOutputSideEnabled(direction)) {
                count++;
            }
        }
        return count;
    }

    private static RateRejectReason fluidRejectReason(InfFluidGeneratorTileEntity tile,
                                                      int side, long amount) {
        if (amount < 0L || amount > InfFluidGeneratorTileEntity.MAX_OUTPUT_RATE_MB) {
            return RateRejectReason.INVALID;
        }
        if (amount > InfinityWhatConfig.maxFluidRatePerSideMb()) {
            return RateRejectReason.LIMIT_SIDE;
        }
        long newTotal;
        if (side < 0) {
            newTotal = amount * enabledFluidSideCount(tile);
        } else {
            EnumFacing direction = EnumFacing.byIndex(side);
            newTotal = tile.getTotalEffectiveRateMb();
            if (tile.isOutputSideEnabled(direction)) {
                newTotal = newTotal - tile.getEffectiveSideRateMb(direction) + amount;
            }
        }
        return newTotal > InfinityWhatConfig.maxFluidTotalRateMb()
                ? RateRejectReason.LIMIT_TOTAL : RateRejectReason.NONE;
    }

    private static int enabledFluidSideCount(InfFluidGeneratorTileEntity tile) {
        int count = 0;
        for (EnumFacing direction : EnumFacing.VALUES) {
            if (tile.isOutputSideEnabled(direction)) {
                count++;
            }
        }
        return count;
    }
}

package com.bandybus.infwhat.network;

import com.bandybus.infwhat.block.entity.InfFluidGeneratorTileEntity;
import com.bandybus.infwhat.container.InfFluidGeneratorContainer;
import com.bandybus.infwhat.container.InfItemGeneratorContainer;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Sent from the client to the server to set a new per-tick output rate. */
public final class SetOutputRateMessage implements IMessage {

    private int windowId;
    private boolean fluidGenerator;
    private long amount;

    public SetOutputRateMessage() {
    }

    public SetOutputRateMessage(int windowId, boolean fluidGenerator, long amount) {
        this.windowId = windowId;
        this.fluidGenerator = fluidGenerator;
        this.amount = amount;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.windowId = buf.readInt();
        this.fluidGenerator = buf.readBoolean();
        this.amount = buf.readLong();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.windowId);
        buf.writeBoolean(this.fluidGenerator);
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
                    Container menu = player.openContainer;
                    boolean success = false;
                    long acceptedAmount = 0L;

                    if (menu != null && message.windowId == menu.windowId
                            && menu.canInteractWith(player)) {
                        if (message.fluidGenerator && menu instanceof InfFluidGeneratorContainer) {
                            InfFluidGeneratorContainer fluidMenu =
                                    (InfFluidGeneratorContainer) menu;
                            if (message.amount >= 1L
                                    && message.amount <= InfFluidGeneratorTileEntity.MAX_OUTPUT_RATE_MB) {
                                fluidMenu.getTileEntity().setOutputAmountPerTick(message.amount);
                                acceptedAmount = fluidMenu.getTileEntity().getOutputAmountPerTick();
                                success = true;
                            }
                        } else if (!message.fluidGenerator
                                && menu instanceof InfItemGeneratorContainer) {
                            InfItemGeneratorContainer itemMenu = (InfItemGeneratorContainer) menu;
                            if (message.amount >= 1L && message.amount <= Integer.MAX_VALUE) {
                                itemMenu.getTileEntity().setOutputItemsPerTick((int) message.amount);
                                acceptedAmount = itemMenu.getTileEntity().getOutputItemsPerTick();
                                success = true;
                            }
                        }
                    }

                    ModNetworking.channel().sendTo(new OutputRateResultMessage(message.windowId,
                            message.fluidGenerator, success, acceptedAmount), player);
                }
            });
            return null;
        }
    }
}

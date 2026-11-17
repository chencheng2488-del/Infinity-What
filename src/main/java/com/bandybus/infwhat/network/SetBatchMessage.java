package com.bandybus.infwhat.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Sent from the client to the server to set the batch output size. */
public final class SetBatchMessage implements IMessage {

    private int windowId;
    private boolean fluidGenerator;
    private int size;

    public SetBatchMessage() {
    }

    public SetBatchMessage(int windowId, boolean fluidGenerator, int size) {
        this.windowId = windowId;
        this.fluidGenerator = fluidGenerator;
        this.size = size;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.windowId = buf.readInt();
        this.fluidGenerator = buf.readBoolean();
        this.size = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.windowId);
        buf.writeBoolean(this.fluidGenerator);
        buf.writeInt(this.size);
    }

    public static final class Handler implements IMessageHandler<SetBatchMessage, IMessage> {

        @Override
        public IMessage onMessage(final SetBatchMessage message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().player;
            if (player == null) {
                return null;
            }
            player.mcServer.addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    if (!ModNetworking.validMenu(player, message.windowId)) {
                        return;
                    }
                    Container menu = player.openContainer;
                    ModNetworking.applyBatch(menu, message.fluidGenerator, message.size);
                }
            });
            return null;
        }
    }
}

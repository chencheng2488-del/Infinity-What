package com.bandybus.infwhat.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;

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
        public IMessage onMessage(SetBatchMessage message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            if (player == null || !ModNetworking.validMenu(player, message.windowId)) {
                return null;
            }
            Container menu = player.openContainer;
            ModNetworking.applyBatch(menu, message.fluidGenerator, message.size);
            return null;
        }
    }
}

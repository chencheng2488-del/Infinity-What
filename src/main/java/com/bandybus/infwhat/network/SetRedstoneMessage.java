package com.bandybus.infwhat.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;

/** Sent from the client to the server to set the redstone mode and pulse timing. */
public final class SetRedstoneMessage implements IMessage {

    private int windowId;
    private boolean fluidGenerator;
    private int mode;
    private int pulseOnTicks;
    private int pulseOffTicks;

    public SetRedstoneMessage() {
    }

    public SetRedstoneMessage(int windowId, boolean fluidGenerator, int mode,
                              int pulseOnTicks, int pulseOffTicks) {
        this.windowId = windowId;
        this.fluidGenerator = fluidGenerator;
        this.mode = mode;
        this.pulseOnTicks = pulseOnTicks;
        this.pulseOffTicks = pulseOffTicks;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.windowId = buf.readInt();
        this.fluidGenerator = buf.readBoolean();
        this.mode = buf.readByte();
        this.pulseOnTicks = buf.readInt();
        this.pulseOffTicks = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.windowId);
        buf.writeBoolean(this.fluidGenerator);
        buf.writeByte(this.mode);
        buf.writeInt(this.pulseOnTicks);
        buf.writeInt(this.pulseOffTicks);
    }

    public static final class Handler implements IMessageHandler<SetRedstoneMessage, IMessage> {

        @Override
        public IMessage onMessage(SetRedstoneMessage message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            if (player == null || !ModNetworking.validMenu(player, message.windowId)) {
                return null;
            }
            Container menu = player.openContainer;
            ModNetworking.applyRedstone(menu, message.fluidGenerator, message.mode,
                    message.pulseOnTicks, message.pulseOffTicks);
            return null;
        }
    }
}

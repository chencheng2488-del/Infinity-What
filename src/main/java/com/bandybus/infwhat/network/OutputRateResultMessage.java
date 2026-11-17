package com.bandybus.infwhat.network;

import com.bandybus.infwhat.client.network.ClientRateResultHandler;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

/** Server acknowledgement telling the GUI whether the requested rate was accepted. */
public final class OutputRateResultMessage implements IMessage {

    private int windowId;
    private boolean fluidGenerator;
    private int side;
    private boolean success;
    private long amount;
    private int reason;

    public OutputRateResultMessage() {
    }

    public OutputRateResultMessage(int windowId, boolean fluidGenerator, int side,
                                   boolean success, long amount, int reason) {
        this.windowId = windowId;
        this.fluidGenerator = fluidGenerator;
        this.side = side;
        this.success = success;
        this.amount = amount;
        this.reason = reason;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.windowId = buf.readInt();
        this.fluidGenerator = buf.readBoolean();
        this.side = buf.readByte();
        this.success = buf.readBoolean();
        this.amount = buf.readLong();
        this.reason = buf.readByte();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.windowId);
        buf.writeBoolean(this.fluidGenerator);
        buf.writeByte(this.side);
        buf.writeBoolean(this.success);
        buf.writeLong(this.amount);
        buf.writeByte(this.reason);
    }

    public static final class Handler implements IMessageHandler<OutputRateResultMessage, IMessage> {

        @Override
        public IMessage onMessage(final OutputRateResultMessage message, MessageContext ctx) {
            if (ctx.side == Side.CLIENT) {
                ClientRateResultHandler.handle(message.windowId, message.fluidGenerator,
                        message.side, message.success, message.amount,
                        RateRejectReason.byId(message.reason));
            }
            return null;
        }
    }
}

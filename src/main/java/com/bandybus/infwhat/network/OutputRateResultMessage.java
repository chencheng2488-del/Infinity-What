package com.bandybus.infwhat.network;

import com.bandybus.infwhat.client.gui.GuiInfFluidGenerator;
import com.bandybus.infwhat.client.gui.GuiInfItemGenerator;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

/** Server acknowledgement telling the GUI whether the requested rate was accepted. */
public final class OutputRateResultMessage implements IMessage {

    private int windowId;
    private boolean fluidGenerator;
    private boolean success;
    private long amount;

    public OutputRateResultMessage() {
    }

    public OutputRateResultMessage(int windowId, boolean fluidGenerator,
                                    boolean success, long amount) {
        this.windowId = windowId;
        this.fluidGenerator = fluidGenerator;
        this.success = success;
        this.amount = amount;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.windowId = buf.readInt();
        this.fluidGenerator = buf.readBoolean();
        this.success = buf.readBoolean();
        this.amount = buf.readLong();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.windowId);
        buf.writeBoolean(this.fluidGenerator);
        buf.writeBoolean(this.success);
        buf.writeLong(this.amount);
    }

    public static final class Handler
            implements IMessageHandler<OutputRateResultMessage, IMessage> {

        @Override
        public IMessage onMessage(OutputRateResultMessage message, MessageContext ctx) {
            if (ctx.side == Side.CLIENT) {
                Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                    @Override
                    public void run() {
                        if (Minecraft.getMinecraft().currentScreen instanceof GuiInfFluidGenerator
                                && message.fluidGenerator) {
                            ((GuiInfFluidGenerator) Minecraft.getMinecraft().currentScreen)
                                    .onOutputRateResult(message.windowId, message.success, message.amount);
                        } else if (Minecraft.getMinecraft().currentScreen instanceof GuiInfItemGenerator
                                && !message.fluidGenerator) {
                            ((GuiInfItemGenerator) Minecraft.getMinecraft().currentScreen)
                                    .onOutputRateResult(message.windowId, message.success, message.amount);
                        }
                    }
                });
            }
            return null;
        }
    }
}

package com.bandybus.infwhat.network;

import com.bandybus.infwhat.block.RedstoneMode;
import com.bandybus.infwhat.block.entity.InfFluidGeneratorTileEntity;
import com.bandybus.infwhat.block.entity.InfItemGeneratorTileEntity;
import com.bandybus.infwhat.container.InfFluidGeneratorContainer;
import com.bandybus.infwhat.container.InfItemGeneratorContainer;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Sent from the client to the server to change the redstone mode and pulse timing. */
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
        public IMessage onMessage(final SetRedstoneMessage message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().player;
            if (player == null) {
                return null;
            }
            player.getServerWorld().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    Container menu = player.openContainer;
                    if (menu == null || message.windowId != menu.windowId
                            || !menu.canInteractWith(player)) {
                        return;
                    }
                    if (message.mode < 0 || message.mode >= RedstoneMode.values().length) {
                        return;
                    }
                    RedstoneMode mode = RedstoneMode.byId(message.mode);
                    if (message.fluidGenerator && menu instanceof InfFluidGeneratorContainer) {
                        InfFluidGeneratorTileEntity tile =
                                ((InfFluidGeneratorContainer) menu).getTileEntity();
                        if (tile.setPulseTicks(message.pulseOnTicks, message.pulseOffTicks)) {
                            tile.setRedstoneMode(mode);
                        }
                    } else if (!message.fluidGenerator
                            && menu instanceof InfItemGeneratorContainer) {
                        InfItemGeneratorTileEntity tile =
                                ((InfItemGeneratorContainer) menu).getTileEntity();
                        if (tile.setPulseTicks(message.pulseOnTicks, message.pulseOffTicks)) {
                            tile.setRedstoneMode(mode);
                        }
                    }
                }
            });
            return null;
        }
    }
}

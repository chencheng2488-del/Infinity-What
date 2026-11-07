package com.bandybus.infwhat.network;

import com.bandybus.infwhat.integration.thaumcraft4.ThaumcraftAltarControlManager;
import com.bandybus.infwhat.integration.thaumcraft4.ThaumcraftIntegration;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;

/** Client-to-server edit for the Ordo Usurpatory Matrix settings. */
public final class SetOrdoSettingsMessage implements IMessage {

    private int x;
    private int y;
    private int z;
    private float speedMultiplier;
    private int stabilityMode;
    private float stabilityValue;

    public SetOrdoSettingsMessage() {
    }

    public SetOrdoSettingsMessage(int x, int y, int z, float speedMultiplier, int stabilityMode,
                                  float stabilityValue) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.speedMultiplier = speedMultiplier;
        this.stabilityMode = stabilityMode;
        this.stabilityValue = stabilityValue;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.x = buf.readInt();
        this.y = buf.readInt();
        this.z = buf.readInt();
        this.speedMultiplier = buf.readFloat();
        this.stabilityMode = buf.readInt();
        this.stabilityValue = buf.readFloat();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.x);
        buf.writeInt(this.y);
        buf.writeInt(this.z);
        buf.writeFloat(this.speedMultiplier);
        buf.writeInt(this.stabilityMode);
        buf.writeFloat(this.stabilityValue);
    }

    public static final class Handler implements IMessageHandler<SetOrdoSettingsMessage, IMessage> {

        @Override
        public IMessage onMessage(SetOrdoSettingsMessage message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            if (player != null && ThaumcraftIntegration.isActive()) {
                // Off-thread here: queue the edit and let the world tick apply it. When the
                // integration is inactive nothing drains the queue, so never enqueue at all.
                ThaumcraftAltarControlManager.requestSettings(player, message.x, message.y,
                        message.z, message.speedMultiplier, message.stabilityMode,
                        message.stabilityValue);
            }
            return null;
        }
    }
}

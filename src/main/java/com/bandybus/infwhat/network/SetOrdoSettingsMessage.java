package com.bandybus.infwhat.network;

import com.bandybus.infwhat.block.entity.OrdoUsurpatoryMatrixTileEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Client-to-server edit for the Ordo Usurpatory Matrix settings. */
public final class SetOrdoSettingsMessage implements IMessage {

    private BlockPos pos = BlockPos.ORIGIN;
    private float speedMultiplier;
    private int stabilityMode;
    private float stabilityValue;

    public SetOrdoSettingsMessage() {
    }

    public SetOrdoSettingsMessage(BlockPos pos, float speedMultiplier, int stabilityMode,
                                  float stabilityValue) {
        this.pos = pos;
        this.speedMultiplier = speedMultiplier;
        this.stabilityMode = stabilityMode;
        this.stabilityValue = stabilityValue;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.pos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
        this.speedMultiplier = buf.readFloat();
        this.stabilityMode = buf.readInt();
        this.stabilityValue = buf.readFloat();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.pos.getX());
        buf.writeInt(this.pos.getY());
        buf.writeInt(this.pos.getZ());
        buf.writeFloat(this.speedMultiplier);
        buf.writeInt(this.stabilityMode);
        buf.writeFloat(this.stabilityValue);
    }

    public static final class Handler implements IMessageHandler<SetOrdoSettingsMessage, IMessage> {

        @Override
        public IMessage onMessage(final SetOrdoSettingsMessage message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().player;
            if (player == null) {
                return null;
            }
            player.getServerWorld().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    if (player.getDistanceSq(message.pos.getX() + 0.5D,
                            message.pos.getY() + 0.5D, message.pos.getZ() + 0.5D) > 64.0D) {
                        return;
                    }
                    TileEntity tile = player.getServerWorld().getTileEntity(message.pos);
                    if (!(tile instanceof OrdoUsurpatoryMatrixTileEntity)) {
                        return;
                    }
                    OrdoUsurpatoryMatrixTileEntity matrix =
                            (OrdoUsurpatoryMatrixTileEntity) tile;
                    OrdoUsurpatoryMatrixTileEntity.StabilityMode[] modes =
                            OrdoUsurpatoryMatrixTileEntity.StabilityMode.values();
                    int mode = Math.max(0, Math.min(modes.length - 1, message.stabilityMode));
                    matrix.setSettings(message.speedMultiplier, modes[mode],
                            message.stabilityValue);
                }
            });
            return null;
        }
    }
}

package com.bandybus.infwhat.client.network;

import com.bandybus.infwhat.network.RateRejectReason;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

/** Client-only entry points used by the common network handler. */
@SideOnly(Side.CLIENT)
public final class ClientPacketHandler {

    private ClientPacketHandler() {
    }

    public static void handleRateResult(int windowId, boolean fluidGenerator, int side,
                                        boolean success, long amount, RateRejectReason reason) {
        ClientRateResultHandler.handle(windowId, fluidGenerator, side, success, amount, reason);
    }

    /** Applies a full machine state sent by the server to the matching client tile entity. */
    public static void applyTileSync(int x, int y, int z, NBTTagCompound compound) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.theWorld == null) {
            return;
        }
        TileEntity tile = minecraft.theWorld.getBlockTileEntity(x, y, z);
        if (tile != null) {
            tile.readFromNBT(compound);
        }
    }
}

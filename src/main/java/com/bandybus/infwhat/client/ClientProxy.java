package com.bandybus.infwhat.client;

import com.bandybus.infwhat.CommonProxy;
import com.bandybus.infwhat.block.entity.OrdoUsurpatoryMatrixTileEntity;
import com.bandybus.infwhat.client.gui.GuiOrdoUsurpatoryMatrix;
import com.bandybus.infwhat.client.render.ArcaneRangeRenderer;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.MinecraftForge;

/**
 * Client-only proxy. 1.7.10 renders blocks and items directly from the texture names declared on
 * the block and item instances, so no model registration is required here.
 */
@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        MinecraftForge.EVENT_BUS.register(new ArcaneRangeRenderer());
    }

    @Override
    public void openOrdoUsurpatoryMatrixGui(int x, int y, int z) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.theWorld == null) {
            return;
        }
        TileEntity tile = minecraft.theWorld.getTileEntity(x, y, z);
        if (tile instanceof OrdoUsurpatoryMatrixTileEntity) {
            minecraft.displayGuiScreen(new GuiOrdoUsurpatoryMatrix(
                    (OrdoUsurpatoryMatrixTileEntity) tile));
        }
    }

    @Override
    public void showNotLinkedWarning(EntityPlayer player) {
        Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(
                new ChatComponentTranslation("message.infwhat.arcane_shard.not_linked")
                        .setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)));
    }
}

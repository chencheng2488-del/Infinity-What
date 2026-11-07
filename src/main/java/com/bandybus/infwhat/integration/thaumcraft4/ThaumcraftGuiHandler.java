package com.bandybus.infwhat.integration.thaumcraft4;

import com.bandybus.infwhat.client.gui.GuiArcaneAspectReplicator;
import com.bandybus.infwhat.client.gui.GuiArcaneEssentiaEmanator;
import com.bandybus.infwhat.container.ArcaneAspectReplicatorContainer;
import com.bandybus.infwhat.container.ArcaneEssentiaEmanatorContainer;
import com.bandybus.infwhat.container.ModGuiHandler;
import com.bandybus.infwhat.container.ModGuiIds;
import cpw.mods.fml.common.network.IGuiHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * Thaumcraft-aware GUI handler. It is only instantiated when Thaumcraft is present, which keeps
 * the Thaumcraft tile classes out of the common GUI handler.
 */
public final class ThaumcraftGuiHandler implements IGuiHandler {

    private static final IGuiHandler COMMON = new ModGuiHandler();

    private ThaumcraftGuiHandler() {
    }

    public static IGuiHandler create() {
        return new ThaumcraftGuiHandler();
    }

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (id == ModGuiIds.ARCANE_ASPECT_REPLICATOR
                && tile instanceof ArcaneAspectReplicatorTileEntity) {
            return new ArcaneAspectReplicatorContainer(player.inventory,
                    (ArcaneAspectReplicatorTileEntity) tile);
        }
        if (id == ModGuiIds.ARCANE_ESSENTIA_EMANATOR
                && tile instanceof ArcaneEssentiaEmanatorTileEntity) {
            return new ArcaneEssentiaEmanatorContainer(player.inventory,
                    (ArcaneEssentiaEmanatorTileEntity) tile);
        }
        return COMMON.getServerGuiElement(id, player, world, x, y, z);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (id == ModGuiIds.ARCANE_ASPECT_REPLICATOR
                && tile instanceof ArcaneAspectReplicatorTileEntity) {
            return new GuiArcaneAspectReplicator(new ArcaneAspectReplicatorContainer(
                    player.inventory, (ArcaneAspectReplicatorTileEntity) tile), player.inventory);
        }
        if (id == ModGuiIds.ARCANE_ESSENTIA_EMANATOR
                && tile instanceof ArcaneEssentiaEmanatorTileEntity) {
            return new GuiArcaneEssentiaEmanator(new ArcaneEssentiaEmanatorContainer(
                    player.inventory, (ArcaneEssentiaEmanatorTileEntity) tile), player.inventory);
        }
        return COMMON.getClientGuiElement(id, player, world, x, y, z);
    }
}

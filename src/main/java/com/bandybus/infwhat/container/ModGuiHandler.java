package com.bandybus.infwhat.container;

import com.bandybus.infwhat.block.entity.InfFluidGeneratorTileEntity;
import com.bandybus.infwhat.block.entity.InfItemGeneratorTileEntity;
import com.bandybus.infwhat.client.gui.GuiInfFluidGenerator;
import com.bandybus.infwhat.client.gui.GuiInfItemGenerator;
import cpw.mods.fml.common.network.IGuiHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/** Maps the two machine GUIs to their server containers and client screens. */
public class ModGuiHandler implements IGuiHandler {

    /** GUI id of the fluid generator. */
    public static final int FLUID_GENERATOR = 0;
    /** GUI id of the item generator. */
    public static final int ITEM_GENERATOR = 1;

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity tileEntity = world.getTileEntity(x, y, z);
        if (id == FLUID_GENERATOR && tileEntity instanceof InfFluidGeneratorTileEntity) {
            return new InfFluidGeneratorContainer(player.inventory,
                    (InfFluidGeneratorTileEntity) tileEntity);
        }
        if (id == ITEM_GENERATOR && tileEntity instanceof InfItemGeneratorTileEntity) {
            return new InfItemGeneratorContainer(player.inventory,
                    (InfItemGeneratorTileEntity) tileEntity);
        }
        return null;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity tileEntity = world.getTileEntity(x, y, z);
        if (id == FLUID_GENERATOR && tileEntity instanceof InfFluidGeneratorTileEntity) {
            return new GuiInfFluidGenerator(new InfFluidGeneratorContainer(player.inventory,
                    (InfFluidGeneratorTileEntity) tileEntity), player.inventory);
        }
        if (id == ITEM_GENERATOR && tileEntity instanceof InfItemGeneratorTileEntity) {
            return new GuiInfItemGenerator(new InfItemGeneratorContainer(player.inventory,
                    (InfItemGeneratorTileEntity) tileEntity), player.inventory);
        }
        return null;
    }
}

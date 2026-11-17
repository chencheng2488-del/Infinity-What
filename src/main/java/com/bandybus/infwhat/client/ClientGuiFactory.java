package com.bandybus.infwhat.client;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.entity.InfFluidGeneratorBlockEntity;
import com.bandybus.infwhat.block.entity.InfItemGeneratorBlockEntity;
import com.bandybus.infwhat.client.screen.InfFluidGeneratorScreen;
import com.bandybus.infwhat.client.screen.InfItemGeneratorScreen;
import com.bandybus.infwhat.menu.InfFluidGeneratorMenu;
import com.bandybus.infwhat.menu.InfItemGeneratorMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.FMLPlayMessages;

/**
 * Builds the client-side GUI for an {@code OpenContainer} packet. The packet carries the block
 * position written by {@code NetworkHooks.openGui}; the tile entity is the client-side copy,
 * and {@code GuiContainer.initGui()} assigns the freshly created container to
 * {@code player.openContainer} so the window id from the packet can be applied.
 */
public final class ClientGuiFactory {

    private static final ResourceLocation FLUID_GENERATOR_GUI =
            new ResourceLocation(InfinityWhat.MOD_ID, "inf_fluid_generator");
    private static final ResourceLocation ITEM_GENERATOR_GUI =
            new ResourceLocation(InfinityWhat.MOD_ID, "inf_item_generator");

    private ClientGuiFactory() {
    }

    public static GuiScreen open(FMLPlayMessages.OpenContainer openContainer) {
        Minecraft minecraft = Minecraft.getInstance();
        EntityPlayerSP player = minecraft.player;
        World world = minecraft.world;
        if (player == null || world == null) {
            return null;
        }
        BlockPos pos = openContainer.getAdditionalData().readBlockPos();
        if (!world.isBlockLoaded(pos)) {
            return null;
        }
        TileEntity tileEntity = world.getTileEntity(pos);
        ResourceLocation id = openContainer.getId();
        if (FLUID_GENERATOR_GUI.equals(id)
                && tileEntity instanceof InfFluidGeneratorBlockEntity) {
            InfFluidGeneratorMenu menu = new InfFluidGeneratorMenu(player.inventory, tileEntity);
            // Forge 1.13 only rewrites openContainer's windowId after displayGuiScreen; the
            // factory must put the freshly built menu into player.openContainer first, otherwise
            // slot clicks carry the old container id and every transfer is rejected.
            player.openContainer = menu;
            return new InfFluidGeneratorScreen(menu, player.inventory,
                    new TextComponentTranslation(
                            "container." + InfinityWhat.MOD_ID + ".inf_fluid_generator"));
        }
        if (ITEM_GENERATOR_GUI.equals(id)
                && tileEntity instanceof InfItemGeneratorBlockEntity) {
            InfItemGeneratorMenu menu = new InfItemGeneratorMenu(player.inventory, tileEntity);
            player.openContainer = menu;
            return new InfItemGeneratorScreen(menu, player.inventory,
                    new TextComponentTranslation(
                            "container." + InfinityWhat.MOD_ID + ".inf_item_generator"));
        }
        return null;
    }
}

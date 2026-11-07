package com.bandybus.infwhat;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import net.minecraft.entity.player.EntityPlayer;

/** Server-side proxy: the 1.7.10 release has no server-specific setup. */
public class CommonProxy {

    /** Server-side pre-initialisation hook; intentionally empty. */
    public void preInit(FMLPreInitializationEvent event) {
    }

    /** Server-side initialisation hook; intentionally empty. */
    public void init(FMLInitializationEvent event) {
    }

    /** Client-only screen hook; the server proxy intentionally does nothing. */
    public void openOrdoUsurpatoryMatrixGui(int x, int y, int z) {
    }

    /** Client-only chat hook used when the shard is clicked on an unsupported machine. */
    public void showNotLinkedWarning(EntityPlayer player) {
    }
}

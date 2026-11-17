package com.bandybus.infwhat;

import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/** Server-side proxy: the old Forge release has no server-specific setup. */
public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
    }

    public void init(FMLInitializationEvent event) {
    }

    /** Client-only screen hook; the server proxy intentionally does nothing. */
    public void openOrdoUsurpatoryMatrixGui(BlockPos pos) {
    }
}

package com.bandybus.infwhat;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

/** Server-side proxy: the 1.7.10 release has no server-specific setup. */
public class CommonProxy {

    /** Server-side pre-initialisation hook; intentionally empty. */
    public void preInit(FMLPreInitializationEvent event) {
    }

    /** Server-side initialisation hook; intentionally empty. */
    public void init(FMLInitializationEvent event) {
    }
}

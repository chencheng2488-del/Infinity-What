package com.bandybus.infwhat.client;

import com.bandybus.infwhat.CommonProxy;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Client-only proxy. 1.7.10 renders blocks and items directly from the texture names declared on
 * the block and item instances, so no model registration is required here.
 */
@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
    }
}

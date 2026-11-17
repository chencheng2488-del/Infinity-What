package com.bandybus.infwhat.block.entity;

import com.bandybus.infwhat.InfinityWhat;
import net.minecraftforge.fml.common.registry.GameRegistry;

/** Tile entity registration for the 1.8.9 Forge release. */
public final class ModTileEntities {

    private ModTileEntities() {
    }

    public static void register() {
        GameRegistry.registerTileEntity(InfFluidGeneratorTileEntity.class,
                InfinityWhat.MODID + ":inf_fluid_generator");
        GameRegistry.registerTileEntity(InfItemGeneratorTileEntity.class,
                InfinityWhat.MODID + ":inf_item_generator");
    }
}

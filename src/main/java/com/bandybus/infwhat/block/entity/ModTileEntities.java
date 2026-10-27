package com.bandybus.infwhat.block.entity;

import com.bandybus.infwhat.InfinityWhat;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.GameRegistry;

/** Tile entity registration for the 1.12.2 Forge release. */
public final class ModTileEntities {

    private ModTileEntities() {
    }

    public static void register() {
        GameRegistry.registerTileEntity(InfFluidGeneratorTileEntity.class,
                new ResourceLocation(InfinityWhat.MODID, "inf_fluid_generator"));
        GameRegistry.registerTileEntity(InfItemGeneratorTileEntity.class,
                new ResourceLocation(InfinityWhat.MODID, "inf_item_generator"));
    }
}

package com.bandybus.infwhat.block.entity;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.integration.thaumcraft6.ThaumcraftIntegration;
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
        if (ThaumcraftIntegration.isActive()) {
            GameRegistry.registerTileEntity(OrdoUsurpatoryMatrixTileEntity.class,
                    new ResourceLocation(InfinityWhat.MODID, "ordo_usurpatory_matrix"));
            GameRegistry.registerTileEntity(
                    com.bandybus.infwhat.integration.thaumcraft6.ArcaneAspectReplicatorTileEntity.class,
                    new ResourceLocation(InfinityWhat.MODID, "arcane_aspect_replicator"));
            GameRegistry.registerTileEntity(
                    com.bandybus.infwhat.integration.thaumcraft6.ArcaneEssentiaEmanatorTileEntity.class,
                    new ResourceLocation(InfinityWhat.MODID, "arcane_essentia_emanator"));
        }
    }
}

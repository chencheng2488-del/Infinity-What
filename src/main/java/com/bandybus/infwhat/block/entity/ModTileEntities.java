package com.bandybus.infwhat.block.entity;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.integration.thaumcraft4.ThaumcraftIntegration;
import cpw.mods.fml.common.registry.GameRegistry;

/** Tile entity registration for the 1.7.10 Forge release. */
public final class ModTileEntities {

    private ModTileEntities() {
    }

    /** Registers both tile entity classes under stable save ids. */
    public static void register() {
        GameRegistry.registerTileEntity(InfFluidGeneratorTileEntity.class,
                InfinityWhat.MODID + ":inf_fluid_generator");
        GameRegistry.registerTileEntity(InfItemGeneratorTileEntity.class,
                InfinityWhat.MODID + ":inf_item_generator");
        if (ThaumcraftIntegration.isActive()) {
            GameRegistry.registerTileEntity(OrdoUsurpatoryMatrixTileEntity.class,
                    InfinityWhat.MODID + ":ordo_usurpatory_matrix");
            GameRegistry.registerTileEntity(
                    com.bandybus.infwhat.integration.thaumcraft4.ArcaneAspectReplicatorTileEntity.class,
                    InfinityWhat.MODID + ":arcane_aspect_replicator");
            GameRegistry.registerTileEntity(
                    com.bandybus.infwhat.integration.thaumcraft4.ArcaneEssentiaEmanatorTileEntity.class,
                    InfinityWhat.MODID + ":arcane_essentia_emanator");
        }
    }
}

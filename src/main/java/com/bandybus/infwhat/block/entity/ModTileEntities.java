package com.bandybus.infwhat.block.entity;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.integration.thaumcraft6.ThaumcraftIntegration;
import net.minecraftforge.fml.common.registry.GameRegistry;

/** Tile entity registration for the 1.12 Forge release. */
public final class ModTileEntities {

    private ModTileEntities() {
    }

    public static void register() {
        // 1.12 的 GameRegistry 只接受字符串注册名。
        GameRegistry.registerTileEntity(InfFluidGeneratorTileEntity.class,
                InfinityWhat.MODID + ":inf_fluid_generator");
        GameRegistry.registerTileEntity(InfItemGeneratorTileEntity.class,
                InfinityWhat.MODID + ":inf_item_generator");
        if (ThaumcraftIntegration.isActive()) {
            GameRegistry.registerTileEntity(OrdoUsurpatoryMatrixTileEntity.class,
                    InfinityWhat.MODID + ":ordo_usurpatory_matrix");
            GameRegistry.registerTileEntity(
                    com.bandybus.infwhat.integration.thaumcraft6.ArcaneAspectReplicatorTileEntity.class,
                    InfinityWhat.MODID + ":arcane_aspect_replicator");
            GameRegistry.registerTileEntity(
                    com.bandybus.infwhat.integration.thaumcraft6.ArcaneEssentiaEmanatorTileEntity.class,
                    InfinityWhat.MODID + ":arcane_essentia_emanator");
        }
    }
}

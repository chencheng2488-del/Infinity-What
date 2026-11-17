package com.bandybus.infwhat.block;

import com.bandybus.infwhat.config.InfinityWhatConfig;
import com.bandybus.infwhat.integration.thaumcraft4.ThaumcraftIntegration;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;

/**
 * Block and BlockItem registration for the 1.6.4 Forge release.
 *
 * <p>1.6.4 registers a block in its constructor ({@code Block.blocksList[id] = this}), so the
 * Thaumcraft-only blocks are created in {@link #register()} only while the integration is active.
 * Creating them unconditionally would occupy their numeric ids and could expose them without
 * Thaumcraft installed.
 */
public final class ModBlocks {

    /** Infinity fluid generator block instance. */
    public static final InfFluidGeneratorBlock INF_FLUID_GENERATOR =
            new InfFluidGeneratorBlock(InfinityWhatConfig.fluidGeneratorBlockId(),
                    "inf_fluid_generator");

    /** Infinity item generator block instance. */
    public static final InfItemGeneratorBlock INF_ITEM_GENERATOR =
            new InfItemGeneratorBlock(InfinityWhatConfig.itemGeneratorBlockId(),
                    "inf_item_generator");

    /** Thaumcraft altar controller; created only when Thaumcraft 4 is present. */
    public static OrdoUsurpatoryMatrixBlock ORDO_USURPATORY_MATRIX;

    /** Arcane Aspect Replicator; created only when Thaumcraft 4 is present. */
    public static ArcaneAspectReplicatorBlock ARCANE_ASPECT_REPLICATOR;

    /** Arcane Essentia Emanator; created only when Thaumcraft 4 is present. */
    public static ArcaneEssentiaEmanatorBlock ARCANE_ESSENTIA_EMANATOR;

    private ModBlocks() {
    }

    /** Registers both machines together with their block items. */
    public static void register() {
        registerBlock(INF_FLUID_GENERATOR, "inf_fluid_generator");
        registerBlock(INF_ITEM_GENERATOR, "inf_item_generator");
        if (ThaumcraftIntegration.isActive()) {
            ORDO_USURPATORY_MATRIX = new OrdoUsurpatoryMatrixBlock(
                    InfinityWhatConfig.ordoMatrixBlockId(), "ordo_usurpatory_matrix");
            // Dedicated item class: it adds the machine tooltip and lore lines.
            GameRegistry.registerBlock(ORDO_USURPATORY_MATRIX,
                    ItemBlockOrdoUsurpatoryMatrix.class, "ordo_usurpatory_matrix");

            ARCANE_ASPECT_REPLICATOR = new ArcaneAspectReplicatorBlock(
                    InfinityWhatConfig.arcaneReplicatorBlockId(), "arcane_aspect_replicator");
            registerBlock(ARCANE_ASPECT_REPLICATOR, "arcane_aspect_replicator");

            ARCANE_ESSENTIA_EMANATOR = new ArcaneEssentiaEmanatorBlock(
                    InfinityWhatConfig.arcaneEmanatorBlockId(), "arcane_essentia_emanator");
            registerBlock(ARCANE_ESSENTIA_EMANATOR, "arcane_essentia_emanator");
        }
    }

    /** Registers one block with a dedicated block item limited to a single item per stack. */
    private static void registerBlock(Block block, String name) {
        // The game registry adds the active mod id itself, so the bare name must be passed here.
        GameRegistry.registerBlock(block, ItemBlockMachine.class, name);
    }
}

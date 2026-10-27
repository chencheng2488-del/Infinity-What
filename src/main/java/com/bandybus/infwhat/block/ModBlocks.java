package com.bandybus.infwhat.block;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;

/**
 * Block and BlockItem registration for the 1.7.10 Forge release. Blocks are created once during
 * class initialisation and published through {@link GameRegistry}.
 */
public final class ModBlocks {

    /** Infinity fluid generator block instance. */
    public static final InfFluidGeneratorBlock INF_FLUID_GENERATOR =
            new InfFluidGeneratorBlock("inf_fluid_generator");

    /** Infinity item generator block instance. */
    public static final InfItemGeneratorBlock INF_ITEM_GENERATOR =
            new InfItemGeneratorBlock("inf_item_generator");

    private ModBlocks() {
    }

    /** Registers both machines together with their block items. */
    public static void register() {
        registerBlock(INF_FLUID_GENERATOR, "inf_fluid_generator");
        registerBlock(INF_ITEM_GENERATOR, "inf_item_generator");
    }

    /** Registers one block with a dedicated block item limited to a single item per stack. */
    private static void registerBlock(Block block, String name) {
        // The game registry adds the active mod id itself, so the bare name must be passed here.
        GameRegistry.registerBlock(block, ItemBlockMachine.class, name);
    }
}

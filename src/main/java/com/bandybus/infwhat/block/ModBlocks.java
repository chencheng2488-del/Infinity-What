package com.bandybus.infwhat.block;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.creative.ModCreativeTab;
import com.bandybus.infwhat.integration.thaumcraft6.ThaumcraftIntegration;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Block and BlockItem registration for the old Forge release. Blocks are created once during
 * class initialisation and published through the 1.12.2 {@link RegistryEvent} pipeline. Every
 * machine stack size is fixed at one, matching the modern versions.
 */
@Mod.EventBusSubscriber(modid = InfinityWhat.MODID)
public final class ModBlocks {

    public static final InfFluidGeneratorBlock INF_FLUID_GENERATOR =
            new InfFluidGeneratorBlock("inf_fluid_generator");
    public static final InfItemGeneratorBlock INF_ITEM_GENERATOR =
            new InfItemGeneratorBlock("inf_item_generator");
    public static final OrdoUsurpatoryMatrixBlock ORDO_USURPATORY_MATRIX =
            new OrdoUsurpatoryMatrixBlock("ordo_usurpatory_matrix");
    public static final ArcaneAspectReplicatorBlock ARCANE_ASPECT_REPLICATOR =
            new ArcaneAspectReplicatorBlock("arcane_aspect_replicator");
    public static final ArcaneEssentiaEmanatorBlock ARCANE_ESSENTIA_EMANATOR =
            new ArcaneEssentiaEmanatorBlock("arcane_essentia_emanator");

    private ModBlocks() {
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().register(INF_FLUID_GENERATOR);
        event.getRegistry().register(INF_ITEM_GENERATOR);
        if (ThaumcraftIntegration.isActive()) {
            event.getRegistry().register(ORDO_USURPATORY_MATRIX);
            event.getRegistry().register(ARCANE_ASPECT_REPLICATOR);
            event.getRegistry().register(ARCANE_ESSENTIA_EMANATOR);
        }
    }

    @SubscribeEvent
    public static void registerBlockItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(createBlockItem(INF_FLUID_GENERATOR));
        event.getRegistry().register(createBlockItem(INF_ITEM_GENERATOR));
        if (ThaumcraftIntegration.isActive()) {
            event.getRegistry().register(createOrdoMatrixItem(ORDO_USURPATORY_MATRIX));
            event.getRegistry().register(createBlockItem(ARCANE_ASPECT_REPLICATOR));
            event.getRegistry().register(createBlockItem(ARCANE_ESSENTIA_EMANATOR));
        }
    }

    private static ItemBlock createOrdoMatrixItem(Block block) {
        ItemBlock item = new OrdoUsurpatoryMatrixItemBlock(block);
        item.setRegistryName(block.getRegistryName());
        item.setMaxStackSize(1);
        item.setCreativeTab(ModCreativeTab.INSTANCE);
        return item;
    }

    private static ItemBlock createBlockItem(Block block) {
        ItemBlock item = new ItemBlock(block);
        item.setRegistryName(block.getRegistryName());
        item.setMaxStackSize(1);
        item.setCreativeTab(ModCreativeTab.INSTANCE);
        return item;
    }
}

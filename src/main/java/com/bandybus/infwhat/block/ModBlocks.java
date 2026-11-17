package com.bandybus.infwhat.block;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.creative.ModCreativeTab;
import com.bandybus.infwhat.item.InfinityItemBlock;
import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.fml.common.registry.GameRegistry;

/**
 * Block and BlockItem registration for the 1.11.2 Forge release.
 */
public final class ModBlocks {

    public static final InfFluidGeneratorBlock INF_FLUID_GENERATOR =
            new InfFluidGeneratorBlock("inf_fluid_generator");
    public static final InfItemGeneratorBlock INF_ITEM_GENERATOR =
            new InfItemGeneratorBlock("inf_item_generator");

    private ModBlocks() {
    }

    public static void register() {
        registerBlock(INF_FLUID_GENERATOR, "inf_fluid_generator");
        registerBlock(INF_ITEM_GENERATOR, "inf_item_generator");
    }

    private static void registerBlock(Block block, String name) {
        GameRegistry.register(block);
        ItemBlock item = new InfinityItemBlock(block);
        item.setRegistryName(InfinityWhat.MODID, name);
        // Spec: stack_size = 1
        item.setMaxStackSize(1);
        item.setCreativeTab(ModCreativeTab.INSTANCE);
        GameRegistry.register(item);
    }
}

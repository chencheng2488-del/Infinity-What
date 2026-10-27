package com.bandybus.infwhat.block;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.creative.ModCreativeTab;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.fml.common.registry.GameRegistry;

/**
 * Block and BlockItem registration for the 1.8.9 Forge release. Blocks are created once during
 * class initialisation and published through {@link GameRegistry}.
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
        GameRegistry.registerBlock(block, ItemBlock.class, InfinityWhat.MODID + ":" + name);
        Item item = Item.getItemFromBlock(block);
        if (item != null) {
            // Spec: stack_size = 1
            item.setMaxStackSize(1);
            item.setCreativeTab(ModCreativeTab.INSTANCE);
        }
    }
}

package com.bandybus.infwhat.block;

import com.bandybus.infwhat.InfinityWhat;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.block.Block;
import net.minecraft.sound.BlockSoundGroup;

import net.minecraft.block.Material;
import net.minecraft.block.MaterialColor;

public class ModBlocks {

    public static InfFluidGeneratorBlock INF_FLUID_GENERATOR;
    public static InfItemGeneratorBlock INF_ITEM_GENERATOR;

    public static void register() {
        INF_FLUID_GENERATOR = registerBlock("inf_fluid_generator",
                new InfFluidGeneratorBlock(Block.Settings.of(Material.METAL,
                        MaterialColor.IRON)
                        .strength(3.5F, 10.0F)));

        INF_ITEM_GENERATOR = registerBlock("inf_item_generator",
                new InfItemGeneratorBlock(Block.Settings.of(Material.METAL,
                        MaterialColor.IRON)
                        .strength(3.5F, 10.0F)));
    }

    private static <T extends Block> T registerBlock(String name, T block) {
        Identifier id = new Identifier(InfinityWhat.MOD_ID, name);
        Registry.register(Registry.BLOCK, id, block);
        // Spec: stack_size = 1
        Registry.register(Registry.ITEM, id,
                new BlockItem(block, new Item.Settings().stackSize(1)));
        return block;
    }

    private ModBlocks() {
    }
}

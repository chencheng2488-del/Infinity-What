package com.bandybus.infwhat.block;

import com.bandybus.infwhat.InfinityWhat;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;

public class ModBlocks {

    public static InfFluidGeneratorBlock INF_FLUID_GENERATOR;
    public static InfItemGeneratorBlock INF_ITEM_GENERATOR;

    public static void register() {
        INF_FLUID_GENERATOR = registerBlock("inf_fluid_generator",
                new InfFluidGeneratorBlock(BlockBehaviour.Properties.of(Material.METAL)
                        .color(MaterialColor.METAL)
                        .strength(3.5F, 10.0F)
                        .sound(SoundType.METAL)
                        .requiresCorrectToolForDrops()));

        INF_ITEM_GENERATOR = registerBlock("inf_item_generator",
                new InfItemGeneratorBlock(BlockBehaviour.Properties.of(Material.METAL)
                        .color(MaterialColor.METAL)
                        .strength(3.5F, 10.0F)
                        .sound(SoundType.METAL)
                        // Prevent a redstone line touching the machine from powering dust on its
                        // far side through the generator's otherwise solid block shape.
                        .isRedstoneConductor((state, level, pos) -> false)
                        .requiresCorrectToolForDrops()));
    }

    private static <T extends Block> T registerBlock(String name, T block) {
        ResourceLocation id = new ResourceLocation(InfinityWhat.MOD_ID, name);
        Registry.register(Registry.BLOCK, id, block);
        // Spec: stack_size = 1
        Registry.register(Registry.ITEM, id,
                new BlockItem(block, new Item.Properties().stacksTo(1)));
        return block;
    }

    private ModBlocks() {
    }
}

package com.bandybus.infwhat.block;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.item.ModItems;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.util.function.Supplier;

public class ModBlocks {
    public static final Block INF_FLUID_GENERATOR = registerBlock("inf_fluid_generator",
            () -> new InfFluidGeneratorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5F, 10.0F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()));

    public static final Block INF_ITEM_GENERATOR = registerBlock("inf_item_generator",
            () -> new InfItemGeneratorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5F, 10.0F)
                    .sound(SoundType.METAL)
                    // Prevent a redstone line touching the machine from powering dust on its
                    // far side through the generator's otherwise solid block shape.
                    .isRedstoneConductor((state, level, pos) -> false)
                    .requiresCorrectToolForDrops()));

    private static <T extends Block> T registerBlock(String name, Supplier<T> block) {
        T registered = Registry.register(BuiltInRegistries.BLOCK, InfinityWhat.id(name), block.get());
        // Spec: stack_size = 1
        ModItems.registerBlockItem(name, registered);
        return registered;
    }

    public static Item item(Block block) {
        return block.asItem();
    }

    public static void register() {
    }
}

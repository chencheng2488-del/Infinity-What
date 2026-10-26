package com.bandybus.infwhat.block;

import com.bandybus.infwhat.InfinityWhat;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.core.Registry;

public class ModBlocks {
    public static Block INF_FLUID_GENERATOR;
    public static Block INF_ITEM_GENERATOR;

    public static void register() {
        INF_FLUID_GENERATOR = registerBlock("inf_fluid_generator",
                new InfFluidGeneratorBlock(baseProperties("inf_fluid_generator")));

        INF_ITEM_GENERATOR = registerBlock("inf_item_generator",
                new InfItemGeneratorBlock(baseProperties("inf_item_generator")
                        // Prevent a redstone line touching the machine from powering dust on its
                        // far side through the generator's otherwise solid block shape.
                        .isRedstoneConductor((state, level, pos) -> false)));
    }

    /** 1.21.2+ requires every block to declare its own registry id, otherwise registration fails at runtime. */
    private static BlockBehaviour.Properties baseProperties(String name) {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(3.5F, 10.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops()
                .setId(ResourceKey.create(Registries.BLOCK,
                        Identifier.fromNamespaceAndPath(InfinityWhat.MOD_ID, name)));
    }

    private static Block registerBlock(String name, Block block) {
        return Registry.register(BuiltInRegistries.BLOCK,
                Identifier.fromNamespaceAndPath(InfinityWhat.MOD_ID, name), block);
    }
}

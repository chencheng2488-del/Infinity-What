package com.bandybus.infwhat.block;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.creative.ModCreativeTabs;
import com.bandybus.infwhat.item.ModItems;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, InfinityWhat.MOD_ID);

    public static final RegistryObject<Block> INF_FLUID_GENERATOR = registerBlock("inf_fluid_generator",
            () -> new InfFluidGeneratorBlock(AbstractBlock.Properties.of(Material.METAL, MaterialColor.METAL)
                    .strength(3.5F, 10.0F)
                    .sound(SoundType.METAL)
                    // 1.16.5 has no mineable/pickaxe-driven tool requirement; Forge's ToolType is the
                    // 1.16.5 equivalent of the modern "wooden pickaxe or better" tag.
                    .harvestTool(ToolType.PICKAXE)
                    .harvestLevel(0)
                    .requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> INF_ITEM_GENERATOR = registerBlock("inf_item_generator",
            () -> new InfItemGeneratorBlock(AbstractBlock.Properties.of(Material.METAL, MaterialColor.METAL)
                    .strength(3.5F, 10.0F)
                    .sound(SoundType.METAL)
                    // Prevent a redstone line touching the machine from powering dust on its
                    // far side through the generator's otherwise solid block shape.
                    .isRedstoneConductor((state, world, pos) -> false)
                    .harvestTool(ToolType.PICKAXE)
                    .harvestLevel(0)
                    .requiresCorrectToolForDrops()));

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem(String name, RegistryObject<T> block) {
        // Spec: stack_size = 1. 1.16.5 attaches items to a creative tab via the item properties.
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()
                .stacksTo(1)
                .tab(ModCreativeTabs.INFINITY_WHAT)));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}

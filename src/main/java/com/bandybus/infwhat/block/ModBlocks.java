package com.bandybus.infwhat.block;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.creative.ModCreativeTabs;
import com.bandybus.infwhat.item.ModItems;
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
            new DeferredRegister<>(ForgeRegistries.BLOCKS, InfinityWhat.MOD_ID);

    public static final RegistryObject<Block> INF_FLUID_GENERATOR = registerBlock("inf_fluid_generator",
            () -> new InfFluidGeneratorBlock(Block.Properties.of(Material.METAL, MaterialColor.METAL)
                    .strength(3.5F, 10.0F)
                    .sound(SoundType.METAL)
                    .harvestTool(ToolType.PICKAXE)
                    .harvestLevel(0)));

    public static final RegistryObject<Block> INF_ITEM_GENERATOR = registerBlock("inf_item_generator",
            () -> new InfItemGeneratorBlock(Block.Properties.of(Material.METAL, MaterialColor.METAL)
                    .strength(3.5F, 10.0F)
                    .sound(SoundType.METAL)
                    .harvestTool(ToolType.PICKAXE)
                    .harvestLevel(0)));

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

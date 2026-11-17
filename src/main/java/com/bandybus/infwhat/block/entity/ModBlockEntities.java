package com.bandybus.infwhat.block.entity;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.ModBlocks;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ModBlockEntities {

    public static BlockEntityType<InfFluidGeneratorBlockEntity> INF_FLUID_GENERATOR;
    public static BlockEntityType<InfItemGeneratorBlockEntity> INF_ITEM_GENERATOR;

    public static void register() {
        INF_FLUID_GENERATOR = Registry.register(Registry.BLOCK_ENTITY_TYPE,
                new ResourceLocation(InfinityWhat.MOD_ID, "inf_fluid_generator"),
                FabricBlockEntityTypeBuilder.create(
                        InfFluidGeneratorBlockEntity::new,
                        ModBlocks.INF_FLUID_GENERATOR
                ).build());

        INF_ITEM_GENERATOR = Registry.register(Registry.BLOCK_ENTITY_TYPE,
                new ResourceLocation(InfinityWhat.MOD_ID, "inf_item_generator"),
                FabricBlockEntityTypeBuilder.create(
                        InfItemGeneratorBlockEntity::new,
                        ModBlocks.INF_ITEM_GENERATOR
                ).build());
    }

    private ModBlockEntities() {
    }
}

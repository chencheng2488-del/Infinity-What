package com.bandybus.infwhat.block.entity;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.ModBlocks;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ModBlockEntities {
    public static BlockEntityType<InfFluidGeneratorBlockEntity> INF_FLUID_GENERATOR;
    public static BlockEntityType<InfItemGeneratorBlockEntity> INF_ITEM_GENERATOR;

    public static void register() {
        INF_FLUID_GENERATOR = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath(InfinityWhat.MOD_ID, "inf_fluid_generator"),
                FabricBlockEntityTypeBuilder.create(InfFluidGeneratorBlockEntity::new,
                        ModBlocks.INF_FLUID_GENERATOR).build());

        INF_ITEM_GENERATOR = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath(InfinityWhat.MOD_ID, "inf_item_generator"),
                FabricBlockEntityTypeBuilder.create(InfItemGeneratorBlockEntity::new,
                        ModBlocks.INF_ITEM_GENERATOR).build());

        // 每个面都暴露同一个储罐/槽位，供管道、AE2 等模组访问。
        FluidStorage.SIDED.registerForBlockEntities(
                (blockEntity, direction) -> blockEntity instanceof InfFluidGeneratorBlockEntity generator
                        ? generator.getFluidStorage() : null,
                INF_FLUID_GENERATOR);
        ItemStorage.SIDED.registerForBlockEntities(
                (blockEntity, direction) -> blockEntity instanceof InfItemGeneratorBlockEntity generator
                        ? generator.getItemStorage() : null,
                INF_ITEM_GENERATOR);
    }
}

package com.bandybus.infwhat.block.entity;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.ModBlocks;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ModBlockEntities {
    public static final BlockEntityType<InfFluidGeneratorBlockEntity> INF_FLUID_GENERATOR =
            Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    InfinityWhat.id("inf_fluid_generator"),
                    FabricBlockEntityTypeBuilder.create(InfFluidGeneratorBlockEntity::new,
                            ModBlocks.INF_FLUID_GENERATOR).build());

    public static final BlockEntityType<InfItemGeneratorBlockEntity> INF_ITEM_GENERATOR =
            Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    InfinityWhat.id("inf_item_generator"),
                    FabricBlockEntityTypeBuilder.create(InfItemGeneratorBlockEntity::new,
                            ModBlocks.INF_ITEM_GENERATOR).build());

    public static void register() {
    }

    public static void registerStorage() {
        FluidStorage.SIDED.registerForBlockEntities(
                (blockEntity, direction) -> ((InfFluidGeneratorBlockEntity) blockEntity).getFluidHandler(),
                INF_FLUID_GENERATOR);
        ItemStorage.SIDED.registerForBlockEntities(
                (blockEntity, direction) -> ((InfItemGeneratorBlockEntity) blockEntity).getItemHandler(),
                INF_ITEM_GENERATOR);
    }
}

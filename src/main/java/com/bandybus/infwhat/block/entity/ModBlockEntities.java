package com.bandybus.infwhat.block.entity;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.ModBlocks;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ModBlockEntities {

    public static BlockEntityType<InfFluidGeneratorBlockEntity> INF_FLUID_GENERATOR;
    public static BlockEntityType<InfItemGeneratorBlockEntity> INF_ITEM_GENERATOR;

    public static void register() {
        INF_FLUID_GENERATOR = Registry.register(Registry.BLOCK_ENTITY_TYPE,
                new ResourceLocation(InfinityWhat.MOD_ID, "inf_fluid_generator"),
                BlockEntityType.Builder.of(InfFluidGeneratorBlockEntity::new,
                        ModBlocks.INF_FLUID_GENERATOR).build(null));

        INF_ITEM_GENERATOR = Registry.register(Registry.BLOCK_ENTITY_TYPE,
                new ResourceLocation(InfinityWhat.MOD_ID, "inf_item_generator"),
                BlockEntityType.Builder.of(InfItemGeneratorBlockEntity::new,
                        ModBlocks.INF_ITEM_GENERATOR).build(null));

        // Expose both machines through the Fabric transfer lookup. Without these providers a
        // neighbouring generator or pipe has no storage to push into, so block-to-block output
        // silently does nothing.
        FluidStorage.SIDED.registerForBlockEntity(
                (blockEntity, direction) -> blockEntity.getFluidStorage(),
                INF_FLUID_GENERATOR);
        ItemStorage.SIDED.registerForBlockEntity(
                (blockEntity, direction) -> blockEntity.getItemStorage(),
                INF_ITEM_GENERATOR);
    }

    private ModBlockEntities() {
    }
}

package com.bandybus.infwhat.block.entity;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.ModBlocks;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ModBlockEntities {

    public static BlockEntityType<InfFluidGeneratorBlockEntity> INF_FLUID_GENERATOR;
    public static BlockEntityType<InfItemGeneratorBlockEntity> INF_ITEM_GENERATOR;

    public static void register() {
        // Fabric API 0.42 (1.16.5) has no FabricBlockEntityTypeBuilder and no generic
        // fluid/item storage lookup, so the vanilla 1.16.5 block-entity factory is used and
        // the machines expose their contents only through the GUI and active output.
        INF_FLUID_GENERATOR = Registry.register(Registry.BLOCK_ENTITY_TYPE,
                new ResourceLocation(InfinityWhat.MOD_ID, "inf_fluid_generator"),
                BlockEntityType.Builder.of(InfFluidGeneratorBlockEntity::new,
                        ModBlocks.INF_FLUID_GENERATOR).build(null));

        INF_ITEM_GENERATOR = Registry.register(Registry.BLOCK_ENTITY_TYPE,
                new ResourceLocation(InfinityWhat.MOD_ID, "inf_item_generator"),
                BlockEntityType.Builder.of(InfItemGeneratorBlockEntity::new,
                        ModBlocks.INF_ITEM_GENERATOR).build(null));
    }

    private ModBlockEntities() {
    }
}

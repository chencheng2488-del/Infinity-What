package com.bandybus.infwhat.block.entity;

import com.bandybus.infwhat.InfinityWhat;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.block.entity.BlockEntityType;

public class ModBlockEntities {

    public static BlockEntityType<InfFluidGeneratorBlockEntity> INF_FLUID_GENERATOR;
    public static BlockEntityType<InfItemGeneratorBlockEntity> INF_ITEM_GENERATOR;

    public static void register() {
        // Fabric API 0.42 (1.16.5) has no FabricBlockEntityTypeBuilder and no generic
        // fluid/item storage lookup, so the vanilla 1.16.5 block-entity factory is used and
        // the machines expose their contents only through the GUI and active output.
        INF_FLUID_GENERATOR = Registry.register(Registry.BLOCK_ENTITY,
                new Identifier(InfinityWhat.MOD_ID, "inf_fluid_generator"),
                BlockEntityType.Builder.create(InfFluidGeneratorBlockEntity::new).build(null));

        INF_ITEM_GENERATOR = Registry.register(Registry.BLOCK_ENTITY,
                new Identifier(InfinityWhat.MOD_ID, "inf_item_generator"),
                BlockEntityType.Builder.create(InfItemGeneratorBlockEntity::new).build(null));
    }

    private ModBlockEntities() {
    }
}

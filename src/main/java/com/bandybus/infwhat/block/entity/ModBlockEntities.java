package com.bandybus.infwhat.block.entity;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, InfinityWhat.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InfFluidGeneratorBlockEntity>> INF_FLUID_GENERATOR =
            BLOCK_ENTITIES.register("inf_fluid_generator", () ->
                    BlockEntityType.Builder.of(
                            InfFluidGeneratorBlockEntity::new,
                            ModBlocks.INF_FLUID_GENERATOR.get()
                    ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InfItemGeneratorBlockEntity>> INF_ITEM_GENERATOR =
            BLOCK_ENTITIES.register("inf_item_generator", () ->
                    BlockEntityType.Builder.of(
                            InfItemGeneratorBlockEntity::new,
                            ModBlocks.INF_ITEM_GENERATOR.get()
                    ).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}

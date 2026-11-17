package com.bandybus.infwhat.block.entity;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.ModBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Set;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, InfinityWhat.MOD_ID);

    public static final RegistryObject<BlockEntityType<InfFluidGeneratorBlockEntity>> INF_FLUID_GENERATOR =
            BLOCK_ENTITIES.register("inf_fluid_generator", () ->
                    new BlockEntityType<>(
                            InfFluidGeneratorBlockEntity::new,
                            Set.of(ModBlocks.INF_FLUID_GENERATOR.get())
                    ));

    public static final RegistryObject<BlockEntityType<InfItemGeneratorBlockEntity>> INF_ITEM_GENERATOR =
            BLOCK_ENTITIES.register("inf_item_generator", () ->
                    new BlockEntityType<>(
                            InfItemGeneratorBlockEntity::new,
                            Set.of(ModBlocks.INF_ITEM_GENERATOR.get())
                    ));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}

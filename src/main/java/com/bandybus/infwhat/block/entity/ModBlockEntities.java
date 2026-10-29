package com.bandybus.infwhat.block.entity;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.ModBlocks;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModBlockEntities {
    public static final DeferredRegister<TileEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, InfinityWhat.MOD_ID);

    public static final RegistryObject<TileEntityType<InfFluidGeneratorBlockEntity>> INF_FLUID_GENERATOR =
            BLOCK_ENTITIES.register("inf_fluid_generator", () ->
                    TileEntityType.Builder.of(
                            InfFluidGeneratorBlockEntity::new,
                            ModBlocks.INF_FLUID_GENERATOR.get()
                    ).build(null));

    public static final RegistryObject<TileEntityType<InfItemGeneratorBlockEntity>> INF_ITEM_GENERATOR =
            BLOCK_ENTITIES.register("inf_item_generator", () ->
                    TileEntityType.Builder.of(
                            InfItemGeneratorBlockEntity::new,
                            ModBlocks.INF_ITEM_GENERATOR.get()
                    ).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}

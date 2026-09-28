package cn.autoforged.infinity_what_1790519020.block.entity;

import cn.autoforged.infinity_what_1790519020.InfinityWhat;
import cn.autoforged.infinity_what_1790519020.block.ModBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, InfinityWhat.MOD_ID);

    public static final RegistryObject<BlockEntityType<InfFluidGeneratorBlockEntity>> INF_FLUID_GENERATOR =
            BLOCK_ENTITIES.register("inf_fluid_generator", () ->
                    BlockEntityType.Builder.of(
                            InfFluidGeneratorBlockEntity::new,
                            ModBlocks.INF_FLUID_GENERATOR.get()
                    ).build(null));

    public static final RegistryObject<BlockEntityType<InfItemGeneratorBlockEntity>> INF_ITEM_GENERATOR =
            BLOCK_ENTITIES.register("inf_item_generator", () ->
                    BlockEntityType.Builder.of(
                            InfItemGeneratorBlockEntity::new,
                            ModBlocks.INF_ITEM_GENERATOR.get()
                    ).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}

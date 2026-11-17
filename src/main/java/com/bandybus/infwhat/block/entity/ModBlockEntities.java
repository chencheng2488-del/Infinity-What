package com.bandybus.infwhat.block.entity;

import com.bandybus.infwhat.InfinityWhat;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.function.Supplier;

/**
 * Tile entity types for 1.13.2. This generation has {@link TileEntityType} but no
 * DeferredRegister, so each type is built eagerly and registered from the registry event.
 * The data-fixer type is {@code null}, exactly like vanilla's {@code Builder#build(null)}.
 */
@Mod.EventBusSubscriber(modid = InfinityWhat.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModBlockEntities {

    public static final TileEntityType<?> INF_FLUID_GENERATOR =
            create("inf_fluid_generator", InfFluidGeneratorBlockEntity::new);

    public static final TileEntityType<?> INF_ITEM_GENERATOR =
            create("inf_item_generator", InfItemGeneratorBlockEntity::new);

    private ModBlockEntities() {
    }

    private static <T extends TileEntity> TileEntityType<?> create(String name, Supplier<T> factory) {
        return new TileEntityType<T>(factory, null)
                .setRegistryName(InfinityWhat.MOD_ID, name);
    }

    @SubscribeEvent
    public static void registerTileEntityTypes(RegistryEvent.Register<TileEntityType<?>> event) {
        IForgeRegistry<TileEntityType<?>> registry = event.getRegistry();
        registry.register(INF_FLUID_GENERATOR);
        registry.register(INF_ITEM_GENERATOR);
    }
}

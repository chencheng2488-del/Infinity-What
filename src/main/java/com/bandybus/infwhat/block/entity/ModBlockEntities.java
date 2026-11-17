package com.bandybus.infwhat.block.entity;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.ModBlocks;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = InfinityWhat.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModBlockEntities {

    public static final TileEntityType<InfFluidGeneratorBlockEntity> INF_FLUID_GENERATOR =
            TileEntityType.Builder.create(InfFluidGeneratorBlockEntity::new,
                    ModBlocks.INF_FLUID_GENERATOR).build(null);

    public static final TileEntityType<InfItemGeneratorBlockEntity> INF_ITEM_GENERATOR =
            TileEntityType.Builder.create(InfItemGeneratorBlockEntity::new,
                    ModBlocks.INF_ITEM_GENERATOR).build(null);

    static {
        INF_FLUID_GENERATOR.setRegistryName(InfinityWhat.MOD_ID, "inf_fluid_generator");
        INF_ITEM_GENERATOR.setRegistryName(InfinityWhat.MOD_ID, "inf_item_generator");
    }

    @SubscribeEvent
    public static void registerTileEntityTypes(RegistryEvent.Register<TileEntityType<?>> event) {
        event.getRegistry().registerAll(INF_FLUID_GENERATOR, INF_ITEM_GENERATOR);
    }
}

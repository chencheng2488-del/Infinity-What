package com.bandybus.infwhat.block;

import com.bandybus.infwhat.InfinityWhat;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.IForgeRegistry;

/**
 * Block instances for 1.13.2. There is no DeferredRegister in this generation, so the objects
 * are created during class initialisation and inserted into the Forge registry from the
 * {@link RegistryEvent.Register} callback on the mod event bus.
 */
@Mod.EventBusSubscriber(modid = InfinityWhat.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModBlocks {

    public static final Block INF_FLUID_GENERATOR = new InfFluidGeneratorBlock(
            Block.Properties.create(Material.IRON, MaterialColor.IRON)
                    .hardnessAndResistance(3.5F, 10.0F)
                    .sound(SoundType.METAL))
            .setRegistryName(InfinityWhat.MOD_ID, "inf_fluid_generator");

    public static final Block INF_ITEM_GENERATOR = new InfItemGeneratorBlock(
            Block.Properties.create(Material.IRON, MaterialColor.IRON)
                    .hardnessAndResistance(3.5F, 10.0F)
                    .sound(SoundType.METAL))
            .setRegistryName(InfinityWhat.MOD_ID, "inf_item_generator");

    private ModBlocks() {
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        IForgeRegistry<Block> registry = event.getRegistry();
        registry.register(INF_FLUID_GENERATOR);
        registry.register(INF_ITEM_GENERATOR);
    }
}

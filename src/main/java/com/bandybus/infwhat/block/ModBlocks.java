package com.bandybus.infwhat.block;

import com.bandybus.infwhat.InfinityWhat;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Blocks are plain static fields on 1.14.3: Forge 27 predates {@code DeferredRegister}, so they
 * are registered from the {@link RegistryEvent.Register} callback instead. Block items live in
 * {@code ModItems} and are registered from the item registry event.
 *
 * <p>Forge 27's fluid API is still the stubbed legacy one ({@code FluidStack#getFluid()} has a
 * null registry delegate and the bucket wrapper returns null), so the fluid generator on this
 * version is a self-contained tank: buckets are handled directly and no fluid capability is
 * exposed. Pipe-based fluid interop returns from 1.14.4 (Forge 28) onwards.</p>
 */
@Mod.EventBusSubscriber(modid = InfinityWhat.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModBlocks {

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

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().registerAll(INF_FLUID_GENERATOR, INF_ITEM_GENERATOR);
    }
}

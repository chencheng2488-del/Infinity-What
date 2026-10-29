package com.bandybus.infwhat.datagen;

import com.bandybus.infwhat.InfinityWhat;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.forge.event.lifecycle.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = InfinityWhat.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModDataGenerators {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();

        // 1.18.2's DataGenerator#addProvider takes no include flag; the GatherDataEvent flags
        // decide which side's providers are added instead.
        if (event.includeClient()) {
            generator.addProvider(new ModBlockStateProvider(generator, existingFileHelper));
        }

        if (event.includeServer()) {
            generator.addProvider(new ModRecipeProvider(generator));
            generator.addProvider(new ModBlockTagsProvider(generator, existingFileHelper));
            generator.addProvider(new ModBlockLootProvider(generator));
        }
    }
}

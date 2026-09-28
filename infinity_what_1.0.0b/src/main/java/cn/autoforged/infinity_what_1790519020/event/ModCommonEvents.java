package cn.autoforged.infinity_what_1790519020.event;

import cn.autoforged.infinity_what_1790519020.InfinityWhat;
import cn.autoforged.infinity_what_1790519020.block.ModBlocks;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = InfinityWhat.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModCommonEvents {

    @SubscribeEvent
    public static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModBlocks.INF_FLUID_GENERATOR.get());
            event.accept(ModBlocks.INF_ITEM_GENERATOR.get());
        }
    }
}

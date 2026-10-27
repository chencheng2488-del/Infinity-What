package com.bandybus.infwhat.client;

import com.bandybus.infwhat.item.ModItems;
import com.bandybus.infwhat.block.ModBlocks;
import com.bandybus.infwhat.CommonProxy;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Client-only proxy: registers item models for the 1.12.2 model loader. */
@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void registerModels(ModelRegistryEvent event) {
        register(Item.getItemFromBlock(ModBlocks.INF_FLUID_GENERATOR), "inf_fluid_generator");
        register(Item.getItemFromBlock(ModBlocks.INF_ITEM_GENERATOR), "inf_item_generator");
        register(ModItems.INFINITE_SOURCE_CONFIG_REPLICATOR, "infinite_source_config_replicator");
    }

    private static void register(Item item, String name) {
        ModelLoader.setCustomModelResourceLocation(item, 0,
                new ModelResourceLocation("infwhat:" + name + "#inventory"));
    }
}

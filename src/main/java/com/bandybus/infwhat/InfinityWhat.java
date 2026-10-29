package com.bandybus.infwhat;

import com.bandybus.infwhat.block.ModBlocks;
import com.bandybus.infwhat.block.entity.ModTileEntities;
import com.bandybus.infwhat.config.InfinityWhatConfig;
import com.bandybus.infwhat.container.ModGuiHandler;
import com.bandybus.infwhat.item.ModItems;
import com.bandybus.infwhat.network.ModNetworking;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Infinity What - adds infinite fluid and item generators plus a configuration replicator.
 *
 * <p>The 1.8.9 release registers blocks and items directly through {@code GameRegistry}; the fluid
 * generator implements the legacy {@code IFluidHandler} interface and the item generator exposes
 * the Forge item handler capability.
 */
@Mod(modid = InfinityWhat.MODID, name = InfinityWhat.NAME, version = InfinityWhat.VERSION,
        acceptedMinecraftVersions = "[1.8.9]")
public class InfinityWhat {

    public static final String MODID = "infwhat";
    public static final String NAME = "Infinity What";
    public static final String VERSION = "2.1.0b";

    public static final Logger LOGGER = LogManager.getLogger(MODID);

    @Mod.Instance(MODID)
    public static InfinityWhat instance;

    @SidedProxy(clientSide = "com.bandybus.infwhat.client.ClientProxy",
            serverSide = "com.bandybus.infwhat.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        InfinityWhatConfig.load(event.getSuggestedConfigurationFile());
        ModBlocks.register();
        ModItems.register();
        ModTileEntities.register();
        ModNetworking.register();
        proxy.preInit(event);
        LOGGER.info("Infinity What {} initialised for Minecraft 1.8.9", VERSION);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        ModRecipes.register();
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new ModGuiHandler());
        proxy.init(event);
    }
}

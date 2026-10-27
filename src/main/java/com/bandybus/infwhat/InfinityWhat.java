package com.bandybus.infwhat;

import com.bandybus.infwhat.block.ModBlocks;
import com.bandybus.infwhat.block.entity.ModTileEntities;
import com.bandybus.infwhat.config.InfinityWhatConfig;
import com.bandybus.infwhat.container.ModGuiHandler;
import com.bandybus.infwhat.item.ModItems;
import com.bandybus.infwhat.network.ModNetworking;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Infinity What - adds infinite fluid and item generators plus a configuration replicator.
 *
 * <p>The 1.7.10 release registers blocks and items through the {@code cpw.mods.fml} game registry.
 * The fluid generator implements the legacy {@code IFluidHandler} interface and the item generator
 * exposes a single-slot {@code IInventory}, which is the transport API available to pipes and
 * hoppers on this Minecraft version (there is no capability system yet).
 */
@Mod(modid = InfinityWhat.MODID, name = InfinityWhat.NAME, version = InfinityWhat.VERSION,
        acceptedMinecraftVersions = "[1.7.10]")
public class InfinityWhat {

    /** Registry id shared by every port of this mod. */
    public static final String MODID = "infwhat";

    /** Human readable mod name shown by the mod list. */
    public static final String NAME = "Infinity What";

    /** Mod version, identical across all maintained Minecraft versions. */
    public static final String VERSION = "2.0.0b";

    /** Shared logger; 1.7.10 already ships log4j 2. */
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    /** Mod instance used as the GUI handler owner. */
    @Mod.Instance(MODID)
    public static InfinityWhat instance;

    /** Side specific proxy; 1.7.10 needs it for the client-only GUI classes. */
    @SidedProxy(clientSide = "com.bandybus.infwhat.client.ClientProxy",
            serverSide = "com.bandybus.infwhat.CommonProxy")
    public static CommonProxy proxy;

    /** Loads configuration and registers blocks, items, tile entities and the network channel. */
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        InfinityWhatConfig.load(event.getSuggestedConfigurationFile());
        ModBlocks.register();
        ModItems.register();
        ModTileEntities.register();
        ModNetworking.register();
        proxy.preInit(event);
        LOGGER.info("Infinity What {} initialised for Minecraft 1.7.10", VERSION);
    }

    /** Registers crafting recipes and the GUI handler. */
    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        ModRecipes.register();
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new ModGuiHandler());
        proxy.init(event);
    }
}

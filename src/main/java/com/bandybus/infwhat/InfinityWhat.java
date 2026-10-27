package com.bandybus.infwhat;

import com.bandybus.infwhat.block.entity.ModTileEntities;
import com.bandybus.infwhat.config.InfinityWhatConfig;
import com.bandybus.infwhat.container.ModGuiHandler;
import com.bandybus.infwhat.network.ModNetworking;
import net.minecraftforge.common.MinecraftForge;
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
 * <p>Both machines expose the standard Forge capabilities ({@code IFluidHandler} and
 * {@code IItemHandler}), so pipes, hoppers and other mods can interact with them without any
 * hard dependency.
 */
@Mod(modid = InfinityWhat.MODID, name = InfinityWhat.NAME, version = InfinityWhat.VERSION,
        acceptedMinecraftVersions = "[1.12.2]")
public class InfinityWhat {

    public static final String MODID = "infwhat";
    public static final String NAME = "Infinity What";
    public static final String VERSION = "2.0.0b";

    public static final Logger LOGGER = LogManager.getLogger(MODID);

    @Mod.Instance(MODID)
    public static InfinityWhat instance;

    @SidedProxy(clientSide = "com.bandybus.infwhat.client.ClientProxy",
            serverSide = "com.bandybus.infwhat.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        InfinityWhatConfig.load(event.getSuggestedConfigurationFile());
        ModTileEntities.register();
        ModNetworking.register();
        MinecraftForge.EVENT_BUS.register(this);
        proxy.preInit(event);
        LOGGER.info("Infinity What {} initialised for Minecraft 1.12.2", VERSION);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new ModGuiHandler());
        proxy.init(event);
    }
}

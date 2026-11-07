package com.bandybus.infwhat;

import com.bandybus.infwhat.block.ModBlocks;
import com.bandybus.infwhat.block.entity.ModTileEntities;
import com.bandybus.infwhat.config.InfinityWhatConfig;
import com.bandybus.infwhat.container.GuiHandlerRouter;
import com.bandybus.infwhat.integration.thaumcraft4.ThaumcraftAltarControlManager;
import com.bandybus.infwhat.integration.thaumcraft4.ThaumcraftIntegration;
import com.bandybus.infwhat.integration.thaumcraft4.ThaumcraftRecipeIntegration;
import com.bandybus.infwhat.integration.thaumcraft4.ThaumicEnergisticsIntegration;
import com.bandybus.infwhat.item.ModItems;
import com.bandybus.infwhat.network.ModNetworking;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import net.minecraftforge.common.MinecraftForge;
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

    /** Mod version. */
    public static final String VERSION = "2.1.1";

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
        if (ThaumcraftIntegration.isActive()) {
            // World ticks are posted on the FML bus on 1.7.10; world unload events use the Forge
            // bus. 1.7.10 only registers handlers from an instance, never from a Class object.
            FMLCommonHandler.instance().bus().register(ThaumcraftAltarControlManager.INSTANCE);
            MinecraftForge.EVENT_BUS.register(ThaumcraftAltarControlManager.INSTANCE);
        }
        proxy.preInit(event);
        LOGGER.info("Infinity What {} initialised for Minecraft 1.7.10", VERSION);
    }

    /** Registers crafting recipes and the GUI handler. */
    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        ModRecipes.register();
        NetworkRegistry.INSTANCE.registerGuiHandler(this, GuiHandlerRouter.create());
        proxy.init(event);
    }

    /**
     * Thaumcraft 4 creates several of its items in its own postInit and creates the research
     * categories afterwards, so both the infusion recipe and the Thaumonomicon entry are
     * registered once every mod has finished postInit.
     */
    @Mod.EventHandler
    public void loadComplete(FMLLoadCompleteEvent event) {
        if (ThaumcraftIntegration.isLoaded()) {
            ThaumicEnergisticsIntegration.registerEmanator();
        }
        if (ThaumcraftIntegration.isActive()) {
            ThaumcraftRecipeIntegration.registerRecipes();
            ThaumcraftRecipeIntegration.registerResearch();
        }
    }
}

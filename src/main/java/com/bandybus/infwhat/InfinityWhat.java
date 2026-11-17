package com.bandybus.infwhat;

import com.bandybus.infwhat.block.ModBlocks;
import com.bandybus.infwhat.block.entity.ModTileEntities;
import com.bandybus.infwhat.config.InfinityWhatConfig;
import com.bandybus.infwhat.container.GuiHandlerRouter;
import com.bandybus.infwhat.integration.thaumcraft4.ThaumcraftAltarControlManager;
import com.bandybus.infwhat.integration.thaumcraft4.ThaumcraftBootstrap;
import com.bandybus.infwhat.integration.thaumcraft4.ThaumcraftIntegration;
import com.bandybus.infwhat.integration.thaumcraft4.ThaumcraftRecipeIntegration;
import com.bandybus.infwhat.item.ModItems;
import com.bandybus.infwhat.network.ModNetworking;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.Side;
import net.minecraftforge.common.MinecraftForge;

import java.util.logging.Logger;

/**
 * Infinity What - adds infinite fluid and item generators plus a configuration replicator.
 *
 * <p>The 1.6.4 release registers blocks and items through the {@code cpw.mods.fml} game registry.
 * Blocks still use numeric ids, so every id comes from the configuration file. The fluid generator
 * implements the Forge {@code IFluidHandler} interface and the item generator exposes a single-slot
 * {@code IInventory}, which is the transport API available to pipes and hoppers on this Minecraft
 * version (there is no capability system yet).
 */
@Mod(modid = InfinityWhat.MODID)
/*
 * 1.6.4's NetworkRegistry.registerGuiHandler ignores mods that are not annotated as a
 * NetworkMod, so without this annotation the machine screens never open. No version bounds are
 * declared because this annotation's bounds apply to the mod version, not to Minecraft.
 */
@NetworkMod(clientSideRequired = true, serverSideRequired = true)
public class InfinityWhat {

    /** Registry id shared by every port of this mod. */
    public static final String MODID = "infwhat";

    /** Human readable mod name shown by the mod list. */
    public static final String NAME = "Infinity What";

    /** Mod version. */
    public static final String VERSION = "2.2.0";

    /** Shared logger; 1.6.4 ships {@code java.util.logging}, not log4j 2. */
    public static final Logger LOGGER = Logger.getLogger(MODID);

    /** Mod instance used as the GUI handler owner. */
    @Mod.Instance(MODID)
    public static InfinityWhat instance;

    /** Side specific proxy; 1.6.4 needs it for the client-only GUI classes. */
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
            // 1.6.4 posts world ticks on the tick registry and world unload events on the Forge
            // bus. The manager implements ITickHandler and a ForgeSubscribe handler.
            TickRegistry.registerTickHandler(ThaumcraftAltarControlManager.INSTANCE, Side.SERVER);
            MinecraftForge.EVENT_BUS.register(ThaumcraftAltarControlManager.INSTANCE);
            registerThaumcraftBootstrap();
        }
        proxy.preInit(event);
        LOGGER.info("Infinity What " + VERSION + " initialised for Minecraft 1.6.4");
    }

    /** Registers crafting recipes and the GUI handler. */
    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        ModRecipes.register();
        NetworkRegistry.instance().registerGuiHandler(this, GuiHandlerRouter.create());
        proxy.init(event);
    }

    /**
     * Registers the deferred Thaumcraft bootstrap. 1.6.4 has no {@code FMLLoadCompleteEvent} hook
     * for {@code @Mod.EventHandler}, and this mod's postInit runs before Thaumcraft creates its
     * research categories, so the recipes/research are registered on the first tick instead.
     */
    private void registerThaumcraftBootstrap() {
        TickRegistry.registerTickHandler(ThaumcraftBootstrap.INSTANCE, Side.CLIENT);
        TickRegistry.registerTickHandler(ThaumcraftBootstrap.INSTANCE, Side.SERVER);
    }
}

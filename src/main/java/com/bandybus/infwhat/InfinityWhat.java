package com.bandybus.infwhat;

import com.bandybus.infwhat.block.ModBlocks;
import com.bandybus.infwhat.block.entity.ModBlockEntities;
import com.bandybus.infwhat.config.InfinityWhatConfig;
import com.bandybus.infwhat.creative.ModCreativeTabs;
import com.bandybus.infwhat.item.ModItems;
import com.bandybus.infwhat.menu.ModMenuTypes;
import com.bandybus.infwhat.network.ModNetworking;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Infinity What - adds an Infinity Fluid Generator block that stores an arbitrary fluid and
 * turns it into an unlimited source.
 *
 * Fabric 1.17.0 NOTE: the API line for 1.17.0 (0.36.x) has no transfer API, so this branch only
 * supports GUI bucket interaction and filling adjacent water cauldrons; the item generator
 * pushes into adjacent vanilla containers.
 *
 * NOTE ON NAMES: the requested registry id is {@code Infwhat}. Minecraft resource locations
 * and mod ids must be lowercase English, so it is normalized to {@code infwhat}; the block is
 * registered as {@code infwhat:inf_fluid_generator}. (The Java package stays
 * {@code com.bandybus.infwhat} per the project constraint and is unrelated
 * to the registry namespace.)
 */
public class InfinityWhat implements ModInitializer {
    public static final String MOD_ID = "infwhat";
    public static final Logger LOGGER = LoggerFactory.getLogger("InfinityWhat");

    /** Optional library/pipe mods we are known to be compatible with. */
    private static final String[] OPTIONAL_FLUID_MODS = {
            "ae2", "mekanism", "pipez", "gtm", "gregtech", "create"
    };

    @Override
    public void onInitialize() {
        InfinityWhatConfig.load();

        ModBlocks.register();
        ModItems.register();
        ModBlockEntities.register();
        ModMenuTypes.register();
        ModCreativeTabs.register();
        ModNetworking.registerServer();

        StringBuilder detected = new StringBuilder();
        for (String id : OPTIONAL_FLUID_MODS) {
            if (FabricLoader.getInstance().isModLoaded(id)) {
                if (detected.length() > 0) {
                    detected.append(", ");
                }
                detected.append(id);
            }
        }
        if (detected.length() == 0) {
            LOGGER.info("Infinity What: 1.17.0 Fabric branch loaded (no transfer API; "
                    + "fluid output fills cauldrons, item output uses vanilla containers).");
        } else {
            LOGGER.info("Infinity What: detected [{}]; this branch has no transfer API, so "
                    + "they are not used for automatic interop.", detected);
        }
    }
}

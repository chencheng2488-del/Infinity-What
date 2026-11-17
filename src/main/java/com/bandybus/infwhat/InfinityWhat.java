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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Infinity What - adds an Infinity Fluid Generator block that stores an arbitrary
 * fluid and turns it into an unlimited source, plus an Infinity Item Generator.
 *
 * 1.16.5 NOTE: Fabric API 0.42 has no transfer API and vanilla 1.16.5 has no FluidStack,
 * so this branch cannot expose pipe-compatible fluid/item storages. The fluid machine is
 * filled/drained through its GUI with vanilla buckets; the item machine pushes into
 * adjacent vanilla containers. Both machines keep the full rate/redstone/batch GUI.
 *
 * NOTE ON NAMES: the requested registry id is {@code Infwhat}. Minecraft resource locations
 * and mod ids must be lowercase English, so it is normalized to {@code infwhat}; the block is
 * registered as {@code infwhat:inf_fluid_generator}.
 */
public class InfinityWhat implements ModInitializer {
    public static final String MOD_ID = "infwhat";
    public static final Logger LOGGER = LogManager.getLogger("InfinityWhat");

    /** Optional pipe/storage mods that would normally interop through a transfer API. */
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
            LOGGER.info("Infinity What: 1.16.5 Fabric branch loaded. Fluid/item pipe interop "
                    + "is unavailable without a transfer API; use the GUI and vanilla containers.");
        } else {
            LOGGER.info("Infinity What: detected [{}]. 1.16.5 has no generic transfer API, so "
                    + "these mods are not used for automatic interop.", detected);
        }
    }
}

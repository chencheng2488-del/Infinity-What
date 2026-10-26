package com.bandybus.infwhat;

import com.bandybus.infwhat.block.ModBlocks;
import com.bandybus.infwhat.block.entity.ModBlockEntities;
import com.bandybus.infwhat.config.InfinityWhatConfig;
import com.bandybus.infwhat.creative.ModCreativeTabs;
import com.bandybus.infwhat.item.ModItems;
import com.bandybus.infwhat.menu.ModMenuTypes;
import com.bandybus.infwhat.network.ModNetworking;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Infinity What - adds an Infinity Fluid Generator block that stores an arbitrary
 * fluid and turns it into an unlimited source, exposing a Forge {@code IFluidHandler}
 * capability so any pipe/cable mod (AE2, GregTech Modern, Pipez, Mekanism, Create, ...)
 * can import/export fluids.
 *
 * NOTE ON NAMES: the requested registry id is {@code Infwhat}. Minecraft resource locations
 * and mod ids must be lowercase English, so it is normalized to {@code infwhat}; the block is
 * registered as {@code infwhat:inf_fluid_generator}. (The Java package stays
 * {@code com.bandybus.infwhat} per the project constraint and is unrelated
 * to the registry namespace.)
 *
 * NOTE ON OPTIONAL DEPS: no hard dependency is declared against AE2/Mekanism/etc. All
 * interop goes through the generic Forge fluid capability, so those mods are detected at
 * runtime and the generator degrades gracefully (works with plain capability pipes) when
 * they are absent.
 */
@Mod(InfinityWhat.MOD_ID)
public class InfinityWhat {
    public static final String MOD_ID = "infwhat";
    public static final Logger LOGGER = LoggerFactory.getLogger("InfinityWhat");

    /** Optional library/pipe mods we are known to be compatible with. */
    private static final String[] OPTIONAL_FLUID_MODS = {
            "ae2", "mekanism", "pipez", "gtm", "gregtech", "create"
    };

    public InfinityWhat(IEventBus modEventBus) {

        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModNetworking.register();

        InfinityWhatConfig.register();

        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            StringBuilder detected = new StringBuilder();
            for (String id : OPTIONAL_FLUID_MODS) {
                if (ModList.get().isLoaded(id)) {
                    if (detected.length() > 0) {
                        detected.append(", ");
                    }
                    detected.append(id);
                }
            }
            if (detected.length() == 0) {
                LOGGER.info("Infinity What: no optional fluid/pipe mods detected. "
                        + "The Infinity Fluid Generator still works through the generic Forge fluid capability.");
            } else {
                LOGGER.info("Infinity What: detected optional fluid/pipe mods [{}]. "
                        + "Capability-based fluid interop enabled.", detected);
            }
        });
    }
}

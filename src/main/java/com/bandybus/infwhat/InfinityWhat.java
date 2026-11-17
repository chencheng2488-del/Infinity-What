package com.bandybus.infwhat;

import com.bandybus.infwhat.config.InfinityWhatConfig;
import com.bandybus.infwhat.creative.ModCreativeTabs;
import com.bandybus.infwhat.network.ModNetworking;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Infinity What - adds an Infinity Fluid Generator block that stores an arbitrary
 * fluid and turns it into an unlimited source, exposing a Forge {@code IFluidHandler}
 * capability so any pipe/cable mod (AE2, GregTech Modern, Pipez, Mekanism, Create, ...)
 * can import/export fluids.
 *
 * NOTE ON NAMES: the requested registry id is {@code Infwhat}. Minecraft resource locations
 * and mod ids must be lowercase English, so it is normalized to {@code infwhat}; the block is
 * registered as {@code infwhat:inf_fluid_generator}.
 *
 * NOTE ON OPTIONAL DEPS: no hard dependency is declared against AE2/Mekanism/etc. All
 * interop goes through the generic Forge capability, so those mods are detected at
 * runtime and the generator degrades gracefully (works with plain capability pipes) when
 * they are absent.
 */
@Mod(InfinityWhat.MOD_ID)
@Mod.EventBusSubscriber(modid = InfinityWhat.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class InfinityWhat {
    public static final String MOD_ID = "infwhat";
    public static final Logger LOGGER = LogManager.getLogger("InfinityWhat");

    /** Optional library/pipe mods we are known to be compatible with. */
    private static final String[] OPTIONAL_FLUID_MODS = {
            "ae2", "mekanism", "pipez", "gtm", "gregtech", "create"
    };

    public InfinityWhat() {
        ModCreativeTabs.init();
        ModNetworking.register();
        InfinityWhatConfig.register();
    }

    @SubscribeEvent
    public static void commonSetup(FMLCommonSetupEvent event) {
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
    }
}

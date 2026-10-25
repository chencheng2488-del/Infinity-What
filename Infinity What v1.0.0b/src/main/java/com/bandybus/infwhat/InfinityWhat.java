package com.bandybus.infwhat;

import com.bandybus.infwhat.block.ModBlocks;
import com.bandybus.infwhat.block.entity.ModBlockEntities;
import com.bandybus.infwhat.config.InfinityWhatConfig;
import com.bandybus.infwhat.item.ModItems;
import com.bandybus.infwhat.menu.ModMenuTypes;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Infinity What - adds an Infinity Fluid Generator and an Infinity Item Generator that turn
 * one stored fluid or item into an unlimited source, exposed through the standard Forge
 * {@code IFluidHandler}/{@code IItemHandler} capabilities so any pipe or storage mod
 * (AE2, Mekanism, Pipez, Create, ...) can interact with them.
 *
 * No hard dependency is declared against those mods: all interop goes through the generic
 * Forge capabilities, which are always available.
 */
@Mod(InfinityWhat.MOD_ID)
public class InfinityWhat {
    public static final String MOD_ID = "infwhat";
    public static final Logger LOGGER = LoggerFactory.getLogger("InfinityWhat");

    /** Optional library/pipe mods we are known to be compatible with. */
    private static final String[] OPTIONAL_FLUID_MODS = {
            "ae2", "mekanism", "pipez", "gtm", "gregtech", "create"
    };

    public InfinityWhat() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModMenuTypes.register(modEventBus);

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

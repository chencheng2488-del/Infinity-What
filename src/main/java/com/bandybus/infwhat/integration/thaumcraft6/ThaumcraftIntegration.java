package com.bandybus.infwhat.integration.thaumcraft6;

import com.bandybus.infwhat.config.InfinityWhatConfig;
import net.minecraftforge.fml.common.Loader;

/**
 * Small, class-loading-safe gate for the optional Thaumcraft integration.
 *
 * <p>This class deliberately references no Thaumcraft types. The actual altar access is done
 * through {@link ThaumcraftAltarControlManager}, which uses reflection and only runs when
 * Thaumcraft is present.
 */
public final class ThaumcraftIntegration {

    public static final String MODID = "thaumcraft";

    private ThaumcraftIntegration() {
    }

    public static boolean isLoaded() {
        return Loader.isModLoaded(MODID);
    }

    public static boolean isActive() {
        return isLoaded() && InfinityWhatConfig.ordoMatrixEnabled();
    }
}

package com.bandybus.infwhat.integration.thaumcraft4;

import com.bandybus.infwhat.config.InfinityWhatConfig;
import cpw.mods.fml.common.Loader;

/**
 * Small, class-loading-safe gate for the optional Thaumcraft 4 integration.
 *
 * <p>This class deliberately references no Thaumcraft types. The actual altar access is done
 * through {@link ThaumcraftAltarControlManager}, which uses reflection and only runs when
 * Thaumcraft 4 is present.
 */
public final class ThaumcraftIntegration {

    /** Thaumcraft 4 declares its mod id with a capital T; Loader.isModLoaded is case sensitive. */
    public static final String MODID = "Thaumcraft";

    private ThaumcraftIntegration() {
    }

    public static boolean isLoaded() {
        return Loader.isModLoaded(MODID);
    }

    public static boolean isActive() {
        return isLoaded() && InfinityWhatConfig.ordoMatrixEnabled();
    }
}

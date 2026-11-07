package com.bandybus.infwhat.integration.thaumcraft4;

import com.bandybus.infwhat.InfinityWhat;
import cpw.mods.fml.common.Loader;

import java.lang.reflect.Method;

/**
 * Optional Thaumic Energistics bridge.
 *
 * <p>ThE 1.x only lets its essentia storage bus extract from explicitly whitelisted tile classes.
 * The public API is used through reflection so the mod still loads without ThE installed.
 */
public final class ThaumicEnergisticsIntegration {

    public static final String MODID = "thaumicenergistics";

    /** Capacity value used for the extract-only permission; extraction ignores it. */
    private static final int EXTRACT_CAPACITY = 64;

    private ThaumicEnergisticsIntegration() {
    }

    public static boolean isLoaded() {
        return Loader.isModLoaded(MODID);
    }

    /**
     * Registers the emanator as an extractable aspect container. Extraction only: the machine is
     * a one-way source and must never be filled through the storage bus.
     */
    public static void registerEmanator() {
        if (!isLoaded()) {
            return;
        }
        try {
            Class<?> apiClass = Class.forName("thaumicenergistics.api.ThEApi");
            Object api = apiClass.getMethod("instance").invoke(null);
            if (api == null) {
                InfinityWhat.LOGGER.warn(
                        "Thaumic Energistics API is not available; skipping essentia bridge");
                return;
            }
            Object permissions = apiClass.getMethod("transportPermissions").invoke(api);
            Class<?> permissionsInterface = Class.forName(
                    "thaumicenergistics.api.IThETransportPermissions");
            Method register = permissionsInterface.getMethod(
                    "addAspectContainerTileToExtractPermissions", Class.class, int.class);
            register.invoke(permissions, ArcaneEssentiaEmanatorTileEntity.class,
                    EXTRACT_CAPACITY);
            InfinityWhat.LOGGER.info(
                    "Registered the arcane essentia emanator for Thaumic Energistics extraction");
        } catch (Throwable throwable) {
            // A wrong ThE version must never break mod loading.
            InfinityWhat.LOGGER.warn("Could not register the arcane essentia emanator for "
                    + "Thaumic Energistics: {}", throwable.toString());
        }
    }
}

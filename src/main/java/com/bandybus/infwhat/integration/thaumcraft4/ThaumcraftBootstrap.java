package com.bandybus.infwhat.integration.thaumcraft4;

import com.bandybus.infwhat.InfinityWhat;
import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;

import java.util.EnumSet;

/**
 * Registers the Thaumcraft recipes and research once Thaumcraft's own postInit has created its
 * research categories.
 *
 * <p>1.6.4 has no working {@code FMLLoadCompleteEvent} hook for {@code @Mod.EventHandler}, and mod
 * postInit order is not guaranteed (this mod's postInit runs before Thaumcraft's on a normal load).
 * {@code ResearchCategories.addResearch} silently drops an entry when its category is not
 * registered yet, so the registration is deferred to the first game tick, after every mod finished
 * postInit. The task is one-shot per JVM; client and server each run their own copy in multiplayer.
 */
public final class ThaumcraftBootstrap implements ITickHandler {

    public static final ThaumcraftBootstrap INSTANCE = new ThaumcraftBootstrap();

    /** Number of ticks to wait for Thaumcraft's categories before giving up. */
    private static final int MAX_WAIT_TICKS = 200;

    private static boolean registered;
    private int waitedTicks;

    private ThaumcraftBootstrap() {
    }

    @Override
    public void tickStart(EnumSet<TickType> type, Object... data) {
        if (registered) {
            return;
        }
        if (!ThaumcraftIntegration.isActive()) {
            registered = true;
            return;
        }
        if (!ThaumcraftRecipeIntegration.categoriesReady()) {
            if (++this.waitedTicks > MAX_WAIT_TICKS) {
                registered = true;
                InfinityWhat.LOGGER.warning(
                        "Thaumcraft integration disabled: research categories never appeared");
            }
            return;
        }
        ThaumcraftRecipeIntegration.registerRecipes();
        ThaumcraftRecipeIntegration.registerResearch();
        registered = true;
        InfinityWhat.LOGGER.info(
                "Thaumcraft integration: recipes and research registered after categories");
    }

    @Override
    public void tickEnd(EnumSet<TickType> type, Object... data) {
    }

    @Override
    public EnumSet<TickType> ticks() {
        return EnumSet.of(TickType.CLIENT, TickType.SERVER);
    }

    @Override
    public String getLabel() {
        return "infwhat.thaumcraft_bootstrap";
    }
}

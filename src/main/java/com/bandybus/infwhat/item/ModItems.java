package com.bandybus.infwhat.item;

import cpw.mods.fml.common.registry.GameRegistry;

/** Standalone item registration for the 1.7.10 Forge release. */
public final class ModItems {

    /** Configuration replicator item instance. */
    public static final InfiniteSourceConfigureReplicatorItem INFINITE_SOURCE_CONFIG_REPLICATOR =
            new InfiniteSourceConfigureReplicatorItem();

    private ModItems() {
    }

    /** Registers the replicator item; the game registry prefixes the mod id. */
    public static void register() {
        GameRegistry.registerItem(INFINITE_SOURCE_CONFIG_REPLICATOR,
                "infinite_source_config_replicator");
    }
}

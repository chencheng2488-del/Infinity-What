package com.bandybus.infwhat.item;

import net.minecraftforge.fml.common.registry.GameRegistry;

/** Standalone item registration for the 1.11.2 Forge release. */
public final class ModItems {

    public static final InfiniteSourceConfigureReplicatorItem INFINITE_SOURCE_CONFIG_REPLICATOR =
            new InfiniteSourceConfigureReplicatorItem();

    private ModItems() {
    }

    public static void register() {
        GameRegistry.register(INFINITE_SOURCE_CONFIG_REPLICATOR);
    }
}

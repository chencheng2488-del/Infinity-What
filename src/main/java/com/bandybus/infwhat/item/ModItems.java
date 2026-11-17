package com.bandybus.infwhat.item;

import com.bandybus.infwhat.InfinityWhat;
import net.minecraftforge.fml.common.registry.GameRegistry;

/** Standalone item registration for the 1.8.9 Forge release. */
public final class ModItems {

    public static final InfiniteSourceConfigureReplicatorItem INFINITE_SOURCE_CONFIG_REPLICATOR =
            new InfiniteSourceConfigureReplicatorItem();

    private ModItems() {
    }

    public static void register() {
        GameRegistry.registerItem(INFINITE_SOURCE_CONFIG_REPLICATOR,
                "infwhat:infinite_source_config_replicator");
    }
}

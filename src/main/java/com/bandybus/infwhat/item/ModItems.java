package com.bandybus.infwhat.item;

import net.minecraftforge.fml.common.registry.GameRegistry;

/** Standalone item registration for the 1.8.8 Forge release. */
public final class ModItems {

    public static final InfiniteSourceConfigureReplicatorItem INFINITE_SOURCE_CONFIG_REPLICATOR =
            new InfiniteSourceConfigureReplicatorItem();

    private ModItems() {
    }

    public static void register() {
        // 1.8 的 GameRegistry 会自动加 modid 前缀，不能重复传 "infwhat:"。
        GameRegistry.registerItem(INFINITE_SOURCE_CONFIG_REPLICATOR,
                "infinite_source_config_replicator");
    }
}

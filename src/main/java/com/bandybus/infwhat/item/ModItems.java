package com.bandybus.infwhat.item;

import com.bandybus.infwhat.config.InfinityWhatConfig;
import com.bandybus.infwhat.integration.thaumcraft4.ThaumcraftIntegration;
import cpw.mods.fml.common.registry.GameRegistry;

/** Standalone item registration for the 1.6.4 Forge release. */
public final class ModItems {

    /** Configuration replicator item instance. */
    public static final InfiniteSourceConfigureReplicatorItem INFINITE_SOURCE_CONFIG_REPLICATOR =
            new InfiniteSourceConfigureReplicatorItem(InfinityWhatConfig.configReplicatorItemId());

    /**
     * Arcane machine configuration shard instance.
     *
     * <p>Created in {@link #register()} only while Thaumcraft is loaded. 1.6.4 registers items in
     * the {@code Item} constructor, so constructing this field unconditionally would publish the
     * shard into the creative tab even when Thaumcraft is absent.
     */
    public static ArcaneOrdRecordShardItem ARCANE_ORD_RECORD_SHARD;

    private ModItems() {
    }

    /** Registers the item; the game registry prefixes the mod id. */
    public static void register() {
        GameRegistry.registerItem(INFINITE_SOURCE_CONFIG_REPLICATOR,
                "infinite_source_config_replicator");
        if (ThaumcraftIntegration.isActive()) {
            ARCANE_ORD_RECORD_SHARD =
                    new ArcaneOrdRecordShardItem(InfinityWhatConfig.arcaneShardItemId());
            GameRegistry.registerItem(ARCANE_ORD_RECORD_SHARD, "arcane_ord_record_shard");
        }
    }
}

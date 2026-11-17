package com.bandybus.infwhat.item;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.integration.thaumcraft6.ThaumcraftIntegration;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Standalone item registration for the old Forge release. */
@Mod.EventBusSubscriber(modid = InfinityWhat.MODID)
public final class ModItems {

    public static final InfiniteSourceConfigureReplicatorItem INFINITE_SOURCE_CONFIG_REPLICATOR =
            new InfiniteSourceConfigureReplicatorItem();

    public static final ArcaneOrdRecordShardItem ARCANE_ORD_RECORD_SHARD =
            new ArcaneOrdRecordShardItem();

    private ModItems() {
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<net.minecraft.item.Item> event) {
        event.getRegistry().register(INFINITE_SOURCE_CONFIG_REPLICATOR);
        if (ThaumcraftIntegration.isActive()) {
            event.getRegistry().register(ARCANE_ORD_RECORD_SHARD);
        }
    }
}

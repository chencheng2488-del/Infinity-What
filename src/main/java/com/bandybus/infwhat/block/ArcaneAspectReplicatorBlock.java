package com.bandybus.infwhat.block;

import com.bandybus.infwhat.container.ModGuiIds;

/** Arcane Aspect Replicator block. */
public class ArcaneAspectReplicatorBlock extends AbstractArcaneMachineBlock {

    private static final String TILE_CLASS =
            "com.bandybus.infwhat.integration.thaumcraft4.ArcaneAspectReplicatorTileEntity";

    public ArcaneAspectReplicatorBlock(String name) {
        super(name, TILE_CLASS, ModGuiIds.ARCANE_ASPECT_REPLICATOR);
    }
}

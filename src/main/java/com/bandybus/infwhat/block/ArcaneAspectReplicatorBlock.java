package com.bandybus.infwhat.block;

import com.bandybus.infwhat.container.ModGuiIds;

/** Arcane Aspect Replicator block for Thaumcraft 6. */
public class ArcaneAspectReplicatorBlock extends AbstractArcaneMachineBlock {

    private static final String TILE_CLASS =
            "com.bandybus.infwhat.integration.thaumcraft6.ArcaneAspectReplicatorTileEntity";

    public ArcaneAspectReplicatorBlock(String name) {
        super(name, TILE_CLASS, ModGuiIds.ARCANE_ASPECT_REPLICATOR);
    }
}

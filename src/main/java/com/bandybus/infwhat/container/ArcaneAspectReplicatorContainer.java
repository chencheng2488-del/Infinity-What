package com.bandybus.infwhat.container;

import com.bandybus.infwhat.integration.thaumcraft4.ArcaneAspectReplicatorTileEntity;
import net.minecraft.entity.player.InventoryPlayer;

/** Container for the Arcane Aspect Replicator. */
public class ArcaneAspectReplicatorContainer extends AbstractArcaneMachineContainer {

    public ArcaneAspectReplicatorContainer(InventoryPlayer playerInventory,
                                           ArcaneAspectReplicatorTileEntity tile) {
        super(playerInventory, tile);
    }

    @Override
    public ArcaneAspectReplicatorTileEntity getTileEntity() {
        return (ArcaneAspectReplicatorTileEntity) super.getTileEntity();
    }
}

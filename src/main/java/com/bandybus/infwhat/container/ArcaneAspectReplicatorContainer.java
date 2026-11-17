package com.bandybus.infwhat.container;

import com.bandybus.infwhat.integration.thaumcraft6.ArcaneAspectReplicatorTileEntity;
import net.minecraft.entity.player.InventoryPlayer;

/** Container for the Arcane Aspect Replicator (1.12.2). */
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

package com.bandybus.infwhat.container;

import com.bandybus.infwhat.integration.thaumcraft6.ArcaneEssentiaEmanatorTileEntity;
import net.minecraft.entity.player.InventoryPlayer;

/** Container for the Arcane Essentia Emanator (1.12.2). */
public class ArcaneEssentiaEmanatorContainer extends AbstractArcaneMachineContainer {

    public ArcaneEssentiaEmanatorContainer(InventoryPlayer playerInventory,
                                           ArcaneEssentiaEmanatorTileEntity tile) {
        super(playerInventory, tile);
    }

    @Override
    public ArcaneEssentiaEmanatorTileEntity getTileEntity() {
        return (ArcaneEssentiaEmanatorTileEntity) super.getTileEntity();
    }
}

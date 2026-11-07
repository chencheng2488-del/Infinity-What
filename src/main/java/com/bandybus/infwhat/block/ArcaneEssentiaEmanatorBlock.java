package com.bandybus.infwhat.block;

import com.bandybus.infwhat.container.ModGuiIds;

/** Arcane Essentia Emanator block. */
public class ArcaneEssentiaEmanatorBlock extends AbstractArcaneMachineBlock {

    private static final String TILE_CLASS =
            "com.bandybus.infwhat.integration.thaumcraft4.ArcaneEssentiaEmanatorTileEntity";

    public ArcaneEssentiaEmanatorBlock(String name) {
        super(name, TILE_CLASS, ModGuiIds.ARCANE_ESSENTIA_EMANATOR);
    }
}

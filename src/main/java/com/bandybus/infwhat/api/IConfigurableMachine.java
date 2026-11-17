package com.bandybus.infwhat.api;

import net.minecraft.nbt.NBTTagCompound;

/**
 * Settings surface shared by every machine whose configuration can be copied onto a card.
 *
 * <p>The snapshot only contains settings the player can change. Inventories, bindings and other
 * positional state must stay out of it, so a copied profile never duplicates contents or points a
 * machine at another machine's partner.
 *
 * <p>The card writes its own {@code Type} key from {@link #getConfigurationType()} into the
 * snapshot, so a profile can only be pasted onto a machine of the same kind.
 */
public interface IConfigurableMachine {

    /** Stable machine kind used to match a saved profile against a paste target. */
    String getConfigurationType();

    /** Snapshot of the configurable settings, safe to store on an item stack. */
    NBTTagCompound writeConfiguration();

    /** Applies a snapshot produced by {@link #writeConfiguration()}. */
    void applyConfiguration(NBTTagCompound configuration);
}

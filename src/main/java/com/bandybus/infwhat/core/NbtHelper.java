package com.bandybus.infwhat.core;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;

/** NBTTagCompound helpers for the 1.6.4 API, which has no typed {@code hasKey}. */
public final class NbtHelper {

    private NbtHelper() {
    }

    /** True when the key exists and holds a tag of the given NBT type id. */
    public static boolean hasKey(NBTTagCompound compound, String key, int type) {
        NBTBase tag = compound.getTag(key);
        return tag != null && tag.getId() == type;
    }
}

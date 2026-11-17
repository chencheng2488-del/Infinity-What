package com.bandybus.infwhat.block;

import java.util.Locale;

/** How redstone affects the generator's active output. */
public enum RedstoneMode {
    ALWAYS,
    HIGH,
    LOW,
    PULSE;

    private static final RedstoneMode[] VALUES = values();

    public static RedstoneMode byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : ALWAYS;
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}

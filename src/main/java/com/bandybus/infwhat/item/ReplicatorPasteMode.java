package com.bandybus.infwhat.item;

import java.util.Locale;

/** Which part of a saved generator configuration a replicator pastes. */
public enum ReplicatorPasteMode {
    ALL,
    OUTPUT,
    RATE,
    REDSTONE,
    BATCH;

    private static final ReplicatorPasteMode[] VALUES = values();

    public static ReplicatorPasteMode byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : ALL;
    }

    public ReplicatorPasteMode next() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}

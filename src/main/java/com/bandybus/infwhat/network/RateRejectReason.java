package com.bandybus.infwhat.network;

/** Why a server-side output-rate write was rejected. */
public enum RateRejectReason {
    NONE,
    INVALID,
    LIMIT_SIDE,
    LIMIT_TOTAL,
    REJECTED;

    private static final RateRejectReason[] VALUES = values();

    public static RateRejectReason byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : REJECTED;
    }
}

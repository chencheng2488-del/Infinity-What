package com.bandybus.infwhat.core;

import net.minecraftforge.common.util.ForgeDirection;

/**
 * Output switch, face mask and rate shared by every infinite source machine.
 *
 * <p>Each machine keeps its own rate unit and clamp range; this component only stores the raw
 * value and enforces the bounds the machine handed to it.
 */
public final class SourceOutputSettings {

    /** Number of faces a machine can push into. */
    public static final int SIDE_BIT_COUNT = ForgeDirection.VALID_DIRECTIONS.length;
    /** Mask with every face enabled. */
    public static final int ALL_SIDES = (1 << SIDE_BIT_COUNT) - 1;

    private final long minRate;
    private final long maxRate;

    private boolean enabled;
    private int sideMask = ALL_SIDES;
    private long rate;

    public SourceOutputSettings(long defaultRate, long minRate, long maxRate) {
        this.minRate = minRate;
        this.maxRate = maxRate;
        this.rate = clamp(defaultRate);
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getSideMask() {
        return this.sideMask;
    }

    public void setSideMask(int mask) {
        this.sideMask = mask & ALL_SIDES;
    }

    public boolean isSideEnabled(ForgeDirection direction) {
        return (this.sideMask & (1 << direction.ordinal())) != 0;
    }

    public void toggleSide(ForgeDirection direction) {
        this.sideMask ^= 1 << direction.ordinal();
    }

    public long getRate() {
        return this.rate;
    }

    public void setRate(long rate) {
        this.rate = clamp(rate);
    }

    public void apply(boolean enabled, int sideMask, long rate) {
        this.enabled = enabled;
        setSideMask(sideMask);
        setRate(rate);
    }

    private long clamp(long value) {
        return Math.max(this.minRate, Math.min(this.maxRate, value));
    }
}

package com.bandybus.infwhat.client.gui;

/** Time unit used by the rate inputs. Rates are always stored per tick. */
public enum RateUnit {
    PER_TICK(1, "/t"),
    PER_SECOND(20, "/s"),
    PER_MINUTE(1200, "/min");

    private final long ticksPerUnit;
    private final String suffix;

    RateUnit(long ticksPerUnit, String suffix) {
        this.ticksPerUnit = ticksPerUnit;
        this.suffix = suffix;
    }

    public long ticksPerUnit() {
        return this.ticksPerUnit;
    }

    public String suffix() {
        return this.suffix;
    }

    public RateUnit next() {
        RateUnit[] units = values();
        return units[(this.ordinal() + 1) % units.length];
    }

    /** True when a value expressed in this unit maps to a whole per-tick rate. */
    public boolean isExact(long valuePerUnit) {
        return valuePerUnit % this.ticksPerUnit == 0L;
    }

    public long toPerTick(long valuePerUnit) {
        return valuePerUnit / this.ticksPerUnit;
    }

    public long fromPerTick(long perTick) {
        return perTick * this.ticksPerUnit;
    }
}

package com.bandybus.infwhat.config;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Configuration for the Infinity generators, stored in {@code config/infwhat.cfg}.
 *
 * <p>Categories and keys mirror the modern release so the same values can be carried over:
 * <ul>
 *   <li>{@code balance.enableInfiniteSource} - master switch for the infinite behaviour.</li>
 *   <li>{@code balance.finiteTankCapacity} - tank size used while the infinite switch is off.</li>
 *   <li>{@code limits.*} - server-side per-side and total output caps.</li>
 *   <li>{@code item_filter} / {@code fluid_filter} - per-type id filters. Each has a
 *       {@code mode} (BLACKLIST by default) and an initially empty {@code list}.</li>
 * </ul>
 */
public final class InfinityWhatConfig {

    /** How a filter list is interpreted. */
    public enum FilterMode {
        /** Everything is allowed except the listed ids. */
        BLACKLIST,
        /** Only the listed ids are allowed. */
        WHITELIST
    }

    private static final long DEFAULT_ITEM_TOTAL_RATE = (long) Integer.MAX_VALUE * 6L;
    private static final long DEFAULT_FLUID_RATE_PER_SIDE_MB = (long) Integer.MAX_VALUE * 1_000L;
    private static final long DEFAULT_FLUID_TOTAL_RATE_MB =
            (long) Integer.MAX_VALUE * 1_000L * 6L;

    private static Configuration configuration;
    private static boolean infiniteEnabled = true;
    private static int finiteCapacity = 16000;
    private static long maxItemRatePerSide = Integer.MAX_VALUE;
    private static long maxItemTotalRate = DEFAULT_ITEM_TOTAL_RATE;
    private static long maxFluidRatePerSideMb = DEFAULT_FLUID_RATE_PER_SIDE_MB;
    private static long maxFluidTotalRateMb = DEFAULT_FLUID_TOTAL_RATE_MB;
    private static FilterMode itemFilterMode = FilterMode.BLACKLIST;
    private static FilterMode fluidFilterMode = FilterMode.BLACKLIST;
    private static List<String> itemFilterList = new ArrayList<String>();
    private static List<String> fluidFilterList = new ArrayList<String>();
    private static boolean ordoMatrixEnabled = true;
    private static int ordoMatrixRange = 8;
    private static int ordoMatrixMaxSpeed = 10;
    private static int ordoMatrixMaxStability = 100;

    private InfinityWhatConfig() {
    }

    /** Loads (and creates on first launch) the configuration file. */
    public static void load(File file) {
        configuration = new Configuration(file);
        configuration.load();

        configuration.setCategoryComment("balance",
                "Balance options for the Infinity generators.");
        infiniteEnabled = configuration.getBoolean("enableInfiniteSource", "balance", true,
                "Master switch for the infinite behaviour of both generators.\n"
                        + "Fluid generator: if false it behaves like a normal tank limited to finiteTankCapacity.\n"
                        + "Item generator: if false it never becomes an unlimited item source.");
        finiteCapacity = configuration.getInt("finiteTankCapacity", "balance", 16000, 1,
                Integer.MAX_VALUE, "Capacity in mB used when enableInfiniteSource is false.");

        configuration.setCategoryComment("limits",
                "Server-side output limits. Rates are per side unless stated otherwise.\n"
                        + "The totals are the sum of all enabled sides; writes above a limit are rejected.");
        maxItemRatePerSide = configuration.getInt("maxItemRatePerSide", "limits",
                Integer.MAX_VALUE, 1, Integer.MAX_VALUE,
                "Maximum item rate per side, in items per tick.");
        maxItemTotalRate = readLong("maxItemTotalRate", DEFAULT_ITEM_TOTAL_RATE,
                "Maximum total item rate, in items per tick.");
        maxFluidRatePerSideMb = readLong("maxFluidRatePerSideMb", DEFAULT_FLUID_RATE_PER_SIDE_MB,
                "Maximum fluid rate per side, in mB per tick.");
        maxFluidTotalRateMb = readLong("maxFluidTotalRateMb", DEFAULT_FLUID_TOTAL_RATE_MB,
                "Maximum total fluid rate, in mB per tick.");

        configuration.setCategoryComment("item_filter", "Item filter for the item generator.");
        itemFilterMode = readFilterMode("item_filter", "mode");
        itemFilterList = sanitize(configuration.getStringList("list", "item_filter",
                new String[0], "Item ids (e.g. \"minecraft:diamond\") the mode above applies to."));

        configuration.setCategoryComment("fluid_filter", "Fluid filter for the fluid generator.");
        fluidFilterMode = readFilterMode("fluid_filter", "mode");
        fluidFilterList = sanitize(configuration.getStringList("list", "fluid_filter",
                new String[0], "Fluid ids (e.g. \"minecraft:water\") the mode above applies to."));

        configuration.setCategoryComment("thaumcraft",
                "Optional Thaumcraft infusion-altar integration.");
        ordoMatrixEnabled = configuration.getBoolean("enableOrdoUsurpatoryMatrix", "thaumcraft",
                true, "Master switch for the Ordo Usurpatory Matrix. The block is only registered "
                        + "when Thaumcraft is loaded and this is true.");
        ordoMatrixRange = configuration.getInt("ordoMatrixRange", "thaumcraft", 8, 2, 32,
                "Maximum block distance between the matrix and its bound altar.");
        ordoMatrixMaxSpeed = configuration.getInt("ordoMatrixMaxSpeed", "thaumcraft", 10, 1, 100,
                "Maximum infusion speed multiplier.");
        ordoMatrixMaxStability = configuration.getInt("ordoMatrixMaxStability", "thaumcraft",
                100, 1, 1000, "Maximum custom stability value accepted by the GUI.");

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }

    /** Reads a long limit; the 1.12.2 Configuration API only exposes string properties. */
    private static long readLong(String key, long defaultValue, String comment) {
        Property property = configuration.get("limits", key, Long.toString(defaultValue), comment);
        return Math.max(1L, property.getLong(defaultValue));
    }

    private static FilterMode readFilterMode(String category, String key) {
        String value = configuration.getString(key, category, FilterMode.BLACKLIST.name(),
                "BLACKLIST: listed ids are rejected.\nWHITELIST: only listed ids are accepted.",
                new String[]{FilterMode.BLACKLIST.name(), FilterMode.WHITELIST.name()});
        try {
            return FilterMode.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return FilterMode.BLACKLIST;
        }
    }

    private static List<String> sanitize(String[] values) {
        List<String> result = new ArrayList<String>();
        for (String value : values) {
            if (value != null && value.trim().contains(":")) {
                result.add(value.trim());
            }
        }
        return result;
    }

    public static boolean infiniteEnabled() {
        return infiniteEnabled;
    }

    public static int finiteCapacity() {
        return finiteCapacity;
    }

    public static long maxItemRatePerSide() {
        return maxItemRatePerSide;
    }

    public static long maxItemTotalRate() {
        return maxItemTotalRate;
    }

    public static long maxFluidRatePerSideMb() {
        return maxFluidRatePerSideMb;
    }

    public static long maxFluidTotalRateMb() {
        return maxFluidTotalRateMb;
    }

    /** Whether the given item id may be inserted into the item generator. */
    public static boolean isItemAllowed(@Nullable String itemId) {
        return isAllowed(itemFilterMode, itemFilterList, itemId);
    }

    /** Whether the given fluid id may be inserted into the fluid generator. */
    public static boolean isFluidAllowed(@Nullable String fluidId) {
        return isAllowed(fluidFilterMode, fluidFilterList, fluidId);
    }

    private static boolean isAllowed(FilterMode mode, List<String> list, @Nullable String id) {
        if (id == null) {
            return true;
        }
        boolean listed = list.contains(id);
        return mode == FilterMode.BLACKLIST ? !listed : listed;
    }

    /** Exposes the immutable snapshot for debug logging. */
    public static List<String> getItemFilterList() {
        return Arrays.asList(itemFilterList.toArray(new String[itemFilterList.size()]));
    }

    public static boolean ordoMatrixEnabled() {
        return ordoMatrixEnabled;
    }

    public static int ordoMatrixRange() {
        return ordoMatrixRange;
    }

    public static int ordoMatrixMaxSpeed() {
        return ordoMatrixMaxSpeed;
    }

    public static int ordoMatrixMaxStability() {
        return ordoMatrixMaxStability;
    }
}

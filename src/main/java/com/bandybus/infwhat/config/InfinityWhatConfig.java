package com.bandybus.infwhat.config;

import net.minecraftforge.common.config.Configuration;

import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for the Infinity generators, stored in {@code config/infwhat.cfg}.
 * The categories and keys mirror the modern release.
 */
public final class InfinityWhatConfig {

    /** How a filter list is interpreted. */
    public enum FilterMode {
        /** Everything is allowed except the listed ids. */
        BLACKLIST,
        /** Only the listed ids are allowed. */
        WHITELIST
    }

    private static Configuration configuration;
    private static boolean infiniteEnabled = true;
    private static int finiteCapacity = 16000;
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

        configuration.setCategoryComment("item_filter", "Item filter for the item generator.");
        itemFilterMode = readFilterMode("item_filter", "mode");
        itemFilterList = sanitize(configuration.getStringList("list", "item_filter",
                new String[0], "Item ids (e.g. \"minecraft:diamond\") the mode above applies to."));

        configuration.setCategoryComment("fluid_filter", "Fluid filter for the fluid generator.");
        fluidFilterMode = readFilterMode("fluid_filter", "mode");
        fluidFilterList = sanitize(configuration.getStringList("list", "fluid_filter",
                new String[0], "Fluid ids (e.g. \"minecraft:water\") the mode above applies to."));

        configuration.setCategoryComment("thaumcraft",
                "Optional Thaumcraft 4 infusion-altar integration.");
        ordoMatrixEnabled = configuration.getBoolean("enableOrdoUsurpatoryMatrix", "thaumcraft",
                true, "Master switch for the Ordo Usurpatory Matrix. The block is only registered "
                        + "when Thaumcraft is loaded and this is true.");
        ordoMatrixRange = configuration.getInt("ordoMatrixRange", "thaumcraft", 8, 2, 32,
                "Maximum block distance between the matrix and its bound altar.");
        ordoMatrixMaxSpeed = configuration.getInt("ordoMatrixMaxSpeed", "thaumcraft", 10, 1, 100,
                "Maximum infusion speed multiplier.");
        ordoMatrixMaxStability = configuration.getInt("ordoMatrixMaxStability", "thaumcraft",
                25, 1, 25, "Maximum custom instability accepted by the GUI. TC4 instability "
                        + "is limited to 0-25.");

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }

    /** Reads one filter mode entry and falls back to a blacklist on invalid input. */
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

    /** Keeps only non-empty entries that look like a namespaced id. */
    private static List<String> sanitize(String[] values) {
        List<String> result = new ArrayList<String>();
        for (String value : values) {
            if (value != null && value.trim().contains(":")) {
                result.add(value.trim());
            }
        }
        return result;
    }

    /** Master switch for the unlimited behaviour of both generators. */
    public static boolean infiniteEnabled() {
        return infiniteEnabled;
    }

    /** Tank capacity in mB used while the infinite behaviour is disabled. */
    public static int finiteCapacity() {
        return finiteCapacity;
    }

    /** Whether the given item id may be inserted into the item generator. */
    public static boolean isItemAllowed(@Nullable String itemId) {
        return isAllowed(itemFilterMode, itemFilterList, itemId);
    }

    /** Whether the given fluid id may be inserted into the fluid generator. */
    public static boolean isFluidAllowed(@Nullable String fluidId) {
        return isAllowed(fluidFilterMode, fluidFilterList, fluidId);
    }

    /** Applies one filter mode to one id. */
    private static boolean isAllowed(FilterMode mode, List<String> list, @Nullable String id) {
        if (id == null) {
            return true;
        }
        boolean listed = list.contains(id);
        return mode == FilterMode.BLACKLIST ? !listed : listed;
    }

    /** Whether the Ordo Usurpatory Matrix block should be registered at all. */
    public static boolean ordoMatrixEnabled() {
        return ordoMatrixEnabled;
    }

    /** Maximum block distance between the matrix and its bound altar. */
    public static int ordoMatrixRange() {
        return ordoMatrixRange;
    }

    /** Maximum infusion speed multiplier accepted from the GUI or a packet. */
    public static int ordoMatrixMaxSpeed() {
        return ordoMatrixMaxSpeed;
    }

    /** Maximum custom stability value accepted from the GUI or a packet. */
    public static int ordoMatrixMaxStability() {
        return ordoMatrixMaxStability;
    }
}

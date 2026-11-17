package com.bandybus.infwhat.config;

import net.minecraftforge.common.Configuration;
import net.minecraftforge.common.Property;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for the Infinity generators, stored in {@code config/infwhat.cfg}.
 *
 * <p>1.6.4 still uses numeric block and item ids, so the ids are part of this file as well. The
 * remaining categories and keys mirror the newer releases.
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
    private static long maxItemRatePerSide = Integer.MAX_VALUE;
    private static long maxItemTotalRate = (long) Integer.MAX_VALUE * 6L;
    private static long maxFluidRatePerSideMb = (long) Integer.MAX_VALUE * 1000L;
    private static long maxFluidTotalRateMb = (long) Integer.MAX_VALUE * 1000L * 6L;
    private static FilterMode itemFilterMode = FilterMode.BLACKLIST;
    private static FilterMode fluidFilterMode = FilterMode.BLACKLIST;
    private static List<String> itemFilterList = new ArrayList<String>();
    private static List<String> fluidFilterList = new ArrayList<String>();
    private static boolean ordoMatrixEnabled = true;
    private static int ordoMatrixRange = 8;
    private static int ordoMatrixMaxSpeed = 10;
    private static int ordoMatrixMaxStability = 100;

    private static int fluidGeneratorBlockId = 3000;
    private static int itemGeneratorBlockId = 3001;
    private static int ordoMatrixBlockId = 3002;
    private static int arcaneReplicatorBlockId = 3003;
    private static int arcaneEmanatorBlockId = 3004;
    private static int configReplicatorItemId = 30000;
    private static int arcaneShardItemId = 30001;

    private InfinityWhatConfig() {
    }

    /** Loads (and creates on first launch) the configuration file. */
    public static void load(File file) {
        configuration = new Configuration(file);
        configuration.load();

        configuration.addCustomCategoryComment("ids",
                "Numeric block and item ids. 1.6.4 has no registry names for blocks and items.");
        fluidGeneratorBlockId = configuration.getBlock("infFluidGenerator", 3000).getInt();
        itemGeneratorBlockId = configuration.getBlock("infItemGenerator", 3001).getInt();
        ordoMatrixBlockId = configuration.getBlock("ordoUsurpatoryMatrix", 3002).getInt();
        arcaneReplicatorBlockId = configuration.getBlock("arcaneAspectReplicator", 3003).getInt();
        arcaneEmanatorBlockId = configuration.getBlock("arcaneEssentiaEmanator", 3004).getInt();
        configReplicatorItemId = configuration.getItem("infiniteSourceConfigReplicator",
                30000).getInt();
        arcaneShardItemId = configuration.getItem("arcaneOrdRecordShard", 30001).getInt();

        configuration.addCustomCategoryComment("balance",
                "Balance options for the Infinity generators.");
        infiniteEnabled = configuration.get("balance", "enableInfiniteSource", true,
                "Master switch for the infinite behaviour of both generators.\n"
                        + "Fluid generator: if false it behaves like a normal tank limited to finiteTankCapacity.\n"
                        + "Item generator: if false it never becomes an unlimited item source.").getBoolean(true);
        finiteCapacity = configuration.get("balance", "finiteTankCapacity", 16000,
                "Capacity in mB used when enableInfiniteSource is false.").getInt();

        configuration.addCustomCategoryComment("limits",
                "Server-side output limits. Rates are per side unless stated otherwise.\n"
                        + "The totals are the sum of all enabled sides; writes above a limit are rejected.");
        maxItemRatePerSide = readLong("maxItemRatePerSide", "limits", Integer.MAX_VALUE,
                1L, Integer.MAX_VALUE, "Maximum item rate per side, in items per tick.");
        maxItemTotalRate = readLong("maxItemTotalRate", "limits",
                (long) Integer.MAX_VALUE * 6L, 1L, Long.MAX_VALUE,
                "Maximum total item rate, in items per tick.");
        maxFluidRatePerSideMb = readLong("maxFluidRatePerSideMb", "limits",
                (long) Integer.MAX_VALUE * 1000L, 1L, (long) Integer.MAX_VALUE * 1000L,
                "Maximum fluid rate per side, in mB per tick.");
        maxFluidTotalRateMb = readLong("maxFluidTotalRateMb", "limits",
                (long) Integer.MAX_VALUE * 1000L * 6L, 1L, Long.MAX_VALUE,
                "Maximum total fluid rate, in mB per tick.");

        configuration.addCustomCategoryComment("item_filter", "Item filter for the item generator.");
        itemFilterMode = readFilterMode("item_filter", "mode");
        itemFilterList = sanitize(configuration.get("item_filter", "list",
                new String[0], "Item ids (e.g. \"264\" or \"264:0\") the mode above applies to.")
                .getStringList());

        configuration.addCustomCategoryComment("fluid_filter", "Fluid filter for the fluid generator.");
        fluidFilterMode = readFilterMode("fluid_filter", "mode");
        fluidFilterList = sanitize(configuration.get("fluid_filter", "list",
                new String[0], "Fluid names (e.g. \"minecraft:water\") the mode above applies to.")
                .getStringList());

        configuration.addCustomCategoryComment("thaumcraft",
                "Optional Thaumcraft 4 infusion-altar integration.");
        ordoMatrixEnabled = configuration.get("thaumcraft", "enableOrdoUsurpatoryMatrix", true,
                "Master switch for the Ordo Usurpatory Matrix. The block is only registered "
                        + "when Thaumcraft is loaded and this is true.").getBoolean(true);
        ordoMatrixRange = configuration.get("thaumcraft", "ordoMatrixRange", 8,
                "Maximum block distance between the matrix and its bound altar.").getInt();
        ordoMatrixMaxSpeed = configuration.get("thaumcraft", "ordoMatrixMaxSpeed", 10,
                "Maximum infusion speed multiplier.").getInt();
        ordoMatrixMaxStability = configuration.get("thaumcraft", "ordoMatrixMaxStability", 25,
                "Maximum custom instability accepted by the GUI. TC4 instability is limited "
                        + "to 0-25.").getInt();

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }

    /** Reads one long limit; stored as text so values above the int range survive a round trip. */
    private static long readLong(String key, String category, long defaultValue, long min, long max,
                                 String comment) {
        Property property = configuration.get(category, key, Long.toString(defaultValue), comment);
        long parsed;
        try {
            parsed = Long.parseLong(property.getString().trim());
        } catch (NumberFormatException exception) {
            parsed = defaultValue;
        }
        return Math.max(min, Math.min(max, parsed));
    }

    /** Reads one filter mode entry and falls back to a blacklist on invalid input. */
    private static FilterMode readFilterMode(String category, String key) {
        Property property = configuration.get(category, key, FilterMode.BLACKLIST.name(),
                "BLACKLIST: listed ids are rejected.\nWHITELIST: only listed ids are accepted.");
        try {
            return FilterMode.valueOf(property.getString().trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return FilterMode.BLACKLIST;
        }
    }

    /** Keeps only non-empty entries. */
    private static List<String> sanitize(String[] values) {
        List<String> result = new ArrayList<String>();
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                result.add(value.trim());
            }
        }
        return result;
    }

    public static int fluidGeneratorBlockId() {
        return fluidGeneratorBlockId;
    }

    public static int itemGeneratorBlockId() {
        return itemGeneratorBlockId;
    }

    public static int ordoMatrixBlockId() {
        return ordoMatrixBlockId;
    }

    public static int arcaneReplicatorBlockId() {
        return arcaneReplicatorBlockId;
    }

    public static int arcaneEmanatorBlockId() {
        return arcaneEmanatorBlockId;
    }

    public static int configReplicatorItemId() {
        return configReplicatorItemId;
    }

    public static int arcaneShardItemId() {
        return arcaneShardItemId;
    }

    /** Master switch for the unlimited behaviour of both generators. */
    public static boolean infiniteEnabled() {
        return infiniteEnabled;
    }

    /** Tank capacity in mB used while the infinite behaviour is disabled. */
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
    public static boolean isItemAllowed(String itemId) {
        return isAllowed(itemFilterMode, itemFilterList, itemId);
    }

    /** Whether the given fluid id may be inserted into the fluid generator. */
    public static boolean isFluidAllowed(String fluidId) {
        return isAllowed(fluidFilterMode, fluidFilterList, fluidId);
    }

    /** Applies one filter mode to one id. */
    private static boolean isAllowed(FilterMode mode, List<String> list, String id) {
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

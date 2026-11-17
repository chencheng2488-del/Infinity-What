package com.bandybus.infwhat.config;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for the Infinity generators, stored in {@code config/infwhat.toml}.
 *
 * <ul>
 *   <li>{@code balance.enableInfiniteSource} - master switch for the infinite behaviour.</li>
 *   <li>{@code balance.finiteTankCapacity} - tank size in mB used while the infinite switch is off.</li>
 *   <li>{@code item_filter} / {@code fluid_filter} - per-type id filters. Each has a
 *       {@code mode} ({@link FilterMode#BLACKLIST} by default) and an initially empty
 *       {@code list}. BLACKLIST rejects listed ids; WHITELIST accepts only listed ids.</li>
 * </ul>
 *
 * <p>Fabric has no config API, so this class contains a minimal TOML reader/writer for
 * exactly the keys above. Unknown keys and comments are ignored.
 */
public final class InfinityWhatConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("InfinityWhat/Config");
    private static final String FILE_NAME = "infwhat.toml";

    /** How a filter list is interpreted. */
    public enum FilterMode {
        /** Everything is allowed except the listed ids. */
        BLACKLIST,
        /** Only the listed ids are allowed. */
        WHITELIST
    }

    private static final long DEFAULT_MAX_ITEM_RATE_PER_SIDE = Integer.MAX_VALUE;
    private static final long DEFAULT_MAX_ITEM_TOTAL_RATE = (long) Integer.MAX_VALUE * 6L;
    private static final long DEFAULT_MAX_FLUID_RATE_PER_SIDE_MB = (long) Integer.MAX_VALUE * 1_000L;
    private static final long DEFAULT_MAX_FLUID_TOTAL_RATE_MB = (long) Integer.MAX_VALUE * 1_000L * 6L;

    private static boolean enableInfiniteSource = true;
    private static int finiteTankCapacity = 16000;
    private static long maxItemRatePerSide = DEFAULT_MAX_ITEM_RATE_PER_SIDE;
    private static long maxItemTotalRate = DEFAULT_MAX_ITEM_TOTAL_RATE;
    private static long maxFluidRatePerSideMb = DEFAULT_MAX_FLUID_RATE_PER_SIDE_MB;
    private static long maxFluidTotalRateMb = DEFAULT_MAX_FLUID_TOTAL_RATE_MB;
    private static FilterMode itemFilterMode = FilterMode.BLACKLIST;
    private static List<String> itemFilterList = List.of();
    private static FilterMode fluidFilterMode = FilterMode.BLACKLIST;
    private static List<String> fluidFilterList = List.of();

    public static boolean infiniteEnabled() {
        return enableInfiniteSource;
    }

    public static int finiteCapacity() {
        return finiteTankCapacity;
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
    public static boolean isItemAllowed(Identifier itemId) {
        return isAllowed(itemFilterMode, itemFilterList, itemId);
    }

    /** Whether the given fluid id may be inserted into the fluid generator. */
    public static boolean isFluidAllowed(Identifier fluidId) {
        return isAllowed(fluidFilterMode, fluidFilterList, fluidId);
    }

    private static boolean isAllowed(FilterMode mode, List<String> list, Identifier id) {
        if (id == null) {
            return true;
        }
        boolean listed = list.contains(id.toString());
        return mode == FilterMode.BLACKLIST ? !listed : listed;
    }

    // ------------------------------------------------------------------
    // Loading
    // ------------------------------------------------------------------

    /** Reads {@code config/infwhat.toml}, generating it with defaults when missing. */
    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path.getParent());
                Files.writeString(path, DEFAULT_FILE, StandardCharsets.UTF_8);
                return;
            }
            parse(Files.readAllLines(path, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            LOGGER.warn("Could not read {}; using built-in defaults.", path, exception);
        }
    }

    private static void parse(List<String> lines) {
        // Reset so a reload always starts from the defaults.
        enableInfiniteSource = true;
        finiteTankCapacity = 16000;
        maxItemRatePerSide = DEFAULT_MAX_ITEM_RATE_PER_SIDE;
        maxItemTotalRate = DEFAULT_MAX_ITEM_TOTAL_RATE;
        maxFluidRatePerSideMb = DEFAULT_MAX_FLUID_RATE_PER_SIDE_MB;
        maxFluidTotalRateMb = DEFAULT_MAX_FLUID_TOTAL_RATE_MB;
        itemFilterMode = FilterMode.BLACKLIST;
        itemFilterList = List.of();
        fluidFilterMode = FilterMode.BLACKLIST;
        fluidFilterList = List.of();

        String section = "";
        String pendingKey = null;
        String pendingSection = null;
        StringBuilder pendingValue = null;

        for (String rawLine : lines) {
            String line = stripComment(rawLine).trim();
            if (pendingValue != null) {
                pendingValue.append(' ').append(line);
                if (line.contains("]")) {
                    applyList(pendingSection, pendingKey, pendingValue.toString());
                    pendingValue = null;
                    pendingKey = null;
                    pendingSection = null;
                }
                continue;
            }
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith("[") && line.endsWith("]")) {
                section = line.substring(1, line.length() - 1).trim();
                continue;
            }
            int equals = line.indexOf('=');
            if (equals < 0) {
                continue;
            }
            String key = line.substring(0, equals).trim();
            String value = line.substring(equals + 1).trim();
            if (value.startsWith("[") && !value.contains("]")) {
                pendingKey = key;
                pendingSection = section;
                pendingValue = new StringBuilder(value);
                continue;
            }
            switch (section) {
                case "balance" -> applyBalance(key, value);
                case "limits" -> applyLimits(key, value);
                case "item_filter" -> applyFilter(key, value, true);
                case "fluid_filter" -> applyFilter(key, value, false);
                default -> {
                    // Ignore unknown sections instead of failing the whole file.
                }
            }
        }
    }

    private static void applyBalance(String key, String value) {
        switch (key) {
            case "enableInfiniteSource" -> enableInfiniteSource = parseBoolean(value, enableInfiniteSource);
            case "finiteTankCapacity" -> finiteTankCapacity = parsePositiveInt(value, finiteTankCapacity);
            default -> {
            }
        }
    }

    private static void applyLimits(String key, String value) {
        switch (key) {
            case "maxItemRatePerSide" -> maxItemRatePerSide = parseLong(value,
                    DEFAULT_MAX_ITEM_RATE_PER_SIDE, 1L, (long) Integer.MAX_VALUE);
            case "maxItemTotalRate" -> maxItemTotalRate = parseLong(value,
                    DEFAULT_MAX_ITEM_TOTAL_RATE, 1L, Long.MAX_VALUE);
            case "maxFluidRatePerSideMb" -> maxFluidRatePerSideMb = parseLong(value,
                    DEFAULT_MAX_FLUID_RATE_PER_SIDE_MB, 1L, (long) Integer.MAX_VALUE * 1_000L);
            case "maxFluidTotalRateMb" -> maxFluidTotalRateMb = parseLong(value,
                    DEFAULT_MAX_FLUID_TOTAL_RATE_MB, 1L, Long.MAX_VALUE);
            default -> {
            }
        }
    }

    private static void applyFilter(String key, String value, boolean itemFilter) {
        if (key.equals("mode")) {
            FilterMode mode = parseMode(value);
            if (itemFilter) {
                itemFilterMode = mode;
            } else {
                fluidFilterMode = mode;
            }
        } else if (key.equals("list")) {
            applyList(itemFilter ? "item_filter" : "fluid_filter", key, value);
        }
    }

    private static void applyList(String section, String key, String value) {
        if (!"list".equals(key)) {
            return;
        }
        List<String> parsed = parseArray(value);
        if ("item_filter".equals(section)) {
            itemFilterList = parsed;
        } else if ("fluid_filter".equals(section)) {
            fluidFilterList = parsed;
        }
    }

    private static boolean parseBoolean(String value, boolean fallback) {
        String normalized = value.trim();
        if (normalized.equalsIgnoreCase("true")) {
            return true;
        }
        if (normalized.equalsIgnoreCase("false")) {
            return false;
        }
        return fallback;
    }

    private static int parsePositiveInt(String value, int fallback) {
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed >= 1 ? parsed : fallback;
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static long parseLong(String value, long fallback, long min, long max) {
        try {
            long parsed = Long.parseLong(value.trim());
            return Math.max(min, Math.min(max, parsed));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static FilterMode parseMode(String value) {
        String normalized = unquote(value).trim().toUpperCase(java.util.Locale.ROOT);
        for (FilterMode mode : FilterMode.values()) {
            if (mode.name().equals(normalized)) {
                return mode;
            }
        }
        return FilterMode.BLACKLIST;
    }

    private static List<String> parseArray(String value) {
        int open = value.indexOf('[');
        int close = value.lastIndexOf(']');
        if (open < 0 || close < open) {
            return List.of();
        }
        String inner = value.substring(open + 1, close);
        List<String> result = new ArrayList<>();
        for (String entry : inner.split(",")) {
            String cleaned = unquote(entry.trim());
            if (!cleaned.isEmpty() && cleaned.contains(":")) {
                result.add(cleaned);
            }
        }
        return List.copyOf(result);
    }

    private static String unquote(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    /** Cuts a {@code #} comment that is outside of a quoted string literal. */
    private static String stripComment(String line) {
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char character = line.charAt(i);
            if (character == '"') {
                quoted = !quoted;
            } else if (character == '#' && !quoted) {
                return line.substring(0, i);
            }
        }
        return line;
    }

    private static final String DEFAULT_FILE = """
            # Infinity What configuration.

            [balance]
            # Master switch for the infinite behaviour of both generators.
            # Fluid generator: if false it behaves like a normal tank limited to finiteTankCapacity.
            # Item generator: if false it never becomes an unlimited item source.
            enableInfiniteSource = true

            # Capacity in mB used when enableInfiniteSource is false.
            finiteTankCapacity = 16000

            [limits]
            # Maximum item rate per side, in items per tick.
            maxItemRatePerSide = 2147483647

            # Maximum total item rate, in items per tick.
            maxItemTotalRate = 12884901882

            # Maximum fluid rate per side, in mB per tick.
            maxFluidRatePerSideMb = 2147483647000

            # Maximum total fluid rate, in mB per tick.
            maxFluidTotalRateMb = 12884901882000

            [item_filter]
            # BLACKLIST: listed item ids can never be inserted.
            # WHITELIST: only listed item ids can be inserted.
            mode = "BLACKLIST"

            # Item ids (e.g. "minecraft:diamond") the mode above applies to.
            list = [
            ]

            [fluid_filter]
            # BLACKLIST: listed fluid ids can never be inserted.
            # WHITELIST: only listed fluid ids can be inserted.
            mode = "BLACKLIST"

            # Fluid ids (e.g. "minecraft:water") the mode above applies to.
            list = [
            ]
            """;

    private InfinityWhatConfig() {
    }
}

package com.bandybus.infwhat.config;

import com.bandybus.infwhat.InfinityWhat;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Configuration for the Infinity generators, stored in {@code config/infwhat.toml}.
 *
 * <ul>
 *   <li>{@code balance.enableInfiniteSource} - master switch for the infinite behaviour.</li>
 *   <li>{@code balance.finiteTankCapacity} - tank size used while the infinite switch is off.</li>
 *   <li>{@code item_filter} / {@code fluid_filter} - per-type id filters. Each has a
 *       {@code mode} ({@link FilterMode#BLACKLIST} by default) and an initially empty
 *       {@code list}. BLACKLIST rejects listed ids; WHITELIST accepts only listed ids.</li>
 * </ul>
 *
 * Fabric has no config system, so this class contains a deliberately small TOML reader: it only
 * understands the sections, booleans, integers, strings and string arrays used above.
 */
public class InfinityWhatConfig {

    /** How a filter list is interpreted. */
    public enum FilterMode {
        /** Everything is allowed except the listed ids. */
        BLACKLIST,
        /** Only the listed ids are allowed. */
        WHITELIST
    }

    private static final String FILE_NAME = "infwhat.toml";
    private static final int DEFAULT_FINITE_CAPACITY = 16000;

    private static final String DEFAULT_FILE = """
            # Infinity What configuration.
            # Invalid values fall back to the defaults below.

            [balance]
            # Master switch for the infinite behaviour of both generators.
            # Fluid generator: if false it behaves like a normal tank limited to finiteTankCapacity.
            # Item generator: if false it never becomes an unlimited item source.
            enableInfiniteSource = true
            # Capacity in mB used when enableInfiniteSource is false.
            finiteTankCapacity = 16000

            [item_filter]
            # BLACKLIST: listed item ids can never be inserted.
            # WHITELIST: only listed item ids can be inserted.
            mode = "BLACKLIST"
            # Item ids (e.g. "minecraft:diamond") the mode above applies to.
            list = []

            [fluid_filter]
            # BLACKLIST: listed fluid ids can never be inserted.
            # WHITELIST: only listed fluid ids can be inserted.
            mode = "BLACKLIST"
            # Fluid ids (e.g. "minecraft:water") the mode above applies to.
            list = []
            """;

    private static final Pattern QUOTED = Pattern.compile("\"([^\"]*)\"");

    private static boolean enableInfiniteSource = true;
    private static int finiteTankCapacity = DEFAULT_FINITE_CAPACITY;
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

    /** Whether the given item id may be inserted into the item generator. */
    public static boolean isItemAllowed(ResourceLocation itemId) {
        return isAllowed(itemFilterMode, itemFilterList, itemId);
    }

    /** Whether the given fluid id may be inserted into the fluid generator. */
    public static boolean isFluidAllowed(ResourceLocation fluidId) {
        return isAllowed(fluidFilterMode, fluidFilterList, fluidId);
    }

    private static boolean isAllowed(FilterMode mode, List<String> list, ResourceLocation id) {
        if (id == null) {
            return true;
        }
        boolean listed = list.contains(id.toString());
        return mode == FilterMode.BLACKLIST ? !listed : listed;
    }

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path.getParent());
                Files.writeString(path, DEFAULT_FILE, StandardCharsets.UTF_8);
            }
            parse(Files.readAllLines(path, StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException exception) {
            InfinityWhat.LOGGER.warn("Infinity What: failed to read {}, using defaults", path, exception);
        }
    }

    private static void parse(List<String> lines) {
        String section = "";
        for (int index = 0; index < lines.size(); index++) {
            String line = stripComment(lines.get(index)).trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith("[") && line.endsWith("]")) {
                section = line.substring(1, line.length() - 1).trim().toLowerCase(Locale.ROOT);
                continue;
            }
            int equals = line.indexOf('=');
            if (equals < 0) {
                continue;
            }
            String key = normalize(line.substring(0, equals));
            String value = line.substring(equals + 1).trim();
            if (value.startsWith("[")) {
                // 多行数组：括号未闭合时继续拼接后续行。
                StringBuilder array = new StringBuilder(value);
                while (bracketDepth(array.toString()) > 0 && index + 1 < lines.size()) {
                    array.append(' ').append(stripComment(lines.get(++index)).trim());
                }
                value = array.toString();
            }
            apply(section, key, value);
        }
    }

    private static void apply(String section, String key, String value) {
        switch (section) {
            case "balance" -> {
                if (key.equals("enableinfinitesource")) {
                    enableInfiniteSource = parseBoolean(value, true);
                } else if (key.equals("finitetankcapacity")) {
                    finiteTankCapacity = parseCapacity(value);
                }
            }
            case "item_filter" -> {
                if (key.equals("mode")) {
                    itemFilterMode = parseMode(value, FilterMode.BLACKLIST);
                } else if (key.equals("list")) {
                    itemFilterList = parseList(value);
                }
            }
            case "fluid_filter" -> {
                if (key.equals("mode")) {
                    fluidFilterMode = parseMode(value, FilterMode.BLACKLIST);
                } else if (key.equals("list")) {
                    fluidFilterList = parseList(value);
                }
            }
            default -> {
            }
        }
    }

    private static String normalize(String key) {
        return key.trim().replace("_", "").toLowerCase(Locale.ROOT);
    }

    private static boolean parseBoolean(String value, boolean fallback) {
        if (value.equalsIgnoreCase("true")) {
            return true;
        }
        if (value.equalsIgnoreCase("false")) {
            return false;
        }
        return fallback;
    }

    private static int parseCapacity(String value) {
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed >= 1 ? parsed : DEFAULT_FINITE_CAPACITY;
        } catch (NumberFormatException exception) {
            return DEFAULT_FINITE_CAPACITY;
        }
    }

    private static FilterMode parseMode(String value, FilterMode fallback) {
        String normalized = value.trim().replace("\"", "").toUpperCase(Locale.ROOT);
        if (normalized.equals("BLACKLIST")) {
            return FilterMode.BLACKLIST;
        }
        if (normalized.equals("WHITELIST")) {
            return FilterMode.WHITELIST;
        }
        return fallback;
    }

    private static List<String> parseList(String value) {
        List<String> result = new ArrayList<>();
        Matcher matcher = QUOTED.matcher(value);
        while (matcher.find()) {
            String id = matcher.group(1).trim();
            if (id.contains(":")) {
                result.add(id);
            }
        }
        return List.copyOf(result);
    }

    /** 去掉引号之外的行内注释。 */
    private static String stripComment(String line) {
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (character == '"') {
                quoted = !quoted;
            } else if (character == '#' && !quoted) {
                return line.substring(0, index);
            }
        }
        return line;
    }

    private static int bracketDepth(String value) {
        int depth = 0;
        boolean quoted = false;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '"') {
                quoted = !quoted;
            } else if (!quoted && character == '[') {
                depth++;
            } else if (!quoted && character == ']') {
                depth--;
            }
        }
        return depth;
    }

    private InfinityWhatConfig() {
    }
}

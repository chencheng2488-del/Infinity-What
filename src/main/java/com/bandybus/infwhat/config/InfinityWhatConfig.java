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
 */
public final class InfinityWhatConfig {

    /** How a filter list is interpreted. */
    public enum FilterMode {
        /** Everything is allowed except the listed ids. */
        BLACKLIST,
        /** Only the listed ids are allowed. */
        WHITELIST
    }

    private static final String DEFAULT_FILE = """
            # Infinity What configuration.
            # Edit and restart the game (or reload the world) to apply changes.

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

    private static boolean infiniteEnabled = true;
    private static int finiteCapacity = 16_000;
    private static FilterMode itemFilterMode = FilterMode.BLACKLIST;
    private static List<String> itemFilterList = List.of();
    private static FilterMode fluidFilterMode = FilterMode.BLACKLIST;
    private static List<String> fluidFilterList = List.of();

    public static boolean infiniteEnabled() {
        return infiniteEnabled;
    }

    public static int finiteCapacity() {
        return finiteCapacity;
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

    public static void register() {
        load();
    }

    public static void reload() {
        load();
    }

    private static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("infwhat.toml");
        try {
            if (Files.notExists(path)) {
                Files.createDirectories(path.getParent());
                Files.writeString(path, DEFAULT_FILE, StandardCharsets.UTF_8);
            }
            parse(Files.readAllLines(path, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            InfinityWhat.LOGGER.warn("Infinity What: could not read {}", path, exception);
        }
    }

    private static void parse(List<String> lines) {
        boolean infiniteEnabled = true;
        int finiteCapacity = 16_000;
        FilterMode itemFilterMode = FilterMode.BLACKLIST;
        List<String> itemFilterList = List.of();
        FilterMode fluidFilterMode = FilterMode.BLACKLIST;
        List<String> fluidFilterList = List.of();

        String section = "";
        for (int i = 0; i < lines.size(); i++) {
            String line = stripComment(lines.get(i)).trim();
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
            while (hasOpenArray(value) && i + 1 < lines.size()) {
                i++;
                value = value + " " + stripComment(lines.get(i)).trim();
            }

            switch (section) {
                case "balance" -> {
                    if (key.equals("enableInfiniteSource")) {
                        infiniteEnabled = Boolean.parseBoolean(unquote(value));
                    } else if (key.equals("finiteTankCapacity")) {
                        try {
                            finiteCapacity = Math.max(1, Integer.parseInt(unquote(value)));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
                case "item_filter" -> {
                    if (key.equals("mode")) {
                        itemFilterMode = parseMode(value);
                    } else if (key.equals("list")) {
                        itemFilterList = parseList(value);
                    }
                }
                case "fluid_filter" -> {
                    if (key.equals("mode")) {
                        fluidFilterMode = parseMode(value);
                    } else if (key.equals("list")) {
                        fluidFilterList = parseList(value);
                    }
                }
                default -> {
                }
            }
        }

        InfinityWhatConfig.infiniteEnabled = infiniteEnabled;
        InfinityWhatConfig.finiteCapacity = finiteCapacity;
        InfinityWhatConfig.itemFilterMode = itemFilterMode;
        InfinityWhatConfig.itemFilterList = itemFilterList;
        InfinityWhatConfig.fluidFilterMode = fluidFilterMode;
        InfinityWhatConfig.fluidFilterList = fluidFilterList;
    }

    private static FilterMode parseMode(String value) {
        return unquote(value).equalsIgnoreCase("WHITELIST") ? FilterMode.WHITELIST : FilterMode.BLACKLIST;
    }

    private static List<String> parseList(String value) {
        String body = value.trim();
        if (body.startsWith("[")) {
            body = body.substring(1);
        }
        if (body.endsWith("]")) {
            body = body.substring(0, body.length() - 1);
        }
        List<String> result = new ArrayList<>();
        for (String part : body.split(",")) {
            String entry = unquote(part.trim());
            if (!entry.isEmpty()) {
                result.add(entry);
            }
        }
        return List.copyOf(result);
    }

    private static String unquote(String value) {
        String trimmed = value.trim();
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    private static boolean hasOpenArray(String value) {
        if (!value.startsWith("[")) {
            return false;
        }
        int depth = 0;
        boolean inString = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"') {
                inString = !inString;
            } else if (!inString) {
                if (c == '[') {
                    depth++;
                } else if (c == ']') {
                    depth--;
                }
            }
        }
        return depth > 0;
    }

    private static String stripComment(String line) {
        boolean inString = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inString = !inString;
            } else if (c == '#' && !inString) {
                return line.substring(0, i);
            }
        }
        return line;
    }

    private InfinityWhatConfig() {
    }
}

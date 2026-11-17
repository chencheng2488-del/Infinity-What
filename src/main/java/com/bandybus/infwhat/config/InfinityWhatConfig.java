package com.bandybus.infwhat.config;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * Configuration for the Infinity generators, stored in {@code config/infwhat.toml}.
 *
 * <ul>
 *   <li>{@code balance.enableInfiniteSource} - master switch for the infinite behaviour.</li>
 *   <li>{@code balance.finiteTankCapacity} - tank size used while the infinite switch is off.</li>
 *   <li>{@code limits} - per-side and total output caps for items and fluids.</li>
 *   <li>{@code item_filter} / {@code fluid_filter} - per-type id filters. Each has a
 *       {@code mode} ({@link FilterMode#BLACKLIST} by default) and an initially empty
 *       {@code list}. BLACKLIST rejects listed ids; WHITELIST accepts only listed ids.</li>
 * </ul>
 */
public class InfinityWhatConfig {

    /** How a filter list is interpreted. */
    public enum FilterMode {
        /** Everything is allowed except the listed ids. */
        BLACKLIST,
        /** Only the listed ids are allowed. */
        WHITELIST
    }

    public static final ForgeConfigSpec SPEC;
    public static final Common COMMON;

    static {
        Pair<Common, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(Common::new);
        COMMON = pair.getLeft();
        SPEC = pair.getRight();
    }

    public static class Common {
        public final ForgeConfigSpec.BooleanValue enableInfiniteSource;
        public final ForgeConfigSpec.IntValue finiteTankCapacity;
        public final ForgeConfigSpec.LongValue maxItemRatePerSide;
        public final ForgeConfigSpec.LongValue maxItemTotalRate;
        public final ForgeConfigSpec.LongValue maxFluidRatePerSideMb;
        public final ForgeConfigSpec.LongValue maxFluidTotalRateMb;
        public final ForgeConfigSpec.EnumValue<FilterMode> itemFilterMode;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> itemFilterList;
        public final ForgeConfigSpec.EnumValue<FilterMode> fluidFilterMode;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> fluidFilterList;

        public Common(ForgeConfigSpec.Builder builder) {
            builder.comment("Balance options for the Infinity generators.").push("balance");
            this.enableInfiniteSource = builder
                    .comment("Master switch for the infinite behaviour of both generators.",
                            "Fluid generator: if false it behaves like a normal tank limited to finiteTankCapacity.",
                            "Item generator: if false it never becomes an unlimited item source.")
                    .define("enableInfiniteSource", true);
            this.finiteTankCapacity = builder
                    .comment("Capacity in mB used when enableInfiniteSource is false.")
                    .defineInRange("finiteTankCapacity", 16000, 1, Integer.MAX_VALUE);
            builder.pop();

            builder.comment("Server-side output limits. Rates are per side unless stated otherwise.",
                            "The totals are the sum of all enabled sides; writes above a limit are rejected.")
                    .push("limits");
            this.maxItemRatePerSide = builder
                    .comment("Maximum item rate per side, in items per tick.")
                    .defineInRange("maxItemRatePerSide",
                            (long) Integer.MAX_VALUE, 1L, (long) Integer.MAX_VALUE);
            this.maxItemTotalRate = builder
                    .comment("Maximum total item rate, in items per tick.")
                    .defineInRange("maxItemTotalRate",
                            (long) Integer.MAX_VALUE * 6L, 1L, Long.MAX_VALUE);
            this.maxFluidRatePerSideMb = builder
                    .comment("Maximum fluid rate per side, in mB per tick.")
                    .defineInRange("maxFluidRatePerSideMb",
                            (long) Integer.MAX_VALUE * 1_000L,
                            1L, (long) Integer.MAX_VALUE * 1_000L);
            this.maxFluidTotalRateMb = builder
                    .comment("Maximum total fluid rate, in mB per tick.")
                    .defineInRange("maxFluidTotalRateMb",
                            (long) Integer.MAX_VALUE * 1_000L * 6L, 1L, Long.MAX_VALUE);
            builder.pop();

            builder.comment("Item filter for the item generator.").push("item_filter");
            this.itemFilterMode = builder
                    .comment("BLACKLIST: listed item ids can never be inserted.",
                            "WHITELIST: only listed item ids can be inserted.")
                    .defineEnum("mode", FilterMode.BLACKLIST);
            this.itemFilterList = builder
                    .comment("Item ids (e.g. \"minecraft:diamond\") the mode above applies to.")
                    .defineList("list", Collections.<String>emptyList(),
                            obj -> obj instanceof String && ((String) obj).contains(":"));
            builder.pop();

            builder.comment("Fluid filter for the fluid generator.").push("fluid_filter");
            this.fluidFilterMode = builder
                    .comment("BLACKLIST: listed fluid ids can never be inserted.",
                            "WHITELIST: only listed fluid ids can be inserted.")
                    .defineEnum("mode", FilterMode.BLACKLIST);
            this.fluidFilterList = builder
                    .comment("Fluid ids (e.g. \"minecraft:water\") the mode above applies to.",
                            "1.13.2 stores the fluid name without a namespace for vanilla fluids,",
                            "so a bare \"water\" is treated the same as \"minecraft:water\".")
                    .defineList("list", Collections.<String>emptyList(),
                            obj -> obj instanceof String && !((String) obj).isEmpty());
            builder.pop();
        }
    }

    public static boolean infiniteEnabled() {
        return COMMON.enableInfiniteSource.get();
    }

    public static int finiteCapacity() {
        return COMMON.finiteTankCapacity.get();
    }

    public static long maxItemRatePerSide() {
        return COMMON.maxItemRatePerSide.get();
    }

    public static long maxItemTotalRate() {
        return COMMON.maxItemTotalRate.get();
    }

    public static long maxFluidRatePerSideMb() {
        return COMMON.maxFluidRatePerSideMb.get();
    }

    public static long maxFluidTotalRateMb() {
        return COMMON.maxFluidTotalRateMb.get();
    }

    /** Whether the given item id may be inserted into the item generator. */
    public static boolean isItemAllowed(@Nullable ResourceLocation itemId) {
        return isAllowed(COMMON.itemFilterMode.get(), COMMON.itemFilterList.get(), itemId);
    }

    /** Whether the given fluid id may be inserted into the fluid generator. */
    public static boolean isFluidAllowed(@Nullable ResourceLocation fluidId) {
        return isAllowed(COMMON.fluidFilterMode.get(), COMMON.fluidFilterList.get(), fluidId);
    }

    /**
     * 1.13.2 identifies fluids by {@code Fluid#getName()}, which has no namespace for the
     * vanilla fluids ("water", "lava"). Normalise both sides so the documented
     * {@code minecraft:water} form and the bare form behave identically.
     */
    public static boolean isFluidAllowedName(@Nullable String fluidName) {
        if (fluidName == null) {
            return true;
        }
        String id = normalizeFluidName(fluidName);
        boolean listed = false;
        for (String entry : COMMON.fluidFilterList.get()) {
            if (normalizeFluidName(entry).equals(id)) {
                listed = true;
                break;
            }
        }
        return COMMON.fluidFilterMode.get() == FilterMode.BLACKLIST ? !listed : listed;
    }

    private static String normalizeFluidName(String name) {
        return name.indexOf(':') < 0 ? "minecraft:" + name : name;
    }

    private static boolean isAllowed(FilterMode mode, List<? extends String> list,
                                     @Nullable ResourceLocation id) {
        if (id == null) {
            return true;
        }
        boolean listed = list.contains(id.toString());
        return mode == FilterMode.BLACKLIST ? !listed : listed;
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC, "infwhat.toml");
    }

    private InfinityWhatConfig() {
    }
}

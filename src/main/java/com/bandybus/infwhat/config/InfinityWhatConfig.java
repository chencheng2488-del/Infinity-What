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
                    .comment("Fluid ids (e.g. \"minecraft:water\") the mode above applies to.")
                    .defineList("list", Collections.<String>emptyList(),
                            obj -> obj instanceof String && ((String) obj).contains(":"));
            builder.pop();
        }
    }

    public static boolean infiniteEnabled() {
        return COMMON.enableInfiniteSource.get();
    }

    public static int finiteCapacity() {
        return COMMON.finiteTankCapacity.get();
    }

    /** Whether the given item id may be inserted into the item generator. */
    public static boolean isItemAllowed(@Nullable ResourceLocation itemId) {
        return isAllowed(COMMON.itemFilterMode.get(), COMMON.itemFilterList.get(), itemId);
    }

    /** Whether the given fluid id may be inserted into the fluid generator. */
    public static boolean isFluidAllowed(@Nullable ResourceLocation fluidId) {
        return isAllowed(COMMON.fluidFilterMode.get(), COMMON.fluidFilterList.get(), fluidId);
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

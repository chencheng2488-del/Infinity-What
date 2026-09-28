package cn.autoforged.infinity_what_1790519020.config;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Configuration for the Infinity Fluid Generator.
 *
 * <ul>
 *   <li>{@code balance.enableInfiniteSource} - the balance switch requested by the spec.
 *       When disabled the block behaves as an ordinary single-fluid tank of
 *       {@code balance.finiteTankCapacity} mB instead of an unlimited source.</li>
 *   <li>{@code blacklist.fluidBlacklist} - fluid ids that may never be stored. Fluids listed
 *       here are rejected by {@code fill(...)}.</li>
 * </ul>
 */
public class InfinityWhatConfig {

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
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> fluidBlacklist;

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

            builder.comment("Fluid blacklist. Listed fluids can never be inserted.").push("blacklist");
            this.fluidBlacklist = builder
                    .comment("Fluid ids (e.g. \"minecraft:water\") that are not allowed to be stored.")
                    .defineList("fluidBlacklist", List.<String>of(),
                            obj -> obj instanceof String s && s.contains(":"));
            builder.pop();
        }
    }

    public static boolean infiniteEnabled() {
        return COMMON.enableInfiniteSource.get();
    }

    public static int finiteCapacity() {
        return COMMON.finiteTankCapacity.get();
    }

    public static boolean isBlacklisted(@Nullable ResourceLocation fluidId) {
        if (fluidId == null) {
            return false;
        }
        return COMMON.fluidBlacklist.get().contains(fluidId.toString());
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC);
    }

    private InfinityWhatConfig() {
    }
}

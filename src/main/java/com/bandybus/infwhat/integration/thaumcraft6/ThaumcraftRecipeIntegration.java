package com.bandybus.infwhat.integration.thaumcraft6;

import com.bandybus.infwhat.block.ModBlocks;
import com.bandybus.infwhat.item.ModItems;
import net.minecraft.block.Block;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.api.crafting.ShapedArcaneRecipe;
import thaumcraft.api.items.ItemsTC;

/**
 * Infusion and arcane recipes plus research registration for Thaumcraft 6.
 *
 * <p>This class references Thaumcraft types directly, so it is only ever loaded from inside a
 * {@link ThaumcraftIntegration#isActive()} branch. Without Thaumcraft the class is never resolved
 * and the mod keeps working.
 *
 * <p>Timing (verified in TC6 6.1.BETA26):
 * <ul>
 *   <li>{@code ThaumcraftApi.registerResearchLocation} only records a resource location; the JSON
 *       is parsed later by {@code ConfigResearch.init()}, which TC6 calls in its own
 *       {@code init} phase. Our {@code preInit} registration is therefore always early enough.</li>
 *   <li>{@code ThaumcraftApi.addInfusionCraftingRecipe} and {@code addArcaneCraftingRecipe} just
 *       put the recipe into the catalog map, so registering them in {@code postInit} is safe;
 *       {@code ItemsTC} is not filled before TC's own {@code init} phase.</li>
 *   <li>{@code InfusionRecipe.matches} checks
 *       {@code ThaumcraftCapabilities.getKnowledge(player).isResearchKnown(research)}, so the
 *       research JSON below intentionally has no {@code AUTOUNLOCK}: the recipe stays locked
 *       behind its parent research.</li>
 * </ul>
 */
public final class ThaumcraftRecipeIntegration {

    /** Research key; must match the "key" in assets/infwhat/research/ordo_usurpatory_matrix.json. */
    public static final String RESEARCH_KEY = "ORDO_MATRIX";

    /** Research key of the Arcane Aspect Replicator. */
    public static final String RESEARCH_REPLICATOR = "ARCANE_REPLICATOR";

    /** Research key of the Arcane Essentia Emanator. */
    public static final String RESEARCH_EMANATOR = "ARCANE_EMANATOR";

    /** Research key of the Arcane Ord-Record Shard. */
    public static final String RESEARCH_SHARD = "ARCANE_SHARD";

    private static final ResourceLocation RECIPE_KEY =
            new ResourceLocation("infwhat", "ordo_usurpatory_matrix");
    private static final ResourceLocation REPLICATOR_RECIPE_KEY =
            new ResourceLocation("infwhat", "arcane_aspect_replicator");
    private static final ResourceLocation EMANATOR_RECIPE_KEY =
            new ResourceLocation("infwhat", "arcane_essentia_emanator");
    private static final ResourceLocation SHARD_RECIPE_KEY =
            new ResourceLocation("infwhat", "arcane_ord_record_shard");

    private static final ResourceLocation ARCANE_GROUP =
            new ResourceLocation("infwhat", "arcane_machines");
    private static final ResourceLocation SHARD_GROUP =
            new ResourceLocation("infwhat", "arcane_tools");

    /** TC6 removed the TC4 primal charm item; the primordial pearl is its closest counterpart. */
    private static final ResourceLocation PRIMAL_ITEM =
            new ResourceLocation("thaumcraft", "primordial_pearl");

    /** Minimum altar stability required before the infusion may start. */
    private static final float MINIMUM_ALTAR_STABILITY = 5.0F;

    /** Arcane workbench vis for the Essentia Emanator recipe (six primal aspects, 32 each). */
    private static final int EMANATOR_VIS = 192;

    /** Arcane workbench vis for the Aspect Replicator recipe (six primal aspects, 16 each). */
    private static final int REPLICATOR_VIS = 96;

    /** Arcane workbench vis for the shard recipe. */
    private static final int SHARD_VIS = 12;

    private ThaumcraftRecipeIntegration() {
    }

    /** Hands TC6 the research JSON locations; they are parsed later in TC's init phase. */
    public static void registerResearchLocations() {
        registerResearchLocation("ordo_usurpatory_matrix");
        registerResearchLocation("arcane_aspect_replicator");
        registerResearchLocation("arcane_essentia_emanator");
        registerResearchLocation("arcane_ord_record_shard");
    }

    private static void registerResearchLocation(String name) {
        ThaumcraftApi.registerResearchLocation(
                new ResourceLocation("infwhat", "research/" + name));
    }

    /** Adds the Ordo infusion recipe and the two machine and shard arcane recipes. */
    public static void registerRecipes() {
        AspectList aspects = new AspectList();
        aspects.add(Aspect.AIR, 16);
        aspects.add(Aspect.EARTH, 16);
        aspects.add(Aspect.FIRE, 16);
        aspects.add(Aspect.WATER, 16);
        aspects.add(Aspect.ORDER, 16);
        aspects.add(Aspect.ENTROPY, 16);
        aspects.add(Aspect.MAGIC, 32);

        ItemStack central = new ItemStack(BlocksTC.metalBlockThaumium);

        Block candle = BlocksTC.candles.get(EnumDyeColor.WHITE);
        Item primal = ForgeRegistries.ITEMS.getValue(PRIMAL_ITEM);
        Object[] components = new Object[]{
                new ItemStack(candle),
                new ItemStack(candle),
                new ItemStack(candle),
                new ItemStack(candle),
                new ItemStack(primal, 1, 0),
                new ItemStack(primal, 1, 0),
                new ItemStack(BlocksTC.logGreatwood),
                new ItemStack(BlocksTC.logGreatwood),
                new ItemStack(ItemsTC.brain),
                new ItemStack(ItemsTC.thaumometer)
        };

        InfusionRecipe recipe = new StabilityGatedInfusionRecipe(RESEARCH_KEY,
                new ItemStack(ModBlocks.ORDO_USURPATORY_MATRIX), 2, aspects, central,
                MINIMUM_ALTAR_STABILITY, components);
        ThaumcraftApi.addInfusionCraftingRecipe(RECIPE_KEY, recipe);

        // 配方 1：坩埚 → 奥术源质溢散仪.
        registerMachineRecipe(RESEARCH_EMANATOR, EMANATOR_RECIPE_KEY,
                new ItemStack(ModBlocks.ARCANE_ESSENTIA_EMANATOR),
                new ItemStack(BlocksTC.crucible), EMANATOR_VIS);
        // 配方 2：台桌 → 奥术相复仪.
        registerMachineRecipe(RESEARCH_REPLICATOR, REPLICATOR_RECIPE_KEY,
                new ItemStack(ModBlocks.ARCANE_ASPECT_REPLICATOR),
                new ItemStack(BlocksTC.tableWood), REPLICATOR_VIS);
        registerShardRecipe();
    }

    /** Registers one 3x3 arcane machine recipe with the given total vis cost. */
    private static void registerMachineRecipe(String researchKey, ResourceLocation recipeKey,
                                              ItemStack output, ItemStack center, int vis) {
        ItemStack primal = new ItemStack(
                ForgeRegistries.ITEMS.getValue(PRIMAL_ITEM), 1, 0);
        ShapedArcaneRecipe recipe = new ShapedArcaneRecipe(ARCANE_GROUP, researchKey, vis,
                null, output,
                "ABA", "CDC", "AEA",
                'A', primal,
                'B', center,
                'C', "ingotThaumium",
                'D', new ItemStack(ItemsTC.phial, 1, 0),
                'E', new ItemStack(BlocksTC.smelterBasic));
        ThaumcraftApi.addArcaneCraftingRecipe(recipeKey, recipe);
    }

    /** Registers the shard recipe: a scrying lens bound with gold and quartz. */
    private static void registerShardRecipe() {
        ShapedArcaneRecipe recipe = new ShapedArcaneRecipe(SHARD_GROUP, RESEARCH_SHARD,
                SHARD_VIS, null, new ItemStack(ModItems.ARCANE_ORD_RECORD_SHARD),
                "GQG", "QTQ", "GQG",
                'G', "ingotGold",
                'Q', "gemQuartz",
                'T', new ItemStack(ItemsTC.thaumometer));
        ThaumcraftApi.addArcaneCraftingRecipe(SHARD_RECIPE_KEY, recipe);
    }
}

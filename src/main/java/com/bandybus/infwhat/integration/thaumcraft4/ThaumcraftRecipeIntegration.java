package com.bandybus.infwhat.integration.thaumcraft4;

import com.bandybus.infwhat.block.ModBlocks;
import com.bandybus.infwhat.item.ModItems;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.api.crafting.ShapedArcaneRecipe;
import thaumcraft.api.research.ResearchItem;
import thaumcraft.api.research.ResearchPage;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.config.ConfigItems;

/**
 * Infusion and arcane recipes plus research registration for Thaumcraft 4.
 *
 * <p>This class references Thaumcraft types directly, so it is only ever loaded from inside a
 * {@link ThaumcraftIntegration#isActive()} branch.
 *
 * <p>TC4 {@code InfusionRecipe.matches} checks research via
 * {@code ThaumcraftApiHelper.isResearchComplete}, so the research entries registered in
 * {@link #registerResearch()} gate the recipes. Arcane recipes carry the research key explicitly.
 *
 * <p>Research registration must happen after Thaumcraft's postInit, because TC4 registers its
 * research categories in {@code ConfigResearch.init()} during postInit. {@code FMLLoadComplete}
 * is therefore used by the mod entry point.
 */
@SuppressWarnings("unchecked")
public final class ThaumcraftRecipeIntegration {

    /** Research key of the Ordo Usurpatory Matrix; must match the infusion recipe. */
    public static final String RESEARCH_KEY = "ORDO_MATRIX";

    /** Research key of the Arcane Aspect Replicator; must match its arcane recipe. */
    public static final String RESEARCH_REPLICATOR = "ARCANE_REPLICATOR";

    /** Research key of the Arcane Essentia Emanator; must match its arcane recipe. */
    public static final String RESEARCH_EMANATOR = "ARCANE_EMANATOR";

    /** Research key of the Arcane Ord-Record Shard; must match its arcane recipe. */
    public static final String RESEARCH_SHARD = "ARCANE_SHARD";

    /** Maximum altar instability accepted before the infusion may start. */
    private static final int MAXIMUM_ALTAR_INSTABILITY = 20;

    /** Arcane workbench vis per primal aspect for the Essentia Emanator recipe. */
    private static final int EMANATOR_VIS_PER_ASPECT = 32;

    /** Arcane workbench vis per primal aspect for the Aspect Replicator recipe. */
    private static final int REPLICATOR_VIS_PER_ASPECT = 16;

    private static InfusionRecipe infusionRecipe;
    private static ShapedArcaneRecipe replicatorRecipe;
    private static ShapedArcaneRecipe emanatorRecipe;
    private static ShapedArcaneRecipe shardRecipe;

    private ThaumcraftRecipeIntegration() {
    }

    /**
     * Adds the infusion recipe, the two arcane machine recipes and the shard recipe. Called from
     * {@code FMLLoadCompleteEvent}, because TC4 fills some of its items and all of its research
     * categories in its own postInit.
     */
    public static void registerRecipes() {
        AspectList aspects = new AspectList();
        aspects.add(Aspect.AIR, 16);
        aspects.add(Aspect.EARTH, 16);
        aspects.add(Aspect.FIRE, 16);
        aspects.add(Aspect.WATER, 16);
        aspects.add(Aspect.ORDER, 16);
        aspects.add(Aspect.ENTROPY, 16);
        aspects.add(Aspect.MAGIC, 32);

        // Thaumium block.
        ItemStack central = new ItemStack(ConfigBlocks.blockCosmeticSolid, 1, 4);
        // White tallow candle.
        ItemStack candle = new ItemStack(ConfigBlocks.blockCandle, 1, 0);
        // Primal charm.
        ItemStack primalCharm = new ItemStack(ConfigItems.itemResource, 1, 15);
        // Greatwood log.
        ItemStack greatwood = new ItemStack(ConfigBlocks.blockMagicalLog, 1, 0);

        ItemStack[] components = new ItemStack[]{
                candle.copy(),
                candle.copy(),
                candle.copy(),
                candle.copy(),
                primalCharm.copy(),
                primalCharm.copy(),
                greatwood.copy(),
                greatwood.copy(),
                new ItemStack(ConfigItems.itemZombieBrain),
                new ItemStack(ConfigItems.itemThaumometer)
        };

        infusionRecipe = new InstabilityGatedInfusionRecipe(RESEARCH_KEY,
                new ItemStack(ModBlocks.ORDO_USURPATORY_MATRIX), 2, aspects, central,
                MAXIMUM_ALTAR_INSTABILITY, components);
        // TC4's helper always builds a plain InfusionRecipe, so the gated subclass is added to the
        // crafting list directly. The altar resolves recipes through ThaumcraftApi.getInfusionRecipe.
        ThaumcraftApi.getCraftingRecipes().add(infusionRecipe);

        // Recipe 1: crucible -> Arcane Essentia Emanator.
        emanatorRecipe = registerMachineRecipe(RESEARCH_EMANATOR,
                new ItemStack(ModBlocks.ARCANE_ESSENTIA_EMANATOR),
                new ItemStack(ConfigBlocks.blockMetalDevice, 1, 0),
                EMANATOR_VIS_PER_ASPECT);
        // Recipe 2: table -> Arcane Aspect Replicator.
        replicatorRecipe = registerMachineRecipe(RESEARCH_REPLICATOR,
                new ItemStack(ModBlocks.ARCANE_ASPECT_REPLICATOR),
                new ItemStack(ConfigBlocks.blockTable, 1, 0),
                REPLICATOR_VIS_PER_ASPECT);
        shardRecipe = registerShardRecipe();
    }

    /** Registers one 3x3 arcane machine recipe with the given vis per primal aspect. */
    private static ShapedArcaneRecipe registerMachineRecipe(String researchKey, ItemStack output,
                                                             ItemStack center, int visPerAspect) {
        AspectList vis = new AspectList();
        vis.add(Aspect.AIR, visPerAspect);
        vis.add(Aspect.EARTH, visPerAspect);
        vis.add(Aspect.FIRE, visPerAspect);
        vis.add(Aspect.WATER, visPerAspect);
        vis.add(Aspect.ORDER, visPerAspect);
        vis.add(Aspect.ENTROPY, visPerAspect);

        return ThaumcraftApi.addArcaneCraftingRecipe(researchKey, output, vis,
                "ABA", "CDC", "AEA",
                'A', new ItemStack(ConfigItems.itemResource, 1, 15),
                'B', center,
                'C', new ItemStack(ConfigItems.itemResource, 1, 2),
                'D', new ItemStack(ConfigItems.itemEssence, 1, 0),
                'E', new ItemStack(ConfigBlocks.blockStoneDevice, 1, 0));
    }

    /** Registers the shard recipe: a scrying lens bound with gold and quartz. */
    private static ShapedArcaneRecipe registerShardRecipe() {
        AspectList vis = new AspectList();
        vis.add(Aspect.ORDER, 8);
        vis.add(Aspect.AIR, 4);

        return ThaumcraftApi.addArcaneCraftingRecipe(RESEARCH_SHARD,
                new ItemStack(ModItems.ARCANE_ORD_RECORD_SHARD), vis,
                "GQG", "QTQ", "GQG",
                'G', new ItemStack(Items.gold_ingot),
                'Q', new ItemStack(Items.quartz),
                'T', new ItemStack(ConfigItems.itemThaumometer));
    }

    /**
     * Registers the Thaumonomicon entries. Must run after TC4's postInit, because the ALCHEMY
     * category does not exist before {@code ConfigResearch.init()}.
     */
    public static void registerResearch() {
        AspectList tags = new AspectList();
        tags.add(Aspect.ORDER, 24);
        tags.add(Aspect.MIND, 16);
        tags.add(Aspect.ENTROPY, 12);

        ResearchItem research = new ResearchItem(RESEARCH_KEY, "ARTIFICE", tags, -1, 1, 3,
                new ItemStack(ModBlocks.ORDO_USURPATORY_MATRIX));
        research.setParents("INFUSION", "BASICARTIFACE");
        research.setPages(
                new ResearchPage("tc.research_page.ORDO_MATRIX.1"),
                new ResearchPage(infusionRecipe));
        research.registerResearchItem();

        AspectList replicatorTags = new AspectList();
        replicatorTags.add(Aspect.ORDER, 8);
        replicatorTags.add(Aspect.MAGIC, 8);
        replicatorTags.add(Aspect.MIND, 4);

        ResearchItem replicator = new ResearchItem(RESEARCH_REPLICATOR, "ALCHEMY",
                replicatorTags, 6, -1, 2, new ItemStack(ModBlocks.ARCANE_ASPECT_REPLICATOR));
        replicator.setParents("DISTILESSENTIA");
        replicator.setPages(
                new ResearchPage("tc.research_page.ARCANE_REPLICATOR.1"),
                new ResearchPage("tc.research_page.ARCANE_REPLICATOR.2"),
                new ResearchPage(replicatorRecipe));
        replicator.registerResearchItem();

        AspectList emanatorTags = new AspectList();
        emanatorTags.add(Aspect.WATER, 8);
        emanatorTags.add(Aspect.MAGIC, 8);
        emanatorTags.add(Aspect.ORDER, 4);

        ResearchItem emanator = new ResearchItem(RESEARCH_EMANATOR, "ALCHEMY",
                emanatorTags, 7, -1, 2, new ItemStack(ModBlocks.ARCANE_ESSENTIA_EMANATOR));
        emanator.setParents("DISTILESSENTIA");
        emanator.setPages(
                new ResearchPage("tc.research_page.ARCANE_EMANATOR.1"),
                new ResearchPage("tc.research_page.ARCANE_EMANATOR.2"),
                new ResearchPage(emanatorRecipe));
        emanator.registerResearchItem();

        AspectList shardTags = new AspectList();
        shardTags.add(Aspect.ORDER, 8);
        shardTags.add(Aspect.MIND, 8);
        shardTags.add(Aspect.ENTROPY, 4);

        ResearchItem shard = new ResearchItem(RESEARCH_SHARD, "ALCHEMY", shardTags, 8, -1, 1,
                new ItemStack(ModItems.ARCANE_ORD_RECORD_SHARD));
        shard.setParents(RESEARCH_REPLICATOR, RESEARCH_EMANATOR);
        shard.setPages(
                new ResearchPage("tc.research_page.ARCANE_SHARD.1"),
                new ResearchPage("tc.research_page.ARCANE_SHARD.2"),
                new ResearchPage(shardRecipe));
        shard.registerResearchItem();
    }
}

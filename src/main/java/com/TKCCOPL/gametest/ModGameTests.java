package com.TKCCOPL.gametest;

import com.TKCCOPL.Config;
import com.TKCCOPL.api.BottlerInfo;
import com.TKCCOPL.api.CyberCultivatorAPI;
import com.TKCCOPL.api.MachineControlInfo;
import com.TKCCOPL.api.RedstoneControlMode;
import com.TKCCOPL.block.entity.BioIncubatorBlockEntity;
import com.TKCCOPL.block.entity.AtmosphericCondenserBlockEntity;
import com.TKCCOPL.block.entity.GeneSplicerBlockEntity;
import com.TKCCOPL.block.entity.SerumBottlerBlockEntity;
import com.TKCCOPL.cybercultivator;
import com.TKCCOPL.compat.patchouli.PatchouliGuideCompat;
import com.TKCCOPL.event.CropMatureEvent;
import com.TKCCOPL.event.GeneSpliceEvent;
import com.TKCCOPL.event.SerumConsumeEvent;
import com.TKCCOPL.event.SerumCraftEvent;
import com.TKCCOPL.event.VillagerTradeEvents;
import com.TKCCOPL.init.ModBlocks;
import com.TKCCOPL.init.CreativeTabVariants;
import com.TKCCOPL.init.ModEffects;
import com.TKCCOPL.init.ModItems;
import com.TKCCOPL.item.GeneticSeedItem;
import com.TKCCOPL.item.SynapticSerumItem;
import com.TKCCOPL.menu.AtmosphericCondenserMenu;
import com.TKCCOPL.menu.GeneSplicerMenu;
import com.TKCCOPL.menu.SerumBottlerMenu;
import com.TKCCOPL.network.GameplayConfigSnapshot;
import com.TKCCOPL.network.GameplayConfigSyncPacket;
import com.TKCCOPL.network.S02DetectionSyncPacket;
import com.TKCCOPL.recipe.IncubatorOutputRecipe;
import com.TKCCOPL.recipe.IncubatorOutputSerializer;
import com.TKCCOPL.recipe.GeneSpliceRules;
import com.TKCCOPL.recipe.RecipeOrdering;
import com.TKCCOPL.recipe.SerumRecipe;
import com.TKCCOPL.recipe.SerumRecipeSerializer;
import com.TKCCOPL.recipe.SerumRecipeIds;
import com.google.gson.JsonParser;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.fml.ModList;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@GameTestHolder(cybercultivator.MODID)
@PrefixGameTestTemplate(false)
public final class ModGameTests {
    private static final String EMPTY_TEMPLATE = "empty";
    private static final String TAG_ACTIVITY = "SynapticActivity";

    private ModGameTests() {}

    @GameTest(template = EMPTY_TEMPLATE)
    public static void activityBoundsAndEffectIds(GameTestHelper helper) {
        ItemStack serum = new ItemStack(ModItems.SYNAPTIC_SERUM_S01.get());
        helper.assertTrue(SynapticSerumItem.getActivity(serum) == 5, "Missing Activity must use default 5");

        serum.getOrCreateTag().putInt(TAG_ACTIVITY, -20);
        helper.assertTrue(SynapticSerumItem.getActivity(serum) == 1, "Negative Activity must clamp to 1");
        serum.getOrCreateTag().putInt(TAG_ACTIVITY, 15);
        helper.assertTrue(SynapticSerumItem.getActivity(serum) == 15, "Activity 15 must be preserved");
        serum.getOrCreateTag().putInt(TAG_ACTIVITY, Integer.MAX_VALUE);
        helper.assertTrue(SynapticSerumItem.getActivity(serum) == 15, "Large Activity must clamp to 15");

        helper.assertTrue(CyberCultivatorAPI.calculateActivity(null) == 5, "Null input array must use Activity 5");
        helper.assertTrue(CyberCultivatorAPI.calculateActivity(new ItemStack[]{null, ItemStack.EMPTY}) == 5,
                "Null input elements must be skipped");
        helper.assertTrue(CyberCultivatorAPI.getSerumEffectInfo(ItemStack.EMPTY) == null,
                "Empty stacks must not expose serum effect info");
        helper.assertTrue(CyberCultivatorAPI.getSerumEffectInfo(new ItemStack(Items.APPLE)) == null,
                "Non-serum items must not expose serum effect info");

        assertEffectId(helper, ModItems.SYNAPTIC_SERUM_S01.get().getDefaultInstance(),
                "cybercultivator:synaptic_overclock");
        assertEffectId(helper, ModItems.SYNAPTIC_SERUM_S02.get().getDefaultInstance(),
                "cybercultivator:visual_enhancement");
        assertEffectId(helper, ModItems.SYNAPTIC_SERUM_S03.get().getDefaultInstance(),
                "cybercultivator:metabolic_boost");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void geneSpliceBalanceRulesAndEventCount(GameTestHelper helper) {
        helper.assertTrue(closeTo(GeneSpliceRules.mutationChance(0, 0,
                        0.05, 0.005, 20, 0.01, 0.25), 0.05),
                "Generation zero mutation chance must start at 5%");
        helper.assertTrue(closeTo(GeneSpliceRules.mutationChance(10, 0,
                        0.05, 0.005, 20, 0.01, 0.25), 0.10),
                "Generation ten mutation chance must be 10%");
        helper.assertTrue(closeTo(GeneSpliceRules.mutationChance(20, 0,
                        0.05, 0.005, 20, 0.01, 0.25), 0.15)
                        && closeTo(GeneSpliceRules.mutationChance(999, 0,
                        0.05, 0.005, 20, 0.01, 0.25), 0.15),
                "Mutation generation contribution must stop at generation 20");
        helper.assertTrue(closeTo(GeneSpliceRules.mutationChance(20, 10,
                        0.05, 0.005, 20, 0.01, 0.25), 0.25),
                "Maximum gene difference must reach but not exceed the 25% cap");

        helper.assertTrue(closeTo(GeneSpliceRules.normalTwinChance(0, 0.10, 0.02, 0.60), 0.10)
                        && closeTo(GeneSpliceRules.normalTwinChance(10, 0.10, 0.02, 0.60), 0.30)
                        && closeTo(GeneSpliceRules.normalTwinChance(20, 0.10, 0.02, 0.60), 0.50)
                        && closeTo(GeneSpliceRules.normalTwinChance(999, 0.10, 0.02, 0.60), 0.60),
                "Normal twin chance must scale by generation and stop at 60%");
        helper.assertTrue(closeTo(GeneSpliceRules.totalTwinChance(0.15, 0.50), 0.575),
                "Total twin probability must include mutation-guaranteed twins without double counting");
        helper.assertTrue(GeneSpliceRules.resolveOffspringCount(false, -10) == 1
                        && GeneSpliceRules.resolveOffspringCount(false, 99) == 2
                        && GeneSpliceRules.resolveOffspringCount(true, 1) == 2,
                "Event output counts must clamp to 1-2 and mutations must force two offspring");

        ItemStack parent = new ItemStack(ModItems.FIBER_REED_SEEDS.get(), 2);
        GeneSpliceEvent event = new GeneSpliceEvent(parent, parent,
                5, 5, 5, 0, 1, false, 0, "", 2);
        parent.shrink(1);
        helper.assertTrue(event.getSeedA().getCount() == 2 && event.getOffspringCount() == 2,
                "GeneSpliceEvent must copy parents and expose offspringCount");
        event.setOffspringCount(7);
        helper.assertTrue(event.getOffspringCount() == 7
                        && GeneSpliceRules.resolveOffspringCount(event.isMutation(), event.getOffspringCount()) == 2,
                "Machine-side resolution must clamp an event-modified offspring count after listeners finish");

        helper.assertTrue(closeTo(Config.migrateMutationChancePerGeneration(1, 0.02), 0.005),
                "Historical default mutation increment must migrate once");
        helper.assertTrue(closeTo(Config.migrateMutationChancePerGeneration(1, 0.013), 0.013)
                        && closeTo(Config.migrateMutationChancePerGeneration(2, 0.02), 0.02),
                "Custom values and already-migrated schemas must remain unchanged");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void apprenticeSeedTradesMatchBalance(GameTestHelper helper) {
        assertSeedTrade(helper, VillagerProfession.FARMER, ModItems.PROTEIN_SOY_SEEDS.get());
        assertSeedTrade(helper, VillagerProfession.CLERIC, ModItems.ALCOHOL_BLOOM_SEEDS.get());
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void defaultSeedGenesAndMaterialQuality(GameTestHelper helper) {
        ItemStack[] seeds = {
                ModItems.FIBER_REED_SEEDS.get().getDefaultInstance(),
                ModItems.PROTEIN_SOY_SEEDS.get().getDefaultInstance(),
                ModItems.ALCOHOL_BLOOM_SEEDS.get().getDefaultInstance()
        };
        String[] recipePaths = {
                "incubator/fiber_reed",
                "incubator/protein_soy",
                "incubator/alcohol_bloom"
        };
        String[] qualityTags = {"Potency", "Concentration", "Purity"};

        for (int i = 0; i < seeds.length; i++) {
            ItemStack seed = seeds[i];
            helper.assertTrue(GeneticSeedItem.getGene(seed, GeneticSeedItem.GENE_SPEED) == 5,
                    "Default seed Speed must be 5");
            helper.assertTrue(GeneticSeedItem.getGene(seed, GeneticSeedItem.GENE_YIELD) == 5,
                    "Default seed Yield must be 5");
            helper.assertTrue(GeneticSeedItem.getGene(seed, GeneticSeedItem.GENE_POTENCY) == 5,
                    "Default seed Potency must be 5");

            ResourceLocation recipeId = ResourceLocation.fromNamespaceAndPath(cybercultivator.MODID, recipePaths[i]);
            Recipe<?> loadedRecipe = helper.getLevel().getRecipeManager().byKey(recipeId).orElse(null);
            helper.assertTrue(loadedRecipe instanceof IncubatorOutputRecipe,
                    "Built-in incubator recipe must be loaded: " + recipeId);
            IncubatorOutputRecipe incubatorRecipe = (IncubatorOutputRecipe) loadedRecipe;
            int[] displayedGenes = incubatorRecipe.getDefaultGenes();
            helper.assertTrue(displayedGenes[0] == 5 && displayedGenes[1] == 5 && displayedGenes[2] == 5,
                    "JEI default genes must match the 5/5/5 seed defaults: " + recipeId);

            ItemStack material = incubatorRecipe.assemble(seed);
            helper.assertTrue(material.getOrCreateTag().getInt(qualityTags[i]) == 5,
                    "Default seed must produce quality 5 material: " + recipeId);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void creativeTabQualityVariantsAreCompleteAndOrdered(GameTestHelper helper) {
        List<ItemStack> seeds = new ArrayList<>();
        CreativeTabVariants.addBalancedSeedVariants((stack, visibility) -> seeds.add(stack.copy()));
        helper.assertTrue(seeds.size() == 30, "Quality tab must contain 30 balanced seed variants");
        for (int geneLevel = 1; geneLevel <= 10; geneLevel++) {
            int index = (geneLevel - 1) * 3;
            assertBalancedSeedVariant(helper, seeds.get(index), ModItems.FIBER_REED_SEEDS.get(), geneLevel);
            assertBalancedSeedVariant(helper, seeds.get(index + 1), ModItems.PROTEIN_SOY_SEEDS.get(), geneLevel);
            assertBalancedSeedVariant(helper, seeds.get(index + 2), ModItems.ALCOHOL_BLOOM_SEEDS.get(), geneLevel);
        }

        List<ItemStack> materials = new ArrayList<>();
        CreativeTabVariants.addMaterialVariants((stack, visibility) -> materials.add(stack.copy()));
        helper.assertTrue(materials.size() == 30, "Quality tab must contain 30 material variants");
        for (int quality = 1; quality <= 10; quality++) {
            int index = (quality - 1) * 3;
            assertTaggedVariant(helper, materials.get(index), ModItems.PLANT_FIBER.get(), "Potency", quality);
            assertTaggedVariant(helper, materials.get(index + 1), ModItems.INDUSTRIAL_ETHANOL.get(), "Purity", quality);
            assertTaggedVariant(helper, materials.get(index + 2), ModItems.BIOCHEMICAL_SOLUTION.get(), "Concentration", quality);
        }

        List<ItemStack> activityItems = new ArrayList<>();
        CreativeTabVariants.addActivityVariants((stack, visibility) -> activityItems.add(stack.copy()));
        helper.assertTrue(activityItems.size() == 60, "Quality tab must contain 60 Activity variants");
        for (int activity = 1; activity <= 15; activity++) {
            assertTaggedVariant(helper, activityItems.get(activity - 1), ModItems.SYNAPTIC_NEURAL_BERRY.get(),
                    TAG_ACTIVITY, activity);
            int serumIndex = 15 + (activity - 1) * 3;
            assertTaggedVariant(helper, activityItems.get(serumIndex), ModItems.SYNAPTIC_SERUM_S01.get(),
                    TAG_ACTIVITY, activity);
            assertTaggedVariant(helper, activityItems.get(serumIndex + 1), ModItems.SYNAPTIC_SERUM_S02.get(),
                    TAG_ACTIVITY, activity);
            assertTaggedVariant(helper, activityItems.get(serumIndex + 2), ModItems.SYNAPTIC_SERUM_S03.get(),
                    TAG_ACTIVITY, activity);
        }
        helper.succeed();
    }

    @SuppressWarnings("deprecation")
    @GameTest(template = EMPTY_TEMPLATE)
    public static void legacyIdsAndSnapshots(GameTestHelper helper) {
        ResourceLocation[] ids = {
                SerumRecipeIds.BERRY_SYNTHESIS,
                SerumRecipeIds.S01_BOTTLING,
                SerumRecipeIds.S02_BOTTLING,
                SerumRecipeIds.S03_BOTTLING
        };
        for (int i = 0; i < ids.length; i++) {
            helper.assertTrue(SerumRecipeIds.legacyIndex(ids[i]) == i, "Built-in recipe legacy index mismatch");
            SerumCraftEvent craftEvent = new SerumCraftEvent(null, ItemStack.EMPTY, 5, ids[i]);
            helper.assertTrue(craftEvent.getRecipeIndex() == i, "SerumCraftEvent legacy index mismatch");
        }
        helper.assertTrue(SerumRecipeIds.legacyIndex(null) == -1, "Null recipe ID must map to -1");
        helper.assertTrue(SerumRecipeIds.legacyIndex(ResourceLocation.fromNamespaceAndPath("example", "custom")) == -1,
                "Custom recipe ID must map to -1");

        ItemStack source = new ItemStack(Items.DIAMOND, 2);
        BottlerInfo info = new BottlerInfo(1, 2, SerumRecipeIds.S01_BOTTLING, source, 5);
        source.shrink(1);
        helper.assertTrue(info.output().getCount() == 2, "DTO constructor must copy ItemStack");
        ItemStack returned = info.output();
        returned.shrink(1);
        helper.assertTrue(info.output().getCount() == 2, "DTO accessor must return an ItemStack copy");
        helper.assertTrue(info.activeRecipe() == 1, "BottlerInfo must reuse the legacy recipe mapping");

        ItemStack input = new ItemStack(Items.IRON_INGOT, 2);
        SerumCraftEvent craftEvent = new SerumCraftEvent(new ItemStack[]{input, null}, ItemStack.EMPTY, 5,
                SerumRecipeIds.BERRY_SYNTHESIS);
        input.shrink(1);
        helper.assertTrue(craftEvent.getInputs()[0].getCount() == 2, "Event constructor must copy inputs");
        ItemStack[] returnedInputs = craftEvent.getInputs();
        returnedInputs[0].shrink(1);
        helper.assertTrue(craftEvent.getInputs()[0].getCount() == 2, "Event getter must return deep copies");
        helper.assertTrue(craftEvent.getInputs()[1].isEmpty(), "Null event inputs must normalize to empty stacks");

        ItemStack mutableOutput = new ItemStack(Items.DIAMOND, 2);
        CropMatureEvent cropEvent = new CropMatureEvent(helper.getLevel(), BlockPos.ZERO, ItemStack.EMPTY, mutableOutput);
        mutableOutput.shrink(1);
        helper.assertTrue(cropEvent.getOutput().getCount() == 2, "Crop event constructor must copy output");
        ItemStack returnedCropOutput = cropEvent.getOutput();
        returnedCropOutput.shrink(1);
        helper.assertTrue(cropEvent.getOutput().getCount() == 2, "Crop event getter must copy output");

        ItemStack replacement = new ItemStack(Items.EMERALD, 2);
        craftEvent.setOutput(replacement);
        replacement.shrink(1);
        helper.assertTrue(craftEvent.getOutput().getCount() == 2, "Craft event setter must copy output");
        craftEvent.setActivity(100);
        helper.assertTrue(craftEvent.getActivity() == 15, "Craft event Activity must clamp to 15");

        helper.assertTrue(CyberCultivatorAPI.getGene(null, null) == 1, "Null seed reads must use gene default");
        helper.assertTrue(CyberCultivatorAPI.getGeneration(null) == 0, "Null seed generation must be zero");
        helper.assertTrue(CyberCultivatorAPI.getSynergy(null) == 0, "Null seed synergy must be zero");
        CyberCultivatorAPI.setGene(null, null, 10);

        List<SerumRecipe> recipes = CyberCultivatorAPI.getSerumRecipes(helper.getLevel());
        boolean immutable = false;
        try {
            recipes.clear();
        } catch (UnsupportedOperationException expected) {
            immutable = true;
        }
        helper.assertTrue(immutable, "Recipe API must return an immutable snapshot");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void recipePriorityIngredientsAndNetworkRoundTrip(GameTestHelper helper) {
        SerumRecipe low = new SerumRecipe(
                ResourceLocation.fromNamespaceAndPath("test", "z_low"),
                new Ingredient[]{Ingredient.of(Items.DIRT)}, new ItemStack(Items.IRON_INGOT),
                20, false, false, 0);
        SerumRecipe highB = new SerumRecipe(
                ResourceLocation.fromNamespaceAndPath("test", "b_high"),
                new Ingredient[]{Ingredient.of(Items.DIRT)}, new ItemStack(Items.GOLD_INGOT),
                20, false, false, 10);
        SerumRecipe highA = new SerumRecipe(
                ResourceLocation.fromNamespaceAndPath("test", "a_high"),
                new Ingredient[]{Ingredient.of(Items.DIRT)}, new ItemStack(Items.DIAMOND),
                20, false, false, 10);
        List<SerumRecipe> ordered = RecipeOrdering.sorted(List.of(low, highB, highA));
        helper.assertTrue(ordered.get(0) == highA && ordered.get(1) == highB && ordered.get(2) == low,
                "Recipes must sort by descending priority and ascending ID");

        Ingredient saplings = Ingredient.of(net.minecraft.tags.ItemTags.SAPLINGS);
        IncubatorOutputRecipe tagged = new IncubatorOutputRecipe(
                ResourceLocation.fromNamespaceAndPath("test", "tagged_seed"), saplings,
                new ItemStack(Items.APPLE), "1", "Potency", new int[]{5, 5, 5}, "Tagged", 7);
        helper.assertTrue(tagged.matches(new ItemStack(Items.OAK_SAPLING)), "Incubator seed tags must match");
        helper.assertFalse(tagged.matches(new ItemStack(Items.WHEAT_SEEDS)), "Unrelated seeds must not match tag");

        SerumRecipeSerializer serumSerializer = new SerumRecipeSerializer();
        var legacySerumJson = JsonParser.parseString("""
                {"ingredients":[{"item":"minecraft:dirt"}],
                 "result":{"item":"minecraft:diamond"},"processing_time":20}
                """).getAsJsonObject();
        SerumRecipe legacySerum = serumSerializer.fromJson(
                ResourceLocation.fromNamespaceAndPath("test", "legacy_serum"), legacySerumJson);
        helper.assertTrue(legacySerum.getPriority() == 0, "Missing serum priority must default to zero");

        FriendlyByteBuf serumBuffer = new FriendlyByteBuf(Unpooled.buffer());
        serumSerializer.toNetwork(serumBuffer, highA);
        serumBuffer.readerIndex(0);
        SerumRecipe serumRoundTrip = serumSerializer.fromNetwork(highA.getId(), serumBuffer);
        helper.assertTrue(serumRoundTrip != null && serumRoundTrip.getPriority() == 10,
                "Serum priority must survive network serialization");

        IncubatorOutputSerializer incubatorSerializer = new IncubatorOutputSerializer();
        var legacyIncubatorJson = JsonParser.parseString("""
                {"seed":{"tag":"minecraft:saplings"},
                 "output":{"item":"minecraft:apple"},"count_formula":"1"}
                """).getAsJsonObject();
        IncubatorOutputRecipe legacyIncubator = incubatorSerializer.fromJson(
                ResourceLocation.fromNamespaceAndPath("test", "legacy_incubator"), legacyIncubatorJson);
        helper.assertTrue(legacyIncubator.getPriority() == 0,
                "Missing incubator priority must default to zero");
        helper.assertTrue(legacyIncubator.matches(new ItemStack(Items.OAK_SAPLING)),
                "Serialized incubator tag must match");

        FriendlyByteBuf incubatorBuffer = new FriendlyByteBuf(Unpooled.buffer());
        incubatorSerializer.toNetwork(incubatorBuffer, tagged);
        incubatorBuffer.readerIndex(0);
        IncubatorOutputRecipe incubatorRoundTrip = incubatorSerializer.fromNetwork(tagged.getId(), incubatorBuffer);
        helper.assertTrue(incubatorRoundTrip != null && incubatorRoundTrip.getPriority() == 7,
                "Incubator priority must survive network serialization");
        helper.assertTrue(incubatorRoundTrip.matches(new ItemStack(Items.OAK_SAPLING)),
                "Incubator Ingredient must survive network serialization");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void kubeJsSchemasAndEvents(GameTestHelper helper) {
        if (!ModList.get().isLoaded("kubejs")) {
            helper.succeed();
            return;
        }

        ResourceLocation serumId = ResourceLocation.fromNamespaceAndPath("kubejs", "cybercultivator_serum_smoke");
        ResourceLocation incubatorId = ResourceLocation.fromNamespaceAndPath("kubejs", "cybercultivator_incubator_smoke");
        Recipe<?> serumLoaded = helper.getLevel().getRecipeManager().byKey(serumId).orElse(null);
        Recipe<?> incubatorLoaded = helper.getLevel().getRecipeManager().byKey(incubatorId).orElse(null);
        helper.assertTrue(serumLoaded instanceof SerumRecipe serum && serum.getPriority() == 100,
                "KubeJS serum DSL recipe must load with priority");
        helper.assertTrue(incubatorLoaded instanceof IncubatorOutputRecipe incubator
                        && incubator.getPriority() == 100
                        && incubator.matches(new ItemStack(Items.WHEAT_SEEDS)),
                "KubeJS incubator DSL recipe must load and match");

        GeneSpliceEvent splice = new GeneSpliceEvent(ItemStack.EMPTY, ItemStack.EMPTY,
                1, 1, 1, 0, 777, false, 0, "");
        MinecraftForge.EVENT_BUS.post(splice);
        helper.assertTrue(splice.getSpeed() == 9 && splice.getOffspringCount() == 2,
                "KubeJS geneSplice listener must modify genes and offspring count");

        CropMatureEvent crop = new CropMatureEvent(helper.getLevel(), new BlockPos(0, 77, 0),
                ItemStack.EMPTY, new ItemStack(Items.WHEAT));
        helper.assertTrue(MinecraftForge.EVENT_BUS.post(crop),
                "KubeJS cropMature cancel() must cancel the Forge event");

        SerumCraftEvent craft = new SerumCraftEvent(new ItemStack[]{new ItemStack(Items.DIRT)},
                new ItemStack(Items.DIAMOND), 5, serumId);
        MinecraftForge.EVENT_BUS.post(craft);
        helper.assertTrue(craft.getActivity() == 12 && craft.getOutput().is(Items.GOLD_INGOT),
                "KubeJS serumCraft listener must modify output and Activity");

        Player player = helper.makeMockSurvivalPlayer();
        SerumConsumeEvent consume = new SerumConsumeEvent(player,
                new ItemStack(ModItems.SYNAPTIC_SERUM_S01.get()),
                ((SynapticSerumItem) ModItems.SYNAPTIC_SERUM_S01.get()).getSerumEffect(), 5, 777, 0);
        MinecraftForge.EVENT_BUS.post(consume);
        helper.assertTrue(consume.getDuration() == 42,
                "KubeJS serumConsume listener must modify duration");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void serumEventOverrides(GameTestHelper helper) {
        SynapticSerumItem item = (SynapticSerumItem) ModItems.SYNAPTIC_SERUM_S01.get();

        Player activityPlayer = helper.makeMockSurvivalPlayer();
        ItemStack activityStack = new ItemStack(item, 2);
        ConsumeListener activityListener = new ConsumeListener(event -> {
            if (event.getEntity() == activityPlayer) {
                event.setActivity(15);
                ItemStack snapshot = event.getSerum();
                snapshot.setCount(0);
            }
        });
        MinecraftForge.EVENT_BUS.register(activityListener);
        try {
            item.finishUsingItem(activityStack, helper.getLevel(), activityPlayer);
        } finally {
            MinecraftForge.EVENT_BUS.unregister(activityListener);
        }
        MobEffectInstance activityEffect = activityPlayer.getEffect(item.getSerumEffect());
        helper.assertTrue(activityEffect != null, "Activity-only override must apply the serum effect");
        helper.assertTrue(activityEffect.getDuration() == SynapticSerumItem.getScaledDuration(Config.s01BaseDuration, 15),
                "Activity-only override must recompute duration");
        int expectedAmplifier = Math.min(SynapticSerumItem.getBaseAmplifier(15)
                + SynapticSerumItem.getActivityBonusAmplifier(15), Config.stackAmplifierCap);
        helper.assertTrue(activityEffect.getAmplifier() == expectedAmplifier,
                "Activity-only override must recompute the first amplifier");
        helper.assertTrue(activityStack.getCount() == 1, "Mutating the event serum snapshot must not mutate the consumed stack");
        helper.assertTrue(countInventoryItem(activityPlayer, Items.GLASS_BOTTLE) == 1,
                "Drinking from a serum stack must place one empty bottle in the inventory");

        Player explicitPlayer = helper.makeMockSurvivalPlayer();
        ItemStack explicitStack = new ItemStack(item, 2);
        ConsumeListener explicitListener = new ConsumeListener(event -> {
            if (event.getEntity() == explicitPlayer) {
                event.setActivity(15);
                event.setDuration(42);
                event.setAmplifier(6);
            }
        });
        MinecraftForge.EVENT_BUS.register(explicitListener);
        try {
            item.finishUsingItem(explicitStack, helper.getLevel(), explicitPlayer);
        } finally {
            MinecraftForge.EVENT_BUS.unregister(explicitListener);
        }
        MobEffectInstance explicitEffect = explicitPlayer.getEffect(item.getSerumEffect());
        helper.assertTrue(explicitEffect != null && explicitEffect.getDuration() == 42,
                "Explicit duration must override Activity recomputation");
        helper.assertTrue(explicitEffect.getAmplifier() == 6,
                "Explicit amplifier must override Activity recomputation");

        Player lastDosePlayer = helper.makeMockSurvivalPlayer();
        ItemStack lastDose = new ItemStack(item);
        ItemStack returned = item.finishUsingItem(lastDose, helper.getLevel(), lastDosePlayer);
        helper.assertTrue(returned.is(Items.GLASS_BOTTLE) && lastDose.isEmpty(),
                "The final serum dose must replace the held stack with a glass bottle");

        Player fullInventoryPlayer = helper.makeMockSurvivalPlayer();
        for (int slot = 0; slot < fullInventoryPlayer.getInventory().items.size(); slot++) {
            fullInventoryPlayer.getInventory().setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
        }
        ItemStack fullInventoryDose = new ItemStack(item, 2);
        item.finishUsingItem(fullInventoryDose, helper.getLevel(), fullInventoryPlayer);
        boolean droppedBottle = !helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                fullInventoryPlayer.getBoundingBox().inflate(2.0),
                entity -> entity.getItem().is(Items.GLASS_BOTTLE)).isEmpty();
        helper.assertTrue(fullInventoryDose.getCount() == 1 && droppedBottle,
                "A stacked serum must drop its empty bottle when the inventory is full");

        Player creativePlayer = helper.makeMockPlayer();
        creativePlayer.getAbilities().instabuild = true;
        ItemStack creativeDose = new ItemStack(item);
        ItemStack creativeReturned = item.finishUsingItem(creativeDose, helper.getLevel(), creativePlayer);
        helper.assertTrue(creativeReturned.is(item) && creativeReturned.getCount() == 1
                        && countInventoryItem(creativePlayer, Items.GLASS_BOTTLE) == 0,
                "Creative-mode serum use must neither consume serum nor return a bottle");

        Player cancelledPlayer = helper.makeMockSurvivalPlayer();
        ItemStack cancelledDose = new ItemStack(item);
        ConsumeListener cancelListener = new ConsumeListener(event -> {
            if (event.getEntity() == cancelledPlayer) event.setCanceled(true);
        });
        MinecraftForge.EVENT_BUS.register(cancelListener);
        ItemStack cancelledReturned;
        try {
            cancelledReturned = item.finishUsingItem(cancelledDose, helper.getLevel(), cancelledPlayer);
        } finally {
            MinecraftForge.EVENT_BUS.unregister(cancelListener);
        }
        helper.assertTrue(cancelledReturned.is(item) && cancelledReturned.getCount() == 1
                        && countInventoryItem(cancelledPlayer, Items.GLASS_BOTTLE) == 0,
                "Cancelled serum use must neither consume serum nor return a bottle");

        SerumConsumeEvent bounds = new SerumConsumeEvent(explicitPlayer, explicitStack, item.getSerumEffect(), 5, 20, 1);
        bounds.setActivity(100);
        bounds.setDuration(0);
        bounds.setAmplifier(-1);
        helper.assertTrue(bounds.getActivity() == 15 && bounds.getDuration() == 1 && bounds.getAmplifier() == 0,
                "Event setters must enforce their public bounds");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void serumStackingPreservesLevelAndDurationIdentity(GameTestHelper helper) {
        SynapticSerumItem item = (SynapticSerumItem) ModItems.SYNAPTIC_SERUM_S01.get();
        Player player = helper.makeMockSurvivalPlayer();
        ItemStack doses = new ItemStack(item, 3);
        int doseDuration = SynapticSerumItem.getScaledDuration(Config.s01BaseDuration,
                SynapticSerumItem.DEFAULT_ACTIVITY);
        int durationCap = SynapticSerumItem.getStackDurationCap(doses);

        item.finishUsingItem(doses, helper.getLevel(), player);
        MobEffectInstance first = player.getEffect(item.getSerumEffect());
        helper.assertTrue(first != null && first.getAmplifier() == 0 && first.getDuration() == doseDuration,
                "First default-Activity S-01 dose must start at level I with one full dose duration");

        item.finishUsingItem(doses, helper.getLevel(), player);
        MobEffectInstance second = player.getEffect(item.getSerumEffect());
        helper.assertTrue(second != null && second.getAmplifier() == 1,
                "Second same-serum dose must raise amplifier by one");
        helper.assertTrue(second.getDuration() == Math.min(doseDuration * 2, durationCap),
                "Second same-serum dose must add its complete duration");

        player.forceAddEffect(new MobEffectInstance(item.getSerumEffect(), durationCap,
                Config.stackAmplifierCap), null);
        int countBeforeBlockedDose = doses.getCount();
        item.finishUsingItem(doses, helper.getLevel(), player);
        helper.assertTrue(doses.getCount() == countBeforeBlockedDose,
                "A serum already at both level and duration caps must not consume another dose");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void removedAndExpiredSerumsStillApplyOverload(GameTestHelper helper) {
        Player milkPlayer = helper.makeMockSurvivalPlayer();
        milkPlayer.addEffect(new MobEffectInstance(ModEffects.SYNAPTIC_OVERCLOCK.get(), 200, 2));
        helper.assertTrue(milkPlayer.curePotionEffects(new ItemStack(Items.MILK_BUCKET)),
                "Milk must be able to end the beneficial serum effect");

        Player expiryPlayer = helper.makeMockSurvivalPlayer();
        MobEffectInstance expiring = new MobEffectInstance(ModEffects.METABOLIC_BOOST.get(), 1, 1);
        expiryPlayer.addEffect(expiring);
        MinecraftForge.EVENT_BUS.post(new MobEffectEvent.Expired(expiryPlayer, expiring));
        expiryPlayer.removeEffectNoUpdate(ModEffects.METABOLIC_BOOST.get());

        helper.runAfterDelay(4, () -> {
            MobEffectInstance milkOverload = milkPlayer.getEffect(ModEffects.NEURAL_OVERLOAD_S01.get());
            helper.assertTrue(milkOverload != null && milkOverload.getAmplifier() == 2,
                    "Removing S-01 with milk must still schedule source-specific overload");
            helper.assertTrue(milkOverload.getCurativeItems().isEmpty(),
                    "Ordinary curative items must not remove neural overload");
            MobEffectInstance expiryOverload = expiryPlayer.getEffect(ModEffects.NEURAL_OVERLOAD_S03.get());
            helper.assertTrue(expiryOverload != null && expiryOverload.getAmplifier() == 1,
                    "Natural S-03 expiration must schedule source-specific overload");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void bottlerReloadAndBlockedPriorityBehavior(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.SERUM_BOTTLER.get());
        SerumBottlerBlockEntity bottler = (SerumBottlerBlockEntity) helper.getBlockEntity(pos);

        CompoundTag removedRecipeState = new CompoundTag();
        removedRecipeState.putInt("Progress", 7);
        removedRecipeState.putInt("MaxProgress", 20);
        removedRecipeState.putString("RecipeId", "kubejs:removed_during_reload");
        removedRecipeState.put("Input0", new ItemStack(Items.STONE).save(new CompoundTag()));
        bottler.load(removedRecipeState);
        SerumBottlerBlockEntity.tick(helper.getLevel(), bottler.getBlockPos(), bottler.getBlockState(), bottler);
        helper.assertTrue(bottler.getProgress() == 0 && bottler.getMaxProgress() == 0,
                "Removed recipes must cancel in-progress bottling after reload");
        helper.assertTrue(bottler.getItem(0).is(Items.STONE),
                "Cancelling a removed recipe must not consume its input");

        if (ModList.get().isLoaded("kubejs")) {
            CompoundTag blockedOutputState = new CompoundTag();
            blockedOutputState.put("Input0", new ItemStack(Items.DIRT).save(new CompoundTag()));
            blockedOutputState.put("Output", new ItemStack(Items.GOLD_INGOT).save(new CompoundTag()));
            bottler.load(blockedOutputState);
            SerumBottlerBlockEntity.tick(helper.getLevel(), bottler.getBlockPos(), bottler.getBlockState(), bottler);
            helper.assertTrue(bottler.getMaxProgress() == 0,
                    "A blocked highest-priority output must not fall back to a lower-priority recipe");
            helper.assertTrue(bottler.getItem(0).is(Items.DIRT) && bottler.getOutput().is(Items.GOLD_INGOT),
                    "Blocked priority selection must preserve both input and existing output");
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void bottlerCraftingRecipeUsesRareEarthCenter(GameTestHelper helper) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(cybercultivator.MODID, "serum_bottler");
        Recipe<?> recipe = helper.getLevel().getRecipeManager().byKey(id).orElse(null);
        helper.assertTrue(recipe instanceof ShapedRecipe, "Serum bottler crafting recipe must be shaped");
        List<net.minecraft.world.item.crafting.Ingredient> ingredients = recipe.getIngredients();
        helper.assertTrue(ingredients.size() == 9, "Serum bottler crafting recipe must be a 3x3 pattern");
        helper.assertTrue(ingredients.get(1).test(new ItemStack(Items.REDSTONE)), "Top center must accept redstone");
        helper.assertTrue(ingredients.get(4).test(new ItemStack(ModItems.RARE_EARTH_DUST.get())),
                "Center must accept rare earth dust");
        helper.assertFalse(ingredients.get(4).test(new ItemStack(Items.REDSTONE)),
                "Legacy double-redstone layout must not match");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void spectrumMonocleHasDeterministicCraftingRecipe(GameTestHelper helper) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(cybercultivator.MODID, "spectrum_monocle");
        Recipe<?> recipe = helper.getLevel().getRecipeManager().byKey(id).orElse(null);
        helper.assertTrue(recipe instanceof ShapedRecipe, "Spectrum monocle recipe must be shaped");
        ShapedRecipe shaped = (ShapedRecipe) recipe;
        helper.assertTrue(shaped.getWidth() == 3 && shaped.getHeight() == 3,
                "Spectrum monocle recipe must retain the intended 3x3 silhouette");
        List<Ingredient> ingredients = shaped.getIngredients();
        helper.assertTrue(ingredients.get(0).test(new ItemStack(Items.IRON_NUGGET))
                        && ingredients.get(1).test(new ItemStack(Items.GLASS_PANE))
                        && ingredients.get(2).test(new ItemStack(Items.IRON_NUGGET)),
                "Monocle lens row must use iron nuggets around a glass pane");
        helper.assertTrue(ingredients.get(3).test(new ItemStack(ModItems.SILICON_SHARD.get()))
                        && ingredients.get(4).test(new ItemStack(ModItems.RARE_EARTH_DUST.get()))
                        && ingredients.get(5).test(new ItemStack(ModItems.SILICON_SHARD.get())),
                "Monocle analysis row must use silicon around rare earth dust");
        helper.assertTrue(ingredients.get(7).test(new ItemStack(Items.IRON_NUGGET)),
                "Monocle handle must use an iron nugget");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void materialStorageBlocksHaveReversibleRecipes(GameTestHelper helper) {
        Item[] materials = {
                ModItems.RAW_SILICON_CRYSTAL.get(), ModItems.SILICON_SHARD.get(),
                ModItems.RAW_RARE_EARTH.get(), ModItems.RARE_EARTH_DUST.get()
        };
        Item[] storageBlocks = {
                ModItems.RAW_SILICON_BLOCK_ITEM.get(), ModItems.SILICON_BLOCK_ITEM.get(),
                ModItems.RAW_RARE_EARTH_BLOCK_ITEM.get(), ModItems.RARE_EARTH_BLOCK_ITEM.get()
        };
        String[] compressionIds = {
                "raw_silicon_block", "silicon_block", "raw_rare_earth_block", "rare_earth_block"
        };
        String[] unpackIds = {
                "raw_silicon_crystal_from_block", "silicon_shard_from_block",
                "raw_rare_earth_from_block", "rare_earth_dust_from_block"
        };

        for (int index = 0; index < materials.length; index++) {
            Recipe<?> compression = helper.getLevel().getRecipeManager().byKey(
                    ResourceLocation.fromNamespaceAndPath(cybercultivator.MODID, compressionIds[index])).orElse(null);
            helper.assertTrue(compression instanceof ShapedRecipe,
                    compressionIds[index] + " must use a shaped compression recipe");
            ShapedRecipe shaped = (ShapedRecipe) compression;
            helper.assertTrue(shaped.getWidth() == 3 && shaped.getHeight() == 3,
                    compressionIds[index] + " must consume a full 3x3 material grid");
            for (Ingredient ingredient : shaped.getIngredients()) {
                helper.assertTrue(ingredient.test(new ItemStack(materials[index])),
                        compressionIds[index] + " must use only its matching material");
            }
            helper.assertTrue(shaped.getResultItem(helper.getLevel().registryAccess()).is(storageBlocks[index]),
                    compressionIds[index] + " must output its matching storage block");

            Recipe<?> unpack = helper.getLevel().getRecipeManager().byKey(
                    ResourceLocation.fromNamespaceAndPath(cybercultivator.MODID, unpackIds[index])).orElse(null);
            helper.assertTrue(unpack instanceof ShapelessRecipe,
                    unpackIds[index] + " must use a shapeless unpacking recipe");
            helper.assertTrue(unpack.getIngredients().size() == 1
                            && unpack.getIngredients().get(0).test(new ItemStack(storageBlocks[index])),
                    unpackIds[index] + " must consume its matching storage block");
            ItemStack unpackResult = unpack.getResultItem(helper.getLevel().registryAccess());
            helper.assertTrue(unpackResult.is(materials[index]) && unpackResult.getCount() == 9,
                    unpackIds[index] + " must return nine matching materials");
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void rawMineralsRequireRefining(GameTestHelper helper) {
        Item[] rawMaterials = {ModItems.RAW_SILICON_CRYSTAL.get(), ModItems.RAW_RARE_EARTH.get()};
        Item[] refinedMaterials = {ModItems.SILICON_SHARD.get(), ModItems.RARE_EARTH_DUST.get()};
        String[] rawNames = {"raw_silicon_crystal", "raw_rare_earth"};
        String[] refinedNames = {"silicon_shard", "rare_earth_dust"};

        for (int index = 0; index < rawMaterials.length; index++) {
            AbstractCookingRecipe smelting = cookingRecipe(helper,
                    refinedNames[index] + "_from_smelting_" + rawNames[index], SmeltingRecipe.class);
            assertRefiningRecipe(helper, smelting, rawMaterials[index], refinedMaterials[index], 200);

            AbstractCookingRecipe blasting = cookingRecipe(helper,
                    refinedNames[index] + "_from_blasting_" + rawNames[index], BlastingRecipe.class);
            assertRefiningRecipe(helper, blasting, rawMaterials[index], refinedMaterials[index], 100);
        }
        helper.succeed();
    }

    private static AbstractCookingRecipe cookingRecipe(GameTestHelper helper,
                                                        String recipePath,
                                                        Class<? extends AbstractCookingRecipe> expectedType) {
        Recipe<?> recipe = helper.getLevel().getRecipeManager().byKey(
                ResourceLocation.fromNamespaceAndPath(cybercultivator.MODID, recipePath)).orElse(null);
        helper.assertTrue(expectedType.isInstance(recipe), recipePath + " must use the expected cooking type");
        return (AbstractCookingRecipe) recipe;
    }

    private static void assertRefiningRecipe(GameTestHelper helper,
                                             AbstractCookingRecipe recipe,
                                             Item rawMaterial,
                                             Item refinedMaterial,
                                             int cookingTime) {
        helper.assertTrue(recipe.getIngredients().size() == 1
                        && recipe.getIngredients().get(0).test(new ItemStack(rawMaterial)),
                "Refining recipe must consume its matching raw mineral");
        helper.assertTrue(recipe.getResultItem(helper.getLevel().registryAccess()).is(refinedMaterial),
                "Refining recipe must output its matching refined material");
        helper.assertTrue(recipe.getCookingTime() == cookingTime,
                "Refining recipe must use the expected cooking time");
        helper.assertTrue(Math.abs(recipe.getExperience() - 0.7F) < 0.0001F,
                "Refining recipe must award 0.7 experience");
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 300)
    public static void cancelledCropMaturityStartsFreshCycle(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.BIO_INCUBATOR.get());
        BioIncubatorBlockEntity incubator = (BioIncubatorBlockEntity) helper.getBlockEntity(pos);
        ItemStack seed = new ItemStack(ModItems.FIBER_REED_SEEDS.get());
        GeneticSeedItem.setGene(seed, GeneticSeedItem.GENE_SPEED, 10);
        helper.assertTrue(incubator.tryInsertSeed(seed), "Test seed must be inserted");
        incubator.addNutrition(100);
        incubator.addPurity(100);
        incubator.addDataSignal(100);

        CropListener[] listenerHolder = new CropListener[1];
        CropListener listener = new CropListener(event -> {
            if (event.getLevel() != helper.getLevel() || !event.getPos().equals(helper.absolutePos(pos))) return;
            int nutritionAtEvent = incubator.getNutrition();
            int purityAtEvent = incubator.getPurity();
            event.setCanceled(true);
            MinecraftForge.EVENT_BUS.unregister(listenerHolder[0]);
            helper.runAfterDelay(1, () -> {
                helper.assertTrue(incubator.hasSeed(), "Cancelled maturity must preserve the seed");
                helper.assertTrue(incubator.getGrowthPercent() <= 2, "Cancelled maturity must reset growth progress");
                helper.assertTrue(nutritionAtEvent - incubator.getNutrition() <= 1,
                        "Cancelled maturity must not deduct the maturity nutrition cost");
                helper.assertTrue(purityAtEvent - incubator.getPurity() <= 1,
                        "Cancelled maturity must not deduct the maturity purity cost");
                helper.assertItemEntityNotPresent(ModItems.PLANT_FIBER.get(), pos, 4.0);
                helper.succeed();
            });
        });
        listenerHolder[0] = listener;
        MinecraftForge.EVENT_BUS.register(listener);
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void incubatorSpeedRemainderAndLegacyBottleMigration(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.BIO_INCUBATOR.get());
        BioIncubatorBlockEntity incubator = (BioIncubatorBlockEntity) helper.getBlockEntity(pos);
        incubator.addNutrition(100);
        incubator.addPurity(100);
        incubator.addDataSignal(100);

        int previousRate = 0;
        for (int speed = 1; speed <= 10; speed++) {
            ItemStack seed = new ItemStack(ModItems.FIBER_REED_SEEDS.get());
            GeneticSeedItem.setGene(seed, GeneticSeedItem.GENE_SPEED, speed);
            incubator.setItem(BioIncubatorBlockEntity.SEED_SLOT, seed);
            int rate = incubator.getCurrentGrowthRateMilli();
            helper.assertTrue(rate == 500 + speed * 150,
                    "Full-resource milli growth rate must follow the continuous Speed formula at " + speed);
            helper.assertTrue(CyberCultivatorAPI.getIncubatorGrowthRateMilli(helper.getLevel(),
                            helper.absolutePos(pos)) == rate,
                    "Public API exact growth query must match the machine at Speed " + speed);
            helper.assertTrue(rate > previousRate,
                    "Speed 1-10 must accelerate growth monotonically at " + speed);
            int expectedEta = (int) Math.ceil(Config.maturationThreshold * 1000.0D / rate / 20.0D);
            helper.assertTrue(incubator.getEstimatedSecondsRemaining() == expectedEta,
                    "ETA must use the same exact milli rate as actual growth at Speed " + speed);
            previousRate = rate;
        }

        CompoundTag state = incubator.saveWithoutMetadata();
        state.putInt("GrowthRemainderMilli", 875);
        BioIncubatorBlockEntity restored = new BioIncubatorBlockEntity(pos, incubator.getBlockState());
        restored.load(state);
        helper.assertTrue(restored.saveWithoutMetadata().getInt("GrowthRemainderMilli") == 875,
                "Fractional growth remainder must survive an NBT round-trip");
        restored.setItem(BioIncubatorBlockEntity.SEED_SLOT, ItemStack.EMPTY);
        helper.assertTrue(restored.saveWithoutMetadata().getInt("GrowthRemainderMilli") == 0,
                "Removing or replacing a seed must clear fractional growth remainder");

        Player player = helper.makeMockSurvivalPlayer();
        CompoundTag legacyState = new CompoundTag();
        legacyState.put("BottleOutput", new ItemStack(Items.GLASS_BOTTLE, 5).save(new CompoundTag()));
        incubator.load(legacyState);
        helper.assertTrue(incubator.getContainerSize() == 5
                        && incubator.getItem(BioIncubatorBlockEntity.BOTTLE_OUTPUT_SLOT).isEmpty(),
                "Legacy bottles must not be exposed as a sixth container slot");
        incubator.createMenu(17, player.getInventory(), player);
        helper.assertTrue(countInventoryItem(player, Items.GLASS_BOTTLE) == 5
                        && !incubator.saveWithoutMetadata().contains("BottleOutput"),
                "Opening an old incubator must migrate its hidden bottles into the player inventory exactly once");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void incubatorAutoInjectionAndSingleOutput(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.BIO_INCUBATOR.get());
        BioIncubatorBlockEntity incubator = (BioIncubatorBlockEntity) helper.getBlockEntity(pos);
        incubator.setItem(BioIncubatorBlockEntity.NUTRITION_SLOT,
                new ItemStack(ModItems.BIOCHEMICAL_SOLUTION.get(), 2));
        incubator.setItem(BioIncubatorBlockEntity.PURITY_SLOT,
                new ItemStack(ModItems.PURIFIED_WATER_BOTTLE.get(), 2));
        incubator.setItem(BioIncubatorBlockEntity.SIGNAL_SLOT,
                new ItemStack(ModItems.SILICON_SHARD.get(), 2));

        helper.assertTrue(incubator.getNutrition() == 0 && incubator.getPurity() == 0
                        && incubator.getDataSignal() == 0,
                "Resource stacks must wait for the first timed injection cycle");
        helper.assertTrue(incubator.getItem(BioIncubatorBlockEntity.NUTRITION_SLOT).getCount() == 2
                        && incubator.getItem(BioIncubatorBlockEntity.PURITY_SLOT).getCount() == 2
                        && incubator.getItem(BioIncubatorBlockEntity.SIGNAL_SLOT).getCount() == 2,
                "Inserting resource stacks must not fill the reservoirs immediately");
        helper.assertFalse(incubator.canPlaceItemThroughFace(BioIncubatorBlockEntity.SEED_SLOT,
                new ItemStack(ModItems.FIBER_REED_SEEDS.get()), Direction.NORTH),
                "Hoppers must not automate the seed slot");
        helper.assertTrue(incubator.canTakeItemThroughFace(BioIncubatorBlockEntity.RESOURCE_OUTPUT_SLOT,
                        new ItemStack(ModItems.PLANT_FIBER.get()), Direction.DOWN),
                "The bottom face must expose the resource output");
        helper.assertTrue(incubator.getContainerSize() == 5
                        && !incubator.canTakeItemThroughFace(BioIncubatorBlockEntity.BOTTLE_OUTPUT_SLOT,
                        ItemStack.EMPTY, Direction.DOWN),
                "The incubator must expose only five slots and no legacy bottle output");

        helper.runAfterDelay(BioIncubatorBlockEntity.INPUT_INJECTION_INTERVAL_TICKS / 2, () -> {
            helper.assertTrue(incubator.getNutrition() == 0 && incubator.getPurity() == 0
                            && incubator.getDataSignal() == 0,
                    "No resource may inject before the configured interval elapses");
        });
        helper.runAfterDelay(BioIncubatorBlockEntity.INPUT_INJECTION_INTERVAL_TICKS + 1, () -> {
            helper.assertTrue(incubator.getNutrition() == Config.nutritionInjectAmount
                            && incubator.getPurity() == Config.purityInjectAmount
                            && incubator.getDataSignal() == Config.dataSignalInjectAmount,
                    "Each timed cycle must inject at most one resource per N/P/D channel");
            helper.assertTrue(incubator.getItem(BioIncubatorBlockEntity.NUTRITION_SLOT).getCount() == 1
                            && incubator.getItem(BioIncubatorBlockEntity.PURITY_SLOT).getCount() == 1
                            && incubator.getItem(BioIncubatorBlockEntity.SIGNAL_SLOT).getCount() == 1,
                    "The first cycle must leave the second resource in every input slot");
            helper.assertTrue(incubator.getItem(BioIncubatorBlockEntity.BOTTLE_OUTPUT_SLOT).isEmpty(),
                    "Timed water injection must consume the bottle without a byproduct");
        });
        helper.runAfterDelay(BioIncubatorBlockEntity.INPUT_INJECTION_INTERVAL_TICKS * 2 + 2, () -> {
            helper.assertTrue(incubator.getNutrition() == Config.nutritionInjectAmount * 2
                            && incubator.getPurity() == Config.purityInjectAmount * 2
                            && incubator.getDataSignal() == Config.dataSignalInjectAmount * 2,
                    "The second resource in each channel must wait for the next interval");
            helper.assertTrue(incubator.getItem(BioIncubatorBlockEntity.NUTRITION_SLOT).isEmpty()
                            && incubator.getItem(BioIncubatorBlockEntity.PURITY_SLOT).isEmpty()
                            && incubator.getItem(BioIncubatorBlockEntity.SIGNAL_SLOT).isEmpty(),
                    "Both timed cycles must consume exactly two resources per channel");

            incubator.setItem(BioIncubatorBlockEntity.PURITY_SLOT,
                    new ItemStack(ModItems.PURIFIED_WATER_BOTTLE.get()));
            helper.assertTrue(incubator.getItem(BioIncubatorBlockEntity.PURITY_SLOT)
                            .is(ModItems.PURIFIED_WATER_BOTTLE.get()),
                    "New water must wait for the next timed injection cycle");
        });
        helper.runAfterDelay(BioIncubatorBlockEntity.INPUT_INJECTION_INTERVAL_TICKS * 3 + 3, () -> {
            helper.assertTrue(incubator.getItem(BioIncubatorBlockEntity.PURITY_SLOT).isEmpty()
                            && incubator.getItem(BioIncubatorBlockEntity.BOTTLE_OUTPUT_SLOT).isEmpty(),
                    "Waiting water must inject without producing a bottle output");

            ItemStack seed = new ItemStack(ModItems.FIBER_REED_SEEDS.get());
            GeneticSeedItem.setGene(seed, GeneticSeedItem.GENE_SPEED, 10);
            helper.assertTrue(incubator.tryInsertSeed(seed), "A test seed must enter the incubator");
            CompoundTag nearMature = incubator.saveWithoutMetadata();
            nearMature.putInt("GrowthProgress", Config.maturationThreshold - 1);
            incubator.load(nearMature);
            BioIncubatorBlockEntity.tick(helper.getLevel(), incubator.getBlockPos(), incubator.getBlockState(), incubator);
            helper.assertTrue(incubator.getItem(BioIncubatorBlockEntity.RESOURCE_OUTPUT_SLOT)
                            .is(ModItems.PLANT_FIBER.get()),
                    "A mature crop must enter the resource output instead of spawning in the world");
            helper.assertItemEntityNotPresent(ModItems.PLANT_FIBER.get(), pos, 4.0);
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void splicerStartsAutomatically(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.GENE_SPLICER.get());
        GeneSplicerBlockEntity splicer = (GeneSplicerBlockEntity) helper.getBlockEntity(pos);
        ItemStack first = new ItemStack(ModItems.FIBER_REED_SEEDS.get());
        ItemStack second = new ItemStack(ModItems.PROTEIN_SOY_SEEDS.get());
        GeneticSeedItem.setGenes(first, 2, 5, 8);
        GeneticSeedItem.setGenes(second, 7, 5, 4);
        first.getOrCreateTag().putInt(GeneticSeedItem.GENE_GENERATION, 2);
        second.getOrCreateTag().putInt(GeneticSeedItem.GENE_GENERATION, 4);
        splicer.setItem(GeneSplicerBlockEntity.SEED_A_SLOT, first);
        splicer.setItem(GeneSplicerBlockEntity.SEED_B_SLOT, second);
        int expectedMutationPermille = (int) Math.round(GeneSpliceRules.mutationChance(4, 5) * 1000.0D);
        int expectedTwinPermille = (int) Math.round(GeneSpliceRules.totalTwinChance(
                GeneSpliceRules.mutationChance(4, 5), GeneSpliceRules.normalTwinChance(4)) * 1000.0D);
        helper.assertTrue(splicer.getPredictedGeneration() == 5,
                "GUI generation preview must use the higher parent generation plus one");
        helper.assertTrue(splicer.getPredictedMutationPermille() == expectedMutationPermille,
                "GUI mutation preview must match the server-side splice formula");
        helper.assertTrue(splicer.getPredictedTwinPermille() == expectedTwinPermille,
                "GUI twin preview must include mutation-guaranteed twins");
        helper.assertTrue(splicer.isSplicing() && splicer.getOutput().isEmpty(),
                "Adding the second parent must automatically start timed splicing");
        GeneSplicerBlockEntity.tick(helper.getLevel(), pos, splicer.getBlockState(), splicer);
        CompoundTag savedSplice = splicer.saveWithoutMetadata();
        GeneSplicerBlockEntity restoredSplice =
                new GeneSplicerBlockEntity(splicer.getBlockPos(), splicer.getBlockState());
        restoredSplice.load(savedSplice);
        helper.assertTrue(restoredSplice.isSplicing() && restoredSplice.getSpliceProgress() == 1,
                "Active splice progress must survive an NBT round-trip");
        for (int tick = 1; tick < GeneSplicerBlockEntity.SPLICE_DURATION_TICKS - 1; tick++) {
            GeneSplicerBlockEntity.tick(helper.getLevel(), pos, splicer.getBlockState(), splicer);
        }
        helper.assertTrue(splicer.getOutput().isEmpty()
                        && splicer.getSpliceProgress() == GeneSplicerBlockEntity.SPLICE_DURATION_TICKS - 1,
                "Splicing must not complete before the configured wait time");
        GeneSplicerBlockEntity.tick(helper.getLevel(), pos, splicer.getBlockState(), splicer);
        helper.assertFalse(splicer.isSplicing(), "Splicing state must clear on completion");
        helper.assertFalse(splicer.getOutput().isEmpty(), "Timed splicing must create an output");
        splicer.extractOutput();
        helper.assertTrue(splicer.getSeedA().isEmpty() && splicer.getSeedB().isEmpty(),
                "Taking the output must clear both parents");

        splicer.setItem(GeneSplicerBlockEntity.SEED_A_SLOT, first);
        splicer.setItem(GeneSplicerBlockEntity.SEED_B_SLOT, second);
        helper.assertTrue(splicer.isSplicing(), "A second pair of parents must start automatically");
        GeneSplicerBlockEntity.tick(helper.getLevel(), pos, splicer.getBlockState(), splicer);
        splicer.setItem(GeneSplicerBlockEntity.SEED_B_SLOT, ItemStack.EMPTY);
        helper.assertFalse(splicer.isSplicing(), "Removing a parent must cancel the active splice");
        helper.assertTrue(splicer.getSpliceProgress() == 0,
                "Removing a parent must reset the splice progress");
        splicer.setItem(GeneSplicerBlockEntity.SEED_B_SLOT, second);
        helper.assertTrue(splicer.isSplicing() && splicer.getSpliceProgress() == 0,
                "Replacing the missing parent must start a fresh splice automatically");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void machineMenuQuickMoveCommitsMachineTransactions(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();

        BlockPos splicerPos = new BlockPos(1, 1, 1);
        helper.setBlock(splicerPos, ModBlocks.GENE_SPLICER.get());
        GeneSplicerBlockEntity splicer = (GeneSplicerBlockEntity) helper.getBlockEntity(splicerPos);
        splicer.setItem(GeneSplicerBlockEntity.SEED_A_SLOT,
                new ItemStack(ModItems.FIBER_REED_SEEDS.get()));
        splicer.setItem(GeneSplicerBlockEntity.SEED_B_SLOT,
                new ItemStack(ModItems.PROTEIN_SOY_SEEDS.get()));
        for (int tick = 0; tick < GeneSplicerBlockEntity.SPLICE_DURATION_TICKS; tick++) {
            GeneSplicerBlockEntity.tick(helper.getLevel(), splicerPos, splicer.getBlockState(), splicer);
        }
        helper.assertFalse(splicer.getOutput().isEmpty(), "Splicer test setup must produce a child seed");
        helper.assertTrue(splicer.getSeedA().isEmpty() && splicer.getSeedB().isEmpty(),
                "Successful splicing must consume both parents immediately");
        GeneSplicerMenu splicerMenu = (GeneSplicerMenu) splicer.createMenu(1, player.getInventory(), player);
        ItemStack committedParent = splicerMenu.quickMoveStack(player, GeneSplicerBlockEntity.SEED_A_SLOT);
        helper.assertTrue(committedParent.isEmpty() && !splicer.getOutput().isEmpty(),
                "Consumed parents must not be recoverable before the child is collected");
        helper.assertFalse(splicerMenu.getSlot(GeneSplicerBlockEntity.SEED_A_SLOT)
                        .mayPlace(new ItemStack(ModItems.FIBER_REED_SEEDS.get())),
                "Parent slots must reject new seeds while an output is waiting");
        helper.assertTrue(splicer.removeItem(GeneSplicerBlockEntity.SEED_B_SLOT, 1).isEmpty()
                        && splicer.extractLastInput().isEmpty(),
                "Container and block interaction paths must not recover consumed parents");
        ItemStack child = splicerMenu.quickMoveStack(player, GeneSplicerBlockEntity.OUTPUT_SLOT);
        helper.assertFalse(child.isEmpty(), "Shift-moving the child must transfer it to the player");
        helper.assertTrue(splicer.getOutput().isEmpty()
                        && splicer.getSeedA().isEmpty() && splicer.getSeedB().isEmpty(),
                "Shift-moving the child must leave the completed transaction empty");

        CompoundTag legacySplicerState = new CompoundTag();
        legacySplicerState.put("SeedA", new ItemStack(ModItems.FIBER_REED_SEEDS.get()).save(new CompoundTag()));
        legacySplicerState.put("SeedB", new ItemStack(ModItems.PROTEIN_SOY_SEEDS.get()).save(new CompoundTag()));
        legacySplicerState.put("Output", child.save(new CompoundTag()));
        GeneSplicerBlockEntity migratedSplicer =
                new GeneSplicerBlockEntity(splicer.getBlockPos(), splicer.getBlockState());
        migratedSplicer.load(legacySplicerState);
        helper.assertTrue(migratedSplicer.getSeedA().isEmpty() && migratedSplicer.getSeedB().isEmpty()
                        && !migratedSplicer.getOutput().isEmpty(),
                "Legacy completed splices must discard retained parents during load");

        BlockPos bottlerPos = new BlockPos(3, 1, 1);
        helper.setBlock(bottlerPos, ModBlocks.SERUM_BOTTLER.get());
        SerumBottlerBlockEntity bottler = (SerumBottlerBlockEntity) helper.getBlockEntity(bottlerPos);
        bottler.setItem(0, new ItemStack(ModItems.PLANT_FIBER.get()));
        bottler.setItem(1, new ItemStack(ModItems.INDUSTRIAL_ETHANOL.get()));
        bottler.setItem(2, new ItemStack(ModItems.BIOCHEMICAL_SOLUTION.get()));
        SerumBottlerBlockEntity.tick(helper.getLevel(), bottlerPos, bottler.getBlockState(), bottler);
        helper.assertTrue(bottler.getMaxProgress() > 0, "Bottler test setup must start processing");
        SerumBottlerMenu bottlerMenu = (SerumBottlerMenu) bottler.createMenu(2, player.getInventory(), player);
        ItemStack ingredient = bottlerMenu.quickMoveStack(player, 0);
        helper.assertFalse(ingredient.isEmpty(), "Shift-moving a bottler input must transfer it");
        helper.assertTrue(bottler.getProgress() == 0 && bottler.getMaxProgress() == 0,
                "Shift-moving a bottler input must cancel processing immediately");

        BlockPos condenserPos = new BlockPos(5, 1, 1);
        helper.setBlock(condenserPos, ModBlocks.ATMOSPHERIC_CONDENSER.get());
        AtmosphericCondenserBlockEntity condenser =
                (AtmosphericCondenserBlockEntity) helper.getBlockEntity(condenserPos);
        CompoundTag condenserState = new CompoundTag();
        condenserState.putInt("Progress", 599);
        condenserState.put("Output", new ItemStack(ModItems.PURIFIED_WATER_BOTTLE.get(), 4)
                .save(new CompoundTag()));
        condenser.load(condenserState);
        AtmosphericCondenserMenu condenserMenu =
                (AtmosphericCondenserMenu) condenser.createMenu(3, player.getInventory(), player);
        ItemStack water = condenserMenu.quickMoveStack(player, AtmosphericCondenserBlockEntity.OUTPUT_SLOT);
        helper.assertTrue(water.getCount() == 4 && condenser.getOutput().isEmpty(),
                "Shift-moving condenser stock must transfer the complete output");
        helper.assertTrue(condenser.getProgress() == 599,
                "Shift-moving condenser stock must preserve the active production progress");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void guiStateCancellationAndCondenserPersistence(GameTestHelper helper) {
        BlockPos bottlerPos = new BlockPos(1, 1, 1);
        helper.setBlock(bottlerPos, ModBlocks.SERUM_BOTTLER.get());
        SerumBottlerBlockEntity bottler = (SerumBottlerBlockEntity) helper.getBlockEntity(bottlerPos);
        CompoundTag processing = new CompoundTag();
        processing.putInt("Progress", 7);
        processing.putInt("MaxProgress", 300);
        processing.putString("RecipeId", SerumRecipeIds.S01_BOTTLING.toString());
        bottler.load(processing);
        CompoundTag resavedBeforeTick = bottler.saveWithoutMetadata();
        helper.assertTrue(SerumRecipeIds.S01_BOTTLING.toString()
                        .equals(resavedBeforeTick.getString("RecipeId")),
                "A loaded bottler must preserve its pending recipe ID if saved before the next tick");
        bottler.setItem(0, new ItemStack(Items.DIRT));
        helper.assertTrue(bottler.getProgress() == 0 && bottler.getMaxProgress() == 0,
                "Changing a bottler input must cancel the cached process");

        CompoundTag orphanedProcessing = new CompoundTag();
        orphanedProcessing.putInt("Progress", 7);
        orphanedProcessing.putInt("MaxProgress", 300);
        bottler.load(orphanedProcessing);
        helper.assertTrue(bottler.getProgress() == 0 && bottler.getMaxProgress() == 0,
                "Processing state without a recipe ID must be discarded during load");

        BlockPos condenserPos = new BlockPos(3, 1, 1);
        helper.setBlock(condenserPos, ModBlocks.ATMOSPHERIC_CONDENSER.get());
        AtmosphericCondenserBlockEntity condenser =
                (AtmosphericCondenserBlockEntity) helper.getBlockEntity(condenserPos);
        CompoundTag inProgress = new CompoundTag();
        inProgress.putInt("Progress", 10);
        inProgress.putBoolean("AutoInject", false);
        inProgress.putBoolean("Paused", true);
        condenser.load(inProgress);
        condenser.setItem(AtmosphericCondenserBlockEntity.BOTTLE_INPUT_SLOT,
                new ItemStack(Items.GLASS_BOTTLE, 2));
        AtmosphericCondenserBlockEntity.tick(helper.getLevel(), condenserPos, condenser.getBlockState(), condenser);
        helper.assertTrue(condenser.getProgress() == 11,
                "Legacy pause state must be ignored after condenser controls are removed");
        CompoundTag saved = condenser.saveWithoutMetadata();
        helper.assertFalse(saved.contains("AutoInject") || saved.contains("Paused"),
                "Removed condenser controls must not be written back to NBT");
        AtmosphericCondenserBlockEntity restored =
                new AtmosphericCondenserBlockEntity(condenser.getBlockPos(), condenser.getBlockState());
        restored.load(saved);
        helper.assertTrue(restored.getProgress() == 11 && restored.getBottleCount() == 2,
                "Glass bottle input must survive an NBT round-trip");

        CompoundTag legacyOverstock = new CompoundTag();
        legacyOverstock.putInt("Progress", 100);
        legacyOverstock.put("BottleInput", new ItemStack(Items.GLASS_BOTTLE, 2).save(new CompoundTag()));
        legacyOverstock.put("Output", new ItemStack(ModItems.PURIFIED_WATER_BOTTLE.get(), 32)
                .save(new CompoundTag()));
        restored.load(legacyOverstock);
        helper.assertTrue(restored.getMaxStock() == 16 && restored.getStock() == 32,
                "Old 17-32 bottle condenser stacks must survive load without truncation");
        AtmosphericCondenserBlockEntity.tick(helper.getLevel(), condenserPos,
                restored.getBlockState(), restored);
        helper.assertTrue(restored.getProgress() == 100 && restored.getStock() == 32,
                "An overstocked condenser must pause rather than discard stock or cycle progress");
        ItemStack hopperExtracted = restored.removeItem(AtmosphericCondenserBlockEntity.OUTPUT_SLOT, 20);
        helper.assertTrue(hopperExtracted.getCount() == 20 && restored.getStock() == 12
                        && restored.getProgress() == 100,
                "Hopper extraction must preserve the next-cycle progress while reducing legacy overstock");
        AtmosphericCondenserBlockEntity.tick(helper.getLevel(), condenserPos,
                restored.getBlockState(), restored);
        helper.assertTrue(restored.getProgress() == 101,
                "Production must resume naturally once legacy stock falls below the new 16-bottle cap");
        restored.extractOutput();
        helper.assertTrue(restored.getProgress() == 101,
                "Manual extraction must not reset the next condensation cycle");
        AtmosphericCondenserBlockEntity legacy =
                new AtmosphericCondenserBlockEntity(condenser.getBlockPos(), condenser.getBlockState());
        legacy.load(new CompoundTag());
        helper.assertTrue(legacy.getProgress() == 0 && legacy.getBottleInput().isEmpty(),
                "Legacy NBT without condenser controls must remain loadable");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void atmosphericCondenserConsumesGlassBottle(GameTestHelper helper) {
        BlockPos condenserPos = new BlockPos(1, 1, 1);
        helper.setBlock(condenserPos, ModBlocks.ATMOSPHERIC_CONDENSER.get());
        AtmosphericCondenserBlockEntity condenser =
                (AtmosphericCondenserBlockEntity) helper.getBlockEntity(condenserPos);

        AtmosphericCondenserBlockEntity.tick(helper.getLevel(), condenserPos, condenser.getBlockState(), condenser);
        helper.assertTrue(condenser.getProgress() == 0 && condenser.getOutput().isEmpty(),
                "A condenser without a glass bottle must not start production");
        helper.assertTrue(condenser.canPlaceItem(AtmosphericCondenserBlockEntity.BOTTLE_INPUT_SLOT,
                        new ItemStack(Items.GLASS_BOTTLE))
                        && !condenser.canPlaceItem(AtmosphericCondenserBlockEntity.BOTTLE_INPUT_SLOT,
                        new ItemStack(Items.DIRT)),
                "Only glass bottles may enter the condenser input slot");
        helper.assertTrue(condenser.getSlotsForFace(Direction.UP).length == 1
                        && condenser.getSlotsForFace(Direction.UP)[0]
                        == AtmosphericCondenserBlockEntity.BOTTLE_INPUT_SLOT,
                "Top automation must expose only the glass bottle input");
        helper.assertTrue(condenser.getSlotsForFace(Direction.DOWN).length == 1
                        && condenser.getSlotsForFace(Direction.DOWN)[0]
                        == AtmosphericCondenserBlockEntity.OUTPUT_SLOT,
                "Bottom automation must expose only purified water output");
        helper.assertTrue(condenser.canPlaceItemThroughFace(AtmosphericCondenserBlockEntity.BOTTLE_INPUT_SLOT,
                        new ItemStack(Items.GLASS_BOTTLE), Direction.NORTH)
                        && !condenser.canPlaceItemThroughFace(AtmosphericCondenserBlockEntity.BOTTLE_INPUT_SLOT,
                        new ItemStack(Items.GLASS_BOTTLE), Direction.DOWN),
                "Side and top automation may insert bottles, while bottom automation may not");

        CompoundTag ready = new CompoundTag();
        ready.putInt("Progress", 599);
        ready.put("BottleInput", new ItemStack(Items.GLASS_BOTTLE, 2).save(new CompoundTag()));
        condenser.load(ready);
        AtmosphericCondenserBlockEntity.tick(helper.getLevel(), condenserPos, condenser.getBlockState(), condenser);
        helper.assertTrue(condenser.getProgress() == 0,
                "Completing a bottle-backed condensing cycle must reset progress");
        helper.assertTrue(condenser.getBottleCount() == 1,
                "Completing a condensing cycle must consume exactly one glass bottle");
        helper.assertTrue(condenser.getOutput().is(ModItems.PURIFIED_WATER_BOTTLE.get())
                        && condenser.getOutput().getCount() == 1,
                "Completing a condensing cycle must produce exactly one purified water bottle");
        helper.assertTrue(condenser.canTakeItemThroughFace(AtmosphericCondenserBlockEntity.OUTPUT_SLOT,
                        condenser.getItem(AtmosphericCondenserBlockEntity.OUTPUT_SLOT), Direction.DOWN)
                        && !condenser.canTakeItemThroughFace(AtmosphericCondenserBlockEntity.OUTPUT_SLOT,
                        condenser.getItem(AtmosphericCondenserBlockEntity.OUTPUT_SLOT), Direction.UP),
                "Bottom and side automation may extract water, while top automation may not");
        helper.succeed();
    }

    // ========== v1.1.7 multiplayer-stability fix regression tests ==========

    @GameTest(template = EMPTY_TEMPLATE)
    public static void deepslateOreVariantsAreRegistered(GameTestHelper helper) {
        var blocks = new net.minecraft.world.level.block.Block[]{
                ModBlocks.DEEPSLATE_SILICON_ORE.get(),
                ModBlocks.DEEPSLATE_RARE_EARTH_ORE.get()
        };
        var items = new BlockItem[]{
                (BlockItem) ModItems.DEEPSLATE_SILICON_ORE_ITEM.get(),
                (BlockItem) ModItems.DEEPSLATE_RARE_EARTH_ORE_ITEM.get()
        };
        for (int i = 0; i < blocks.length; i++) {
            helper.assertTrue(blocks[i].defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE),
                    "Deepslate ore must be mineable with a pickaxe");
            helper.assertTrue(blocks[i].defaultBlockState().is(BlockTags.NEEDS_STONE_TOOL),
                    "Deepslate ore must require a stone tool");
            helper.assertTrue(items[i].getBlock() == blocks[i],
                    "Deepslate ore BlockItem must point at its registered block");
        }
        helper.assertTrue(ModBlocks.SILICON_ORE.getId().equals(
                        ResourceLocation.fromNamespaceAndPath(cybercultivator.MODID, "silicon_ore")),
                "Existing silicon ore ID must remain unchanged");
        helper.assertTrue(ModBlocks.RARE_EARTH_ORE.getId().equals(
                        ResourceLocation.fromNamespaceAndPath(cybercultivator.MODID, "rare_earth_ore")),
                "Existing rare earth ore ID must remain unchanged");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void neuralOverloadVariantsAreRegistered(GameTestHelper helper) {
        // 修复 4：神经过载来源丢失和串线 —— 三个独立效果必须全部注册
        String[] ids = {"neural_overload", "neural_overload_s01", "neural_overload_s02", "neural_overload_s03"};
        byte[][] textures = new byte[ids.length][];
        int textureIndex = 0;
        for (String id : ids) {
            ResourceLocation key = ResourceLocation.fromNamespaceAndPath(cybercultivator.MODID, id);
            var effect = net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getValue(key);
            helper.assertTrue(effect != null, "NeuralOverload variant must be registered: " + id);
            String texturePath = "assets/cybercultivator/textures/mob_effect/" + id + ".png";
            textures[textureIndex++] = readResourceBytes(texturePath);
            helper.assertTrue(textures[textureIndex - 1].length > 0,
                    "NeuralOverload variant must have its own effect-bar texture: " + id);
        }
        for (int first = 0; first < textures.length; first++) {
            for (int second = first + 1; second < textures.length; second++) {
                helper.assertTrue(!Arrays.equals(textures[first], textures[second]),
                        "NeuralOverload variants must use visually distinct texture files: "
                                + ids[first] + " / " + ids[second]);
            }
        }
        // 三个子类必须各自是独立类型，而非共享旧 SOURCE_MAP 的旧 NeuralOverloadEffect
        helper.assertTrue(ModEffects.NEURAL_OVERLOAD_S01.get() != ModEffects.NEURAL_OVERLOAD.get(),
                "S-01 overload must be a distinct effect instance");
        helper.assertTrue(ModEffects.NEURAL_OVERLOAD_S02.get() != ModEffects.NEURAL_OVERLOAD.get(),
                "S-02 overload must be a distinct effect instance");
        helper.assertTrue(ModEffects.NEURAL_OVERLOAD_S03.get() != ModEffects.NEURAL_OVERLOAD.get(),
                "S-03 overload must be a distinct effect instance");

        String[] beneficialIds = {"synaptic_overclock", "visual_enhancement", "metabolic_boost"};
        byte[][] beneficialTextures = new byte[beneficialIds.length][];
        for (int i = 0; i < beneficialIds.length; i++) {
            beneficialTextures[i] = readResourceBytes(
                    "assets/cybercultivator/textures/mob_effect/" + beneficialIds[i] + ".png");
            byte[] png = beneficialTextures[i];
            helper.assertTrue(png.length > 24
                            && png[16] == 0 && png[17] == 0 && png[18] == 0 && png[19] == 18
                            && png[20] == 0 && png[21] == 0 && png[22] == 0 && png[23] == 18,
                    "Beneficial effect icon must be an 18x18 PNG: " + beneficialIds[i]);
        }
        for (int first = 0; first < beneficialTextures.length; first++) {
            for (int second = first + 1; second < beneficialTextures.length; second++) {
                helper.assertTrue(!Arrays.equals(beneficialTextures[first], beneficialTextures[second]),
                        "Beneficial effects must use distinct icon files: "
                                + beneficialIds[first] + " / " + beneficialIds[second]);
            }
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void optionalPatchouliGuideLifecycle(GameTestHelper helper) {
        ResourceLocation recipeId = ResourceLocation.fromNamespaceAndPath(
                cybercultivator.MODID, "bio_synthesis_guide");
        Recipe<?> recipe = helper.getLevel().getRecipeManager().byKey(recipeId).orElse(null);
        if (!ModList.get().isLoaded("patchouli")) {
            helper.assertTrue(recipe == null,
                    "Guide recovery recipe must be skipped cleanly when Patchouli is absent");
            helper.succeed();
            return;
        }

        helper.assertTrue(recipe != null,
                "Guide recovery recipe must load when Patchouli is present");
        Player player = helper.makeMockSurvivalPlayer();
        helper.assertTrue(PatchouliGuideCompat.tryGrantGuide(player)
                        && countPatchouliGuides(player) == 1,
                "First login with Patchouli must grant the Bio-Synthesis guide directly to the inventory");
        helper.assertTrue(!PatchouliGuideCompat.tryGrantGuide(player)
                        && countPatchouliGuides(player) == 1,
                "Repeated login must not grant a duplicate guide");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void neuralOverloadS01AppliesFatigueAndHighLevelWither(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        player.getInventory().clearContent();
        ModEffects.NEURAL_OVERLOAD_S01.get().applyEffectTick(player, 7);
        helper.assertTrue(player.getEffect(MobEffects.WEAKNESS) != null, "S-01 overload must apply Weakness");
        helper.assertTrue(player.getEffect(MobEffects.DIG_SLOWDOWN) != null, "S-01 overload must apply Mining Fatigue");
        helper.assertTrue(player.getEffect(MobEffects.WITHER) != null, "High-level S-01 overload must apply Wither I");
        helper.assertTrue(player.getEffect(MobEffects.HUNGER) == null, "S-01 overload must no longer apply Hunger");
        helper.assertTrue(player.getEffect(MobEffects.BLINDNESS) == null, "S-01 overload must not apply Blindness (S-02 crosstalk)");
        helper.assertTrue(player.getEffect(MobEffects.POISON) == null, "S-01 overload must not apply Poison (S-03 crosstalk)");
        helper.assertTrue(player.getEffect(MobEffects.MOVEMENT_SLOWDOWN) == null,
                "S-01 overload must not apply Slowness (legacy overload crosstalk)");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void neuralOverloadS02AppliesSensoryCrash(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        player.getInventory().clearContent();
        player.addEffect(new MobEffectInstance(ModEffects.NEURAL_OVERLOAD_S02.get(), 440, 7));
        ModEffects.NEURAL_OVERLOAD_S02.get().applyEffectTick(player, 7);
        helper.assertTrue(player.getEffect(MobEffects.BLINDNESS) != null
                        && player.getEffect(MobEffects.BLINDNESS).getDuration() > 400,
                "S-02 overload Blindness must follow the parent duration instead of restarting at 21 ticks");
        helper.assertTrue(player.getEffect(MobEffects.CONFUSION) != null, "High-level S-02 overload must apply Nausea");
        helper.assertTrue(player.getEffect(MobEffects.MOVEMENT_SLOWDOWN) != null,
                "Level VII-VIII S-02 overload must apply Slowness I");
        helper.assertTrue(player.getEffect(MobEffects.HUNGER) == null, "S-02 overload must no longer apply Hunger");
        helper.assertTrue(player.getEffect(MobEffects.WITHER) == null, "S-02 overload must not apply Wither (S-01 crosstalk)");
        helper.assertTrue(player.getEffect(MobEffects.POISON) == null, "S-02 overload must not apply Poison (S-03 crosstalk)");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void neuralOverloadS03AppliesMetabolicCrash(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        player.getInventory().clearContent();
        ModEffects.NEURAL_OVERLOAD_S03.get().applyEffectTick(player, 7);
        helper.assertTrue(player.getEffect(MobEffects.MOVEMENT_SLOWDOWN) != null, "S-03 overload must apply Slowness");
        helper.assertTrue(player.getEffect(MobEffects.HUNGER) != null, "S-03 overload must apply Hunger");
        helper.assertTrue(player.getEffect(MobEffects.POISON) != null
                        && player.getEffect(MobEffects.POISON).getAmplifier() == 1,
                "Level VIII S-03 overload must apply Poison II");
        helper.assertTrue(player.getEffect(MobEffects.WITHER) == null, "S-03 overload must not apply Wither (S-01 crosstalk)");
        helper.assertTrue(player.getEffect(MobEffects.BLINDNESS) == null, "S-03 overload must not apply Blindness (S-02 crosstalk)");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void s02SerumDoesNotApplyGlowingToNearbyEntities(GameTestHelper helper) {
        // 修复 3：S-02 私有轮廓 —— 不再使用 MobEffects.GLOWING，避免对附近实体造成全服可见的串扰
        Player drinker = helper.makeMockSurvivalPlayer();
        net.minecraft.world.entity.animal.Cow nearby = helper.spawn(net.minecraft.world.entity.EntityType.COW,
                new BlockPos(2, 1, 2));
        // 直接调用 applyEffectTick 绕过按等级调节的扫描节流，验证 S-02 内部逻辑
        try {
            ModEffects.VISUAL_ENHANCEMENT.get().applyEffectTick(drinker, 0);
        } catch (Exception ignored) {
            // 模拟玩家可能没有完整网络连接，发包异常不影响 GLOWING 副作用的断言
        }
        helper.assertTrue(drinker.getEffect(MobEffects.NIGHT_VISION) != null,
                "S-02 must still grant Night Vision to the drinker");
        helper.assertTrue(drinker.getEffect(MobEffects.NIGHT_VISION).getDuration() > 200,
                "S-02 Night Vision refresh must remain above the vanilla flicker threshold");
        helper.assertTrue(drinker.getEffect(MobEffects.FIRE_RESISTANCE) == null,
                "S-02 must no longer grant unrelated Fire Resistance");
        helper.assertTrue(com.TKCCOPL.effect.VisualEnhancementEffect.getScanRange(7, 64) == 64.0D
                        && com.TKCCOPL.effect.VisualEnhancementEffect.getScanInterval(7) == 32,
                "S-02 level VIII must reach 64 blocks and refresh every 32 ticks");
        helper.assertTrue(nearby.getEffect(MobEffects.GLOWING) == null,
                "S-02 must not apply GLOWING to nearby entities (v1.1.7 regression)");
        helper.assertTrue(drinker.getEffect(MobEffects.GLOWING) == null,
                "S-02 must not apply GLOWING to the drinker either");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void s02NightVisionDoesNotFlickerOrOutliveItsParent(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        player.addEffect(new MobEffectInstance(ModEffects.VISUAL_ENHANCEMENT.get(), 200, 0));
        MobEffectInstance nightVision = player.getEffect(MobEffects.NIGHT_VISION);
        helper.assertTrue(nightVision != null && nightVision.getDuration() > 200,
                "S-02 must apply Night Vision immediately above the vanilla flicker threshold");

        player.removeEffect(ModEffects.VISUAL_ENHANCEMENT.get());
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(player.getEffect(MobEffects.NIGHT_VISION) == null,
                    "S-02-owned Night Vision must be removed after the parent effect ends");
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void serumPrimaryEffectsScaleThroughLevelEight(GameTestHelper helper) {
        Player s01Player = helper.makeMockSurvivalPlayer();
        double baseAttackSpeed = s01Player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED);
        s01Player.addEffect(new MobEffectInstance(ModEffects.SYNAPTIC_OVERCLOCK.get(), 200, 7));
        helper.assertTrue(s01Player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED)
                        > baseAttackSpeed * 1.44D,
                "S-01 level VIII must immediately grant 45% attack speed");
        helper.assertTrue(s01Player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE)
                        >= 0.45D,
                "S-01 level VIII must grant 45% knockback resistance");

        Player s03Player = helper.makeMockSurvivalPlayer();
        s03Player.setHealth(10.0F);
        s03Player.getFoodData().setFoodLevel(20);
        ModEffects.METABOLIC_BOOST.get().applyEffectTick(s03Player, 7);
        helper.assertTrue(Math.abs(s03Player.getHealth() - 14.5F) < 0.001F,
                "S-03 level VIII must heal 4.5 HP per second while food is available");
        s03Player.setHealth(10.0F);
        s03Player.getFoodData().setFoodLevel(6);
        ModEffects.METABOLIC_BOOST.get().applyEffectTick(s03Player, 7);
        helper.assertTrue(Math.abs(s03Player.getHealth() - 10.0F) < 0.001F,
                "S-03 healing must pause at food level 6 or below");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void s02DetectionSyncPacketRoundTrip(GameTestHelper helper) {
        // 修复 3：S-02 侦测同步包必须能正确编解码，包括空列表（效果结束时使用）
        int[] original = new int[]{3, 7, 42, 256, 1024, 65535};
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        S02DetectionSyncPacket.encode(new S02DetectionSyncPacket(original), buf);
        buf.readerIndex(0);
        int[] decoded = S02DetectionSyncPacket.decode(buf).getEntityIds();
        helper.assertTrue(decoded.length == original.length, "Round-trip must preserve array length");
        for (int i = 0; i < original.length; i++) {
            helper.assertTrue(decoded[i] == original[i], "Round-trip must preserve element at index " + i);
        }
        // 空列表用于效果结束/换维度时清理客户端残留
        FriendlyByteBuf emptyBuf = new FriendlyByteBuf(Unpooled.buffer());
        S02DetectionSyncPacket.encode(new S02DetectionSyncPacket(new int[0]), emptyBuf);
        emptyBuf.readerIndex(0);
        helper.assertTrue(S02DetectionSyncPacket.decode(emptyBuf).getEntityIds().length == 0,
                "Empty target list must round-trip cleanly");
        // null 构造必须归一化为空数组（防御性）
        helper.assertTrue(new S02DetectionSyncPacket(null).getEntityIds().length == 0,
                "Null entity ID array must normalize to empty");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void gameplayConfigSnapshotDefaultsAndServerConfigMirror(GameTestHelper helper) {
        // 修复 5：服务端配置迁移 —— 客户端快照默认值必须与 Config 出厂默认一致，
        // 且 fromServerConfig 必须镜像运行时 Config（GameTest 世界已加载 SERVER 类型 Config）
        GameplayConfigSnapshot defaults = GameplayConfigSnapshot.empty();
        helper.assertTrue(defaults.mutationRange() == 2, "Default mutationRange must be 2");
        helper.assertTrue(closeTo(defaults.mutationChancePerGen(), 0.005)
                        && defaults.mutationGenerationCap() == 20
                        && closeTo(defaults.mutationChanceCap(), 0.25),
                "Default mutation generation increment and caps must match v1.2.0 balance");
        helper.assertTrue(closeTo(defaults.twinChanceBase(), 0.10)
                        && closeTo(defaults.twinChancePerGen(), 0.02)
                        && closeTo(defaults.twinChanceCap(), 0.60),
                "Default twin probability fields must match v1.2.0 balance");
        helper.assertTrue(defaults.s01BaseDuration() == 300, "Default s01BaseDuration must be 300");
        helper.assertTrue(defaults.s02BaseDuration() == 400, "Default s02BaseDuration must be 400");
        helper.assertTrue(defaults.s03BaseDuration() == 200, "Default s03BaseDuration must be 200");
        helper.assertTrue(defaults.stackAmplifierCap() == 7, "Serum stacking must retain the level VIII cap");
        helper.assertTrue(defaults.stackDurationCap() == 2400
                        && defaults.s01StackDurationCap() == 1800
                        && defaults.s02StackDurationCap() == 2400
                        && defaults.s03StackDurationCap() == 1200,
                "Default serum duration caps must be 90/120/60 seconds under the global 120-second cap");
        helper.assertTrue(defaults.glowScanRangeCap() == 64, "Default glowScanRangeCap must be 64");
        helper.assertTrue(defaults.beltScanRange() == 3, "Default beltScanRange must be 3");
        helper.assertTrue(Math.abs(defaults.packHealThreshold() - 6.0F) < 0.001F,
                "Default packHealThreshold must be 6.0");
        helper.assertTrue(defaults.packHealCooldown() == 1200, "Default packHealCooldown must be 1200");

        GameplayConfigSnapshot fromServer = GameplayConfigSnapshot.fromServerConfig();
        helper.assertTrue(fromServer.s01BaseDuration() == Config.s01BaseDuration,
                "Snapshot.fromServerConfig must mirror Config.s01BaseDuration");
        helper.assertTrue(fromServer.s01StackDurationCap() == Config.s01StackDurationCap
                        && fromServer.s02StackDurationCap() == Config.s02StackDurationCap
                        && fromServer.s03StackDurationCap() == Config.s03StackDurationCap,
                "Snapshot.fromServerConfig must mirror all type-specific serum duration caps");
        helper.assertTrue(fromServer.glowScanRangeCap() == Config.glowScanRangeCap,
                "Snapshot.fromServerConfig must mirror Config.glowScanRangeCap");
        helper.assertTrue(fromServer.beltScanRange() == Config.beltScanRange,
                "Snapshot.fromServerConfig must mirror Config.beltScanRange");
        helper.assertTrue(fromServer.maturationThreshold() == Config.maturationThreshold,
                "Snapshot.fromServerConfig must mirror Config.maturationThreshold");
        helper.assertTrue(fromServer.packHealThreshold() == Config.packHealThreshold,
                "Snapshot.fromServerConfig must mirror Config.packHealThreshold");

        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        GameplayConfigSyncPacket.encode(new GameplayConfigSyncPacket(fromServer), buffer);
        buffer.readerIndex(0);
        GameplayConfigSnapshot decoded = GameplayConfigSyncPacket.decode(buffer).getSnapshot();
        helper.assertTrue(decoded.mutationGenerationCap() == Config.mutationGenerationCap
                        && decoded.mutationChanceCap() == Config.mutationChanceCap
                        && decoded.twinChanceBase() == Config.twinChanceBase
                        && decoded.twinChancePerGen() == Config.twinChancePerGen
                        && decoded.twinChanceCap() == Config.twinChanceCap
                        && decoded.s01StackDurationCap() == Config.s01StackDurationCap
                        && decoded.s02StackDurationCap() == Config.s02StackDurationCap
                        && decoded.s03StackDurationCap() == Config.s03StackDurationCap,
                "Protocol v3 must round-trip mutation, twin and serum duration-cap fields");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void synapticSerumBaseAmplifierThresholdOverload(GameTestHelper helper) {
        // 修复 5：新加 threshold 重载供客户端 Tooltip 从快照读取阈值
        helper.assertTrue(SynapticSerumItem.getBaseAmplifier(7, 8) == 0,
                "Activity below threshold must produce amplifier 0");
        helper.assertTrue(SynapticSerumItem.getBaseAmplifier(8, 8) == 1,
                "Activity at threshold must produce amplifier 1");
        helper.assertTrue(SynapticSerumItem.getBaseAmplifier(15, 8) == 1,
                "Activity above threshold must produce amplifier 1");
        helper.assertTrue(SynapticSerumItem.getBaseAmplifier(0, 8) == 0,
                "Activity 0 must produce amplifier 0");
        // 阈值变更必须直接影响 amplifier：阈值=15 时，所有 Activity<15 都返回 0
        helper.assertTrue(SynapticSerumItem.getBaseAmplifier(14, 15) == 0,
                "Activity below custom threshold must produce amplifier 0");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void serumRecipesExposeInheritFlags(GameTestHelper helper) {
        // 修复 6：JEI 使用 isInheritActivity / isInheritMutation 而非物品类型判断 —— 通过 API 校验标志
        List<SerumRecipe> recipes = CyberCultivatorAPI.getSerumRecipes(helper.getLevel());
        SerumRecipe berry = recipes.stream()
                .filter(r -> r.getId().equals(SerumRecipeIds.BERRY_SYNTHESIS))
                .findFirst().orElse(null);
        helper.assertTrue(berry != null, "Built-in berry_synthesis recipe must be exposed via API");
        helper.assertTrue(berry.isInheritActivity(),
                "Berry synthesis must declare inheritActivity (Activity computed from inputs)");
        helper.assertTrue(berry.isInheritMutation(),
                "Berry synthesis must declare inheritMutation (Mutation tag flows through)");

        for (ResourceLocation bottlingId : new ResourceLocation[]{
                SerumRecipeIds.S01_BOTTLING, SerumRecipeIds.S02_BOTTLING, SerumRecipeIds.S03_BOTTLING}) {
            SerumRecipe bottling = recipes.stream()
                    .filter(r -> r.getId().equals(bottlingId))
                    .findFirst().orElse(null);
            helper.assertTrue(bottling != null, "Built-in serum bottling recipe must be exposed: " + bottlingId);
            helper.assertTrue(bottling.isInheritActivity(),
                    "Serum bottling must inherit Activity from berry input: " + bottlingId);
            helper.assertFalse(bottling.isInheritMutation(),
                    "Serum bottling must not re-inherit Mutation (berry already carries it): " + bottlingId);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void incubatorOutputRecipeAssemblePreservesTemplateNbt(GameTestHelper helper) {
        // 修复 7：培养输出 NBT 丢失 —— outputItem.copy() 必须保留模板 NBT（CustomModelData、显示名等），
        // 而非旧的 new ItemStack(item, count) 只保留基础物品
        ResourceLocation recipeId = ResourceLocation.fromNamespaceAndPath(cybercultivator.MODID, "incubator/fiber_reed");
        Recipe<?> loaded = helper.getLevel().getRecipeManager().byKey(recipeId).orElse(null);
        helper.assertTrue(loaded instanceof IncubatorOutputRecipe,
                "fiber_reed incubator recipe must be loaded");
        IncubatorOutputRecipe recipe = (IncubatorOutputRecipe) loaded;

        ItemStack seed = new ItemStack(ModItems.FIBER_REED_SEEDS.get());
        GeneticSeedItem.setGene(seed, GeneticSeedItem.GENE_POTENCY, 7);
        GeneticSeedItem.setGene(seed, GeneticSeedItem.GENE_YIELD, 6);

        ItemStack result = recipe.assemble(seed);
        helper.assertTrue(result.is(ModItems.PLANT_FIBER.get()), "Result must be plant fiber");
        CompoundTag resultTag = result.getTag();
        helper.assertTrue(resultTag != null, "Result must carry NBT (quality + Generation tags)");
        helper.assertTrue(resultTag.getInt("Potency") == 7,
                "Result Potency must be derived from the seed's Potency gene (was lost in v1.1.6)");
        helper.assertTrue(resultTag.contains("Generation"),
                "Result must carry Generation tag from the seed");
        helper.assertTrue(result.getCount() >= 1,
                "Result count must follow the count_formula evaluated with Yield gene");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 300)
    public static void cropMatureEventEmptyOutputSoftCancelsPreservingSeed(GameTestHelper helper) {
        // 修复 7：监听器通过 setOutput(EMPTY) 软取消成熟时，必须保留种子和资源（不消耗成熟成本），
        // 仅重置进度避免每 tick 重复触发。区别于 setCanceled(true) 的硬取消路径。
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.BIO_INCUBATOR.get());
        BioIncubatorBlockEntity incubator = (BioIncubatorBlockEntity) helper.getBlockEntity(pos);
        ItemStack seed = new ItemStack(ModItems.FIBER_REED_SEEDS.get());
        GeneticSeedItem.setGene(seed, GeneticSeedItem.GENE_SPEED, 10);
        helper.assertTrue(incubator.tryInsertSeed(seed), "Test seed must be inserted");
        incubator.addNutrition(100);
        incubator.addPurity(100);
        incubator.addDataSignal(100);

        CropListener[] listenerHolder = new CropListener[1];
        CropListener listener = new CropListener(event -> {
            if (event.getLevel() != helper.getLevel() || !event.getPos().equals(helper.absolutePos(pos))) return;
            int nutritionAtEvent = incubator.getNutrition();
            int purityAtEvent = incubator.getPurity();
            // 软取消：仅清空输出，不调用 setCanceled(true)
            event.setOutput(ItemStack.EMPTY);
            MinecraftForge.EVENT_BUS.unregister(listenerHolder[0]);
            helper.runAfterDelay(1, () -> {
                helper.assertTrue(incubator.hasSeed(),
                        "Soft-cancelled maturity must preserve the seed");
                helper.assertTrue(incubator.getGrowthPercent() <= 2,
                        "Soft-cancelled maturity must reset growth progress to avoid re-firing every tick");
                helper.assertTrue(nutritionAtEvent - incubator.getNutrition() <= 1,
                        "Soft-cancelled maturity must not deduct the maturity nutrition cost");
                helper.assertTrue(purityAtEvent - incubator.getPurity() <= 1,
                        "Soft-cancelled maturity must not deduct the maturity purity cost");
                helper.assertItemEntityNotPresent(ModItems.PLANT_FIBER.get(), pos, 4.0);
                helper.succeed();
            });
        });
        listenerHolder[0] = listener;
        MinecraftForge.EVENT_BUS.register(listener);
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void geneSpliceCompleteAdvancementExistsWithCustomTrigger(GameTestHelper helper) {
        // 修复 8：新增 gene_splice_complete 进度，使用自定义触发器
        var advancement = helper.getLevel().getServer().getAdvancements().getAdvancement(
                ResourceLocation.fromNamespaceAndPath(cybercultivator.MODID, "gene_splice_complete"));
        helper.assertTrue(advancement != null,
                "gene_splice_complete advancement must be loaded (v1.1.7 fix)");
        String[][] requirements = advancement.getRequirements();
        helper.assertTrue(requirements.length == 1 && requirements[0].length == 1
                        && "gene_splice_complete".equals(requirements[0][0]),
                "gene_splice_complete advancement must have a single criterion with matching name");
        // 触发器必须已注册（ModTriggers 静态字段在 CriteriaTriggers.register() 调用后非空，
        // 且其 ID 必须与进度触发器 ID 一致）
        com.TKCCOPL.advancement.GeneSpliceCompleteTrigger trigger =
                com.TKCCOPL.advancement.ModTriggers.GENE_SPLICE_COMPLETE;
        helper.assertTrue(trigger != null,
                "ModTriggers.GENE_SPLICE_COMPLETE must be non-null (initialized via CriteriaTriggers.register)");
        helper.assertTrue(trigger.getId().equals(
                        ResourceLocation.fromNamespaceAndPath(cybercultivator.MODID, "gene_splice_complete")),
                "Trigger ID must match the advancement criterion ID");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void rootAdvancementRequiresRawSiliconCrystal(GameTestHelper helper) {
        // 修复 8：根进度触发条件改为 raw_silicon_crystal（之前错误地指向 silicon_shard，
        // 导致玩家必须先合成 silicon_shard 才能开启进度链，绕过了原始矿物阶段）
        var advancement = helper.getLevel().getServer().getAdvancements().getAdvancement(
                ResourceLocation.fromNamespaceAndPath(cybercultivator.MODID, "root"));
        helper.assertTrue(advancement != null, "root advancement must be loaded");
        String[][] requirements = advancement.getRequirements();
        helper.assertTrue(requirements.length >= 1 && requirements[0].length >= 1
                        && "has_raw_silicon_crystal".equals(requirements[0][0]),
                "root advancement must require the has_raw_silicon_crystal criterion (v1.1.7 fix)");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void bottlerCanPlaceItemRejectsNonRecipeStacks(GameTestHelper helper) {
        // 修复 10：装瓶机漏斗查询性能优化 —— canPlaceItem 使用缓存的 Ingredient 列表，
        // 行为必须与遍历 RecipeManager 一致（接受配方原料，拒绝其他）
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.SERUM_BOTTLER.get());
        SerumBottlerBlockEntity bottler = (SerumBottlerBlockEntity) helper.getBlockEntity(pos);

        // 配方接受的有效原料（覆盖四条配方）
        helper.assertTrue(bottler.canPlaceItem(0, new ItemStack(ModItems.PLANT_FIBER.get())),
                "Bottler must accept plant fiber (berry synthesis)");
        helper.assertTrue(bottler.canPlaceItem(0, new ItemStack(ModItems.INDUSTRIAL_ETHANOL.get())),
                "Bottler must accept industrial ethanol (berry synthesis / S-03)");
        helper.assertTrue(bottler.canPlaceItem(0, new ItemStack(ModItems.BIOCHEMICAL_SOLUTION.get())),
                "Bottler must accept biochemical solution (berry synthesis / S-01)");
        helper.assertTrue(bottler.canPlaceItem(0, new ItemStack(ModItems.RARE_EARTH_DUST.get())),
                "Bottler must accept rare earth dust (S-02)");
        helper.assertTrue(bottler.canPlaceItem(0, new ItemStack(ModItems.SYNAPTIC_NEURAL_BERRY.get())),
                "Bottler must accept synaptic neural berry (S-01/S-02/S-03)");
        helper.assertTrue(bottler.canPlaceItem(0, new ItemStack(Items.GLASS_BOTTLE)),
                "Bottler must accept glass bottle (S-01/S-03)");

        // 配方不接受的物品（性能优化后必须仍然拒绝）
        // 注意：使用 bedrock 而非 dirt，因为 KubeJS 烟雾脚本可能注册 dirt 为血清配方原料
        helper.assertFalse(bottler.canPlaceItem(0, new ItemStack(Items.BEDROCK)),
                "Bottler must reject bedrock");
        helper.assertFalse(bottler.canPlaceItem(0, new ItemStack(Items.STONE)),
                "Bottler must reject stone");
        helper.assertFalse(bottler.canPlaceItem(0, new ItemStack(Items.IRON_INGOT)),
                "Bottler must reject iron ingot");

        // 多次调用以热身 Ingredient 缓存，再验证结果一致（缓存路径不引入回归）
        for (int i = 0; i < 5; i++) {
            helper.assertTrue(bottler.canPlaceItem(0, new ItemStack(ModItems.PLANT_FIBER.get())),
                    "Cached canPlaceItem must consistently accept valid items on iteration " + i);
            helper.assertFalse(bottler.canPlaceItem(0, new ItemStack(Items.BEDROCK)),
                    "Cached canPlaceItem must consistently reject invalid items on iteration " + i);
        }

        // 越界槽位与空栈必须返回 false
        helper.assertFalse(bottler.canPlaceItem(-1, new ItemStack(ModItems.PLANT_FIBER.get())),
                "Negative slot index must be rejected");
        helper.assertFalse(bottler.canPlaceItem(99, new ItemStack(ModItems.PLANT_FIBER.get())),
                "Out-of-range slot index must be rejected");
        helper.assertFalse(bottler.canPlaceItem(0, ItemStack.EMPTY),
                "Empty stack must be rejected");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void bottlerFindRecipeCacheSkipsIdleTraversal(GameTestHelper helper) {
        // 修复 10：lastRecipeQueryFailed 缓存 —— 空闲 bottler 输入未变化时跳过 RecipeManager 遍历。
        // 验证：无输入时 tick 不应启动加工；放入匹配输入后 tick 应启动加工（缓存不会造成 false-negative）
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.SERUM_BOTTLER.get());
        SerumBottlerBlockEntity bottler = (SerumBottlerBlockEntity) helper.getBlockEntity(pos);

        // 空输入连续 tick：始终空闲
        for (int i = 0; i < 3; i++) {
            SerumBottlerBlockEntity.tick(helper.getLevel(), pos, bottler.getBlockState(), bottler);
            helper.assertTrue(bottler.getMaxProgress() == 0,
                    "Empty bottler must stay idle on iteration " + i);
        }
        // 放入 bedrock（无配方匹配）连续 tick：仍不应启动加工
        // 注意：使用 bedrock 而非 dirt，因为 KubeJS 烟雾脚本可能注册 dirt 为血清配方原料
        bottler.setItem(0, new ItemStack(Items.BEDROCK));
        for (int i = 0; i < 3; i++) {
            SerumBottlerBlockEntity.tick(helper.getLevel(), pos, bottler.getBlockState(), bottler);
            helper.assertTrue(bottler.getMaxProgress() == 0,
                    "Non-matching input must not start processing on iteration " + i);
        }
        // 清空后放入完整匹配输入：必须启动加工（lastRecipeQueryFailed 不能阻止后续匹配）
        bottler.setItem(0, ItemStack.EMPTY);
        SerumBottlerBlockEntity.tick(helper.getLevel(), pos, bottler.getBlockState(), bottler);
        bottler.setItem(0, new ItemStack(ModItems.PLANT_FIBER.get()));
        bottler.setItem(1, new ItemStack(ModItems.INDUSTRIAL_ETHANOL.get()));
        bottler.setItem(2, new ItemStack(ModItems.BIOCHEMICAL_SOLUTION.get()));
        SerumBottlerBlockEntity.tick(helper.getLevel(), pos, bottler.getBlockState(), bottler);
        helper.assertTrue(bottler.getMaxProgress() > 0,
                "Berry synthesis inputs must start processing despite the previous failed-query cache");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void redstoneLegacyDefaultsToIgnore(GameTestHelper helper) {
        BlockPos condenserPos = new BlockPos(1, 1, 1);
        helper.setBlock(condenserPos, ModBlocks.ATMOSPHERIC_CONDENSER.get());
        AtmosphericCondenserBlockEntity condenser =
                (AtmosphericCondenserBlockEntity) helper.getBlockEntity(condenserPos);
        condenser.load(new CompoundTag());
        helper.assertTrue(condenser.getRedstoneState().getMode() == RedstoneControlMode.IGNORE,
                "Legacy condenser must default to IGNORE");
        helper.assertTrue(condenser.getRedstoneState().isProcessingAllowed(),
                "IGNORE mode must allow processing");
        AtmosphericCondenserBlockEntity.tick(helper.getLevel(), condenserPos,
                condenser.getBlockState(), condenser);

        BlockPos incubatorPos = new BlockPos(2, 1, 1);
        helper.setBlock(incubatorPos, ModBlocks.BIO_INCUBATOR.get());
        BioIncubatorBlockEntity incubator =
                (BioIncubatorBlockEntity) helper.getBlockEntity(incubatorPos);
        incubator.load(new CompoundTag());
        helper.assertTrue(incubator.getRedstoneState().getMode() == RedstoneControlMode.IGNORE,
                "Legacy incubator must default to IGNORE");
        BioIncubatorBlockEntity.tick(helper.getLevel(), incubatorPos,
                incubator.getBlockState(), incubator);

        BlockPos splicerPos = new BlockPos(3, 1, 1);
        helper.setBlock(splicerPos, ModBlocks.GENE_SPLICER.get());
        GeneSplicerBlockEntity splicer =
                (GeneSplicerBlockEntity) helper.getBlockEntity(splicerPos);
        splicer.load(new CompoundTag());
        helper.assertTrue(splicer.getRedstoneState().getMode() == RedstoneControlMode.IGNORE,
                "Legacy splicer must default to IGNORE");
        GeneSplicerBlockEntity.tick(helper.getLevel(), splicerPos,
                splicer.getBlockState(), splicer);

        BlockPos bottlerPos = new BlockPos(4, 1, 1);
        helper.setBlock(bottlerPos, ModBlocks.SERUM_BOTTLER.get());
        SerumBottlerBlockEntity bottler =
                (SerumBottlerBlockEntity) helper.getBlockEntity(bottlerPos);
        bottler.load(new CompoundTag());
        helper.assertTrue(bottler.getRedstoneState().getMode() == RedstoneControlMode.IGNORE,
                "Legacy bottler must default to IGNORE");
        SerumBottlerBlockEntity.tick(helper.getLevel(), bottlerPos,
                bottler.getBlockState(), bottler);

        // M5: NBT 未知字符串 → IGNORE
        CompoundTag unknownModeTag = new CompoundTag();
        unknownModeTag.putString("RedstoneMode", "bogus");
        splicer.load(unknownModeTag);
        helper.assertTrue(splicer.getRedstoneState().getMode() == RedstoneControlMode.IGNORE,
                "Unknown redstone mode string must fall back to IGNORE");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void redstoneHighLowFreezesAndResumesWithoutConsuming(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.GENE_SPLICER.get());
        GeneSplicerBlockEntity splicer = (GeneSplicerBlockEntity) helper.getBlockEntity(pos);
        ItemStack seedA = new ItemStack(ModItems.FIBER_REED_SEEDS.get());
        ItemStack seedB = new ItemStack(ModItems.PROTEIN_SOY_SEEDS.get());
        splicer.setItem(GeneSplicerBlockEntity.SEED_A_SLOT, seedA);
        splicer.setItem(GeneSplicerBlockEntity.SEED_B_SLOT, seedB);
        helper.assertTrue(splicer.isSplicing() && splicer.getSpliceProgress() == 0,
                "Two parents must start splicing with progress=0");

        // 推进 1 tick（IGNORE 默认允许加工）
        GeneSplicerBlockEntity.tick(helper.getLevel(), pos, splicer.getBlockState(), splicer);
        helper.assertTrue(splicer.getSpliceProgress() == 1,
                "IGNORE mode must allow progress advancement");

        // HIGH 模式 + 未供电 → 冻结
        splicer.getRedstoneState().setMode(RedstoneControlMode.HIGH);
        splicer.getRedstoneState().updatePowered(false);
        helper.assertFalse(splicer.getRedstoneState().isProcessingAllowed(),
                "HIGH mode without power must block processing");
        for (int i = 0; i < 5; i++) {
            GeneSplicerBlockEntity.tick(helper.getLevel(), pos, splicer.getBlockState(), splicer);
        }
        helper.assertTrue(splicer.getSpliceProgress() == 1,
                "HIGH mode without power must freeze splice progress");
        helper.assertFalse(splicer.getSeedA().isEmpty() || splicer.getSeedB().isEmpty(),
                "Frozen splice must not consume parents");

        // HIGH 模式 + 供电 → 恢复
        splicer.getRedstoneState().updatePowered(true);
        helper.assertTrue(splicer.getRedstoneState().isProcessingAllowed(),
                "HIGH mode with power must allow processing");
        for (int i = 0; i < 5; i++) {
            GeneSplicerBlockEntity.tick(helper.getLevel(), pos, splicer.getBlockState(), splicer);
        }
        helper.assertTrue(splicer.getSpliceProgress() == 6,
                "HIGH mode with power must resume splice progress");

        // LOW 模式 + 供电 → 冻结
        splicer.getRedstoneState().setMode(RedstoneControlMode.LOW);
        helper.assertFalse(splicer.getRedstoneState().isProcessingAllowed(),
                "LOW mode with power must block processing");
        for (int i = 0; i < 5; i++) {
            GeneSplicerBlockEntity.tick(helper.getLevel(), pos, splicer.getBlockState(), splicer);
        }
        helper.assertTrue(splicer.getSpliceProgress() == 6,
                "LOW mode with power must freeze splice progress");

        // LOW 模式 + 未供电 → 恢复
        splicer.getRedstoneState().updatePowered(false);
        helper.assertTrue(splicer.getRedstoneState().isProcessingAllowed(),
                "LOW mode without power must allow processing");
        for (int i = 0; i < 5; i++) {
            GeneSplicerBlockEntity.tick(helper.getLevel(), pos, splicer.getBlockState(), splicer);
        }
        helper.assertTrue(splicer.getSpliceProgress() == 11,
                "LOW mode without power must resume splice progress");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void comparatorBoundariesAcrossMachines(GameTestHelper helper) {
        // === 灌装机 ===
        BlockPos bottlerPos = new BlockPos(1, 1, 1);
        helper.setBlock(bottlerPos, ModBlocks.SERUM_BOTTLER.get());
        SerumBottlerBlockEntity bottler = (SerumBottlerBlockEntity) helper.getBlockEntity(bottlerPos);
        helper.assertTrue(bottler.getComparatorSignal() == 0,
                "Empty bottler must emit 0");
        CompoundTag bottlerProgress = new CompoundTag();
        bottlerProgress.putInt("Progress", 1);
        bottlerProgress.putInt("MaxProgress", 300);
        bottlerProgress.putString("RecipeId", SerumRecipeIds.S01_BOTTLING.toString());
        bottler.load(bottlerProgress);
        helper.assertTrue(bottler.getComparatorSignal() == 1,
                "Bottler at progress=1/300 must emit 1 (lower bound)");
        bottlerProgress.putInt("Progress", 297);
        bottler.load(bottlerProgress);
        helper.assertTrue(bottler.getComparatorSignal() == 14,
                "Bottler at progress=297/300 must emit 14 (upper bound)");
        bottlerProgress.put("Output",
                new ItemStack(ModItems.SYNAPTIC_SERUM_S01.get()).save(new CompoundTag()));
        bottler.load(bottlerProgress);
        helper.assertTrue(bottler.getComparatorSignal() == 15,
                "Bottler with output must emit 15 (priority over progress)");

        // === 拼接机 ===
        BlockPos splicerPos = new BlockPos(2, 1, 1);
        helper.setBlock(splicerPos, ModBlocks.GENE_SPLICER.get());
        GeneSplicerBlockEntity splicer = (GeneSplicerBlockEntity) helper.getBlockEntity(splicerPos);
        helper.assertTrue(splicer.getComparatorSignal() == 0,
                "Empty splicer must emit 0");
        CompoundTag splicerProgress = new CompoundTag();
        splicerProgress.put("SeedA", new ItemStack(ModItems.FIBER_REED_SEEDS.get()).save(new CompoundTag()));
        splicerProgress.put("SeedB", new ItemStack(ModItems.PROTEIN_SOY_SEEDS.get()).save(new CompoundTag()));
        splicerProgress.putInt("SpliceProgress", 0);
        splicerProgress.putBoolean("Splicing", true);
        splicerProgress.putBoolean("AutomaticWorkflow", true);
        splicer.load(splicerProgress);
        helper.assertTrue(splicer.isSplicing() && splicer.getSpliceProgress() == 0,
                "Splicer with two parents must be splicing at progress=0");
        helper.assertTrue(splicer.getComparatorSignal() == 1,
                "Splicer at splicing=true progress=0 must emit 1 (§3.3 startup boundary)");
        splicerProgress.putInt("SpliceProgress", 99);
        splicer.load(splicerProgress);
        helper.assertTrue(splicer.getComparatorSignal() == 14,
                "Splicer at progress=99/100 must emit 14");
        CompoundTag splicerOutput = new CompoundTag();
        splicerOutput.put("Output", new ItemStack(ModItems.FIBER_REED_SEEDS.get()).save(new CompoundTag()));
        splicer.load(splicerOutput);
        helper.assertTrue(splicer.getComparatorSignal() == 15,
                "Splicer with output must emit 15");

        // === 冷凝器 ===
        BlockPos condenserPos = new BlockPos(3, 1, 1);
        helper.setBlock(condenserPos, ModBlocks.ATMOSPHERIC_CONDENSER.get());
        AtmosphericCondenserBlockEntity condenser =
                (AtmosphericCondenserBlockEntity) helper.getBlockEntity(condenserPos);
        helper.assertTrue(condenser.getComparatorSignal() == 0,
                "Empty condenser must emit 0");
        CompoundTag condenserProgress = new CompoundTag();
        condenserProgress.putInt("Progress", 1);
        condenser.load(condenserProgress);
        helper.assertTrue(condenser.getComparatorSignal() == 1,
                "Condenser at progress=1/600 must emit 1");
        condenserProgress.putInt("Progress", 599);
        condenser.load(condenserProgress);
        helper.assertTrue(condenser.getComparatorSignal() == 14,
                "Condenser at progress=599/600 must emit 14");
        CompoundTag condenserOutput = new CompoundTag();
        condenserOutput.put("Output",
                new ItemStack(ModItems.PURIFIED_WATER_BOTTLE.get()).save(new CompoundTag()));
        condenser.load(condenserOutput);
        helper.assertTrue(condenser.getComparatorSignal() == 15,
                "Condenser with output must emit 15");

        // === 培养槽 ===
        BlockPos incubatorPos = new BlockPos(4, 1, 1);
        helper.setBlock(incubatorPos, ModBlocks.BIO_INCUBATOR.get());
        BioIncubatorBlockEntity incubator =
                (BioIncubatorBlockEntity) helper.getBlockEntity(incubatorPos);
        helper.assertTrue(incubator.getComparatorSignal() == 0,
                "Empty incubator must emit 0");
        CompoundTag incubatorProgress = new CompoundTag();
        incubatorProgress.put("Seed", new ItemStack(ModItems.FIBER_REED_SEEDS.get()).save(new CompoundTag()));
        incubatorProgress.putInt("GrowthProgress", 1);
        incubator.load(incubatorProgress);
        int expectedLower = Math.max(1, Math.min(14,
                (int) Math.ceil((double) 1 * 14 / Config.maturationThreshold)));
        helper.assertTrue(incubator.getComparatorSignal() == expectedLower,
                "Incubator at growth=1 must emit " + expectedLower + " (lower bound)");
        incubatorProgress.putInt("GrowthProgress", Config.maturationThreshold - 1);
        incubator.load(incubatorProgress);
        int expectedUpper = Math.max(1, Math.min(14,
                (int) Math.ceil((double) (Config.maturationThreshold - 1) * 14 / Config.maturationThreshold)));
        helper.assertTrue(incubator.getComparatorSignal() == expectedUpper,
                "Incubator near maturity must emit " + expectedUpper + " (upper bound)");
        CompoundTag incubatorOutput = new CompoundTag();
        incubatorOutput.put("ResourceOutput",
                new ItemStack(ModItems.PLANT_FIBER.get()).save(new CompoundTag()));
        incubator.load(incubatorOutput);
        helper.assertTrue(incubator.getComparatorSignal() == 15,
                "Incubator with resource output must emit 15");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void clickMenuButtonCyclesRedstoneMode(GameTestHelper helper) {
        BlockPos bottlerPos = new BlockPos(1, 1, 1);
        helper.setBlock(bottlerPos, ModBlocks.SERUM_BOTTLER.get());
        SerumBottlerBlockEntity bottler = (SerumBottlerBlockEntity) helper.getBlockEntity(bottlerPos);
        Player player = helper.makeMockSurvivalPlayer();
        SerumBottlerMenu menu = (SerumBottlerMenu) bottler.createMenu(1, player.getInventory(), player);

        helper.assertTrue(bottler.getRedstoneState().getMode() == RedstoneControlMode.IGNORE,
                "Bottler must start in IGNORE mode");
        helper.assertTrue(menu.clickMenuButton(player, SerumBottlerMenu.BUTTON_CYCLE_REDSTONE),
                "clickMenuButton must handle redstone cycle");
        helper.assertTrue(bottler.getRedstoneState().getMode() == RedstoneControlMode.HIGH,
                "First click must cycle IGNORE → HIGH");
        helper.assertTrue(menu.clickMenuButton(player, SerumBottlerMenu.BUTTON_CYCLE_REDSTONE),
                "clickMenuButton must handle second cycle");
        helper.assertTrue(bottler.getRedstoneState().getMode() == RedstoneControlMode.LOW,
                "Second click must cycle HIGH → LOW");
        helper.assertTrue(menu.clickMenuButton(player, SerumBottlerMenu.BUTTON_CYCLE_REDSTONE),
                "clickMenuButton must handle third cycle");
        helper.assertTrue(bottler.getRedstoneState().getMode() == RedstoneControlMode.IGNORE,
                "Third click must cycle LOW → IGNORE");

        // 当前冷凝器已移除旧的暂停/自动注入按钮，红石模式使用唯一按钮 ID 0。
        BlockPos condenserPos = new BlockPos(2, 1, 1);
        helper.setBlock(condenserPos, ModBlocks.ATMOSPHERIC_CONDENSER.get());
        AtmosphericCondenserBlockEntity condenser =
                (AtmosphericCondenserBlockEntity) helper.getBlockEntity(condenserPos);
        AtmosphericCondenserMenu condenserMenu =
                (AtmosphericCondenserMenu) condenser.createMenu(2, player.getInventory(), player);
        helper.assertTrue(condenser.getRedstoneState().getMode() == RedstoneControlMode.IGNORE,
                "Condenser must start in IGNORE mode");
        helper.assertTrue(condenserMenu.clickMenuButton(player, AtmosphericCondenserMenu.BUTTON_CYCLE_REDSTONE),
                "Condenser clickMenuButton must handle redstone cycle");
        helper.assertTrue(condenser.getRedstoneState().getMode() == RedstoneControlMode.HIGH,
                "Condenser first click must cycle IGNORE → HIGH");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE, timeoutTicks = 200)
    public static void apiSetMachineRedstoneModeGuards(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.SERUM_BOTTLER.get());
        SerumBottlerBlockEntity bottler = (SerumBottlerBlockEntity) helper.getBlockEntity(pos);
        // 公开 API 使用绝对坐标（helper.setBlock/getBlockEntity 内部转绝对，但 API 直接调用 level）
        BlockPos absPos = helper.absolutePos(pos);

        // null 参数 → false
        helper.assertFalse(CyberCultivatorAPI.setMachineRedstoneMode(null, absPos, RedstoneControlMode.HIGH),
                "Null level must be rejected");
        helper.assertFalse(CyberCultivatorAPI.setMachineRedstoneMode(helper.getLevel(), null, RedstoneControlMode.HIGH),
                "Null pos must be rejected");
        helper.assertFalse(CyberCultivatorAPI.setMachineRedstoneMode(helper.getLevel(), absPos, null),
                "Null mode must be rejected");

        // 非机器 BE → false
        BlockPos dirtPos = new BlockPos(2, 1, 1);
        helper.setBlock(dirtPos, net.minecraft.world.level.block.Blocks.STONE);
        BlockPos absDirtPos = helper.absolutePos(dirtPos);
        helper.assertFalse(CyberCultivatorAPI.setMachineRedstoneMode(helper.getLevel(), absDirtPos, RedstoneControlMode.HIGH),
                "Non-machine block must be rejected");

        // 服务端主线程调用 → true
        helper.assertTrue(CyberCultivatorAPI.setMachineRedstoneMode(helper.getLevel(), absPos, RedstoneControlMode.HIGH),
                "Server main-thread call must succeed");
        helper.assertTrue(bottler.getRedstoneState().getMode() == RedstoneControlMode.HIGH,
                "API must actually change the redstone mode");

        // 非主线程调用 → false（M7）
        AtomicBoolean threadResult = new AtomicBoolean(true);
        CountDownLatch latch = new CountDownLatch(1);
        Thread other = new Thread(() -> {
            threadResult.set(CyberCultivatorAPI.setMachineRedstoneMode(
                    helper.getLevel(), absPos, RedstoneControlMode.LOW));
            latch.countDown();
        }, "v1.1.7-redstone-api-test");
        other.start();
        try {
            latch.await();
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        helper.assertFalse(threadResult.get(),
                "Non-main-thread API call must be rejected (M7)");
        helper.assertTrue(bottler.getRedstoneState().getMode() == RedstoneControlMode.HIGH,
                "Rejected non-main-thread call must not change the mode");

        // getMachineControlInfo 返回正确快照
        MachineControlInfo info = CyberCultivatorAPI.getMachineControlInfo(helper.getLevel(), absPos);
        helper.assertTrue(info != null, "getMachineControlInfo must return a snapshot");
        helper.assertTrue(info.mode() == RedstoneControlMode.HIGH,
                "Snapshot must reflect current mode");
        helper.assertTrue(info.comparatorSignal() == bottler.getComparatorSignal(),
                "Snapshot must reflect current comparator signal");

        // getMachineControlInfo null/非机器 → null
        helper.assertTrue(CyberCultivatorAPI.getMachineControlInfo(null, absPos) == null,
                "Null level must return null snapshot");
        helper.assertTrue(CyberCultivatorAPI.getMachineControlInfo(helper.getLevel(), absDirtPos) == null,
                "Non-machine block must return null snapshot");

        // 注：客户端调用（level.isClientSide == true）拒绝逻辑由代码审查覆盖，
        // GameTest 在服务端运行无法访问 ClientLevel。
        helper.succeed();
    }

    // === v1.1.7 §12.1 自动化能力测试（IItemHandler + WorldlyContainer 一致性） ===

    @GameTest(template = EMPTY_TEMPLATE)
    public static void machineCapabilityMatrix(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.GENE_SPLICER.get());
        GeneSplicerBlockEntity splicer = (GeneSplicerBlockEntity) helper.getBlockEntity(pos);

        // visibleSlots(UP) 与 visibleSlots(NORTH) 应包含两个亲本槽
        int[] upSlots = splicer.visibleSlots(Direction.UP);
        int[] horizontalSlots = splicer.visibleSlots(Direction.NORTH);
        helper.assertTrue(upSlots.length == 2 && horizontalSlots.length == 2,
                "Splicer UP/horizontal must expose 2 seed slots");
        // visibleSlots(DOWN) 仅输出槽
        int[] downSlots = splicer.visibleSlots(Direction.DOWN);
        helper.assertTrue(downSlots.length == 1 && downSlots[0] == GeneSplicerBlockEntity.OUTPUT_SLOT,
                "Splicer DOWN must expose only output slot");
        // null side 暴露全部 3 槽位
        int[] nullSlots = splicer.visibleSlots(null);
        helper.assertTrue(nullSlots.length == 3, "Splicer null side must expose all slots");

        // canInsert：亲本槽接受种子，输出槽拒绝；DOWN 拒绝插入
        ItemStack seed = ModItems.FIBER_REED_SEEDS.get().getDefaultInstance();
        helper.assertTrue(splicer.canInsert(GeneSplicerBlockEntity.SEED_A_SLOT, seed, Direction.UP),
                "Splicer SEED_A must accept seed from UP");
        helper.assertTrue(!splicer.canInsert(GeneSplicerBlockEntity.OUTPUT_SLOT, seed, Direction.UP),
                "Splicer OUTPUT must reject insertion");
        helper.assertTrue(!splicer.canInsert(GeneSplicerBlockEntity.SEED_A_SLOT, seed, Direction.DOWN),
                "Splicer DOWN must reject seed insertion");
        // canExtract：仅 DOWN 可抽取输出；亲本槽拒绝
        helper.assertTrue(splicer.canExtract(GeneSplicerBlockEntity.OUTPUT_SLOT, ItemStack.EMPTY, Direction.DOWN),
                "Splicer OUTPUT must be extractable from DOWN");
        helper.assertTrue(!splicer.canExtract(GeneSplicerBlockEntity.SEED_A_SLOT, ItemStack.EMPTY, Direction.DOWN),
                "Splicer SEED_A must not be extractable");
        helper.assertTrue(!splicer.canExtract(GeneSplicerBlockEntity.OUTPUT_SLOT, ItemStack.EMPTY, Direction.UP),
                "Splicer OUTPUT must not be extractable from UP");

        helper.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());

        // 培养槽：UP=种子只入；水平=N/P/D 只入；DOWN=输出只出
        helper.setBlock(pos, ModBlocks.BIO_INCUBATOR.get());
        BioIncubatorBlockEntity incubator = (BioIncubatorBlockEntity) helper.getBlockEntity(pos);
        int[] incUp = incubator.visibleSlots(Direction.UP);
        helper.assertTrue(incUp.length == 1 && incUp[0] == BioIncubatorBlockEntity.SEED_SLOT,
                "Incubator UP must expose only seed slot");
        int[] incHorizontal = incubator.visibleSlots(Direction.NORTH);
        helper.assertTrue(incHorizontal.length == 3, "Incubator horizontal must expose 3 input slots");
        int[] incDown = incubator.visibleSlots(Direction.DOWN);
        helper.assertTrue(incDown.length == 1
                        && incDown[0] == BioIncubatorBlockEntity.RESOURCE_OUTPUT_SLOT,
                "Incubator DOWN must expose only mature resource output");
        int[] incNull = incubator.visibleSlots(null);
        helper.assertTrue(incNull.length == 5, "Incubator null side must expose all 5 slots");

        // canInsert：UP 接受种子，UP 拒绝 NUTRITION；horizontal 接受 NUTRITION，horizontal 拒绝种子
        helper.assertTrue(incubator.canInsert(BioIncubatorBlockEntity.SEED_SLOT, seed, Direction.UP),
                "Incubator SEED must accept seed from UP");
        helper.assertTrue(!incubator.canInsert(BioIncubatorBlockEntity.SEED_SLOT, seed, Direction.NORTH),
                "Incubator SEED must reject seed from horizontal");
        ItemStack biochem = ModItems.BIOCHEMICAL_SOLUTION.get().getDefaultInstance();
        helper.assertTrue(incubator.canInsert(BioIncubatorBlockEntity.NUTRITION_SLOT, biochem, Direction.NORTH),
                "Incubator NUTRITION must accept biochem from horizontal");
        helper.assertTrue(!incubator.canInsert(BioIncubatorBlockEntity.NUTRITION_SLOT, biochem, Direction.UP),
                "Incubator NUTRITION must reject biochem from UP");

        helper.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());

        // 灌装机：UP/horizontal=3 材料槽只入；DOWN=成品只出
        helper.setBlock(pos, ModBlocks.SERUM_BOTTLER.get());
        SerumBottlerBlockEntity bottler = (SerumBottlerBlockEntity) helper.getBlockEntity(pos);
        int[] botUp = bottler.visibleSlots(Direction.UP);
        helper.assertTrue(botUp.length == 3, "Bottler UP must expose 3 input slots");
        int[] botDown = bottler.visibleSlots(Direction.DOWN);
        helper.assertTrue(botDown.length == 1, "Bottler DOWN must expose only output slot");
        int[] botNull = bottler.visibleSlots(null);
        helper.assertTrue(botNull.length == 4, "Bottler null side must expose all 4 slots");

        // canInsert：DOWN 拒绝；UP 接受合法材料（莓）
        ItemStack berry = ModItems.SYNAPTIC_NEURAL_BERRY.get().getDefaultInstance();
        helper.assertTrue(bottler.canInsert(0, berry, Direction.UP), "Bottler must accept berry from UP");
        helper.assertTrue(!bottler.canInsert(0, berry, Direction.DOWN), "Bottler DOWN must reject insertion");
        // canExtract：仅 DOWN 可抽取 OUTPUT
        helper.assertTrue(bottler.canExtract(3, ItemStack.EMPTY, Direction.DOWN),
                "Bottler OUTPUT must be extractable from DOWN");
        helper.assertTrue(!bottler.canExtract(0, ItemStack.EMPTY, Direction.DOWN),
                "Bottler INPUT must not be extractable");

        helper.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());

        // 冷凝器：UP=玻璃瓶只入；水平=玻璃瓶输入+纯净水输出；DOWN=纯净水只出
        helper.setBlock(pos, ModBlocks.ATMOSPHERIC_CONDENSER.get());
        AtmosphericCondenserBlockEntity condenser = (AtmosphericCondenserBlockEntity) helper.getBlockEntity(pos);
        int[] condUp = condenser.visibleSlots(Direction.UP);
        helper.assertTrue(condUp.length == 1
                        && condUp[0] == AtmosphericCondenserBlockEntity.BOTTLE_INPUT_SLOT,
                "Condenser UP must expose only bottle input");
        int[] condHorizontal = condenser.visibleSlots(Direction.NORTH);
        helper.assertTrue(condHorizontal.length == 2,
                "Condenser horizontal side must expose bottle input and water output");
        int[] condDown = condenser.visibleSlots(Direction.DOWN);
        helper.assertTrue(condDown.length == 1
                        && condDown[0] == AtmosphericCondenserBlockEntity.OUTPUT_SLOT,
                "Condenser DOWN must expose only water output");
        ItemStack glassBottle = Items.GLASS_BOTTLE.getDefaultInstance();
        helper.assertTrue(condenser.canInsert(AtmosphericCondenserBlockEntity.BOTTLE_INPUT_SLOT,
                        glassBottle, Direction.UP),
                "Condenser must accept glass bottles from UP");
        helper.assertTrue(!condenser.canInsert(AtmosphericCondenserBlockEntity.BOTTLE_INPUT_SLOT,
                        glassBottle, Direction.DOWN),
                "Condenser DOWN must reject bottle insertion");
        helper.assertTrue(!condenser.canExtract(AtmosphericCondenserBlockEntity.OUTPUT_SLOT,
                        ItemStack.EMPTY, Direction.UP),
                "Condenser UP must reject extraction");

        helper.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void machineCapabilitySimulateNoSideEffect(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.GENE_SPLICER.get());
        GeneSplicerBlockEntity splicer = (GeneSplicerBlockEntity) helper.getBlockEntity(pos);

        ItemStack seed = ModItems.FIBER_REED_SEEDS.get().getDefaultInstance();
        ItemStack seedCopy = seed.copy();

        // 模拟插入：库存不变
        var capHandler = splicer.getCapability(
                net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER, Direction.UP).orElse(null);
        helper.assertTrue(capHandler != null, "Splicer UP capability must be present");

        ItemStack remaining = capHandler.insertItem(0, seed, true);
        helper.assertTrue(remaining.isEmpty(), "Simulated insert of 1 seed into empty slot must succeed fully");
        // 库存应保持空
        helper.assertTrue(splicer.getItem(GeneSplicerBlockEntity.SEED_A_SLOT).isEmpty(),
                "Simulated insert must not modify inventory");
        // 输入 stack 不应被修改
        helper.assertTrue(ItemStack.matches(seed, seedCopy), "Simulated insert must not modify input stack");

        // 实际插入：库存变化
        ItemStack remainingReal = capHandler.insertItem(0, seed, false);
        helper.assertTrue(remainingReal.isEmpty(), "Real insert must succeed");
        helper.assertTrue(!splicer.getItem(GeneSplicerBlockEntity.SEED_A_SLOT).isEmpty(),
                "Real insert must modify inventory");
        helper.assertTrue(splicer.getItem(GeneSplicerBlockEntity.SEED_A_SLOT).getCount() == 1,
                "Seed slot must hold exactly 1 item");

        // 模拟抽取：库存不变（output 槽当前为空，模拟抽取返回 EMPTY）
        ItemStack simulatedExtract = capHandler.extractItem(0, 1, true);
        // SEED_A 不在 UP 槽位映射中？等等 - UP 映射到 [SEED_A, SEED_B]，所以 slot 0 是 SEED_A
        // 但 canExtract(SEED_A, ..., UP) 返回 false，所以模拟抽取返回 EMPTY
        helper.assertTrue(simulatedExtract.isEmpty(),
                "Simulated extract of SEED_A from UP must be rejected (canExtract=false)");

        // 模拟插入到非空亲本槽（已满，limit=1）：返回原 stack
        ItemStack extraSeed = ModItems.PROTEIN_SOY_SEEDS.get().getDefaultInstance();
        ItemStack extraCopy = extraSeed.copy();
        ItemStack remainingFull = capHandler.insertItem(0, extraSeed, true);
        helper.assertTrue(remainingFull.getCount() == 1,
                "Simulated insert into full slot must return full remaining");
        helper.assertTrue(ItemStack.matches(extraSeed, extraCopy),
                "Simulated insert into full slot must not modify input stack");

        // 抽取路径：使用 DOWN capability 抽取 OUTPUT（但 output 为空）
        var downHandler = splicer.getCapability(
                net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER, Direction.DOWN).orElse(null);
        helper.assertTrue(downHandler != null, "Splicer DOWN capability must be present");
        ItemStack emptyExtract = downHandler.extractItem(0, 1, true);
        helper.assertTrue(emptyExtract.isEmpty(), "Simulated extract from empty OUTPUT must return EMPTY");

        helper.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void machineCapabilityLifecycle(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.SERUM_BOTTLER.get());
        SerumBottlerBlockEntity bottler = (SerumBottlerBlockEntity) helper.getBlockEntity(pos);

        // 第一次查询：创建并缓存
        var handler1 = bottler.getCapability(
                net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER, Direction.UP);
        helper.assertTrue(handler1.isPresent(), "First capability query must return handler");

        // 同方向第二次查询：应返回同一缓存实例
        var handler2 = bottler.getCapability(
                net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER, Direction.UP);
        helper.assertTrue(handler2.isPresent(), "Second capability query must return handler");

        // invalidateCaps 后旧 LazyOptional 应失效
        bottler.invalidateCaps();
        helper.assertTrue(!handler1.isPresent(),
                "Old LazyOptional must be invalid after invalidateCaps()");

        // reviveCaps 后重新查询应得到新实例
        bottler.reviveCaps();
        var handler3 = bottler.getCapability(
                net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER, Direction.UP);
        helper.assertTrue(handler3.isPresent(), "Capability must be re-queryable after reviveCaps()");

        // 水平方向应共享同一 handler 实例（按角色映射缓存）
        var northHandler = bottler.getCapability(
                net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER, Direction.NORTH);
        var southHandler = bottler.getCapability(
                net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER, Direction.SOUTH);
        // 两个 LazyOptional 应是同一实例（按角色映射共享）
        helper.assertTrue(northHandler == southHandler,
                "NORTH and SOUTH must share same LazyOptional (role-based cache)");

        helper.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void incubatorTopSeedAutoInput(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.BIO_INCUBATOR.get());
        BioIncubatorBlockEntity incubator = (BioIncubatorBlockEntity) helper.getBlockEntity(pos);

        // 初始状态：种子槽为空
        helper.assertTrue(incubator.getItem(BioIncubatorBlockEntity.SEED_SLOT).isEmpty(),
                "Seed slot must start empty");

        // 通过 UP capability 插入种子（模拟漏斗从顶部输入）
        var upHandler = incubator.getCapability(
                net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER, Direction.UP).orElse(null);
        helper.assertTrue(upHandler != null, "Incubator UP capability must be present");

        ItemStack seed = ModItems.FIBER_REED_SEEDS.get().getDefaultInstance();
        ItemStack remaining = upHandler.insertItem(0, seed, false);
        helper.assertTrue(remaining.isEmpty(), "Seed insert must succeed");
        helper.assertTrue(!incubator.getItem(BioIncubatorBlockEntity.SEED_SLOT).isEmpty(),
                "Seed slot must be populated after insert");
        helper.assertTrue(incubator.getItem(BioIncubatorBlockEntity.SEED_SLOT).getCount() == 1,
                "Seed slot must hold exactly 1 item");

        // 通过 NORTH capability 尝试插入种子到 NUTRITION 槽：应被拒绝（horizontal 不接受种子）
        var northHandler = incubator.getCapability(
                net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER, Direction.NORTH).orElse(null);
        ItemStack remaining2 = northHandler.insertItem(0, seed, false);
        helper.assertTrue(!remaining2.isEmpty(), "Seed insert to NUTRITION slot must be rejected");

        // 通过 UP capability 尝试插入 NUTRITION（biochem）：UP 不接受 NUTRITION
        ItemStack biochem = ModItems.BIOCHEMICAL_SOLUTION.get().getDefaultInstance();
        ItemStack remaining3 = upHandler.insertItem(0, biochem, false);
        helper.assertTrue(!remaining3.isEmpty(), "Biochem insert to UP must be rejected (UP is seed-only)");

        helper.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void geneSplicerWorldlyContainerHopperPath(GameTestHelper helper) {
        // §9.3 验证 GeneSplicer 改造为 WorldlyContainer 后的漏斗路径
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.GENE_SPLICER.get());
        GeneSplicerBlockEntity splicer = (GeneSplicerBlockEntity) helper.getBlockEntity(pos);

        // getSlotsForFace 与 visibleSlots 一致
        int[] upFace = splicer.getSlotsForFace(Direction.UP);
        int[] upPolicy = splicer.visibleSlots(Direction.UP);
        helper.assertTrue(upFace.length == upPolicy.length,
                "getSlotsForFace must match visibleSlots");

        // canPlaceItemThroughFace 委托给 canInsert
        ItemStack seed = ModItems.FIBER_REED_SEEDS.get().getDefaultInstance();
        helper.assertTrue(splicer.canPlaceItemThroughFace(GeneSplicerBlockEntity.SEED_A_SLOT, seed, Direction.UP),
                "canPlaceItemThroughFace must delegate to canInsert");

        // canTakeItemThroughFace 委托给 canExtract
        helper.assertTrue(splicer.canTakeItemThroughFace(GeneSplicerBlockEntity.OUTPUT_SLOT, ItemStack.EMPTY, Direction.DOWN),
                "canTakeItemThroughFace must delegate to canExtract");
        helper.assertTrue(!splicer.canTakeItemThroughFace(GeneSplicerBlockEntity.SEED_A_SLOT, ItemStack.EMPTY, Direction.DOWN),
                "SEED_A must not be takable through face");

        // 通过 WorldlyContainer.setItem 插入种子（漏斗路径），应触发 normalizeInsertedStack
        splicer.setItem(GeneSplicerBlockEntity.SEED_A_SLOT, seed);
        ItemStack inserted = splicer.getItem(GeneSplicerBlockEntity.SEED_A_SLOT);
        helper.assertTrue(!inserted.isEmpty() && inserted.getCount() == 1,
                "setItem must normalize seed count to 1");
        // ensureGeneData 应补齐 GENE 标签
        helper.assertTrue(inserted.hasTag() && inserted.getTag().contains(GeneticSeedItem.GENE_SPEED),
                "setItem must ensure gene data via normalizeInsertedStack");

        helper.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        helper.succeed();
    }

    // === v1.1.7 hotfix 边界回归测试（问题 1/2/3/5/7） ===

    /** 问题 1：IItemHandler 异物插入不得覆盖原物品（防止物品复制/丢失）。 */
    @GameTest(template = EMPTY_TEMPLATE)
    public static void itemHandlerRejectsMismatchedInsert(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.GENE_SPLICER.get());
        GeneSplicerBlockEntity splicer = (GeneSplicerBlockEntity) helper.getBlockEntity(pos);

        ItemStack first = new ItemStack(ModItems.FIBER_REED_SEEDS.get());
        splicer.setItem(GeneSplicerBlockEntity.SEED_A_SLOT, first);

        var upHandler = splicer.getCapability(
                net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER, Direction.UP).orElse(null);
        helper.assertTrue(upHandler != null, "Splicer UP capability must be present");

        ItemStack mismatched = new ItemStack(ModItems.PROTEIN_SOY_SEEDS.get());
        ItemStack mismatchedCopy = mismatched.copy();
        ItemStack remaining = upHandler.insertItem(0, mismatched, false);

        helper.assertTrue(remaining.getCount() == 1,
                "Mismatched insert must return full remaining (no silent accept)");
        helper.assertTrue(ItemStack.matches(remaining, mismatchedCopy),
                "Returned remaining must match input (no item duplication)");
        ItemStack slotAfter = splicer.getItem(GeneSplicerBlockEntity.SEED_A_SLOT);
        helper.assertTrue(slotAfter.is(ModItems.FIBER_REED_SEEDS.get()),
                "Existing item must not be overwritten by mismatched insert");
        helper.assertTrue(slotAfter.getCount() == 1,
                "Existing item count must remain 1");

        helper.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        helper.succeed();
    }

    /** 问题 2：种子/亲本槽严格 1，漏斗连续输入不得堆叠。 */
    @GameTest(template = EMPTY_TEMPLATE)
    public static void hopperCannotStackOccupiedSeedSlot(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.BIO_INCUBATOR.get());
        BioIncubatorBlockEntity incubator = (BioIncubatorBlockEntity) helper.getBlockEntity(pos);

        var upHandler = incubator.getCapability(
                net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER, Direction.UP).orElse(null);
        helper.assertTrue(upHandler != null, "Incubator UP capability must be present");

        ItemStack first = new ItemStack(ModItems.FIBER_REED_SEEDS.get());
        ItemStack remaining1 = upHandler.insertItem(0, first, false);
        helper.assertTrue(remaining1.isEmpty(), "First seed insert must succeed");
        helper.assertTrue(incubator.getItem(BioIncubatorBlockEntity.SEED_SLOT).getCount() == 1,
                "Seed slot must hold 1 after first insert");

        ItemStack second = new ItemStack(ModItems.FIBER_REED_SEEDS.get());
        ItemStack remaining2 = upHandler.insertItem(0, second, false);
        helper.assertTrue(remaining2.getCount() == 1,
                "Second seed insert must be rejected (slot occupied, no stacking)");
        helper.assertTrue(incubator.getItem(BioIncubatorBlockEntity.SEED_SLOT).getCount() == 1,
                "Seed slot must remain at 1 after rejected insert");

        helper.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        helper.succeed();
    }

    /** 问题 3：产物抽走后比较器信号必须从 15 归 0（提前 return 路径仍调用 updateComparatorIfChanged）。 */
    @GameTest(template = EMPTY_TEMPLATE)
    public static void comparatorDropsToZeroAfterExtraction(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.GENE_SPLICER.get());
        GeneSplicerBlockEntity splicer = (GeneSplicerBlockEntity) helper.getBlockEntity(pos);

        // 初始：output 非空 → 比较器 = 15
        CompoundTag withOutput = new CompoundTag();
        withOutput.put("Output", new ItemStack(ModItems.FIBER_REED_SEEDS.get()).save(new CompoundTag()));
        splicer.load(withOutput);
        helper.assertTrue(splicer.getComparatorSignal() == 15,
                "Splicer with output must emit 15");
        // 触发 tick 让 lastComparatorSignal 缓存为 15（走 !splicing 提前 return 路径）
        GeneSplicerBlockEntity.tick(helper.getLevel(), pos, splicer.getBlockState(), splicer);

        // 抽走产物
        splicer.setItem(GeneSplicerBlockEntity.OUTPUT_SLOT, ItemStack.EMPTY);
        helper.assertTrue(splicer.getComparatorSignal() == 0,
                "getComparatorSignal must return 0 immediately after output cleared");

        // 再次 tick：!splicing 提前 return 路径必须调用 updateComparatorIfChanged
        GeneSplicerBlockEntity.tick(helper.getLevel(), pos, splicer.getBlockState(), splicer);
        helper.assertTrue(splicer.getComparatorSignal() == 0,
                "Comparator must remain 0 after tick (no stale 15 cache)");

        helper.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        helper.succeed();
    }

    /** 问题 5：HIGH 未供电时灌装机不得选取配方（cachedRecipe/maxProgress 保持 0）。 */
    @GameTest(template = EMPTY_TEMPLATE)
    public static void bottlerSkipsRecipeSelectionWhenRedstoneBlocked(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.SERUM_BOTTLER.get());
        SerumBottlerBlockEntity bottler = (SerumBottlerBlockEntity) helper.getBlockEntity(pos);

        // 输入合法配方材料（莓合成配方）
        bottler.setItem(0, new ItemStack(ModItems.PLANT_FIBER.get()));
        bottler.setItem(1, new ItemStack(ModItems.INDUSTRIAL_ETHANOL.get()));
        bottler.setItem(2, new ItemStack(ModItems.BIOCHEMICAL_SOLUTION.get()));

        // HIGH 模式 + 未供电 → 红石阻塞
        bottler.getRedstoneState().setMode(RedstoneControlMode.HIGH);
        bottler.getRedstoneState().updatePowered(false);
        helper.assertFalse(bottler.getRedstoneState().isProcessingAllowed(),
                "HIGH mode without power must block processing");

        // 触发 tick：应跳过配方选取
        SerumBottlerBlockEntity.tick(helper.getLevel(), pos, bottler.getBlockState(), bottler);
        helper.assertTrue(bottler.getMaxProgress() == 0,
                "Bottler must not select recipe when redstone blocked (maxProgress must stay 0)");
        helper.assertTrue(bottler.getProgress() == 0,
                "Bottler must not advance progress when redstone blocked");
        helper.assertTrue(bottler.getComparatorSignal() == 0,
                "Blocked bottler must emit comparator 0 (no batch started)");

        // 供电恢复后应立即选取配方
        bottler.getRedstoneState().updatePowered(true);
        SerumBottlerBlockEntity.tick(helper.getLevel(), pos, bottler.getBlockState(), bottler);
        helper.assertTrue(bottler.getMaxProgress() > 0,
                "Bottler must select recipe after power restored");

        helper.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        helper.succeed();
    }

    /** 问题 7：拼接机冻结期间状态稳定（不推进、不消耗、不抛异常）。 */
    @GameTest(template = EMPTY_TEMPLATE)
    public static void splicerFrozenStaysStableAcrossManyTicks(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.GENE_SPLICER.get());
        GeneSplicerBlockEntity splicer = (GeneSplicerBlockEntity) helper.getBlockEntity(pos);

        ItemStack seedA = new ItemStack(ModItems.FIBER_REED_SEEDS.get());
        ItemStack seedB = new ItemStack(ModItems.PROTEIN_SOY_SEEDS.get());
        splicer.setItem(GeneSplicerBlockEntity.SEED_A_SLOT, seedA);
        splicer.setItem(GeneSplicerBlockEntity.SEED_B_SLOT, seedB);
        helper.assertTrue(splicer.isSplicing(), "Two parents must start splicing");

        // 推进 1 tick（IGNORE 默认允许加工）
        GeneSplicerBlockEntity.tick(helper.getLevel(), pos, splicer.getBlockState(), splicer);
        helper.assertTrue(splicer.getSpliceProgress() == 1,
                "Initial tick must advance progress to 1");

        // HIGH 模式 + 未供电 → 冻结
        splicer.getRedstoneState().setMode(RedstoneControlMode.HIGH);
        splicer.getRedstoneState().updatePowered(false);

        // 100 tick 冻结验证（远超原 syncToClient 间隔 5，验证删除冗余同步后行为仍正确）
        for (int i = 0; i < 100; i++) {
            GeneSplicerBlockEntity.tick(helper.getLevel(), pos, splicer.getBlockState(), splicer);
        }
        helper.assertTrue(splicer.getSpliceProgress() == 1,
                "Frozen splice must stay at progress=1 across 100 ticks");
        helper.assertTrue(splicer.isSplicing(),
                "Frozen splice must remain in splicing state");
        helper.assertFalse(splicer.getSeedA().isEmpty() || splicer.getSeedB().isEmpty(),
                "Frozen splice must not consume parents");
        helper.assertTrue(splicer.getComparatorSignal() == 1,
                "Frozen splicer at progress=1 must emit comparator signal 1");

        // 恢复供电：进度立即恢复推进
        splicer.getRedstoneState().updatePowered(true);
        GeneSplicerBlockEntity.tick(helper.getLevel(), pos, splicer.getBlockState(), splicer);
        helper.assertTrue(splicer.getSpliceProgress() == 2,
                "Resumed splice must advance progress again");

        helper.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        helper.succeed();
    }

    private static void assertEffectId(GameTestHelper helper, ItemStack serum, String expected) {
        var info = CyberCultivatorAPI.getSerumEffectInfo(serum);
        helper.assertTrue(info != null && expected.equals(info.effectId()), "Unexpected serum effect registry ID");
    }

    private static void assertTaggedVariant(GameTestHelper helper, ItemStack stack, net.minecraft.world.item.Item item,
                                            String tagKey, int expectedValue) {
        helper.assertTrue(stack.is(item), "Creative tab variant item order mismatch");
        helper.assertTrue(stack.hasTag() && stack.getTag().getInt(tagKey) == expectedValue,
                "Creative tab variant NBT mismatch for " + tagKey + "=" + expectedValue);
    }

    private static void assertBalancedSeedVariant(GameTestHelper helper, ItemStack stack,
                                                  net.minecraft.world.item.Item item, int expectedValue) {
        helper.assertTrue(stack.is(item), "Creative tab seed order mismatch");
        helper.assertTrue(GeneticSeedItem.getGene(stack, GeneticSeedItem.GENE_SPEED) == expectedValue
                        && GeneticSeedItem.getGene(stack, GeneticSeedItem.GENE_YIELD) == expectedValue
                        && GeneticSeedItem.getGene(stack, GeneticSeedItem.GENE_POTENCY) == expectedValue,
                "Creative tab seed genes must be balanced at " + expectedValue);
    }

    private static void assertSeedTrade(GameTestHelper helper, VillagerProfession profession, Item expectedSeed) {
        Int2ObjectOpenHashMap<List<VillagerTrades.ItemListing>> trades = new Int2ObjectOpenHashMap<>();
        for (int level = 1; level <= 5; level++) trades.put(level, new ArrayList<>());
        VillagerTradeEvents.addSeedTrades(new VillagerTradesEvent(trades, profession));
        helper.assertTrue(trades.get(2).size() == 1,
                "Each configured profession must receive exactly one apprentice seed trade");
        MerchantOffer offer = trades.get(2).get(0).getOffer(
                helper.makeMockSurvivalPlayer(), RandomSource.create(1234L));
        helper.assertTrue(offer != null
                        && offer.getBaseCostA().is(Items.EMERALD) && offer.getBaseCostA().getCount() == 3
                        && offer.getResult().is(expectedSeed) && offer.getResult().getCount() == 2
                        && offer.getMaxUses() == 8 && offer.getXp() == 10
                        && Math.abs(offer.getPriceMultiplier() - 0.05F) < 0.0001F,
                "Apprentice seed trade must cost 3 emeralds for 2 seeds with 8 uses, 10 XP and 0.05 pricing");
        helper.assertTrue(trades.get(1).isEmpty() && trades.get(3).isEmpty(),
                "Seed trade must be registered only at apprentice level");
    }

    private static int countInventoryItem(Player player, Item item) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) count += stack.getCount();
        }
        return count;
    }

    private static int countPatchouliGuides(Player player) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            ResourceLocation key = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (ResourceLocation.fromNamespaceAndPath("patchouli", "guide_book").equals(key)
                    && stack.hasTag()
                    && "cybercultivator:bio_synthesis_guide".equals(
                    stack.getTag().getString("patchouli:book"))) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static byte[] readResourceBytes(String path) {
        try (InputStream stream = ModGameTests.class.getClassLoader().getResourceAsStream(path)) {
            return stream == null ? new byte[0] : stream.readAllBytes();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read test resource: " + path, exception);
        }
    }

    private static boolean closeTo(double actual, double expected) {
        return Math.abs(actual - expected) < 0.0000001D;
    }

    private static final class ConsumeListener {
        private final Consumer<SerumConsumeEvent> action;

        private ConsumeListener(Consumer<SerumConsumeEvent> action) {
            this.action = action;
        }

        @SubscribeEvent
        public void onConsume(SerumConsumeEvent event) {
            action.accept(event);
        }
    }

    private static final class CropListener {
        private final Consumer<CropMatureEvent> action;

        private CropListener(Consumer<CropMatureEvent> action) {
            this.action = action;
        }

        @SubscribeEvent
        public void onMature(CropMatureEvent event) {
            action.accept(event);
        }
    }
}

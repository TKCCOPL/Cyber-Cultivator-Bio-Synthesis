package com.TKCCOPL.gametest;

import com.TKCCOPL.Config;
import com.TKCCOPL.api.BottlerInfo;
import com.TKCCOPL.api.CyberCultivatorAPI;
import com.TKCCOPL.block.entity.BioIncubatorBlockEntity;
import com.TKCCOPL.block.entity.AtmosphericCondenserBlockEntity;
import com.TKCCOPL.block.entity.GeneSplicerBlockEntity;
import com.TKCCOPL.block.entity.SerumBottlerBlockEntity;
import com.TKCCOPL.cybercultivator;
import com.TKCCOPL.event.CropMatureEvent;
import com.TKCCOPL.event.GeneSpliceEvent;
import com.TKCCOPL.event.SerumConsumeEvent;
import com.TKCCOPL.event.SerumCraftEvent;
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
import com.TKCCOPL.network.S02DetectionSyncPacket;
import com.TKCCOPL.recipe.IncubatorOutputRecipe;
import com.TKCCOPL.recipe.IncubatorOutputSerializer;
import com.TKCCOPL.recipe.RecipeOrdering;
import com.TKCCOPL.recipe.SerumRecipe;
import com.TKCCOPL.recipe.SerumRecipeSerializer;
import com.TKCCOPL.recipe.SerumRecipeIds;
import com.google.gson.JsonParser;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.fml.ModList;

import java.util.ArrayList;
import java.util.List;
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
        helper.assertTrue(splice.getSpeed() == 9, "KubeJS geneSplice listener must modify the Forge event");

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

        SerumConsumeEvent bounds = new SerumConsumeEvent(explicitPlayer, explicitStack, item.getSerumEffect(), 5, 20, 1);
        bounds.setActivity(100);
        bounds.setDuration(0);
        bounds.setAmplifier(-1);
        helper.assertTrue(bounds.getActivity() == 15 && bounds.getDuration() == 1 && bounds.getAmplifier() == 0,
                "Event setters must enforce their public bounds");
        helper.succeed();
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
    public static void incubatorAutoInjectionAndDualOutputs(GameTestHelper helper) {
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
        helper.assertTrue(incubator.canTakeItemThroughFace(BioIncubatorBlockEntity.BOTTLE_OUTPUT_SLOT,
                incubator.getItem(BioIncubatorBlockEntity.BOTTLE_OUTPUT_SLOT), Direction.DOWN),
                "The bottom face must expose the bottle output");

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
            helper.assertTrue(incubator.getItem(BioIncubatorBlockEntity.BOTTLE_OUTPUT_SLOT).is(Items.GLASS_BOTTLE)
                            && incubator.getItem(BioIncubatorBlockEntity.BOTTLE_OUTPUT_SLOT).getCount() == 1,
                    "Each timed water injection must return exactly one empty bottle");
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

            incubator.setItem(BioIncubatorBlockEntity.BOTTLE_OUTPUT_SLOT,
                    new ItemStack(Items.GLASS_BOTTLE, Items.GLASS_BOTTLE.getDefaultInstance().getMaxStackSize()));
            incubator.setItem(BioIncubatorBlockEntity.PURITY_SLOT,
                    new ItemStack(ModItems.PURIFIED_WATER_BOTTLE.get()));
            helper.assertTrue(incubator.getItem(BioIncubatorBlockEntity.PURITY_SLOT)
                            .is(ModItems.PURIFIED_WATER_BOTTLE.get()),
                    "Water input must wait while the bottle output is full");
            incubator.setItem(BioIncubatorBlockEntity.BOTTLE_OUTPUT_SLOT, ItemStack.EMPTY);
        });
        helper.runAfterDelay(BioIncubatorBlockEntity.INPUT_INJECTION_INTERVAL_TICKS * 3 + 3, () -> {
            helper.assertTrue(incubator.getItem(BioIncubatorBlockEntity.PURITY_SLOT).isEmpty()
                            && incubator.getItem(BioIncubatorBlockEntity.BOTTLE_OUTPUT_SLOT).is(Items.GLASS_BOTTLE),
                    "Waiting water input must resume on a later interval after output space becomes available");

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
        int expectedMutationPermille = (int) Math.round(Math.max(0.0D, Math.min(1.0D,
                Config.mutationChanceBase
                        + 4 * Config.mutationChancePerGen
                        + 5 * Config.mutationChancePerGeneDiff)) * 1000.0D);
        helper.assertTrue(splicer.getPredictedGeneration() == 5,
                "GUI generation preview must use the higher parent generation plus one");
        helper.assertTrue(splicer.getPredictedMutationPermille() == expectedMutationPermille,
                "GUI mutation preview must match the server-side splice formula");
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
        ItemStack water = condenserMenu.quickMoveStack(player, 0);
        helper.assertTrue(water.getCount() == 4 && condenser.getOutput().isEmpty(),
                "Shift-moving condenser stock must transfer the complete output");
        helper.assertTrue(condenser.getProgress() == 0,
                "Shift-moving condenser stock must reset progress like every other extraction path");
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
        helper.assertTrue(condenser.isAutoInject(), "Existing and new condensers must default auto injection to on");
        helper.assertFalse(condenser.isPaused(), "New condensers must start production unpaused");
        CompoundTag inProgress = new CompoundTag();
        inProgress.putInt("Progress", 10);
        condenser.load(inProgress);
        Player player = helper.makeMockSurvivalPlayer();
        AtmosphericCondenserMenu condenserMenu =
                (AtmosphericCondenserMenu) condenser.createMenu(4, player.getInventory(), player);
        helper.assertTrue(condenserMenu.clickMenuButton(player, AtmosphericCondenserMenu.BUTTON_TOGGLE_PAUSED),
                "Pause button must be handled by the condenser menu");
        helper.assertTrue(condenser.isPaused(), "Pause button must pause condenser production");
        AtmosphericCondenserBlockEntity.tick(helper.getLevel(), condenserPos, condenser.getBlockState(), condenser);
        helper.assertTrue(condenser.getProgress() == 10, "Paused condensers must not advance production");
        condenser.toggleAutoInject();
        CompoundTag saved = condenser.saveWithoutMetadata();
        AtmosphericCondenserBlockEntity restored =
                new AtmosphericCondenserBlockEntity(condenser.getBlockPos(), condenser.getBlockState());
        restored.load(saved);
        helper.assertFalse(restored.isAutoInject(), "Auto injection mode must survive NBT round-trip");
        helper.assertTrue(restored.isPaused(), "Paused production state must survive NBT round-trip");
        helper.assertTrue(condenserMenu.clickMenuButton(player, AtmosphericCondenserMenu.BUTTON_TOGGLE_PAUSED),
                "Resume button must be handled by the condenser menu");
        AtmosphericCondenserBlockEntity.tick(helper.getLevel(), condenserPos, condenser.getBlockState(), condenser);
        helper.assertTrue(condenser.getProgress() == 11, "Resumed condensers must continue from stored progress");
        AtmosphericCondenserBlockEntity legacy =
                new AtmosphericCondenserBlockEntity(condenser.getBlockPos(), condenser.getBlockState());
        legacy.load(new CompoundTag());
        helper.assertTrue(legacy.isAutoInject(), "Legacy NBT without AutoInject must remain enabled");
        helper.assertFalse(legacy.isPaused(), "Legacy NBT without Paused must remain operational");
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
        for (String id : ids) {
            ResourceLocation key = ResourceLocation.fromNamespaceAndPath(cybercultivator.MODID, id);
            var effect = net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getValue(key);
            helper.assertTrue(effect != null, "NeuralOverload variant must be registered: " + id);
        }
        // 三个子类必须各自是独立类型，而非共享旧 SOURCE_MAP 的旧 NeuralOverloadEffect
        helper.assertTrue(ModEffects.NEURAL_OVERLOAD_S01.get() != ModEffects.NEURAL_OVERLOAD.get(),
                "S-01 overload must be a distinct effect instance");
        helper.assertTrue(ModEffects.NEURAL_OVERLOAD_S02.get() != ModEffects.NEURAL_OVERLOAD.get(),
                "S-02 overload must be a distinct effect instance");
        helper.assertTrue(ModEffects.NEURAL_OVERLOAD_S03.get() != ModEffects.NEURAL_OVERLOAD.get(),
                "S-03 overload must be a distinct effect instance");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void neuralOverloadS01AppliesWitherAndHunger(GameTestHelper helper) {
        // 修复 4：S-01 过载必须是凋零 + 饥饿，且不得串线到 S-02/S-03 的副作用
        Player player = helper.makeMockSurvivalPlayer();
        player.getInventory().clearContent();
        ModEffects.NEURAL_OVERLOAD_S01.get().applyEffectTick(player, 0);
        helper.assertTrue(player.getEffect(MobEffects.WITHER) != null, "S-01 overload must apply Wither");
        helper.assertTrue(player.getEffect(MobEffects.HUNGER) != null, "S-01 overload must apply Hunger");
        helper.assertTrue(player.getEffect(MobEffects.BLINDNESS) == null, "S-01 overload must not apply Blindness (S-02 crosstalk)");
        helper.assertTrue(player.getEffect(MobEffects.POISON) == null, "S-01 overload must not apply Poison (S-03 crosstalk)");
        helper.assertTrue(player.getEffect(MobEffects.MOVEMENT_SLOWDOWN) == null,
                "S-01 overload must not apply Slowness (legacy overload crosstalk)");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void neuralOverloadS02AppliesBlindnessAndHunger(GameTestHelper helper) {
        // 修复 4：S-02 过载必须是失明 + 饥饿
        Player player = helper.makeMockSurvivalPlayer();
        player.getInventory().clearContent();
        ModEffects.NEURAL_OVERLOAD_S02.get().applyEffectTick(player, 0);
        helper.assertTrue(player.getEffect(MobEffects.BLINDNESS) != null, "S-02 overload must apply Blindness");
        helper.assertTrue(player.getEffect(MobEffects.HUNGER) != null, "S-02 overload must apply Hunger");
        helper.assertTrue(player.getEffect(MobEffects.WITHER) == null, "S-02 overload must not apply Wither (S-01 crosstalk)");
        helper.assertTrue(player.getEffect(MobEffects.POISON) == null, "S-02 overload must not apply Poison (S-03 crosstalk)");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void neuralOverloadS03AppliesSlownessAndPoison(GameTestHelper helper) {
        // 修复 4：S-03 过载必须是缓慢 + 中毒
        Player player = helper.makeMockSurvivalPlayer();
        player.getInventory().clearContent();
        ModEffects.NEURAL_OVERLOAD_S03.get().applyEffectTick(player, 0);
        helper.assertTrue(player.getEffect(MobEffects.MOVEMENT_SLOWDOWN) != null, "S-03 overload must apply Slowness");
        helper.assertTrue(player.getEffect(MobEffects.POISON) != null, "S-03 overload must apply Poison");
        helper.assertTrue(player.getEffect(MobEffects.WITHER) == null, "S-03 overload must not apply Wither (S-01 crosstalk)");
        helper.assertTrue(player.getEffect(MobEffects.BLINDNESS) == null, "S-03 overload must not apply Blindness (S-02 crosstalk)");
        helper.assertTrue(player.getEffect(MobEffects.HUNGER) == null,
                "S-03 overload must not apply Hunger (legacy/S-01/S-02 crosstalk)");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void s02SerumDoesNotApplyGlowingToNearbyEntities(GameTestHelper helper) {
        // 修复 3：S-02 私有轮廓 —— 不再使用 MobEffects.GLOWING，避免对附近实体造成全服可见的串扰
        Player drinker = helper.makeMockSurvivalPlayer();
        net.minecraft.world.entity.animal.Cow nearby = helper.spawn(net.minecraft.world.entity.EntityType.COW,
                new BlockPos(2, 1, 2));
        // 直接调用 applyEffectTick 绕过 60-tick 节流，验证 S-02 内部逻辑
        try {
            ModEffects.VISUAL_ENHANCEMENT.get().applyEffectTick(drinker, 0);
        } catch (Exception ignored) {
            // 模拟玩家可能没有完整网络连接，发包异常不影响 GLOWING 副作用的断言
        }
        helper.assertTrue(drinker.getEffect(MobEffects.NIGHT_VISION) != null,
                "S-02 must still grant Night Vision to the drinker");
        helper.assertTrue(drinker.getEffect(MobEffects.FIRE_RESISTANCE) != null,
                "S-02 must still grant Fire Resistance to the drinker");
        helper.assertTrue(nearby.getEffect(MobEffects.GLOWING) == null,
                "S-02 must not apply GLOWING to nearby entities (v1.1.7 regression)");
        helper.assertTrue(drinker.getEffect(MobEffects.GLOWING) == null,
                "S-02 must not apply GLOWING to the drinker either");
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
        helper.assertTrue(defaults.s01BaseDuration() == 500, "Default s01BaseDuration must be 500");
        helper.assertTrue(defaults.s02BaseDuration() == 600, "Default s02BaseDuration must be 600");
        helper.assertTrue(defaults.s03BaseDuration() == 300, "Default s03BaseDuration must be 300");
        helper.assertTrue(defaults.glowScanRangeCap() == 48, "Default glowScanRangeCap must be 48");
        helper.assertTrue(defaults.beltScanRange() == 3, "Default beltScanRange must be 3");
        helper.assertTrue(Math.abs(defaults.packHealThreshold() - 6.0F) < 0.001F,
                "Default packHealThreshold must be 6.0");
        helper.assertTrue(defaults.packHealCooldown() == 1200, "Default packHealCooldown must be 1200");

        GameplayConfigSnapshot fromServer = GameplayConfigSnapshot.fromServerConfig();
        helper.assertTrue(fromServer.s01BaseDuration() == Config.s01BaseDuration,
                "Snapshot.fromServerConfig must mirror Config.s01BaseDuration");
        helper.assertTrue(fromServer.glowScanRangeCap() == Config.glowScanRangeCap,
                "Snapshot.fromServerConfig must mirror Config.glowScanRangeCap");
        helper.assertTrue(fromServer.beltScanRange() == Config.beltScanRange,
                "Snapshot.fromServerConfig must mirror Config.beltScanRange");
        helper.assertTrue(fromServer.maturationThreshold() == Config.maturationThreshold,
                "Snapshot.fromServerConfig must mirror Config.maturationThreshold");
        helper.assertTrue(fromServer.packHealThreshold() == Config.packHealThreshold,
                "Snapshot.fromServerConfig must mirror Config.packHealThreshold");
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

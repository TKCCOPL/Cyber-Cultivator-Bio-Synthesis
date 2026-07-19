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
import com.TKCCOPL.event.CropMatureEvent;
import com.TKCCOPL.event.GeneSpliceEvent;
import com.TKCCOPL.event.SerumConsumeEvent;
import com.TKCCOPL.event.SerumCraftEvent;
import com.TKCCOPL.init.ModBlocks;
import com.TKCCOPL.init.CreativeTabVariants;
import com.TKCCOPL.init.ModItems;
import com.TKCCOPL.item.GeneticSeedItem;
import com.TKCCOPL.item.SynapticSerumItem;
import com.TKCCOPL.menu.AtmosphericCondenserMenu;
import com.TKCCOPL.menu.GeneSplicerMenu;
import com.TKCCOPL.menu.SerumBottlerMenu;
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
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
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

        // 冷凝器按钮 ID = 2（M1：避开现有 0=暂停、1=注入开关）
        BlockPos condenserPos = new BlockPos(2, 1, 1);
        helper.setBlock(condenserPos, ModBlocks.ATMOSPHERIC_CONDENSER.get());
        AtmosphericCondenserBlockEntity condenser =
                (AtmosphericCondenserBlockEntity) helper.getBlockEntity(condenserPos);
        AtmosphericCondenserMenu condenserMenu =
                (AtmosphericCondenserMenu) condenser.createMenu(2, player.getInventory(), player);
        helper.assertTrue(condenser.getRedstoneState().getMode() == RedstoneControlMode.IGNORE,
                "Condenser must start in IGNORE mode");
        helper.assertTrue(condenserMenu.clickMenuButton(player, AtmosphericCondenserMenu.BUTTON_CYCLE_REDSTONE),
                "Condenser clickMenuButton must handle redstone cycle (id=2)");
        helper.assertTrue(condenser.getRedstoneState().getMode() == RedstoneControlMode.HIGH,
                "Condenser first click must cycle IGNORE → HIGH");
        // 验证现有按钮（0=自动注入、1=暂停）仍然工作，未因 RS 按钮 ID=2 冲突
        helper.assertTrue(condenserMenu.clickMenuButton(player, AtmosphericCondenserMenu.BUTTON_TOGGLE_PAUSED),
                "Existing pause button (id=1) must still work alongside redstone button");
        helper.assertTrue(condenser.isPaused(), "Pause button must pause condenser");
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
        helper.assertTrue(incDown.length == 2, "Incubator DOWN must expose 2 output slots");
        int[] incNull = incubator.visibleSlots(null);
        helper.assertTrue(incNull.length == 6, "Incubator null side must expose all 6 slots");

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

        // 冷凝器：UP 无能力槽；其他面=纯净水只出
        helper.setBlock(pos, ModBlocks.ATMOSPHERIC_CONDENSER.get());
        AtmosphericCondenserBlockEntity condenser = (AtmosphericCondenserBlockEntity) helper.getBlockEntity(pos);
        int[] condUp = condenser.visibleSlots(Direction.UP);
        helper.assertTrue(condUp.length == 0, "Condenser UP must expose 0 slots");
        int[] condDown = condenser.visibleSlots(Direction.DOWN);
        helper.assertTrue(condDown.length == 1, "Condenser DOWN must expose 1 slot");
        // canInsert 永远返回 false（无外部输入）
        helper.assertTrue(!condenser.canInsert(0, ItemStack.EMPTY, Direction.DOWN),
                "Condenser must reject all insertion");
        // canExtract：UP 拒绝；其他面允许（输出非空时）
        helper.assertTrue(!condenser.canExtract(0, ItemStack.EMPTY, Direction.UP),
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

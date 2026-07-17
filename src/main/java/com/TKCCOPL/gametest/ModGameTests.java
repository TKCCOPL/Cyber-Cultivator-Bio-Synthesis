package com.TKCCOPL.gametest;

import com.TKCCOPL.Config;
import com.TKCCOPL.api.BottlerInfo;
import com.TKCCOPL.api.CyberCultivatorAPI;
import com.TKCCOPL.block.entity.BioIncubatorBlockEntity;
import com.TKCCOPL.block.entity.SerumBottlerBlockEntity;
import com.TKCCOPL.cybercultivator;
import com.TKCCOPL.event.CropMatureEvent;
import com.TKCCOPL.event.GeneSpliceEvent;
import com.TKCCOPL.event.SerumConsumeEvent;
import com.TKCCOPL.event.SerumCraftEvent;
import com.TKCCOPL.init.ModBlocks;
import com.TKCCOPL.init.ModItems;
import com.TKCCOPL.item.GeneticSeedItem;
import com.TKCCOPL.item.SynapticSerumItem;
import com.TKCCOPL.recipe.IncubatorOutputRecipe;
import com.TKCCOPL.recipe.IncubatorOutputSerializer;
import com.TKCCOPL.recipe.RecipeOrdering;
import com.TKCCOPL.recipe.SerumRecipe;
import com.TKCCOPL.recipe.SerumRecipeSerializer;
import com.TKCCOPL.recipe.SerumRecipeIds;
import com.google.gson.JsonParser;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.fml.ModList;

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

    private static void assertEffectId(GameTestHelper helper, ItemStack serum, String expected) {
        var info = CyberCultivatorAPI.getSerumEffectInfo(serum);
        helper.assertTrue(info != null && expected.equals(info.effectId()), "Unexpected serum effect registry ID");
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

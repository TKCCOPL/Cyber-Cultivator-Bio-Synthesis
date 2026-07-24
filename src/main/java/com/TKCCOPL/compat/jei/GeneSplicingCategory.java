package com.TKCCOPL.compat.jei;

import com.TKCCOPL.client.ClientGameplayConfig;
import com.TKCCOPL.cybercultivator;
import com.TKCCOPL.init.ModItems;
import com.TKCCOPL.item.GeneticSeedItem;
import com.TKCCOPL.recipe.IncubatorOutputRecipe;
import com.TKCCOPL.recipe.ModRecipeTypes;
import com.TKCCOPL.recipe.RecipeOrdering;
import com.TKCCOPL.recipe.GeneSpliceRules;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GeneSplicingCategory extends MachineRecipeCategory<GeneSplicingCategory.DisplayRecipe> {
    public static final RecipeType<DisplayRecipe> RECIPE_TYPE =
            new RecipeType<>(ResourceLocation.fromNamespaceAndPath(cybercultivator.MODID, "gene_splicing"),
                    DisplayRecipe.class);
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(cybercultivator.MODID, "textures/gui/gene_splicer.png");

    public record DisplayRecipe(ItemStack seedA, ItemStack seedB,
                                int speedA, int yieldA, int potencyA,
                                int speedB, int yieldB, int potencyB,
                                double mutationChance, double twinChance) {
    }

    public GeneSplicingCategory(IGuiHelper guiHelper) {
        super(guiHelper, RECIPE_TYPE, "jei.cybercultivator.gene_splicing",
                new ItemStack(ModItems.GENE_SPLICER_ITEM.get()), TEXTURE);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, DisplayRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 30, 29).addItemStack(recipe.seedA());
        builder.addSlot(RecipeIngredientRole.INPUT, 66, 29).addItemStack(recipe.seedB());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 138, 29).addItemStack(createOutput(recipe));
    }

    @Override
    public void draw(DisplayRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics,
                     double mouseX, double mouseY) {
        drawCentered(graphics, Component.translatable("gui.cybercultivator.splicer.parent_a"),
                39, 19, 0x4B3D4A);
        drawCentered(graphics, Component.translatable("gui.cybercultivator.splicer.parent_b"),
                75, 19, 0x4B3D4A);
        drawFitted(graphics, Component.translatable("gui.cybercultivator.splicer.offspring"),
                136, 19, 34, 0x4B3D4A);

        renderProgressArrow(graphics);

        int avgSpeed = average(recipe.speedA(), recipe.speedB());
        int avgYield = average(recipe.yieldA(), recipe.yieldB());
        int avgPotency = average(recipe.potencyA(), recipe.potencyB());
        drawCentered(graphics, Component.translatable("jei.cybercultivator.splicer.meta",
                formatPercent(recipe.mutationChance()), formatPercent(recipe.twinChance())),
                89, 54, 0x5C3D58);
        int range = mutationRange();
        var cfg = ClientGameplayConfig.getSnapshot();
        drawCentered(graphics, Component.translatable("jei.cybercultivator.splicer.range",
                Math.max(cfg.geneMin(), avgSpeed - range), Math.min(cfg.geneMax(), avgSpeed + range),
                Math.max(cfg.geneMin(), avgYield - range), Math.min(cfg.geneMax(), avgYield + range),
                Math.max(cfg.geneMin(), avgPotency - range), Math.min(cfg.geneMax(), avgPotency + range)),
                89, 69, 0x78406F);
    }

    @Override
    public List<Component> getTooltipStrings(DisplayRecipe recipe, IRecipeSlotsView recipeSlotsView,
                                             double mouseX, double mouseY) {
        if (mouseX >= 4 && mouseX <= 174 && mouseY >= 52 && mouseY <= 84) {
            var cfg = ClientGameplayConfig.getSnapshot();
            return List.of(
                    Component.translatable("jei.cybercultivator.tooltip.mutation_formula_config",
                                    formatPercent(configChance(cfg.mutationChanceBase(), 0.05D)),
                                    formatPercent(configChance(cfg.mutationChancePerGen(), 0.005D)),
                                    formatPercent(configChance(cfg.mutationChancePerGeneDiff(), 0.01D)),
                                    cfg.mutationGenerationCap(),
                                    formatPercent(configChance(cfg.mutationChanceCap(), 0.25D)))
                            .withStyle(ChatFormatting.GRAY),
                    Component.translatable("jei.cybercultivator.tooltip.twin_formula",
                                    formatPercent(configChance(cfg.twinChanceBase(), 0.10D)),
                                    formatPercent(configChance(cfg.twinChancePerGen(), 0.02D)),
                                    formatPercent(configChance(cfg.twinChanceCap(), 0.60D)))
                            .withStyle(ChatFormatting.GRAY),
                    Component.translatable("jei.cybercultivator.tooltip.gene_formula")
                            .withStyle(ChatFormatting.GRAY));
        }
        return List.of();
    }

    private static final int ARROW_START_X = 91;
    private static final int ARROW_WIDTH = 42;
    private static final int ARROW_HEAD_X = 125;
    private static final int ARROW_COLOR = 0xFFB868B2;

    private void renderProgressArrow(GuiGraphics graphics) {
        int progress = animationValue();
        int maximum = animationMaximum();
        if (progress <= 0 || maximum <= 0) return;

        int filled = Math.min(ARROW_WIDTH,
                (int) Math.ceil((double) progress * ARROW_WIDTH / maximum));
        for (int offset = 0; offset < filled; offset++) {
            int x = ARROW_START_X + offset;
            int top = 34;
            int bottom = 39;
            if (x >= ARROW_HEAD_X) {
                int inset = Math.max(0, x - ARROW_HEAD_X - 1);
                top = 30 + inset;
                bottom = 43 - inset;
            }
            graphics.fill(x, top, x + 1, bottom, ARROW_COLOR);
        }
    }

    private static ItemStack createOutput(DisplayRecipe recipe) {
        ItemStack output = seedWithGenes(new ItemStack(recipe.seedA().getItem()),
                average(recipe.speedA(), recipe.speedB()),
                average(recipe.yieldA(), recipe.yieldB()),
                average(recipe.potencyA(), recipe.potencyB()));
        output.getOrCreateTag().putInt(GeneticSeedItem.GENE_GENERATION, 1);
        return output;
    }

    private static int average(int first, int second) {
        return (first + second) / 2;
    }

    private static int mutationRange() {
        int configured = ClientGameplayConfig.getSnapshot().mutationRange();
        return configured > 0 ? configured : 2;
    }

    private static String formatPercent(double chance) {
        double percent = Math.max(0.0D, Math.min(1.0D, chance)) * 100.0D;
        return percent == Math.rint(percent) ? Integer.toString((int) percent)
                : String.format(Locale.ROOT, "%.1f", percent);
    }

    private static double configChance(double configured, double fallback) {
        return ClientGameplayConfig.getSnapshot().geneMax() > 0 ? configured : fallback;
    }

    private static double displayMutationChance(int generation, int geneDifference) {
        var cfg = ClientGameplayConfig.getSnapshot();
        return GeneSpliceRules.mutationChance(generation, geneDifference,
                configChance(cfg.mutationChanceBase(), 0.05D),
                configChance(cfg.mutationChancePerGen(), 0.005D),
                cfg.mutationGenerationCap(),
                configChance(cfg.mutationChancePerGeneDiff(), 0.01D),
                configChance(cfg.mutationChanceCap(), 0.25D));
    }

    private static double displayTwinChance(int generation, int geneDifference) {
        var cfg = ClientGameplayConfig.getSnapshot();
        double mutation = displayMutationChance(generation, geneDifference);
        double normalTwin = GeneSpliceRules.normalTwinChance(generation,
                configChance(cfg.twinChanceBase(), 0.10D),
                configChance(cfg.twinChancePerGen(), 0.02D),
                configChance(cfg.twinChanceCap(), 0.60D));
        return GeneSpliceRules.totalTwinChance(mutation, normalTwin);
    }

    private static ItemStack seedWithGenes(ItemStack seed, int speed, int yield, int potency) {
        ItemStack stack = seed.copy();
        GeneticSeedItem.setGenes(stack, speed, yield, potency);
        stack.getOrCreateTag().putInt(GeneticSeedItem.GENE_GENERATION, 0);
        return stack;
    }

    public static List<DisplayRecipe> buildRecipes(Level level) {
        List<DisplayRecipe> recipes = new ArrayList<>();
        if (level == null) return recipes;

        // 使用 RecipeManager 真实配方数据，确保 JEI 与机器实际可用配方一致
        List<IncubatorOutputRecipe> outputs = RecipeOrdering.sorted(
                level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.INCUBATOR_OUTPUT.get()));

        // 自交：每个种子类型与自己拼接
        for (var output : outputs) {
            ItemStack seed = firstSeed(output);
            if (seed.isEmpty()) continue;
            int[] genes = output.getDefaultGenes();
            recipes.add(new DisplayRecipe(
                    seedWithGenes(seed, genes[0], genes[1], genes[2]),
                    seedWithGenes(seed, genes[0], genes[1], genes[2]),
                    genes[0], genes[1], genes[2], genes[0], genes[1], genes[2],
                    displayMutationChance(0, 0), displayTwinChance(0, 0)));
        }

        // 杂交：不同种子类型两两组合
        for (int i = 0; i < outputs.size(); i++) {
            for (int j = i + 1; j < outputs.size(); j++) {
                var outA = outputs.get(i);
                var outB = outputs.get(j);
                ItemStack seedA = firstSeed(outA);
                ItemStack seedB = firstSeed(outB);
                if (seedA.isEmpty() || seedB.isEmpty()) continue;
                int[] genesA = outA.getDefaultGenes();
                int[] genesB = outB.getDefaultGenes();
                int geneDiff = Math.max(Math.abs(genesA[0] - genesB[0]),
                        Math.max(Math.abs(genesA[1] - genesB[1]), Math.abs(genesA[2] - genesB[2])));
                recipes.add(new DisplayRecipe(
                        seedWithGenes(seedA, genesA[0], genesA[1], genesA[2]),
                        seedWithGenes(seedB, genesB[0], genesB[1], genesB[2]),
                        genesA[0], genesA[1], genesA[2], genesB[0], genesB[1], genesB[2],
                        displayMutationChance(0, geneDiff), displayTwinChance(0, geneDiff)));
            }
        }
        return recipes;
    }

    /** 获取配方的首个种子物品（Ingredient.getItems() 已处理 tag 匹配） */
    private static ItemStack firstSeed(IncubatorOutputRecipe recipe) {
        ItemStack[] items = recipe.getSeedIngredient().getItems();
        return items.length == 0 ? ItemStack.EMPTY : items[0].copy();
    }
}

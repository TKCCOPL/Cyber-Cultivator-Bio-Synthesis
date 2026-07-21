package com.TKCCOPL.compat.jei;

import com.TKCCOPL.client.ClientGameplayConfig;
import com.TKCCOPL.cybercultivator;
import com.TKCCOPL.init.ModItems;
import com.TKCCOPL.item.GeneticSeedItem;
import com.TKCCOPL.recipe.ModRecipes;
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
                                double mutationChance) {
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
        drawFitted(graphics, Component.translatable("gui.cybercultivator.splicer.parent_a"),
                30, 19, 28, 0x4B3D4A);
        drawFitted(graphics, Component.translatable("gui.cybercultivator.splicer.parent_b"),
                66, 19, 28, 0x4B3D4A);
        drawFitted(graphics, Component.translatable("gui.cybercultivator.splicer.offspring"),
                136, 19, 34, 0x4B3D4A);

        thinHorizontalBar(graphics, 94, 35, 27, 0xFFB868B2);
        renderConnectorAnimation(graphics);

        int avgSpeed = average(recipe.speedA(), recipe.speedB());
        int avgYield = average(recipe.yieldA(), recipe.yieldB());
        int avgPotency = average(recipe.potencyA(), recipe.potencyB());
        drawFitted(graphics, Component.translatable("jei.cybercultivator.splicer.automatic", 5),
                4, 49, 170, 0x78406F);
        drawFitted(graphics, Component.translatable("jei.cybercultivator.splicer.meta",
                1, formatPercent(recipe.mutationChance())), 4, 62, 170, 0x5C3D58);
        drawFitted(graphics, Component.translatable("jei.cybercultivator.splicer.average",
                avgSpeed, avgYield, avgPotency, mutationRange()), 4, 77, 170, 0x78406F);
    }

    @Override
    public List<Component> getTooltipStrings(DisplayRecipe recipe, IRecipeSlotsView recipeSlotsView,
                                             double mouseX, double mouseY) {
        if (mouseX >= 4 && mouseX <= 174 && mouseY >= 60 && mouseY <= 94) {
            var cfg = ClientGameplayConfig.getSnapshot();
            return List.of(
                    Component.translatable("jei.cybercultivator.tooltip.mutation_formula_config",
                                    formatPercent(configChance(cfg.mutationChanceBase(), 0.05D)),
                                    formatPercent(configChance(cfg.mutationChancePerGen(), 0.02D)),
                                    formatPercent(configChance(cfg.mutationChancePerGeneDiff(), 0.01D)))
                            .withStyle(ChatFormatting.GRAY),
                    Component.translatable("jei.cybercultivator.tooltip.gene_formula")
                            .withStyle(ChatFormatting.GRAY));
        }
        return List.of();
    }

    private void renderConnectorAnimation(GuiGraphics graphics) {
        float phase = animationValue() * 0.32F;
        drawConnectorPulse(graphics, phase % 32.0F, true);
        drawConnectorPulse(graphics, phase % 32.0F, false);
        drawConnectorPulse(graphics, (phase + 16.0F) % 32.0F, true);
        drawConnectorPulse(graphics, (phase + 16.0F) % 32.0F, false);
    }

    private void drawConnectorPulse(GuiGraphics graphics, float phase, boolean leftBranch) {
        int step = (int) phase;
        int x;
        int y;
        if (step < 14) {
            x = leftBranch ? 39 : 75;
            y = 26 - step;
        } else {
            int horizontalStep = Math.min(17, step - 14);
            x = leftBranch ? 39 + horizontalStep : 75 - horizontalStep;
            y = 12;
        }
        graphics.fill(x - 1, y - 1, x + 2, y + 2, 0xFFB868B2);
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
        return configChance(cfg.mutationChanceBase(), 0.05D)
                + generation * configChance(cfg.mutationChancePerGen(), 0.02D)
                + geneDifference * configChance(cfg.mutationChancePerGeneDiff(), 0.01D);
    }

    private static ItemStack seedWithGenes(ItemStack seed, int speed, int yield, int potency) {
        ItemStack stack = seed.copy();
        GeneticSeedItem.setGenes(stack, speed, yield, potency);
        stack.getOrCreateTag().putInt(GeneticSeedItem.GENE_GENERATION, 0);
        return stack;
    }

    public static List<DisplayRecipe> buildRecipes() {
        List<DisplayRecipe> recipes = new ArrayList<>();
        var outputs = ModRecipes.getINCUBATOR_OUTPUTS();
        for (var output : outputs) {
            ItemStack seed = ModRecipes.getSeedItemForType(output.getSeedType());
            if (seed.isEmpty()) continue;
            int[] genes = output.getDefaultGenes();
            recipes.add(new DisplayRecipe(
                    seedWithGenes(seed, genes[0], genes[1], genes[2]),
                    seedWithGenes(seed, genes[0], genes[1], genes[2]),
                    genes[0], genes[1], genes[2], genes[0], genes[1], genes[2],
                    displayMutationChance(0, 0)));
        }

        for (int i = 0; i < outputs.size(); i++) {
            for (int j = i + 1; j < outputs.size(); j++) {
                var outA = outputs.get(i);
                var outB = outputs.get(j);
                ItemStack seedA = ModRecipes.getSeedItemForType(outA.getSeedType());
                ItemStack seedB = ModRecipes.getSeedItemForType(outB.getSeedType());
                if (seedA.isEmpty() || seedB.isEmpty()) continue;
                int[] genesA = outA.getDefaultGenes();
                int[] genesB = outB.getDefaultGenes();
                int geneDiff = Math.max(Math.abs(genesA[0] - genesB[0]),
                        Math.max(Math.abs(genesA[1] - genesB[1]), Math.abs(genesA[2] - genesB[2])));
                recipes.add(new DisplayRecipe(
                        seedWithGenes(seedA, genesA[0], genesA[1], genesA[2]),
                        seedWithGenes(seedB, genesB[0], genesB[1], genesB[2]),
                        genesA[0], genesA[1], genesA[2], genesB[0], genesB[1], genesB[2],
                        displayMutationChance(0, geneDiff)));
            }
        }
        return recipes;
    }
}

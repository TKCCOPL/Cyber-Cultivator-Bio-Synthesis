package com.TKCCOPL.compat.jei;

import com.TKCCOPL.Config;
import com.TKCCOPL.cybercultivator;
import com.TKCCOPL.init.ModItems;
import com.TKCCOPL.recipe.ModRecipeTypes;
import com.TKCCOPL.recipe.RecipeOrdering;
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
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class IncubatorOutputCategory extends MachineRecipeCategory<IncubatorOutputCategory.DisplayRecipe> {
    public static final RecipeType<DisplayRecipe> RECIPE_TYPE =
            new RecipeType<>(ResourceLocation.fromNamespaceAndPath(cybercultivator.MODID, "incubator_output"),
                    DisplayRecipe.class);
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(cybercultivator.MODID, "textures/gui/bio_incubator.png");

    public record DisplayRecipe(ResourceLocation id, List<ItemStack> seeds, ItemStack output,
                                int defaultSpeed, int defaultYield, int defaultPotency) {
    }

    public IncubatorOutputCategory(IGuiHelper guiHelper) {
        super(guiHelper, RECIPE_TYPE, "jei.cybercultivator.incubator_output",
                new ItemStack(ModItems.BIO_INCUBATOR_ITEM.get()), TEXTURE);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, DisplayRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 22, 31)
                .addItemStacks(recipe.seeds());
        builder.addSlot(RecipeIngredientRole.INPUT, 56, 31)
                .addItemStack(new ItemStack(ModItems.BIOCHEMICAL_SOLUTION.get()))
                .addTooltipCallback((slot, tooltip) -> tooltip.add(Component.translatable(
                        "jei.cybercultivator.incubator.resource_nutrition", injectAmount(Config.nutritionInjectAmount, 25))
                        .withStyle(ChatFormatting.GRAY)));
        builder.addSlot(RecipeIngredientRole.INPUT, 86, 31)
                .addItemStack(new ItemStack(ModItems.PURIFIED_WATER_BOTTLE.get()))
                .addTooltipCallback((slot, tooltip) -> tooltip.add(Component.translatable(
                        "jei.cybercultivator.incubator.resource_purity", injectAmount(Config.purityInjectAmount, 20))
                        .withStyle(ChatFormatting.GRAY)));
        builder.addSlot(RecipeIngredientRole.INPUT, 116, 31)
                .addItemStack(new ItemStack(ModItems.SILICON_SHARD.get()))
                .addTooltipCallback((slot, tooltip) -> tooltip.add(Component.translatable(
                        "jei.cybercultivator.incubator.resource_signal", injectAmount(Config.dataSignalInjectAmount, 15))
                        .withStyle(ChatFormatting.GRAY)));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 146, 31)
                .addItemStack(recipe.output());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 86, 55)
                .addItemStack(new ItemStack(Items.GLASS_BOTTLE))
                .addTooltipCallback((slot, tooltip) -> tooltip.add(Component.translatable(
                        "jei.cybercultivator.incubator.bottle_byproduct").withStyle(ChatFormatting.GRAY)));
    }

    @Override
    public void draw(DisplayRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics,
                     double mouseX, double mouseY) {
        drawFitted(graphics, Component.literal("N"), 46, 19, 10, 0x6B741E);
        drawFitted(graphics, Component.literal("P"), 76, 19, 10, 0x2F6F79);
        drawFitted(graphics, Component.literal("D"), 106, 19, 10, 0x6B4C12);
        verticalBar(graphics, 48, 31, 16, 0xFFB7CF45);
        verticalBar(graphics, 78, 31, 16, 0xFF49B8D1);
        verticalBar(graphics, 108, 31, 16, 0xFFDBB441);

        drawFitted(graphics, Component.translatable("jei.cybercultivator.incubator.summary",
                recipe.defaultSpeed(), recipe.defaultYield(), recipe.defaultPotency(), recipe.output().getCount()),
                14, 72, 155, 0x3F6F32);
        horizontalBar(graphics, 14, 83, 155, 0xFF75BD4B);
    }

    @Override
    public List<Component> getTooltipStrings(DisplayRecipe recipe, IRecipeSlotsView recipeSlotsView,
                                             double mouseX, double mouseY) {
        if (mouseX >= 14 && mouseX <= 169 && mouseY >= 70 && mouseY <= 94) {
            double rate = 0.5D + recipe.defaultSpeed() / 10.0D * 1.5D;
            return List.of(
                    Component.translatable("jei.cybercultivator.tooltip.output_formula").withStyle(ChatFormatting.GRAY),
                    Component.translatable("jei.cybercultivator.tooltip.rate_value",
                                    String.format(Locale.ROOT, "%.1f", rate))
                            .withStyle(ChatFormatting.GRAY),
                    Component.translatable("jei.cybercultivator.tooltip.quality_formula")
                            .withStyle(ChatFormatting.GRAY));
        }
        return List.of();
    }

    @Override
    public ResourceLocation getRegistryName(DisplayRecipe recipe) {
        return recipe.id();
    }

    private static int injectAmount(int configured, int fallback) {
        return configured > 0 ? configured : fallback;
    }

    private static ItemStack seedWithGenes(ItemStack seed, int speed, int yield, int potency) {
        ItemStack stack = seed.copy();
        stack.getOrCreateTag().putInt("Gene_Speed", speed);
        stack.getOrCreateTag().putInt("Gene_Yield", yield);
        stack.getOrCreateTag().putInt("Gene_Potency", potency);
        stack.getOrCreateTag().putInt("Gene_Generation", 0);
        return stack;
    }

    public static List<DisplayRecipe> buildRecipes(net.minecraft.world.level.Level level) {
        if (level == null) return List.of();

        List<DisplayRecipe> recipes = new ArrayList<>();
        for (var recipe : RecipeOrdering.sorted(
                level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.INCUBATOR_OUTPUT.get()))) {
            int[] genes = recipe.getDefaultGenes();
            List<ItemStack> seeds = Arrays.stream(recipe.getSeedIngredient().getItems())
                    .map(seed -> seedWithGenes(seed, genes[0], genes[1], genes[2]))
                    .toList();
            if (seeds.isEmpty()) continue;
            recipes.add(new DisplayRecipe(recipe.getId(), seeds, recipe.assemble(seeds.get(0)),
                    genes[0], genes[1], genes[2]));
        }
        return recipes;
    }
}

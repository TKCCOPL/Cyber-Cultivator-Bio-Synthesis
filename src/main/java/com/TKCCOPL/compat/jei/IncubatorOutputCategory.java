package com.TKCCOPL.compat.jei;

import com.TKCCOPL.cybercultivator;
import com.TKCCOPL.init.ModItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class IncubatorOutputCategory implements IRecipeCategory<IncubatorOutputCategory.DisplayRecipe> {
    public static final RecipeType<DisplayRecipe> RECIPE_TYPE =
            new RecipeType<>(new ResourceLocation(cybercultivator.MODID, "incubator_output"), DisplayRecipe.class);

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(cybercultivator.MODID, "textures/gui/jei_incubator_output.png");

    private final IDrawable background;
    private final IDrawable icon;

    /** JEI 展示用配方数据 */
    public record DisplayRecipe(
            ItemStack seed, ItemStack output,
            String cropName,
            int defaultSpeed, int defaultYield, int defaultPotency
    ) {}

    public IncubatorOutputCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(TEXTURE, 0, 0, 140, 50);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.BIO_INCUBATOR_ITEM.get()));
    }

    @Override
    public RecipeType<DisplayRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.cybercultivator.incubator_output");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, DisplayRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 11)
                .addItemStack(recipe.seed());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 113, 11)
                .addItemStack(recipe.output());
    }

    @Override
    public void draw(DisplayRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics,
                     double mouseX, double mouseY) {
        // 默认基因值
        String genes = String.format("S:%d Y:%d P:%d",
                recipe.defaultSpeed(), recipe.defaultYield(), recipe.defaultPotency());
        guiGraphics.drawString(Minecraft.getInstance().font, genes, 30, 2, 0x808080, false);

        // 产出数量范围（基于 Yield：2 + yield/3）
        int minOutput = 2;
        int maxOutput = 2 + recipe.defaultYield() / 3;
        String outputRange = String.format("产出: %d-%d", minOutput, maxOutput);
        guiGraphics.drawString(Minecraft.getInstance().font, outputRange, 30, 14, 0x55FF55, false);

        // 生长速率（基于 Speed：0.5 + speed/10*1.5）
        double growthRate = 0.5 + recipe.defaultSpeed() / 10.0 * 1.5;
        String rate = String.format("速率: %.1fx", growthRate);
        guiGraphics.drawString(Minecraft.getInstance().font, rate, 30, 26, 0xFFFF55, false);
    }

    /** 设置种子基因值（用于 JEI 展示） */
    private static ItemStack seedWithGenes(ItemStack seed, int speed, int yield, int potency) {
        ItemStack stack = seed.copy();
        stack.getOrCreateTag().putInt("Gene_Speed", speed);
        stack.getOrCreateTag().putInt("Gene_Yield", yield);
        stack.getOrCreateTag().putInt("Gene_Potency", potency);
        return stack;
    }

    /** 构建展示用配方列表 */
    public static List<DisplayRecipe> buildRecipes() {
        List<DisplayRecipe> recipes = new ArrayList<>();

        // Fiber Reed: Speed=4, Yield=7, Potency=3
        recipes.add(new DisplayRecipe(
                seedWithGenes(new ItemStack(ModItems.FIBER_REED_SEEDS.get()), 4, 7, 3),
                new ItemStack(ModItems.PLANT_FIBER.get()),
                "纤维草", 4, 7, 3
        ));

        // Protein Soy: Speed=5, Yield=4, Potency=7
        recipes.add(new DisplayRecipe(
                seedWithGenes(new ItemStack(ModItems.PROTEIN_SOY_SEEDS.get()), 5, 4, 7),
                new ItemStack(ModItems.BIOCHEMICAL_SOLUTION.get()),
                "蛋白质豆", 5, 4, 7
        ));

        // Alcohol Bloom: Speed=6, Yield=3, Potency=5
        recipes.add(new DisplayRecipe(
                seedWithGenes(new ItemStack(ModItems.ALCOHOL_BLOOM_SEEDS.get()), 6, 3, 5),
                new ItemStack(ModItems.INDUSTRIAL_ETHANOL.get()),
                "酒精花", 6, 3, 5
        ));

        return recipes;
    }
}

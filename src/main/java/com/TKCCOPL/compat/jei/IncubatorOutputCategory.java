package com.TKCCOPL.compat.jei;

import com.TKCCOPL.cybercultivator;
import com.TKCCOPL.init.ModItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
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
    public record DisplayRecipe(ItemStack seed, ItemStack output, String cropName) {}

    public IncubatorOutputCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(TEXTURE, 0, 0, 120, 40);
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
        builder.addSlot(RecipeIngredientRole.OUTPUT, 91, 11)
                .addItemStack(recipe.output());
    }

    /** 构建展示用配方列表 */
    public static List<DisplayRecipe> buildRecipes() {
        List<DisplayRecipe> recipes = new ArrayList<>();
        recipes.add(new DisplayRecipe(
                new ItemStack(ModItems.FIBER_REED_SEEDS.get()),
                new ItemStack(ModItems.PLANT_FIBER.get()),
                "纤维草"
        ));
        recipes.add(new DisplayRecipe(
                new ItemStack(ModItems.PROTEIN_SOY_SEEDS.get()),
                new ItemStack(ModItems.BIOCHEMICAL_SOLUTION.get()),
                "蛋白质豆"
        ));
        recipes.add(new DisplayRecipe(
                new ItemStack(ModItems.ALCOHOL_BLOOM_SEEDS.get()),
                new ItemStack(ModItems.INDUSTRIAL_ETHANOL.get()),
                "酒精花"
        ));
        return recipes;
    }
}

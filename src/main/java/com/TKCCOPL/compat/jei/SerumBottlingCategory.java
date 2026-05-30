package com.TKCCOPL.compat.jei;

import com.TKCCOPL.cybercultivator;
import com.TKCCOPL.init.ModItems;
import com.TKCCOPL.recipe.SerumRecipe;
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
import net.minecraft.world.item.crafting.Ingredient;

public class SerumBottlingCategory implements IRecipeCategory<SerumRecipe> {
    public static final RecipeType<SerumRecipe> RECIPE_TYPE =
            new RecipeType<>(new ResourceLocation(cybercultivator.MODID, "serum_bottling"), SerumRecipe.class);

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(cybercultivator.MODID, "textures/gui/jei_serum_bottling.png");

    private final IDrawable background;
    private final IDrawable icon;

    public SerumBottlingCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(TEXTURE, 0, 0, 120, 40);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.SERUM_BOTTLER_ITEM.get()));
    }

    @Override
    public RecipeType<SerumRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.cybercultivator.serum_bottling");
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
    public void setRecipe(IRecipeLayoutBuilder builder, SerumRecipe recipe, IFocusGroup focuses) {
        Ingredient[] inputs = recipe.getInputs();
        for (int i = 0; i < inputs.length; i++) {
            builder.addSlot(RecipeIngredientRole.INPUT, 1 + i * 18, 1)
                    .addIngredients(inputs[i]);
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, 91, 11)
                .addItemStack(recipe.getBaseOutput());
    }
}

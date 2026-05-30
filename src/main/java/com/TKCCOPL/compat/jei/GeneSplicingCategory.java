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

public class GeneSplicingCategory implements IRecipeCategory<GeneSplicingCategory.DisplayRecipe> {
    public static final RecipeType<DisplayRecipe> RECIPE_TYPE =
            new RecipeType<>(new ResourceLocation(cybercultivator.MODID, "gene_splicing"), DisplayRecipe.class);

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(cybercultivator.MODID, "textures/gui/jei_gene_splicing.png");

    private final IDrawable background;
    private final IDrawable icon;

    /** JEI 展示用配方数据 */
    public record DisplayRecipe(ItemStack seedA, ItemStack seedB, String description) {}

    public GeneSplicingCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(TEXTURE, 0, 0, 120, 40);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.GENE_SPLICER_ITEM.get()));
    }

    @Override
    public RecipeType<DisplayRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.cybercultivator.gene_splicing");
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
                .addItemStack(recipe.seedA());
        builder.addSlot(RecipeIngredientRole.INPUT, 37, 11)
                .addItemStack(recipe.seedB());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 91, 11)
                .addItemStack(new ItemStack(ModItems.FIBER_REED_SEEDS.get()));
    }

    /** 构建展示用配方列表 */
    public static List<DisplayRecipe> buildRecipes() {
        List<DisplayRecipe> recipes = new ArrayList<>();
        // 展示不同种子类型的拼接示例
        ItemStack fiber = new ItemStack(ModItems.FIBER_REED_SEEDS.get());
        ItemStack soy = new ItemStack(ModItems.PROTEIN_SOY_SEEDS.get());
        ItemStack bloom = new ItemStack(ModItems.ALCOHOL_BLOOM_SEEDS.get());

        recipes.add(new DisplayRecipe(fiber, soy, "同类种子拼接"));
        recipes.add(new DisplayRecipe(fiber, bloom, "跨类型拼接"));
        return recipes;
    }
}

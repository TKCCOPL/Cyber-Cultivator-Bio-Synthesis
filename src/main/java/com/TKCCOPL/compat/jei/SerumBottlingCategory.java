package com.TKCCOPL.compat.jei;

import com.TKCCOPL.cybercultivator;
import com.TKCCOPL.init.ModItems;
import com.TKCCOPL.recipe.SerumRecipe;
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
import net.minecraft.world.item.crafting.Ingredient;

public class SerumBottlingCategory implements IRecipeCategory<SerumRecipe> {
    public static final RecipeType<SerumRecipe> RECIPE_TYPE =
            new RecipeType<>(new ResourceLocation(cybercultivator.MODID, "serum_bottling"), SerumRecipe.class);

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(cybercultivator.MODID, "textures/gui/jei_serum_bottling.png");

    private final IDrawable background;
    private final IDrawable icon;

    public SerumBottlingCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(TEXTURE, 0, 0, 140, 50);
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
            builder.addSlot(RecipeIngredientRole.INPUT, 1 + i * 18, 11)
                    .addIngredients(inputs[i]);
        }

        // 输出物品（带 Activity NBT 用于展示）
        ItemStack output = recipe.getBaseOutput();
        if (!output.is(ModItems.SYNAPTIC_NEURAL_BERRY.get())) {
            output.getOrCreateTag().putInt("SynapticActivity", 8);
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, 113, 11)
                .addItemStack(output);
    }

    @Override
    public void draw(SerumRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics,
                     double mouseX, double mouseY) {
        // 加工时间
        int seconds = recipe.getProcessingTime() / 20;
        String time = seconds + "s";
        guiGraphics.drawString(Minecraft.getInstance().font, time, 60, 2, 0x808080, false);

        // Activity 值（仅血清配方）
        ItemStack output = recipe.getBaseOutput();
        if (!output.is(ModItems.SYNAPTIC_NEURAL_BERRY.get())) {
            guiGraphics.drawString(Minecraft.getInstance().font, "活性: 8", 60, 38, 0xFFAA00, false);
        }
    }
}

package com.TKCCOPL.compat.jei;

import com.TKCCOPL.cybercultivator;
import com.TKCCOPL.init.ModItems;
import com.TKCCOPL.recipe.SerumRecipe;
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
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;

public class SerumBottlingCategory extends MachineRecipeCategory<SerumRecipe> {
    public static final RecipeType<SerumRecipe> RECIPE_TYPE =
            new RecipeType<>(ResourceLocation.fromNamespaceAndPath(cybercultivator.MODID, "serum_bottling"),
                    SerumRecipe.class);
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(cybercultivator.MODID, "textures/gui/serum_bottler.png");

    public SerumBottlingCategory(IGuiHelper guiHelper) {
        super(guiHelper, RECIPE_TYPE, "jei.cybercultivator.serum_bottling",
                new ItemStack(ModItems.SERUM_BOTTLER_ITEM.get()), TEXTURE);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, SerumRecipe recipe, IFocusGroup focuses) {
        Ingredient[] inputs = recipe.getInputs();
        for (int i = 0; i < inputs.length && i < 3; i++) {
            builder.addSlot(RecipeIngredientRole.INPUT, 22 + i * 24, 31).addIngredients(inputs[i]);
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, 150, 31).addItemStack(recipe.getBaseOutput());
    }

    @Override
    public void draw(SerumRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics,
                     double mouseX, double mouseY) {
        horizontalBar(graphics, 98, 37, 32, 0xFF5DB9C7);

        ItemStack output = recipe.getBaseOutput();
        drawFitted(graphics, Component.translatable("jei.cybercultivator.bottler.output",
                output.getHoverName()), 8, 62, 162, 0x373737);
    }

    @Override
    public List<Component> getTooltipStrings(SerumRecipe recipe, IRecipeSlotsView recipeSlotsView,
                                             double mouseX, double mouseY) {
        if (mouseX >= 8 && mouseX <= 170 && mouseY >= 59 && mouseY <= 75) {
            boolean calculatedFromRaw = recipe.isInheritActivity() && recipe.isInheritMutation();
            return calculatedFromRaw
                    ? List.of(
                            Component.translatable("jei.cybercultivator.tooltip.quality_tags")
                                    .withStyle(ChatFormatting.GRAY),
                            Component.translatable("jei.cybercultivator.tooltip.activity_formula")
                                    .withStyle(ChatFormatting.GOLD))
                    : List.of(
                            Component.translatable("jei.cybercultivator.tooltip.serum_chain")
                                    .withStyle(ChatFormatting.GRAY),
                            Component.translatable("jei.cybercultivator.tooltip.level_scaling")
                                    .withStyle(ChatFormatting.GRAY),
                            Component.translatable("jei.cybercultivator.tooltip.serum_side_effect")
                                    .withStyle(ChatFormatting.RED));
        }
        return List.of();
    }
}

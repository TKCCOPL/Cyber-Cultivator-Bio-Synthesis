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
import java.util.Locale;

public class SerumBottlingCategory extends MachineRecipeCategory<SerumRecipe> {
    public static final RecipeType<SerumRecipe> RECIPE_TYPE =
            new RecipeType<>(ResourceLocation.fromNamespaceAndPath(cybercultivator.MODID, "serum_bottling"),
                    SerumRecipe.class);
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(cybercultivator.MODID, "textures/gui/serum_bottler.png");
    private static final int[] INPUT_CENTER_X = {30, 54, 78};

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
        renderFlowPulse(graphics);

        ItemStack output = recipe.getBaseOutput();
        boolean berry = output.is(ModItems.SYNAPTIC_NEURAL_BERRY.get());
        drawFitted(graphics, Component.translatable("jei.cybercultivator.bottler.automatic",
                formatSeconds(recipe.getProcessingTime())), 8, 59, 162, 0x2F6F79);
        drawFitted(graphics, Component.translatable("jei.cybercultivator.bottler.output",
                output.getHoverName()), 8, 71, 162, 0x373737);
        drawFitted(graphics, Component.translatable(berry
                        ? "jei.cybercultivator.bottler.activity_calculated"
                        : "jei.cybercultivator.bottler.activity_inherited"),
                8, 83, 162, 0x6B4C12);
    }

    @Override
    public List<Component> getTooltipStrings(SerumRecipe recipe, IRecipeSlotsView recipeSlotsView,
                                             double mouseX, double mouseY) {
        if (mouseX >= 8 && mouseX <= 170 && mouseY >= 69 && mouseY <= 94) {
            boolean berry = recipe.getBaseOutput().is(ModItems.SYNAPTIC_NEURAL_BERRY.get());
            return berry
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

    private void renderFlowPulse(GuiGraphics graphics) {
        float animationTick = animationValue() * 0.54F;
        int inputIndex = (int) (animationTick / 18.0F) % INPUT_CENTER_X.length;
        float phase = animationTick % 18.0F;
        int inputX = INPUT_CENTER_X[inputIndex];

        if (phase < 6.0F) {
            int y = 27 - Math.round(Math.min(1.0F, phase / 5.0F) * 8.0F);
            graphics.fill(inputX - 1, y, inputX + 2, y + 3, 0xFF5DB9C7);
        } else if (phase < 15.0F) {
            int x = inputX - 1 + Math.round(Math.min(1.0F, (phase - 6.0F) / 8.0F) * (93 - inputX));
            graphics.fill(x, 17, x + 3, 20, 0xFF5DB9C7);
        } else {
            int y = 19 + Math.round(Math.min(1.0F, (phase - 15.0F) / 2.0F) * 15.0F);
            graphics.fill(92, y, 95, y + 3, 0xFF5DB9C7);
        }
    }

    private static String formatSeconds(int ticks) {
        if (ticks % 20 == 0) return Integer.toString(ticks / 20);
        return String.format(Locale.ROOT, "%.1f", ticks / 20.0D);
    }
}

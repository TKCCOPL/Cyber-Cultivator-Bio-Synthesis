package com.TKCCOPL.compat.jei;

import com.TKCCOPL.cybercultivator;
import com.TKCCOPL.init.ModItems;
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

import java.util.List;

public class AtmosphericCondensingCategory extends MachineRecipeCategory<AtmosphericCondensingCategory.DisplayRecipe> {
    public static final RecipeType<DisplayRecipe> RECIPE_TYPE = new RecipeType<>(
            ResourceLocation.fromNamespaceAndPath(cybercultivator.MODID, "atmospheric_condensing"),
            DisplayRecipe.class);
    public static final DisplayRecipe RECIPE = new DisplayRecipe(
            ResourceLocation.fromNamespaceAndPath(cybercultivator.MODID,
                    "atmospheric_condensing/purified_water"), 600, 16);
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(cybercultivator.MODID,
                    "textures/gui/atmospheric_condenser.png");

    public record DisplayRecipe(ResourceLocation id, int processingTicks, int maxStock) {
    }

    public AtmosphericCondensingCategory(IGuiHelper guiHelper) {
        super(guiHelper, RECIPE_TYPE, "jei.cybercultivator.atmospheric_condensing",
                new ItemStack(ModItems.ATMOSPHERIC_CONDENSER_ITEM.get()), TEXTURE);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, DisplayRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 10, 31)
                .addItemStack(new ItemStack(Items.GLASS_BOTTLE));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 150, 31)
                .addItemStack(new ItemStack(ModItems.PURIFIED_WATER_BOTTLE.get()));
    }

    @Override
    public void draw(DisplayRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics,
                     double mouseX, double mouseY) {
        verticalBar(graphics, 103, 20, 30, 0xFF5DB9C7);
        renderCondensationScan(graphics);

        drawFitted(graphics, Component.translatable("jei.cybercultivator.condenser.cycle",
                recipe.processingTicks() / 20), 8, 65, 162, 0x2F6F79);
    }

    @Override
    public List<Component> getTooltipStrings(DisplayRecipe recipe, IRecipeSlotsView recipeSlotsView,
                                             double mouseX, double mouseY) {
        if (mouseX >= 8 && mouseX <= 170 && mouseY >= 58 && mouseY <= 75) {
            return List.of(Component.translatable("jei.cybercultivator.condenser.tooltip")
                    .withStyle(ChatFormatting.GRAY));
        }
        return List.of();
    }

    @Override
    public ResourceLocation getRegistryName(DisplayRecipe recipe) {
        return recipe.id();
    }

    private static final int[] FIN_X = {48, 55, 62, 69, 76, 83};

    private void renderCondensationScan(GuiGraphics graphics) {
        float animationTick = animationValue();
        int activeFin = (int) (animationTick / 4.0F) % FIN_X.length;
        graphics.fill(FIN_X[activeFin], 25, FIN_X[activeFin] + 3, 48, 0xFF5DB9C7);
    }
}

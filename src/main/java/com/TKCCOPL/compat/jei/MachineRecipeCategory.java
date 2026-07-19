package com.TKCCOPL.compat.jei;

import mezz.jei.api.gui.ITickTimer;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

abstract class MachineRecipeCategory<T> implements IRecipeCategory<T> {
    protected static final int CROP_X = 8;
    protected static final int CROP_Y = 19;
    protected static final int WIDTH = 178;
    protected static final int HEIGHT = 95;

    private final RecipeType<T> recipeType;
    private final Component title;
    private final IDrawable background;
    private final IDrawable icon;
    private final ITickTimer animationTimer;

    protected MachineRecipeCategory(IGuiHelper guiHelper, RecipeType<T> recipeType,
                                    String titleKey, ItemStack iconStack, ResourceLocation texture) {
        this.recipeType = recipeType;
        this.title = Component.translatable(titleKey);
        this.background = guiHelper.createDrawable(texture, CROP_X, CROP_Y, WIDTH, HEIGHT);
        this.icon = guiHelper.createDrawableItemStack(iconStack);
        this.animationTimer = guiHelper.createTickTimer(100, 100, false);
    }

    @Override
    public final RecipeType<T> getRecipeType() {
        return recipeType;
    }

    @Override
    public final Component getTitle() {
        return title;
    }

    @Override
    public final IDrawable getBackground() {
        return background;
    }

    @Override
    public final IDrawable getIcon() {
        return icon;
    }

    protected final int animationValue() {
        return animationTimer.getValue();
    }

    protected final int animationMaximum() {
        return animationTimer.getMaxValue();
    }

    protected final void drawFitted(GuiGraphics graphics, Component text, int x, int y,
                                    int maxWidth, int color) {
        var font = Minecraft.getInstance().font;
        String value = text.getString();
        if (font.width(value) > maxWidth) {
            String ellipsis = "...";
            value = font.plainSubstrByWidth(value, Math.max(0, maxWidth - font.width(ellipsis))) + ellipsis;
        }
        graphics.drawString(font, value, x, y, color, false);
    }

    protected final void horizontalBar(GuiGraphics graphics, int x, int y, int width, int color) {
        int filled = scaledFill(Math.max(0, width - 2));
        graphics.fill(x, y, x + width, y + 5, 0xFF373737);
        if (filled > 0) {
            graphics.fill(x + 1, y + 1, x + 1 + filled, y + 4, color);
        }
    }

    protected final void thinHorizontalBar(GuiGraphics graphics, int x, int y, int width, int color) {
        int filled = scaledFill(Math.max(0, width - 2));
        graphics.fill(x, y, x + width, y + 3, 0xFF373737);
        if (filled > 0) {
            graphics.fill(x + 1, y + 1, x + 1 + filled, y + 2, color);
        }
    }

    protected final void verticalBar(GuiGraphics graphics, int x, int y, int height, int color) {
        int filled = scaledFill(Math.max(0, height - 2));
        graphics.fill(x, y, x + 6, y + height, 0xFF373737);
        if (filled > 0) {
            graphics.fill(x + 1, y + height - 1 - filled, x + 5, y + height - 1, color);
        }
    }

    private int scaledFill(int extent) {
        int maximum = animationMaximum();
        if (extent <= 0 || maximum <= 0) return 0;
        return Math.min(extent, (int) Math.ceil((double) animationValue() * extent / maximum));
    }
}

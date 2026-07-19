package com.TKCCOPL.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

public abstract class MachineScreen<M extends AbstractContainerMenu> extends AbstractContainerScreen<M> {
    private final ResourceLocation texture;

    protected MachineScreen(M menu, Inventory inventory, Component title, ResourceLocation texture) {
        super(menu, inventory, title);
        this.texture = texture;
        imageWidth = 194;
        imageHeight = 210;
        inventoryLabelX = 16;
        inventoryLabelY = 116;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        renderMachineState(graphics, partialTick);
    }

    protected abstract void renderMachineState(GuiGraphics graphics, float partialTick);

    protected void sendButton(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    protected void drawFitted(GuiGraphics graphics, Component text, int x, int y, int maxWidth, int color) {
        String value = text.getString();
        if (font.width(value) > maxWidth) {
            String ellipsis = "...";
            value = font.plainSubstrByWidth(value, Math.max(0, maxWidth - font.width(ellipsis))) + ellipsis;
        }
        graphics.drawString(font, value, x, y, color, false);
    }

    protected void horizontalBar(GuiGraphics graphics, int x, int y, int width, int value, int maximum, int color) {
        int innerWidth = Math.max(0, width - 2);
        int filled = scaledFill(innerWidth, value, maximum);
        graphics.fill(leftPos + x, topPos + y, leftPos + x + width, topPos + y + 5, 0xFF373737);
        if (filled > 0) {
            graphics.fill(leftPos + x + 1, topPos + y + 1,
                    leftPos + x + 1 + filled, topPos + y + 4, color);
        }
    }

    protected void thinHorizontalBar(GuiGraphics graphics, int x, int y, int width,
                                     int value, int maximum, int color) {
        int innerWidth = Math.max(0, width - 2);
        int filled = scaledFill(innerWidth, value, maximum);
        graphics.fill(leftPos + x, topPos + y, leftPos + x + width, topPos + y + 3, 0xFF373737);
        if (filled > 0) {
            graphics.fill(leftPos + x + 1, topPos + y + 1,
                    leftPos + x + 1 + filled, topPos + y + 2, color);
        }
    }

    protected void verticalBar(GuiGraphics graphics, int x, int y, int height, int value, int maximum, int color) {
        int innerHeight = Math.max(0, height - 2);
        int filled = scaledFill(innerHeight, value, maximum);
        graphics.fill(leftPos + x, topPos + y, leftPos + x + 6, topPos + y + height, 0xFF373737);
        if (filled > 0) {
            graphics.fill(leftPos + x + 1, topPos + y + height - 1 - filled,
                    leftPos + x + 5, topPos + y + height - 1, color);
        }
    }

    private int scaledFill(int extent, int value, int maximum) {
        if (extent <= 0 || value <= 0 || maximum <= 0) return 0;
        return Math.min(extent, (int) Math.ceil((double) value * extent / maximum));
    }
}

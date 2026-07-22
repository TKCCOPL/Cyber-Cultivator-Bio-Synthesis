package com.TKCCOPL.client.screen;

import com.TKCCOPL.cybercultivator;
import com.TKCCOPL.menu.AtmosphericCondenserMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class AtmosphericCondenserScreen extends MachineScreen<AtmosphericCondenserMenu> {
    private static final int MAX_STOCK = 32;
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(cybercultivator.MODID, "textures/gui/atmospheric_condenser.png");

    public AtmosphericCondenserScreen(AtmosphericCondenserMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, TEXTURE);
    }

    @Override
    protected void renderMachineState(GuiGraphics graphics, float partialTick) {
        verticalBar(graphics, 111, 39, 30, menu.getProgress(), menu.getMaxProgress(), 0xFF5DB9C7);
        renderCondensationScan(graphics, partialTick);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0x373737, false);
        drawFitted(graphics, getStatusLine(), 16, 77, 162,
                menu.getStock() >= MAX_STOCK ? 0x6B4C12 : 0x2F6F79);
        drawFitted(graphics, getStockLine(), 16, 91, 162, 0x373737);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    private Component getStatusLine() {
        if (menu.getStock() >= MAX_STOCK) {
            return Component.translatable("gui.cybercultivator.condenser.status_full");
        }
        if (!menu.hasBottle()) {
            return Component.translatable("gui.cybercultivator.condenser.status_waiting_bottle");
        }
        int maximum = menu.getMaxProgress();
        int percent = maximum <= 0 ? 0 : Math.min(100, menu.getProgress() * 100 / maximum);
        int remainingSeconds = Math.max(0,
                (int) Math.ceil((maximum - menu.getProgress()) / 20.0D));
        return Component.translatable("gui.cybercultivator.condenser.status_condensing",
                percent, remainingSeconds);
    }

    private Component getStockLine() {
        String key = menu.isDownstreamConnected()
                ? "gui.cybercultivator.condenser.stock_connected"
                : "gui.cybercultivator.condenser.stock_waiting_downstream";
        return Component.translatable(key, menu.getStock(), MAX_STOCK);
    }

    private void renderCondensationScan(GuiGraphics graphics, float partialTick) {
        if (menu.getStock() >= MAX_STOCK || !menu.hasBottle()) return;

        float animationTick = menu.getProgress() + partialTick;
        int activeFin = (int) (animationTick / 4.0F) % 6;
        fillRelative(graphics, 54 + activeFin * 8, 43, 2, 20, 0xFF5DB9C7);

        int pipeOffset = (int) (animationTick % 30.0F);
        if (pipeOffset < 20) {
            fillRelative(graphics, 112, 40 + pipeOffset, 3, 3, 0xFF5DB9C7);
        } else {
            fillRelative(graphics, 124 + pipeOffset - 20, 53, 3, 3, 0xFF5DB9C7);
        }
    }

    private void fillRelative(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(leftPos + x, topPos + y, leftPos + x + width, topPos + y + height, color);
    }
}

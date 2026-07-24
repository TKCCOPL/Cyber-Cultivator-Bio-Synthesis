package com.TKCCOPL.client.screen;

import com.TKCCOPL.cybercultivator;
import com.TKCCOPL.menu.AtmosphericCondenserMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class AtmosphericCondenserScreen extends MachineScreen<AtmosphericCondenserMenu> {
    private static final int MAX_STOCK = 16;
    private static final int[] CONDENSER_FIN_X = {56, 63, 70, 77, 84, 91};
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
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    private void renderCondensationScan(GuiGraphics graphics, float partialTick) {
        if (menu.getStock() >= MAX_STOCK || !menu.hasBottle()) return;

        float animationTick = menu.getProgress() + partialTick;
        int activeFin = (int) (animationTick / 4.0F) % 6;
        fillRelative(graphics, CONDENSER_FIN_X[activeFin], 44, 3, 23, 0xFF5DB9C7);
    }

    private void fillRelative(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(leftPos + x, topPos + y, leftPos + x + width, topPos + y + height, color);
    }
}

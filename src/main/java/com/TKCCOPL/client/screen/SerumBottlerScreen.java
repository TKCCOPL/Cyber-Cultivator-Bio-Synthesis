package com.TKCCOPL.client.screen;

import com.TKCCOPL.cybercultivator;
import com.TKCCOPL.menu.SerumBottlerMenu;
import com.TKCCOPL.recipe.SerumRecipe;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class SerumBottlerScreen extends MachineScreen<SerumBottlerMenu> {
    private static final int[] INPUT_CENTER_X = {38, 62, 86};
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(cybercultivator.MODID, "textures/gui/serum_bottler.png");

    public SerumBottlerScreen(SerumBottlerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, TEXTURE);
    }

    @Override
    protected void renderMachineState(GuiGraphics graphics, float partialTick) {
        horizontalBar(graphics, 106, 56, 32, menu.getProgress(), menu.getMaxProgress(), 0xFF5DB9C7);
        renderFlowPulse(graphics, partialTick);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0x373737, false);
        SerumRecipe recipe = menu.getDisplayRecipe();
        Component statusLine = getStatusLine(recipe);
        int statusColor = menu.isProcessing() ? 0x2F6F79 : menu.hasOutput() ? 0x3F6F32 : 0x555555;
        drawFitted(graphics, statusLine, 16, 78, 162, statusColor);

        if (recipe == null) {
            drawFitted(graphics, Component.translatable("gui.cybercultivator.bottler.input_count",
                    menu.getOccupiedInputCount()), 16, 90, 162, 0x373737);
            drawFitted(graphics, Component.translatable("gui.cybercultivator.bottler.flow_hint"),
                    16, 102, 162, 0x555555);
        } else {
            drawFitted(graphics, Component.translatable("gui.cybercultivator.bottler.recipe",
                    recipe.getBaseOutput().getHoverName(), recipe.getProcessingTime() / 20.0F),
                    16, 90, 162, 0x373737);
            drawFitted(graphics, Component.translatable("gui.cybercultivator.bottler.activity",
                    menu.getPredictedActivity()), 16, 102, 162, 0x6B4C12);
        }
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    private Component getStatusLine(SerumRecipe recipe) {
        if (menu.isProcessing()) {
            int maximum = menu.getMaxProgress();
            int percent = maximum <= 0 ? 0 : Math.min(100, menu.getProgress() * 100 / maximum);
            int remainingSeconds = Math.max(0,
                    (int) Math.ceil((maximum - menu.getProgress()) / 20.0D));
            return Component.translatable("gui.cybercultivator.bottler.status_processing",
                    percent, remainingSeconds);
        }
        if (recipe != null && menu.hasOutput()) {
            return Component.translatable("gui.cybercultivator.bottler.status_output_blocked");
        }
        if (recipe != null) {
            return Component.translatable("gui.cybercultivator.bottler.status_ready");
        }
        if (menu.hasOutput()) {
            return Component.translatable("gui.cybercultivator.bottler.status_complete");
        }
        if (menu.getOccupiedInputCount() > 0) {
            return Component.translatable("gui.cybercultivator.bottler.status_incomplete");
        }
        return Component.translatable("gui.cybercultivator.bottler.status_waiting");
    }

    private void renderFlowPulse(GuiGraphics graphics, float partialTick) {
        if (!menu.isProcessing()) return;

        float animationTick = menu.getProgress() + partialTick;
        int inputIndex = (int) (animationTick / 18.0F) % INPUT_CENTER_X.length;
        float phase = animationTick % 18.0F;
        int inputX = INPUT_CENTER_X[inputIndex];

        if (phase < 6.0F) {
            float travel = phase / 5.0F;
            int y = 46 - Math.round(Math.min(1.0F, travel) * 8.0F);
            fillRelative(graphics, inputX - 1, y, 3, 3, 0xFF5DB9C7);
        } else if (phase < 15.0F) {
            float travel = (phase - 6.0F) / 8.0F;
            int x = inputX - 1 + Math.round(Math.min(1.0F, travel) * (101 - inputX));
            fillRelative(graphics, x, 36, 3, 3, 0xFF5DB9C7);
        } else {
            float travel = (phase - 15.0F) / 2.0F;
            int y = 38 + Math.round(Math.min(1.0F, travel) * 15.0F);
            fillRelative(graphics, 100, y, 3, 3, 0xFF5DB9C7);
        }
    }

    private void fillRelative(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(leftPos + x, topPos + y, leftPos + x + width, topPos + y + height, color);
    }
}

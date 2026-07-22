package com.TKCCOPL.client.screen;

import com.TKCCOPL.cybercultivator;
import com.TKCCOPL.menu.AtmosphericCondenserMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class AtmosphericCondenserScreen extends MachineScreen<AtmosphericCondenserMenu> {
    private static final int MAX_STOCK = 32;
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(cybercultivator.MODID, "textures/gui/atmospheric_condenser.png");
    private Button autoInjectButton;
    private Button pauseButton;

    public AtmosphericCondenserScreen(AtmosphericCondenserMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, TEXTURE);
    }

    @Override
    protected void init() {
        super.init();
        autoInjectButton = addRenderableWidget(Button.builder(autoInjectText(), button -> {
                    sendButton(AtmosphericCondenserMenu.BUTTON_TOGGLE_AUTO_INJECT);
                }).bounds(leftPos + 16, topPos + 95, 102, 16)
                .tooltip(Tooltip.create(Component.translatable("gui.cybercultivator.condenser.auto_tooltip")))
                .build());
        pauseButton = addRenderableWidget(Button.builder(
                        pauseText(),
                        button -> sendButton(AtmosphericCondenserMenu.BUTTON_TOGGLE_PAUSED))
                .bounds(leftPos + 124, topPos + 95, 54, 16)
                .tooltip(Tooltip.create(Component.translatable("gui.cybercultivator.condenser.pause_tooltip")))
                .build());
        updateButtonState();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateButtonState();
    }

    private Component autoInjectText() {
        return Component.translatable(menu.isAutoInject()
                ? "gui.cybercultivator.condenser.auto_on"
                : "gui.cybercultivator.condenser.auto_off");
    }

    private Component pauseText() {
        return Component.translatable(menu.isPaused()
                ? "gui.cybercultivator.condenser.resume"
                : "gui.cybercultivator.condenser.pause");
    }

    private void updateButtonState() {
        if (autoInjectButton != null) autoInjectButton.setMessage(autoInjectText());
        if (pauseButton != null) pauseButton.setMessage(pauseText());
    }

    @Override
    protected void renderMachineState(GuiGraphics graphics, float partialTick) {
        // 复用纹理中现有的原版风格槽位边框，为玻璃瓶输入补充第二个槽位。
        graphics.blit(TEXTURE, leftPos + 9, topPos + 49,
                157, 49, 18, 18, 256, 256);
        verticalBar(graphics, 117, 28, 32, menu.getProgress(), menu.getMaxProgress(), 0xFF5DB9C7);
        renderCondensationScan(graphics, partialTick);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0x373737, false);
        drawFitted(graphics, getStatusLine(), 16, 70, 162,
                menu.isPaused() || menu.getStock() >= MAX_STOCK ? 0x6B4C12 : 0x2F6F79);
        drawFitted(graphics, getStockLine(), 16, 82, 162, 0x373737);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    private Component getStatusLine() {
        if (menu.isPaused()) {
            int maximum = menu.getMaxProgress();
            int percent = maximum <= 0 ? 0 : Math.min(100, menu.getProgress() * 100 / maximum);
            return Component.translatable("gui.cybercultivator.condenser.status_paused", percent);
        }
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
        String key;
        if (!menu.isAutoInject()) {
            key = "gui.cybercultivator.condenser.stock_manual";
        } else if (menu.isDownstreamConnected()) {
            key = "gui.cybercultivator.condenser.stock_connected";
        } else {
            key = "gui.cybercultivator.condenser.stock_waiting_downstream";
        }
        return Component.translatable(key, menu.getStock(), MAX_STOCK);
    }

    private void renderCondensationScan(GuiGraphics graphics, float partialTick) {
        if (menu.isPaused() || menu.getStock() >= MAX_STOCK || !menu.hasBottle()) return;

        float animationTick = menu.getProgress() + partialTick;
        int activeFin = (int) (animationTick / 4.0F) % 6;
        fillRelative(graphics, 32 + activeFin * 10, 33, 2, 24, 0xFF5DB9C7);

        int pipeOffset = (int) (animationTick % 30.0F);
        if (pipeOffset < 20) {
            fillRelative(graphics, 101, 34 + pipeOffset, 3, 3, 0xFF5DB9C7);
        } else {
            fillRelative(graphics, 103 + pipeOffset - 20, 51, 3, 3, 0xFF5DB9C7);
        }
    }

    private void fillRelative(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(leftPos + x, topPos + y, leftPos + x + width, topPos + y + height, color);
    }
}

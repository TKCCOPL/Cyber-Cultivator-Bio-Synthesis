package com.TKCCOPL.client.screen;

import com.TKCCOPL.api.RedstoneControlMode;
import com.TKCCOPL.menu.RedstoneMenuAccess;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

public abstract class MachineScreen<M extends AbstractContainerMenu> extends AbstractContainerScreen<M> {
    private final ResourceLocation texture;
    /** v1.1.7 RS 按钮位置与尺寸（4 机器共享） */
    private static final int RS_BUTTON_X = 128;
    private static final int RS_BUTTON_Y = 4;
    private static final int RS_BUTTON_W = 60;
    private static final int RS_BUTTON_H = 14;
    private Button redstoneButton;

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

    @Override
    protected void init() {
        super.init();
        // v1.1.7 自动添加 RS 按钮（仅当 menu 实现 RedstoneMenuAccess）
        if (menu instanceof RedstoneMenuAccess redstoneMenu) {
            redstoneButton = addRenderableWidget(Button.builder(
                            redstoneLabel(redstoneMenu),
                            button -> sendButton(redstoneMenu.getRedstoneButtonId()))
                    .bounds(leftPos + RS_BUTTON_X, topPos + RS_BUTTON_Y, RS_BUTTON_W, RS_BUTTON_H)
                    .tooltip(redstoneTooltip(redstoneMenu))
                    .build());
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateRedstoneButton();
    }

    private void updateRedstoneButton() {
        if (redstoneButton != null && menu instanceof RedstoneMenuAccess redstoneMenu) {
            redstoneButton.setMessage(redstoneLabel(redstoneMenu));
            redstoneButton.setTooltip(redstoneTooltip(redstoneMenu));
        }
    }

    /** 根据当前模式返回按钮文字（RS:忽略 / RS:高电平 / RS:低电平）。 */
    private Component redstoneLabel(RedstoneMenuAccess redstoneMenu) {
        RedstoneControlMode mode = safeMode(redstoneMenu.getRedstoneModeOrdinal());
        return Component.translatable("gui.cybercultivator.redstone.label." + mode.getSerializedName());
    }

    /** 根据当前模式 + 供电状态返回 tooltip。 */
    private Tooltip redstoneTooltip(RedstoneMenuAccess redstoneMenu) {
        RedstoneControlMode mode = safeMode(redstoneMenu.getRedstoneModeOrdinal());
        boolean powered = redstoneMenu.isRedstonePowered();
        String poweredKey = powered
                ? "gui.cybercultivator.redstone.powered.yes"
                : "gui.cybercultivator.redstone.powered.no";
        return Tooltip.create(Component.translatable(
                "gui.cybercultivator.redstone.tooltip." + mode.getSerializedName(),
                Component.translatable(poweredKey)));
    }

    private static RedstoneControlMode safeMode(int ordinal) {
        RedstoneControlMode[] values = RedstoneControlMode.values();
        if (ordinal < 0 || ordinal >= values.length) return RedstoneControlMode.IGNORE;
        return values[ordinal];
    }

    /**
     * v1.1.7 hotfix：返回红石阻塞状态行。
     * 仅在 {@link RedstoneMenuAccess#isRedstoneProcessingAllowed()} 为 {@code false} 时调用。
     * 子类状态行方法应在最高优先级判定中调用本方法。
     */
    protected Component redstoneBlockedStatus() {
        if (!(menu instanceof RedstoneMenuAccess redstoneMenu)) {
            return Component.empty();
        }
        RedstoneControlMode mode = safeMode(redstoneMenu.getRedstoneModeOrdinal());
        return Component.translatable("gui.cybercultivator.status.redstone_blocked." + mode.getSerializedName());
    }

    /** v1.1.7 hotfix：红石阻塞状态颜色（与"阻塞/等待"一致，使用警示橙）。 */
    protected static final int REDSTONE_BLOCKED_COLOR = 0x6B4C12;

    /** v1.1.7 hotfix：判断当前 menu 是否处于红石阻塞状态。 */
    protected boolean isRedstoneBlocked() {
        return menu instanceof RedstoneMenuAccess redstoneMenu
                && !redstoneMenu.isRedstoneProcessingAllowed();
    }

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

    protected void drawCentered(GuiGraphics graphics, Component text, int centerX, int y, int color) {
        graphics.drawString(font, text, centerX - font.width(text) / 2, y, color, false);
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

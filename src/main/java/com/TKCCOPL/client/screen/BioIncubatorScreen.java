package com.TKCCOPL.client.screen;

import com.TKCCOPL.Config;
import com.TKCCOPL.cybercultivator;
import com.TKCCOPL.menu.BioIncubatorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class BioIncubatorScreen extends MachineScreen<BioIncubatorMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(cybercultivator.MODID, "textures/gui/bio_incubator.png");

    public BioIncubatorScreen(BioIncubatorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, TEXTURE);
    }

    @Override
    protected void renderMachineState(GuiGraphics graphics, float partialTick) {
        verticalBar(graphics, 56, 50, 16, menu.getNutrition(), 100, 0xFFB7CF45);
        verticalBar(graphics, 86, 50, 16, menu.getPurity(), 100, 0xFF49B8D1);
        verticalBar(graphics, 116, 50, 16, menu.getDataSignal(), 100, 0xFFDBB441);
        horizontalBar(graphics, 22, 102, 155, menu.getGrowthPercent(), 100, 0xFF75BD4B);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0x373737, false);
        drawFitted(graphics, Component.literal("N " + menu.getNutrition()), 54, 38, 28, 0x6B741E);
        drawFitted(graphics, Component.literal("P " + menu.getPurity()), 84, 38, 28, 0x2F6F79);
        drawFitted(graphics, Component.literal("D " + menu.getDataSignal()), 114, 38, 28, 0x6B4C12);
        drawFitted(graphics, getStatusLine(), 22, 92, 155, getStatusColor());
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    private Component getStatusLine() {
        // v1.1.7 hotfix：红石阻塞优先级最高（高于产物阻塞、加工中等所有状态）
        if (isRedstoneBlocked()) {
            return redstoneBlockedStatus();
        }
        if (!menu.hasSeed()) {
            if (menu.hasResourceOutput()) {
                return Component.translatable("gui.cybercultivator.incubator.status_complete");
            }
            return Component.translatable("gui.cybercultivator.incubator.status_waiting_seed");
        }
        if (menu.getGrowthPercent() >= 100) {
            return Component.translatable("gui.cybercultivator.incubator.status_output_blocked");
        }

        int eta = menu.getEtaSeconds();
        if (eta >= 0) {
            return Component.translatable("gui.cybercultivator.incubator.status_growing",
                    menu.getGrowthPercent(), eta);
        }
        return Component.translatable("gui.cybercultivator.incubator.status_resources",
                getMissingResources());
    }

    private String getMissingResources() {
        StringBuilder missing = new StringBuilder();
        appendMissing(missing, menu.getNutrition() <= Config.resourceThreshold, "N");
        appendMissing(missing, menu.getPurity() <= Config.resourceThreshold, "P");
        appendMissing(missing, menu.getDataSignal() <= 0, "D");
        return missing.length() == 0 ? "N/P/D" : missing.toString();
    }

    private void appendMissing(StringBuilder missing, boolean required, String channel) {
        if (!required) return;
        if (missing.length() > 0) missing.append('/');
        missing.append(channel);
    }

    private int getStatusColor() {
        // v1.1.7 hotfix：红石阻塞使用警示橙
        if (isRedstoneBlocked()) return REDSTONE_BLOCKED_COLOR;
        if (!menu.hasSeed() || menu.hasResourceOutput() && menu.getGrowthPercent() == 0) return 0x555555;
        if (menu.getGrowthPercent() >= 100 || menu.getEtaSeconds() < 0) return 0x6B4C12;
        return 0x3F6F32;
    }
}

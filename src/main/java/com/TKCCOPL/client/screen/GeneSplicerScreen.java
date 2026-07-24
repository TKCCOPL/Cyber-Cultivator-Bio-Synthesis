package com.TKCCOPL.client.screen;

import com.TKCCOPL.cybercultivator;
import com.TKCCOPL.item.GeneticSeedItem;
import com.TKCCOPL.menu.GeneSplicerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class GeneSplicerScreen extends MachineScreen<GeneSplicerMenu> {
    private static final int PROGRESS_START_X = 99;
    private static final int PROGRESS_WIDTH = 42;
    private static final int PROGRESS_ARROW_HEAD_X = 133;
    private static final int PROGRESS_COLOR = 0xFFB868B2;
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(cybercultivator.MODID, "textures/gui/gene_splicer.png");

    public GeneSplicerScreen(GeneSplicerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, TEXTURE);
    }

    @Override
    protected void renderMachineState(GuiGraphics graphics, float partialTick) {
        ItemStack output = menu.getSlot(2).getItem();
        boolean hasOutput = !output.isEmpty();

        int progress = hasOutput ? menu.getSpliceDuration() : menu.getSpliceProgress();
        renderProgressArrow(graphics, progress, menu.getSpliceDuration());
    }

    private void renderProgressArrow(GuiGraphics graphics, int progress, int maximum) {
        if (progress <= 0 || maximum <= 0) return;

        int filled = Math.min(PROGRESS_WIDTH,
                (int) Math.ceil((double) progress * PROGRESS_WIDTH / maximum));
        for (int offset = 0; offset < filled; offset++) {
            int x = PROGRESS_START_X + offset;
            int top = 53;
            int bottom = 58;
            if (x >= PROGRESS_ARROW_HEAD_X) {
                int inset = Math.max(0, x - PROGRESS_ARROW_HEAD_X - 1);
                top = 49 + inset;
                bottom = 62 - inset;
            }
            graphics.fill(leftPos + x, topPos + top,
                    leftPos + x + 1, topPos + bottom, PROGRESS_COLOR);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        ItemStack first = menu.getSlot(0).getItem();
        ItemStack second = menu.getSlot(1).getItem();
        ItemStack output = menu.getSlot(2).getItem();

        graphics.drawString(font, title, titleLabelX, titleLabelY, 0x373737, false);
        Component parentA = Component.translatable("gui.cybercultivator.splicer.parent_a");
        Component parentB = Component.translatable("gui.cybercultivator.splicer.parent_b");
        graphics.drawString(font, parentA, 46 - font.width(parentA) / 2, 38, 0x4B3D4A, false);
        graphics.drawString(font, parentB, 82 - font.width(parentB) / 2, 38, 0x4B3D4A, false);
        graphics.drawString(font, Component.translatable("gui.cybercultivator.splicer.offspring"),
                144, 38, 0x4B3D4A, false);

        if (first.getItem() instanceof GeneticSeedItem && second.getItem() instanceof GeneticSeedItem
                && !(output.getItem() instanceof GeneticSeedItem)) {
            drawCentered(graphics, Component.translatable("gui.cybercultivator.splicer.prediction_meta",
                    formatPermille(menu.getPredictedMutationPermille()),
                    formatPermille(menu.getPredictedTwinPermille())), 88, 81, 0x5C3D58);
            drawCentered(graphics, getPreviewRange(first, second), 88, 96, 0x555555);
        }
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    private Component getPreviewRange(ItemStack first, ItemStack second) {
        int[] speed = ordinaryRange(first, second, GeneticSeedItem.GENE_SPEED);
        int[] yield = ordinaryRange(first, second, GeneticSeedItem.GENE_YIELD);
        int[] potency = ordinaryRange(first, second, GeneticSeedItem.GENE_POTENCY);
        return Component.translatable("gui.cybercultivator.splicer.preview_range",
                speed[0], speed[1], yield[0], yield[1], potency[0], potency[1]);
    }

    private int[] ordinaryRange(ItemStack first, ItemStack second, String key) {
        int center = (GeneticSeedItem.getGene(first, key) + GeneticSeedItem.getGene(second, key)) / 2;
        var config = com.TKCCOPL.client.ClientGameplayConfig.getSnapshot();
        int range = Math.max(0, config.mutationRange());
        return new int[]{Math.max(config.geneMin(), center - range), Math.min(config.geneMax(), center + range)};
    }

    private String formatPermille(int value) {
        int tenths = Math.max(0, Math.min(1000, value));
        if (tenths % 10 == 0) {
            return Integer.toString(tenths / 10);
        }
        return tenths / 10 + "." + tenths % 10;
    }
}

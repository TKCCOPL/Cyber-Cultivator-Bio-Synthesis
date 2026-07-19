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
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(cybercultivator.MODID, "textures/gui/gene_splicer.png");

    public GeneSplicerScreen(GeneSplicerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, TEXTURE);
    }

    @Override
    protected void renderMachineState(GuiGraphics graphics, float partialTick) {
        ItemStack first = menu.getSlot(0).getItem();
        ItemStack second = menu.getSlot(1).getItem();
        ItemStack output = menu.getSlot(2).getItem();
        boolean hasOutput = !output.isEmpty();

        int progress = hasOutput ? menu.getSpliceDuration() : menu.getSpliceProgress();
        thinHorizontalBar(graphics, 102, 54, 27, progress, menu.getSpliceDuration(), 0xFFB868B2);
        renderConnectorAnimation(graphics, partialTick);
    }

    private void renderConnectorAnimation(GuiGraphics graphics, float partialTick) {
        if (!menu.isSplicing()) return;
        float phase = ((menu.getSpliceProgress() + partialTick) * 0.8F) % 32.0F;
        drawConnectorPulse(graphics, phase, true);
        drawConnectorPulse(graphics, phase, false);
        drawConnectorPulse(graphics, (phase + 16.0F) % 32.0F, true);
        drawConnectorPulse(graphics, (phase + 16.0F) % 32.0F, false);
    }

    private void drawConnectorPulse(GuiGraphics graphics, float phase, boolean leftBranch) {
        int step = (int) phase;
        int x;
        int y;
        if (step < 14) {
            x = leftBranch ? 47 : 83;
            y = 45 - step;
        } else {
            int horizontalStep = Math.min(17, step - 14);
            x = leftBranch ? 47 + horizontalStep : 83 - horizontalStep;
            y = 31;
        }
        graphics.fill(leftPos + x - 1, topPos + y - 1,
                leftPos + x + 2, topPos + y + 2, 0xFFB868B2);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        ItemStack first = menu.getSlot(0).getItem();
        ItemStack second = menu.getSlot(1).getItem();
        ItemStack output = menu.getSlot(2).getItem();

        graphics.drawString(font, title, titleLabelX, titleLabelY, 0x373737, false);
        graphics.drawString(font, Component.translatable("gui.cybercultivator.splicer.parent_a"),
                38, 38, 0x4B3D4A, false);
        graphics.drawString(font, Component.translatable("gui.cybercultivator.splicer.parent_b"),
                74, 38, 0x4B3D4A, false);
        graphics.drawString(font, Component.translatable("gui.cybercultivator.splicer.offspring"),
                144, 38, 0x4B3D4A, false);

        drawFitted(graphics, getStatus(first, second, output), 12, 68, 164,
                getStatusColor(menu.isSplicing() || !output.isEmpty()));
        if (output.getItem() instanceof GeneticSeedItem) {
            drawFitted(graphics, Component.translatable("gui.cybercultivator.splicer.offspring_generation",
                    GeneticSeedItem.getGeneration(output)), 12, 81, 164, 0x5C3D58);
        } else if (first.getItem() instanceof GeneticSeedItem && second.getItem() instanceof GeneticSeedItem) {
            drawFitted(graphics, Component.translatable("gui.cybercultivator.splicer.prediction_meta",
                    menu.getPredictedGeneration(), formatMutationPercent()), 12, 81, 164, 0x5C3D58);
        }
        drawFitted(graphics, getOffspringInfo(first, second, output), 12, 96, 164,
                output.isEmpty() ? 0x555555 : 0x78406F);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    private Component getStatus(ItemStack first, ItemStack second, ItemStack output) {
        // v1.1.7 hotfix：红石阻塞优先级最高（高于产物就绪、拼接中等所有状态）
        if (isRedstoneBlocked()) {
            return redstoneBlockedStatus();
        }
        if (!output.isEmpty()) {
            String key = output.hasTag() && output.getTag().getInt("Mutation") > 0
                    ? "gui.cybercultivator.splicer.status_mutated"
                    : "gui.cybercultivator.splicer.status_complete";
            return Component.translatable(key);
        }
        if (menu.isSplicing()) {
            return Component.translatable("gui.cybercultivator.splicer.status_splicing",
                    menu.getRemainingSeconds());
        }
        if (!first.isEmpty() && !second.isEmpty()) {
            return Component.translatable("gui.cybercultivator.splicer.status_ready");
        }
        return Component.translatable(first.isEmpty() && second.isEmpty()
                ? "gui.cybercultivator.splicer.status_waiting_two"
                : "gui.cybercultivator.splicer.status_waiting_one");
    }

    /** v1.1.7 hotfix：拼接机状态行颜色，红石阻塞优先。 */
    private int getStatusColor(boolean active) {
        if (isRedstoneBlocked()) return REDSTONE_BLOCKED_COLOR;
        return active ? 0x78406F : 0x555555;
    }

    private Component getOffspringInfo(ItemStack first, ItemStack second, ItemStack output) {
        if (output.getItem() instanceof GeneticSeedItem) {
            int synergy = GeneticSeedItem.getSynergy(output);
            String key = synergy > 0
                    ? "gui.cybercultivator.splicer.offspring_summary_synergy"
                    : "gui.cybercultivator.splicer.offspring_summary";
            if (synergy > 0) {
                return Component.translatable(key,
                        GeneticSeedItem.getGene(output, GeneticSeedItem.GENE_SPEED),
                        GeneticSeedItem.getGene(output, GeneticSeedItem.GENE_YIELD),
                        GeneticSeedItem.getGene(output, GeneticSeedItem.GENE_POTENCY), synergy);
            }
            return Component.translatable(key,
                    GeneticSeedItem.getGene(output, GeneticSeedItem.GENE_SPEED),
                    GeneticSeedItem.getGene(output, GeneticSeedItem.GENE_YIELD),
                    GeneticSeedItem.getGene(output, GeneticSeedItem.GENE_POTENCY));
        }
        if (first.getItem() instanceof GeneticSeedItem && second.getItem() instanceof GeneticSeedItem) {
            return Component.translatable("gui.cybercultivator.splicer.preview",
                    averageGene(first, second, GeneticSeedItem.GENE_SPEED),
                    averageGene(first, second, GeneticSeedItem.GENE_YIELD),
                    averageGene(first, second, GeneticSeedItem.GENE_POTENCY));
        }
        return Component.empty();
    }

    private int averageGene(ItemStack first, ItemStack second, String key) {
        return (GeneticSeedItem.getGene(first, key) + GeneticSeedItem.getGene(second, key)) / 2;
    }

    private String formatMutationPercent() {
        int tenths = Math.max(0, Math.min(1000, menu.getPredictedMutationPermille()));
        if (tenths % 10 == 0) {
            return Integer.toString(tenths / 10);
        }
        return tenths / 10 + "." + tenths % 10;
    }
}

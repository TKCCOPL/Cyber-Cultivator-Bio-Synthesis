package com.TKCCOPL.client;

import com.TKCCOPL.Config;
import com.TKCCOPL.block.entity.AtmosphericCondenserBlockEntity;
import com.TKCCOPL.block.entity.BioIncubatorBlockEntity;
import com.TKCCOPL.block.entity.GeneSplicerBlockEntity;
import com.TKCCOPL.block.entity.SerumBottlerBlockEntity;
import com.TKCCOPL.curios.CuriosCompat;
import com.TKCCOPL.item.GeneticSeedItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "cybercultivator", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class IncubatorHudOverlay {
    private IncubatorHudOverlay() {
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui) return;

        if (!CuriosCompat.isCuriosLoaded() || !CuriosCompat.hasSpectrumMonocle(player)) return;

        // Check if player is looking at a block entity within range
        HitResult hit = mc.hitResult;
        if (!(hit instanceof BlockHitResult blockHit)) return;
        BlockPos targetPos = blockHit.getBlockPos();
        if (player.distanceToSqr(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5)
                > Config.monocleHudRange * Config.monocleHudRange) return;

        BlockEntity be = player.level().getBlockEntity(targetPos);
        GuiGraphics gui = event.getGuiGraphics();

        if (be instanceof BioIncubatorBlockEntity incubator) {
            drawIncubatorHud(gui, mc, incubator);
        } else if (be instanceof SerumBottlerBlockEntity bottler) {
            drawBottlerHud(gui, mc, bottler);
        } else if (be instanceof AtmosphericCondenserBlockEntity condenser) {
            drawCondenserHud(gui, mc, condenser);
        } else if (be instanceof GeneSplicerBlockEntity splicer) {
            drawSplicerHud(gui, mc, splicer);
        }
    }

    private static void drawIncubatorHud(GuiGraphics gui, Minecraft mc, BioIncubatorBlockEntity incubator) {
        int x = 10;
        int y = 10;

        // Background — seed present: 97px (includes mutation line), no seed: 63px
        int bgHeight = incubator.hasSeed() ? 97 : 63;
        gui.fill(x, y, x + 130, y + bgHeight, 0xAA000000);

        // Title
        gui.drawString(mc.font, Component.translatable("hud.cybercultivator.incubator"), x + 4, y + 2, 0x44F7FF);

        // Nutrition bar
        drawBar(gui, mc, x + 4, y + 14, "N", incubator.getNutrition(), 0x44FF44);

        // Purity bar
        drawBar(gui, mc, x + 4, y + 26, "P", incubator.getPurity(), 0x4488FF);

        // Data Signal bar
        drawBar(gui, mc, x + 4, y + 38, "D", incubator.getDataSignal(), 0xFFFF44);

        // Seed status
        Component seedText = incubator.hasSeed()
                ? Component.translatable("hud.cybercultivator.seed_in")
                : Component.translatable("hud.cybercultivator.seed_empty");
        gui.drawString(mc.font, seedText, x + 4, y + 50,
                incubator.hasSeed() ? 0x44FF44 : 0xFF4444);

        // Growth progress (only when seed is present)
        if (incubator.hasSeed()) {
            // Mutation marker on seed
            ItemStack seedStack = incubator.getSeed();
            net.minecraft.nbt.CompoundTag seedTag = seedStack.getTag();
            if (seedTag != null && seedTag.getBoolean("Mutation")) {
                gui.drawString(mc.font, Component.translatable("hud.cybercultivator.mutation")
                        .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE),
                        x + 4, y + 62, 0xFF55FF);
            }

            int growthPercent = incubator.getGrowthPercent();
            drawBar(gui, mc, x + 4, y + 73, "G", growthPercent, 0xFF8844);
            int eta = incubator.getEstimatedSecondsRemaining();
            String etaText = eta >= 0 ? "ETA: 约" + eta + "s" : "ETA: 资源不足";
            gui.drawString(mc.font, Component.literal(etaText), x + 4, y + 85, 0xCCCCCC);
        }
    }

    private static void drawBottlerHud(GuiGraphics gui, Minecraft mc, SerumBottlerBlockEntity bottler) {
        int x = 10;
        int y = 10;
        int activeRecipe = bottler.getActiveRecipe();
        boolean processing = bottler.getMaxProgress() > 0;

        int hudHeight = processing ? 74 : 50;
        gui.fill(x, y, x + 130, y + hudHeight, 0xAA000000);

        // Title
        gui.drawString(mc.font, Component.translatable("hud.cybercultivator.bottler"), x + 4, y + 2, 0x44F7FF);

        // Recipe status
        Component recipeName = getRecipeName(activeRecipe);
        int recipeColor;
        if (activeRecipe >= 0 && processing) {
            recipeColor = 0x44FF44; // green — active
        } else {
            recipeColor = 0x999999; // gray — idle
        }
        gui.drawString(mc.font, Component.translatable("hud.cybercultivator.recipe").append(recipeName), x + 4, y + 14, recipeColor);

        // Progress bar (only when processing)
        if (processing) {
            int progressPercent = (int) (bottler.getProgress() * 100.0 / bottler.getMaxProgress());
            drawBar(gui, mc, x + 4, y + 26, "P", progressPercent, 0xFF8844);
        }

        // Output slot Activity
        net.minecraft.world.item.ItemStack output = bottler.getOutput();
        if (!output.isEmpty()) {
            int activity = SerumBottlerBlockEntity.getActivity(output);
            gui.drawString(mc.font, Component.translatable("hud.cybercultivator.activity", activity), x + 4, y + (processing ? 40 : 26),
                    0xFFFF44);
        }

        // Output item name (when idle and has output) — truncate to fit HUD width
        if (!processing && !output.isEmpty()) {
            String rawName = output.getHoverName().getString();
            int maxNameWidth = 114;
            String displayName = rawName;
            if (mc.font.width(rawName) > maxNameWidth) {
                displayName = mc.font.plainSubstrByWidth(rawName, maxNameWidth - 6) + "...";
            }
            gui.drawString(mc.font, Component.translatable("hud.cybercultivator.output_item", displayName),
                    x + 4, y + 38, 0xCCCCCC);
        }
    }

    private static void drawCondenserHud(GuiGraphics gui, Minecraft mc, AtmosphericCondenserBlockEntity condenser) {
        int x = 10;
        int y = 10;
        int hudHeight = 50;
        gui.fill(x, y, x + 130, y + hudHeight, 0xAA000000);

        // Title
        gui.drawString(mc.font, Component.translatable("hud.cybercultivator.condenser"), x + 4, y + 2, 0x44F7FF);

        // Progress bar
        int progressPercent = (int) (condenser.getProgress() * 100.0 / condenser.getMaxProgress());
        drawBar(gui, mc, x + 4, y + 14, "P", progressPercent, 0x4488FF);

        // Stock
        gui.drawString(mc.font, Component.translatable("hud.cybercultivator.stock", condenser.getStock(), condenser.getMaxStock()), x + 4, y + 26, 0xCCCCCC);

        // Status
        Component statusText;
        int statusColor;
        if (condenser.getStock() >= condenser.getMaxStock()) {
            statusText = Component.translatable("hud.cybercultivator.full");
            statusColor = 0xFFFF44; // yellow
        } else if (condenser.getProgress() > 0) {
            statusText = Component.translatable("hud.cybercultivator.producing");
            statusColor = 0x44FF44; // green
        } else {
            statusText = Component.translatable("hud.cybercultivator.idle");
            statusColor = 0x999999; // gray
        }
        gui.drawString(mc.font, statusText, x + 4, y + 38, statusColor);
    }

    private static void drawSplicerHud(GuiGraphics gui, Minecraft mc, GeneSplicerBlockEntity splicer) {
        int x = 10;
        int y = 10;
        ItemStack output = splicer.getOutput();
        boolean hasOutput = !output.isEmpty();

        // Check seed mutations (affects layout height)
        boolean seedAMutation = isSeedMutation(splicer.getSeedA());
        boolean seedBMutation = isSeedMutation(splicer.getSeedB());
        int seedExtraLines = (seedAMutation ? 1 : 0) + (seedBMutation ? 1 : 0);

        // 有 output 时显示 4-5 行（父本 A + 父本 B + 空行 + 结果 [+ 突变标记]），否则 3 行
        boolean hasOutputMutation = false;
        if (hasOutput) {
            net.minecraft.nbt.CompoundTag outTag = output.getTag();
            hasOutputMutation = outTag != null && outTag.getBoolean("Mutation");
        }
        int baseHeight = hasOutput ? (hasOutputMutation ? 74 : 62) : 50;
        int hudHeight = baseHeight + seedExtraLines * 11;
        gui.fill(x, y, x + 200, y + hudHeight, 0xAA000000);

        // Title
        gui.drawString(mc.font, Component.translatable("hud.cybercultivator.splicer"), x + 4, y + 2, 0x44F7FF);

        // Seed A (always show) — spacing 13px to accommodate mutation marker
        drawSeedInfo(gui, mc, x + 4, y + 14, "A", splicer.getSeedA());

        // Seed B — offset depends on seed A mutation
        int seedBY = 27 + (seedAMutation ? 11 : 0);
        drawSeedInfo(gui, mc, x + 4, y + seedBY, "B", splicer.getSeedB());

        // Output section — offset depends on seed mutations
        int outputBaseY = 38 + seedExtraLines * 11;
        if (hasOutput) {
            // 分隔线
            gui.fill(x + 4, y + outputBaseY - 1, x + 196, y + outputBaseY, 0xFF333333);
            int speed = GeneticSeedItem.getGene(output, GeneticSeedItem.GENE_SPEED);
            int yield = GeneticSeedItem.getGene(output, GeneticSeedItem.GENE_YIELD);
            int potency = GeneticSeedItem.getGene(output, GeneticSeedItem.GENE_POTENCY);
            int purity = GeneticSeedItem.getPurity(output);
            String outLine = String.format("Out: [S:%d Y:%d P:%d]", speed, yield, potency);
            if (purity > 0) {
                outLine += String.format(" Pur:%d", purity);
            }
            gui.drawString(mc.font, Component.literal(outLine),
                    x + 4, y + outputBaseY + 2, 0xFFAA00);
            // Mutation marker on output
            net.minecraft.nbt.CompoundTag outputTag = output.getTag();
            if (outputTag != null && outputTag.getBoolean("Mutation")) {
                gui.drawString(mc.font, Component.translatable("hud.cybercultivator.mutation")
                        .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE),
                        x + 4, y + outputBaseY + 14, 0xFF55FF);
                gui.drawString(mc.font, Component.translatable("hud.cybercultivator.ready_extract"), x + 4, y + outputBaseY + 24, 0xCCCCCC);
            } else {
                gui.drawString(mc.font, Component.translatable("hud.cybercultivator.ready_extract"), x + 4, y + outputBaseY + 14, 0xCCCCCC);
            }
        } else {
            gui.drawString(mc.font, Component.translatable("hud.cybercultivator.out_empty"), x + 4, y + outputBaseY, 0x999999);
        }
    }

    private static boolean isSeedMutation(ItemStack seed) {
        if (seed.isEmpty()) return false;
        net.minecraft.nbt.CompoundTag tag = seed.getTag();
        return tag != null && tag.getBoolean("Mutation");
    }

    private static void drawSeedInfo(GuiGraphics gui, Minecraft mc, int x, int y, String label, ItemStack seed) {
        if (seed.isEmpty()) {
            gui.drawString(mc.font, Component.literal(label + ": Empty"), x, y, 0x999999);
        } else {
            int speed = GeneticSeedItem.getGene(seed, GeneticSeedItem.GENE_SPEED);
            int yield = GeneticSeedItem.getGene(seed, GeneticSeedItem.GENE_YIELD);
            int potency = GeneticSeedItem.getGene(seed, GeneticSeedItem.GENE_POTENCY);
            int gen = GeneticSeedItem.getGeneration(seed);
            String text = label + ": [S:" + speed + " Y:" + yield + " P:" + potency + "]";
            if (gen > 0) {
                text += " Gen:" + gen;
            }
            gui.drawString(mc.font, Component.literal(text), x, y, 0x44FF44);
            // Mutation marker — displayed on next line to avoid overflow
            net.minecraft.nbt.CompoundTag tag = seed.getTag();
            if (tag != null && tag.getBoolean("Mutation")) {
                gui.drawString(mc.font, Component.translatable("hud.cybercultivator.mutation")
                        .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE),
                        x, y + 10, 0xFF55FF);
            }
        }
    }

    private static Component getRecipeName(int recipe) {
        return switch (recipe) {
            case 0 -> Component.translatable("hud.cybercultivator.recipe_berry");
            case 1 -> Component.translatable("hud.cybercultivator.recipe_s01");
            case 2 -> Component.translatable("hud.cybercultivator.recipe_s02");
            case 3 -> Component.translatable("hud.cybercultivator.recipe_s03");
            default -> Component.translatable("hud.cybercultivator.recipe_idle");
        };
    }

    private static void drawBar(GuiGraphics gui, Minecraft mc, int x, int y, String label, int value, int color) {
        // Label
        gui.drawString(mc.font, Component.literal(label), x, y, color);

        // Bar background
        int barX = x + 12;
        int barWidth = 80;
        gui.fill(barX, y, barX + barWidth, y + 8, 0xFF333333);

        // Bar fill
        int fillWidth = (int) (barWidth * value / 100.0);
        gui.fill(barX, y, barX + fillWidth, y + 8, color);

        // Value text
        gui.drawString(mc.font, Component.literal(String.valueOf(value)), barX + barWidth + 4, y, 0xCCCCCC);
    }
}

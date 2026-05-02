package com.TKCCOPL.client;

import com.TKCCOPL.Config;
import com.TKCCOPL.block.entity.BioIncubatorBlockEntity;
import com.TKCCOPL.curios.CuriosCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
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

        // Check if player is looking at an incubator within range
        HitResult hit = mc.hitResult;
        if (!(hit instanceof BlockHitResult blockHit)) return;
        BlockPos targetPos = blockHit.getBlockPos();
        if (player.distanceToSqr(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5)
                > Config.monocleHudRange * Config.monocleHudRange) return;

        BlockEntity be = player.level().getBlockEntity(targetPos);
        if (!(be instanceof BioIncubatorBlockEntity incubator)) return;

        GuiGraphics gui = event.getGuiGraphics();

        int x = 10;
        int y = 10;

        // Background
        gui.fill(x, y, x + 130, y + 60, 0xAA000000);

        // Title
        gui.drawString(mc.font, Component.literal("[Bio-Incubator]"), x + 4, y + 2, 0x44F7FF);

        // Nutrition bar
        drawBar(gui, mc, x + 4, y + 14, "N", incubator.getNutrition(), 0x44FF44);

        // Purity bar
        drawBar(gui, mc, x + 4, y + 26, "P", incubator.getPurity(), 0x4488FF);

        // Data Signal bar
        drawBar(gui, mc, x + 4, y + 38, "D", incubator.getDataSignal(), 0xFFFF44);

        // Seed status
        String seedText = incubator.hasSeed() ? "Seed: In" : "Seed: Empty";
        gui.drawString(mc.font, Component.literal(seedText), x + 4, y + 50,
                incubator.hasSeed() ? 0x44FF44 : 0xFF4444);
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

package com.TKCCOPL.curios;

import com.TKCCOPL.cybercultivator;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = cybercultivator.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CurioEventHandler {
    private CurioEventHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (player.level().isClientSide) return;
        if (!CuriosCompat.isCuriosLoaded()) return;

        // The belt only works every five seconds; avoid querying Curios on the 99 idle ticks.
        if (player.level().getGameTime() % 100L == 0L && CuriosCompat.hasBioPulseBelt(player)) {
            BioPulseBeltItem.tick(player);
        }

        // Query every 10 ticks for mitigation, or immediately when emergency healing can trigger.
        if (LifeSupportPackItem.shouldTick(player) && CuriosCompat.hasLifeSupportPack(player)) {
            LifeSupportPackItem.tick(player);
        }
    }
}

package com.TKCCOPL.curios;

import com.TKCCOPL.cybercultivator;
import com.TKCCOPL.init.ModItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

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

        // Bio-Pulse Belt tick
        if (CuriosCompat.hasCurioItem(player, ModItems.BIO_PULSE_BELT.get())) {
            BioPulseBeltItem.tick(player);
        }

        // Life Support Pack tick
        ItemStack packStack = findCurioStack(player, ModItems.LIFE_SUPPORT_PACK.get());
        if (packStack != null) {
            LifeSupportPackItem.tick(player, packStack);
        }
    }

    private static ItemStack findCurioStack(Player player, net.minecraft.world.item.Item item) {
        try {
            var inventory = CuriosApi.getCuriosInventory(player).resolve();
            if (inventory.isEmpty()) return null;
            var curio = inventory.get().findFirstCurio(item);
            return curio.map(slotResult -> slotResult.stack()).orElse(null);
        } catch (Throwable ignored) {
            return null;
        }
    }
}

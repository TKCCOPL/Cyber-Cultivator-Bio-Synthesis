package com.TKCCOPL.event;

import com.TKCCOPL.cybercultivator;
import com.TKCCOPL.init.ModItems;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.BasicItemListing;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = cybercultivator.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class VillagerTradeEvents {
    private static final int APPRENTICE_LEVEL = 2;
    private static final int EMERALD_COST = 3;
    private static final int SEED_COUNT = 2;
    private static final int MAX_TRADES = 8;
    private static final int VILLAGER_XP = 10;
    private static final float PRICE_MULTIPLIER = 0.05F;

    private VillagerTradeEvents() {
    }

    @SubscribeEvent
    public static void addSeedTrades(VillagerTradesEvent event) {
        if (event.getType() == VillagerProfession.FARMER) {
            event.getTrades().get(APPRENTICE_LEVEL).add(new BasicItemListing(
                    EMERALD_COST, new ItemStack(ModItems.PROTEIN_SOY_SEEDS.get(), SEED_COUNT),
                    MAX_TRADES, VILLAGER_XP, PRICE_MULTIPLIER));
        } else if (event.getType() == VillagerProfession.CLERIC) {
            event.getTrades().get(APPRENTICE_LEVEL).add(new BasicItemListing(
                    EMERALD_COST, new ItemStack(ModItems.ALCOHOL_BLOOM_SEEDS.get(), SEED_COUNT),
                    MAX_TRADES, VILLAGER_XP, PRICE_MULTIPLIER));
        }
    }
}

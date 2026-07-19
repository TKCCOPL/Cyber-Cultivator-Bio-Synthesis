package com.TKCCOPL.curios;

import com.TKCCOPL.Config;
import com.TKCCOPL.block.entity.BioIncubatorBlockEntity;
import com.TKCCOPL.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class BioPulseBeltItem extends CurioAccessoryItem {
    public BioPulseBeltItem(Properties properties) {
        super(properties, "belt", "tooltip.cybercultivator.bio_pulse_belt");
    }

    public static void tick(Player player) {
        Level level = player.level();
        if (level.isClientSide || level.getGameTime() % 100L != 0L) return; // 每 5 秒扫描一次

        int range = Math.min(Config.beltScanRange, 5); // 硬编码上限 5（11³=1331）
        BlockPos origin = player.blockPosition();

        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-range, -range, -range), origin.offset(range, range, range))) {
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof BioIncubatorBlockEntity incubator)) continue;

            if (incubator.getNutrition() < Config.beltNutritionThreshold) {
                if (AccessoryInventoryHelper.consumeOne(player,
                        ModItems.BIOCHEMICAL_SOLUTION.get(), null)) {
                    incubator.addNutrition(Config.nutritionInjectAmount);
                }
            }

            if (incubator.getPurity() < Config.beltPurityThreshold) {
                if (AccessoryInventoryHelper.consumeOne(player,
                        ModItems.PURIFIED_WATER_BOTTLE.get(), Items.GLASS_BOTTLE)) {
                    incubator.addPurity(Config.purityInjectAmount);
                }
            }

            if (incubator.getDataSignal() < Config.beltDataSignalThreshold) {
                if (AccessoryInventoryHelper.consumeOne(player, ModItems.SILICON_SHARD.get(), null)) {
                    incubator.addDataSignal(Config.dataSignalInjectAmount);
                }
            }
        }
    }
}

package com.TKCCOPL.curios;

import com.TKCCOPL.Config;
import com.TKCCOPL.block.entity.BioIncubatorBlockEntity;
import com.TKCCOPL.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class BioPulseBeltItem extends CurioAccessoryItem {
    public BioPulseBeltItem(Properties properties) {
        super(properties, "belt", "tooltip.cybercultivator.bio_pulse_belt");
    }

    public static void tick(Player player) {
        Level level = player.level();
        if (level.isClientSide || level.getGameTime() % 20L != 0L) return;

        int range = Config.beltScanRange;
        BlockPos origin = player.blockPosition();

        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-range, -range, -range), origin.offset(range, range, range))) {
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof BioIncubatorBlockEntity incubator)) continue;

            if (incubator.getNutrition() < Config.beltNutritionThreshold) {
                if (consumeFromInventory(player, ModItems.BIOCHEMICAL_SOLUTION.get())) {
                    incubator.addNutrition(25);
                }
            }

            if (incubator.getPurity() < Config.beltPurityThreshold) {
                if (consumeFromInventory(player, ModItems.PURIFIED_WATER_BOTTLE.get())) {
                    incubator.addPurity(20);
                }
            }

            if (incubator.getDataSignal() < Config.beltDataSignalThreshold) {
                if (consumeFromInventory(player, ModItems.SILICON_SHARD.get())) {
                    incubator.addDataSignal(15);
                }
            }
        }
    }

    private static boolean consumeFromInventory(Player player, net.minecraft.world.item.Item item) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack slotStack = player.getInventory().getItem(i);
            if (slotStack.is(item)) {
                slotStack.shrink(1);
                return true;
            }
        }
        return false;
    }
}

package com.TKCCOPL.curios;

import com.TKCCOPL.Config;
import com.TKCCOPL.block.entity.BioIncubatorBlockEntity;
import com.TKCCOPL.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashSet;
import java.util.Set;

public class BioPulseBeltItem extends CurioAccessoryItem {
    public BioPulseBeltItem(Properties properties) {
        super(properties, "belt", "tooltip.cybercultivator.bio_pulse_belt");
    }

    public static void tick(Player player) {
        Level level = player.level();
        if (level.isClientSide || level.getGameTime() % 100L != 0L) return; // 每 5 秒扫描一次
        performScan(player);
    }

    /**
     * 实际执行扫描与注入。分离出来便于单元测试直接调用，
     * 避免测试受 5 秒节流影响。生产路径仍由 {@link #tick} 节流后调用。
     */
    static void performScan(Player player) {
        Level level = player.level();
        if (level.isClientSide) return;

        int range = Math.min(Config.beltScanRange, 5); // 硬编码上限 5（11³=1331）
        BlockPos origin = player.blockPosition();

        // 单次库存扫描：构建当前持有的关键物品集合，避免每个 incubator × 每条通道都做完整库存扫描
        Set<Item> available = new HashSet<>();
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty()) {
                available.add(stack.getItem());
            }
        }
        boolean hasBiochem = available.contains(ModItems.BIOCHEMICAL_SOLUTION.get());
        boolean hasWater = available.contains(ModItems.PURIFIED_WATER_BOTTLE.get());
        boolean hasSilicon = available.contains(ModItems.SILICON_SHARD.get());

        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-range, -range, -range), origin.offset(range, range, range))) {
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof BioIncubatorBlockEntity incubator)) continue;

            // 跳过缺料通道，避免对空库存执行 consumeOne 的完整扫描
            if (hasBiochem && incubator.getNutrition() < Config.beltNutritionThreshold) {
                if (AccessoryInventoryHelper.consumeOne(player,
                        ModItems.BIOCHEMICAL_SOLUTION.get(), null)) {
                    incubator.addNutrition(Config.nutritionInjectAmount);
                } else {
                    hasBiochem = false;
                }
            }

            if (hasWater && incubator.getPurity() < Config.beltPurityThreshold) {
                if (AccessoryInventoryHelper.consumeOne(player,
                        ModItems.PURIFIED_WATER_BOTTLE.get(), Items.GLASS_BOTTLE)) {
                    incubator.addPurity(Config.purityInjectAmount);
                } else {
                    hasWater = false;
                }
            }

            if (hasSilicon && incubator.getDataSignal() < Config.beltDataSignalThreshold) {
                if (AccessoryInventoryHelper.consumeOne(player, ModItems.SILICON_SHARD.get(), null)) {
                    incubator.addDataSignal(Config.dataSignalInjectAmount);
                } else {
                    hasSilicon = false;
                }
            }
        }
    }
}

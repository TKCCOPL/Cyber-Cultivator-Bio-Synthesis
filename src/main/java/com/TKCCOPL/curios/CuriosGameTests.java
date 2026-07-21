package com.TKCCOPL.curios;

import com.TKCCOPL.Config;
import com.TKCCOPL.block.entity.BioIncubatorBlockEntity;
import com.TKCCOPL.cybercultivator;
import com.TKCCOPL.init.ModBlocks;
import com.TKCCOPL.init.ModItems;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(cybercultivator.MODID)
@PrefixGameTestTemplate(false)
public final class CuriosGameTests {
    private static final String EMPTY_TEMPLATE = "empty";

    private CuriosGameTests() {
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void accessoryConsumptionRejectsEmptyStacksAndReturnsContainers(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        player.getInventory().clearContent();
        player.getInventory().setItem(0, new ItemStack(ModItems.BIOCHEMICAL_SOLUTION.get()));

        helper.assertTrue(AccessoryInventoryHelper.consumeOne(
                        player, ModItems.BIOCHEMICAL_SOLUTION.get(), null),
                "A non-empty accessory resource must be consumed");
        helper.assertTrue(player.getInventory().getItem(0).isEmpty(),
                "Consuming the final resource must clear its inventory slot");
        helper.assertFalse(AccessoryInventoryHelper.consumeOne(
                        player, ModItems.BIOCHEMICAL_SOLUTION.get(), null),
                "An empty slot must not be consumed repeatedly");

        player.getInventory().setItem(0, new ItemStack(ModItems.PURIFIED_WATER_BOTTLE.get()));
        helper.assertTrue(AccessoryInventoryHelper.consumeOne(
                        player, ModItems.PURIFIED_WATER_BOTTLE.get(), Items.GLASS_BOTTLE),
                "Purified water must be consumed");
        helper.assertTrue(player.getInventory().contains(new ItemStack(Items.GLASS_BOTTLE)),
                "Consuming purified water must return its glass bottle");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void lifeSupportUsesPlayerCooldownAndRejectsDeadPlayers(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        player.getInventory().clearContent();
        player.setHealth(4.0F);
        player.getInventory().setItem(0, new ItemStack(ModItems.BIOCHEMICAL_SOLUTION.get()));

        LifeSupportPackItem.tick(player);
        helper.assertTrue(player.getHealth() == 8.0F, "Life support must heal four health points");
        helper.assertTrue(player.getInventory().getItem(0).isEmpty(),
                "Life support must consume one biochemical solution");
        helper.assertTrue(player.getCooldowns().isOnCooldown(ModItems.LIFE_SUPPORT_PACK.get()),
                "Life support cooldown must belong to the player rather than an accessory stack");

        player.setHealth(4.0F);
        player.getInventory().setItem(0, new ItemStack(ModItems.BIOCHEMICAL_SOLUTION.get()));
        LifeSupportPackItem.tick(player);
        helper.assertTrue(player.getHealth() == 4.0F,
                "An active cooldown must prevent another emergency heal");
        helper.assertTrue(player.getInventory().getItem(0).getCount() == 1,
                "An active cooldown must not consume another solution");

        Player deadPlayer = helper.makeMockSurvivalPlayer();
        deadPlayer.getInventory().clearContent();
        deadPlayer.setHealth(0.0F);
        deadPlayer.getInventory().setItem(0, new ItemStack(ModItems.BIOCHEMICAL_SOLUTION.get()));
        LifeSupportPackItem.tick(deadPlayer);
        helper.assertTrue(deadPlayer.getInventory().getItem(0).getCount() == 1,
                "Life support must not consume resources from a dead player");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void accessoryAdvancementAcceptsAnyAccessory(GameTestHelper helper) {
        Advancement advancement = helper.getLevel().getServer().getAdvancements().getAdvancement(
                ResourceLocation.fromNamespaceAndPath(cybercultivator.MODID, "cyber_equip"));
        helper.assertTrue(advancement != null, "The accessory advancement must be loaded");
        String[][] requirements = advancement.getRequirements();
        helper.assertTrue(requirements.length == 1 && requirements[0].length == 3,
                "The accessory advancement must accept any one of its three criteria");
        helper.succeed();
    }

    // ========== v1.1.7 perf optimization regression tests (BioPulseBelt) ==========

    @GameTest(template = EMPTY_TEMPLATE)
    public static void bioPulseBeltInjectsMaterialsIntoNearbyIncubator(GameTestHelper helper) {
        // 修复 10：BioPulseBelt 单次库存扫描 + 缺料通道跳过 —— 必须仍能正确注入三项数值
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.BIO_INCUBATOR.get());
        BioIncubatorBlockEntity incubator = (BioIncubatorBlockEntity) helper.getBlockEntity(pos);
        Player player = helper.makeMockSurvivalPlayer();
        player.getInventory().clearContent();
        player.getInventory().setItem(0, new ItemStack(ModItems.BIOCHEMICAL_SOLUTION.get(), 3));
        player.getInventory().setItem(1, new ItemStack(ModItems.PURIFIED_WATER_BOTTLE.get(), 3));
        player.getInventory().setItem(2, new ItemStack(ModItems.SILICON_SHARD.get(), 3));
        BlockPos absolute = helper.absolutePos(pos);
        player.setPos(absolute.getX() + 0.5, absolute.getY() + 0.5, absolute.getZ() + 0.5);

        // 直接调用 performScan 绕过 5 秒节流
        BioPulseBeltItem.performScan(player);

        helper.assertTrue(incubator.getNutrition() == Config.nutritionInjectAmount,
                "BioPulseBelt must inject nutrition from inventory into nearby incubator");
        helper.assertTrue(incubator.getPurity() == Config.purityInjectAmount,
                "BioPulseBelt must inject purity from inventory into nearby incubator");
        helper.assertTrue(incubator.getDataSignal() == Config.dataSignalInjectAmount,
                "BioPulseBelt must inject data signal from inventory into nearby incubator");
        // 三项原料库存必须各消耗 1
        helper.assertTrue(player.getInventory().getItem(0).getCount() == 2,
                "BioPulseBelt must consume exactly one biochemical solution");
        helper.assertTrue(player.getInventory().getItem(1).getCount() == 2,
                "BioPulseBelt must consume exactly one purified water bottle");
        // 净水消耗后返回玻璃瓶
        helper.assertTrue(player.getInventory().countItem(Items.GLASS_BOTTLE) == 1,
                "BioPulseBelt must return a glass bottle after consuming purified water");
        helper.assertTrue(player.getInventory().getItem(2).getCount() == 2,
                "BioPulseBelt must consume exactly one silicon shard");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void bioPulseBeltSkipsMissingMaterialChannelWithoutScanning(GameTestHelper helper) {
        // 修复 10：缺料通道必须跳过 —— 玩家只持有 biochem + silicon 时，purity 通道不触发
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.BIO_INCUBATOR.get());
        BioIncubatorBlockEntity incubator = (BioIncubatorBlockEntity) helper.getBlockEntity(pos);
        Player player = helper.makeMockSurvivalPlayer();
        player.getInventory().clearContent();
        // 故意不持有 PURIFIED_WATER_BOTTLE —— purity 通道应被跳过
        player.getInventory().setItem(0, new ItemStack(ModItems.BIOCHEMICAL_SOLUTION.get(), 3));
        player.getInventory().setItem(1, new ItemStack(ModItems.SILICON_SHARD.get(), 3));
        BlockPos absolute = helper.absolutePos(pos);
        player.setPos(absolute.getX() + 0.5, absolute.getY() + 0.5, absolute.getZ() + 0.5);

        BioPulseBeltItem.performScan(player);

        helper.assertTrue(incubator.getNutrition() == Config.nutritionInjectAmount,
                "Nutrition channel must still inject when biochem is available");
        helper.assertTrue(incubator.getPurity() == 0,
                "Purity channel must be skipped when no purified water is in inventory");
        helper.assertTrue(incubator.getDataSignal() == Config.dataSignalInjectAmount,
                "Data signal channel must still inject when silicon is available");
        // 持有的两项原料各消耗 1
        helper.assertTrue(player.getInventory().getItem(0).getCount() == 2,
                "Biochem must still be consumed for the nutrition channel");
        helper.assertTrue(player.getInventory().getItem(1).getCount() == 2,
                "Silicon shard must still be consumed for the data signal channel");
        // 不应生成空玻璃瓶（purity 通道未触发）
        helper.assertTrue(player.getInventory().countItem(Items.GLASS_BOTTLE) == 0,
                "No glass bottle must be returned when purity channel is skipped");
        helper.succeed();
    }

    @GameTest(template = EMPTY_TEMPLATE)
    public static void bioPulseBeltStopsScanningChannelAfterExhaustion(GameTestHelper helper) {
        // 修复 10：当库存只够注入一个 incubator 时，第二个 incubator 的同通道应跳过 consumeOne 完整扫描
        BlockPos posA = new BlockPos(1, 1, 1);
        BlockPos posB = new BlockPos(2, 1, 1);
        helper.setBlock(posA, ModBlocks.BIO_INCUBATOR.get());
        helper.setBlock(posB, ModBlocks.BIO_INCUBATOR.get());
        BioIncubatorBlockEntity incubatorA = (BioIncubatorBlockEntity) helper.getBlockEntity(posA);
        BioIncubatorBlockEntity incubatorB = (BioIncubatorBlockEntity) helper.getBlockEntity(posB);
        Player player = helper.makeMockSurvivalPlayer();
        player.getInventory().clearContent();
        // 只持有一份 biochem —— 第一个 incubator 注入后 hasBiochem 翻为 false，第二个跳过
        player.getInventory().setItem(0, new ItemStack(ModItems.BIOCHEMICAL_SOLUTION.get(), 1));
        BlockPos absolute = helper.absolutePos(posA);
        player.setPos(absolute.getX() + 0.5, absolute.getY() + 0.5, absolute.getZ() + 0.5);

        BioPulseBeltItem.performScan(player);

        // 至少一个 incubator 必须收到注入；另一个因库存耗尽而跳过
        int injected = (incubatorA.getNutrition() == Config.nutritionInjectAmount ? 1 : 0)
                + (incubatorB.getNutrition() == Config.nutritionInjectAmount ? 1 : 0);
        helper.assertTrue(injected == 1,
                "Exactly one incubator must receive nutrition when only one biochem is in inventory");
        helper.assertTrue(player.getInventory().getItem(0).isEmpty(),
                "Biochem stack must be fully consumed after one injection");
        helper.succeed();
    }
}

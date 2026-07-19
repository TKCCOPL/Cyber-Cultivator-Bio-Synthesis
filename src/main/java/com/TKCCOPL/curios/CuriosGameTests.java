package com.TKCCOPL.curios;

import com.TKCCOPL.cybercultivator;
import com.TKCCOPL.init.ModItems;
import net.minecraft.advancements.Advancement;
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
}

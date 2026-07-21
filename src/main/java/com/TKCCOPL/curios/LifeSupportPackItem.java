package com.TKCCOPL.curios;

import com.TKCCOPL.Config;
import com.TKCCOPL.init.ModEffects;
import com.TKCCOPL.init.ModItems;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public class LifeSupportPackItem extends CurioAccessoryItem {
    public LifeSupportPackItem(Properties properties) {
        super(properties, "back", "tooltip.cybercultivator.life_support_pack");
    }

    static boolean shouldTick(Player player) {
        if (!player.isAlive()) return false;
        if (player.level().getGameTime() % 10L == 0L) return true;
        return player.getHealth() <= Config.packHealThreshold
                && !player.getCooldowns().isOnCooldown(ModItems.LIFE_SUPPORT_PACK.get());
    }

    public static void tick(Player player) {
        if (player.level().isClientSide || !player.isAlive()) return;

        // Side effect mitigation: reduce any NeuralOverload variant duration (every 10 ticks)
        if (player.level().getGameTime() % 10L == 0L) {
            for (MobEffect overload : List.of(
                    ModEffects.NEURAL_OVERLOAD.get(),
                    ModEffects.NEURAL_OVERLOAD_S01.get(),
                    ModEffects.NEURAL_OVERLOAD_S02.get(),
                    ModEffects.NEURAL_OVERLOAD_S03.get())) {
                MobEffectInstance instance = player.getEffect(overload);
                if (instance == null) continue;
                int newDuration = instance.getDuration() - Config.packEffectReductionRate * 10;
                if (newDuration > 0) {
                    player.forceAddEffect(new MobEffectInstance(
                            overload, newDuration, instance.getAmplifier(),
                            instance.isAmbient(), instance.isVisible(), false), null);
                } else {
                    player.removeEffect(overload);
                }
            }
        }

        // Low HP emergency heal
        if (player.getCooldowns().isOnCooldown(ModItems.LIFE_SUPPORT_PACK.get())) return;

        if (player.getHealth() <= Config.packHealThreshold) {
            if (AccessoryInventoryHelper.consumeOne(player, ModItems.BIOCHEMICAL_SOLUTION.get(), null)) {
                player.heal(4.0F);
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 200, 0,
                        true, false, true));
                player.getCooldowns().addCooldown(ModItems.LIFE_SUPPORT_PACK.get(), Config.packHealCooldown);
            }
        }
    }
}

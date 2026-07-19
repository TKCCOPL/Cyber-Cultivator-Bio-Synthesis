package com.TKCCOPL.curios;

import com.TKCCOPL.Config;
import com.TKCCOPL.init.ModEffects;
import com.TKCCOPL.init.ModItems;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

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

        // Side effect mitigation: reduce NeuralOverload duration (every 10 ticks)
        if (player.level().getGameTime() % 10L == 0L) {
            MobEffectInstance overload = player.getEffect(ModEffects.NEURAL_OVERLOAD.get());
            if (overload != null) {
                int newDuration = overload.getDuration() - Config.packEffectReductionRate * 10;
                if (newDuration > 0) {
                    player.forceAddEffect(new MobEffectInstance(
                            ModEffects.NEURAL_OVERLOAD.get(), newDuration, overload.getAmplifier(),
                            overload.isAmbient(), overload.isVisible(), false), null);
                } else {
                    player.removeEffect(ModEffects.NEURAL_OVERLOAD.get());
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

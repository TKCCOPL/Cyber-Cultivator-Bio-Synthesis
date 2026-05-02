package com.TKCCOPL.curios;

import com.TKCCOPL.Config;
import com.TKCCOPL.init.ModEffects;
import com.TKCCOPL.init.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class LifeSupportPackItem extends CurioAccessoryItem {
    private static final String TAG_HEAL_COOLDOWN = "HealCooldown";

    public LifeSupportPackItem(Properties properties) {
        super(properties, "back", "tooltip.cybercultivator.life_support_pack");
    }

    public static void tick(Player player, ItemStack packStack) {
        if (player.level().isClientSide) return;

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
        int cooldown = getHealCooldown(packStack);
        if (cooldown > 0) {
            setHealCooldown(packStack, cooldown - 1);
            return;
        }

        if (player.getHealth() <= Config.packHealThreshold) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack slotStack = player.getInventory().getItem(i);
                if (slotStack.is(ModItems.BIOCHEMICAL_SOLUTION.get())) {
                    slotStack.shrink(1);
                    player.heal(4.0F);
                    player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 200, 0, true, false, true));
                    setHealCooldown(packStack, Config.packHealCooldown);
                    break;
                }
            }
        }
    }

    private static int getHealCooldown(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null ? tag.getInt(TAG_HEAL_COOLDOWN) : 0;
    }

    private static void setHealCooldown(ItemStack stack, int cooldown) {
        stack.getOrCreateTag().putInt(TAG_HEAL_COOLDOWN, cooldown);
    }
}

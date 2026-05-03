package com.TKCCOPL.effect;

import com.TKCCOPL.init.ModEffects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

public class MetabolicBoostEffect extends MobEffect {
    public MetabolicBoostEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFF6644);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 10 == 0; // Every 0.5 seconds
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // Heal 1 HP per tick cycle
        entity.heal(1.0F + amplifier * 0.5F);
        // Refresh haste
        entity.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 30, amplifier, true, false, true));
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, net.minecraft.world.entity.ai.attributes.AttributeMap attributeMap, int amplifier) {
        super.removeAttributeModifiers(entity, attributeMap, amplifier);
        if (!entity.level().isClientSide) {
            // 延迟到当前 tick 结束后施加 NeuralOverload，避免 curePotionEffects 遍历时 ConcurrentModificationException
            entity.level().getServer().tell(new net.minecraft.server.TickTask(
                entity.level().getServer().getTickCount() + 1,
                () -> entity.addEffect(new MobEffectInstance(ModEffects.NEURAL_OVERLOAD.get(), 20 * (12 + amplifier * 4), amplifier))
            ));
        }
    }
}

package com.TKCCOPL.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * S-01 突触超频血清的神经过载副作用：凋零 + 饥饿。
 *
 * <p>独立效果，避免旧 SOURCE_MAP 在多人服务器上的来源丢失/串线问题。
 */
public class NeuralOverloadEffectS01 extends MobEffect {
    public NeuralOverloadEffectS01() {
        super(MobEffectCategory.HARMFUL, 0xD94848);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        entity.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 1 + Math.min(amplifier, 2), true, false, false));
        entity.addEffect(new MobEffectInstance(MobEffects.HUNGER, 40, Math.min(amplifier, 3), true, false, false));
    }
}

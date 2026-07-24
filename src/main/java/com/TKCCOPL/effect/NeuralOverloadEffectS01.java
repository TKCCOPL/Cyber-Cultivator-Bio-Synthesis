package com.TKCCOPL.effect;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * S-01 突触超频血清的神经过载副作用：虚弱 + 挖掘疲劳，高等级追加凋零。
 *
 * <p>独立效果，避免旧 SOURCE_MAP 在多人服务器上的来源丢失/串线问题。
 */
public class NeuralOverloadEffectS01 extends NonCurableNeuralOverloadEffect {
    public NeuralOverloadEffectS01() {
        super(0xD94848);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        int fatigueAmplifier = Math.min(amplifier / 2, 3);
        entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 21, fatigueAmplifier, true, false, false));
        entity.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 21, fatigueAmplifier, true, false, false));
        if (amplifier >= 5) {
            entity.addEffect(new MobEffectInstance(MobEffects.WITHER, 21, 0, true, false, false));
        }
    }
}

package com.TKCCOPL.effect;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * S-03 代谢加速血清的神经过载副作用：缓慢 + 饥饿，高等级追加中毒。
 *
 * <p>独立效果，避免旧 SOURCE_MAP 在多人服务器上的来源丢失/串线问题。
 */
public class NeuralOverloadEffectS03 extends NonCurableNeuralOverloadEffect {
    public NeuralOverloadEffectS03() {
        super(0xD94848);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        int crashAmplifier = Math.min(amplifier / 2, 3);
        entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 21, crashAmplifier, true, false, false));
        entity.addEffect(new MobEffectInstance(MobEffects.HUNGER, 21, crashAmplifier, true, false, false));
        if (amplifier >= 4) {
            int poisonAmplifier = amplifier >= 7 ? 1 : 0;
            entity.addEffect(new MobEffectInstance(MobEffects.POISON, 21, poisonAmplifier, true, false, false));
        }
    }
}

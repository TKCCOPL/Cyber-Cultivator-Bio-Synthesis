package com.TKCCOPL.effect;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * 旧版神经过载效果（保留注册以兼容旧存档）。
 *
 * <p>v1.1.7 之前使用 SOURCE_MAP 按 S-01/S-02/S-03 来源分发不同副作用，存在
 * 多人服务器上来源丢失和串线的问题。新存档使用三个独立子类
 * {@link NeuralOverloadEffectS01}/{@link NeuralOverloadEffectS02}/{@link NeuralOverloadEffectS03}。
 *
 * <p>此旧效果保留默认行为：缓慢 + 饥饿，仅用于已经持有该效果的旧存档玩家。
 */
public class NeuralOverloadEffect extends NonCurableNeuralOverloadEffect {
    public NeuralOverloadEffect() {
        super(0xD94848);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1 + Math.min(amplifier, 3), true, false, false));
        entity.addEffect(new MobEffectInstance(MobEffects.HUNGER, 40, Math.min(amplifier, 3), true, false, false));
    }
}

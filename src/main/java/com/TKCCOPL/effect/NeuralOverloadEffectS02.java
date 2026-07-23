package com.TKCCOPL.effect;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * S-02 视觉强化血清的神经过载副作用：失明，中高等级追加反胃与缓慢。
 *
 * <p>独立效果，避免旧 SOURCE_MAP 在多人服务器上的来源丢失/串线问题。
 */
public class NeuralOverloadEffectS02 extends NonCurableNeuralOverloadEffect {
    public NeuralOverloadEffectS02() {
        super(0xD94848);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        MobEffectInstance overload = entity.getEffect(this);
        int childDuration = overload == null ? 21 : Math.max(21, overload.getDuration() + 1);
        // 子效果直接覆盖神经过载剩余时长，避免 21-tick 失明反复触发原版淡入淡出。
        entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, childDuration, 0, true, false, false));
        if (amplifier >= 3) {
            entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, childDuration, 0, true, false, false));
        }
        if (amplifier >= 6) {
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, childDuration, 0, true, false, false));
        }
    }
}

package com.TKCCOPL.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public class NeuralOverloadEffect extends MobEffect {
    public NeuralOverloadEffect() {
        super(MobEffectCategory.HARMFUL, 0xD94848);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // Apply classic crash-down side effects while overload is active.
        entity.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 40, 1 + amplifier, true, false, true));
        entity.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.HUNGER, 40, 0 + amplifier, true, false, true));
    }
}

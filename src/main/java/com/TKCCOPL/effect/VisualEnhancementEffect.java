package com.TKCCOPL.effect;

import com.TKCCOPL.init.ModEffects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class VisualEnhancementEffect extends MobEffect {

    public VisualEnhancementEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x88FFAA);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 60 == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // 夜视
        entity.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 100, 0, true, false, true));

        // 抗火（上限 III = amplifier 2）
        int fireResAmp = Math.min(amplifier, 2);
        entity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 100, fireResAmp, true, false, true));

        // 发光范围随 amplifier 增长：16 + amp * 8
        if (!entity.level().isClientSide) {
            double scanRange = 16.0 + amplifier * 8.0;
            AABB area = entity.getBoundingBox().inflate(scanRange);
            List<LivingEntity> nearby = entity.level().getEntitiesOfClass(LivingEntity.class, area, e -> e != entity);
            for (LivingEntity target : nearby) {
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0, true, false, true));
            }
        }
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
        super.removeAttributeModifiers(entity, attributeMap, amplifier);
        if (!entity.level().isClientSide) {
            // 只在效果自然过期时施加副作用，叠加替换时跳过
            if (entity.getEffect(this) == null) {
                entity.level().getServer().tell(new net.minecraft.server.TickTask(
                    entity.level().getServer().getTickCount() + 1,
                    () -> {
                        // 设置来源为 S-02，amplifier 保持实际效果等级
                        NeuralOverloadEffect.setSource(entity, 2);
                        entity.addEffect(new MobEffectInstance(ModEffects.NEURAL_OVERLOAD.get(),
                                20 * (12 + amplifier * 4), amplifier));
                    }
                ));
            }
        }
    }
}

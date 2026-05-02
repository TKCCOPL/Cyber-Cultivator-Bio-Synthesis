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
    private static final double SCAN_RANGE = 32.0;

    public VisualEnhancementEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x88FFAA);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 60 == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        entity.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 100, 0, true, false, true));
        if (!entity.level().isClientSide) {
            AABB area = entity.getBoundingBox().inflate(SCAN_RANGE);
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
            entity.addEffect(new MobEffectInstance(ModEffects.NEURAL_OVERLOAD.get(), 20 * (15 + amplifier * 5), amplifier));
        }
    }
}

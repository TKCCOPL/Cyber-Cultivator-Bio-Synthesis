package com.TKCCOPL.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

public class SynapticOverclockEffect extends MobEffect {
    private static final UUID ATTACK_SPEED_UUID = UUID.fromString("5b7d8f1d-24fc-4f4e-8f7a-14a1ec752c9e");
    private static final UUID KNOCKBACK_RESISTANCE_UUID = UUID.fromString("c2c72513-c92c-49d9-b981-651a05737a2e");

    public SynapticOverclockEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x44F7FF);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // 每秒刷新一次力量效果；动态属性由 addAttributeModifiers 立即应用。
        return duration % 20 == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        entity.addEffect(new MobEffectInstance(
                MobEffects.DAMAGE_BOOST, 21, amplifier, true, false, true));
    }

    @Override
    public void addAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
        super.addAttributeModifiers(entity, attributeMap, amplifier);

        // 动态攻速：10%~45%，完整保留 I~VIII 的等级收益。
        double attackSpeed = 0.10 + amplifier * 0.05;
        AttributeInstance attackAttr = attributeMap.getInstance(Attributes.ATTACK_SPEED);
        if (attackAttr != null) {
            attackAttr.removeModifier(ATTACK_SPEED_UUID);
            attackAttr.addTransientModifier(new AttributeModifier(
                    ATTACK_SPEED_UUID, "synaptic_attack_speed", attackSpeed,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
        }

        // 击退抗性：10%~45%，使用 ADDITION 避免基础值为 0 时乘法无效。
        double knockbackResistance = 0.10 + amplifier * 0.05;
        AttributeInstance knockbackAttr = attributeMap.getInstance(Attributes.KNOCKBACK_RESISTANCE);
        if (knockbackAttr != null) {
            knockbackAttr.removeModifier(KNOCKBACK_RESISTANCE_UUID);
            knockbackAttr.addTransientModifier(new AttributeModifier(
                    KNOCKBACK_RESISTANCE_UUID, "synaptic_knockback_resistance", knockbackResistance,
                    AttributeModifier.Operation.ADDITION));
        }
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
        super.removeAttributeModifiers(entity, attributeMap, amplifier);

        AttributeInstance attackAttr = attributeMap.getInstance(Attributes.ATTACK_SPEED);
        if (attackAttr != null) {
            attackAttr.removeModifier(ATTACK_SPEED_UUID);
        }
        AttributeInstance knockbackAttr = attributeMap.getInstance(Attributes.KNOCKBACK_RESISTANCE);
        if (knockbackAttr != null) {
            knockbackAttr.removeModifier(KNOCKBACK_RESISTANCE_UUID);
        }
    }
}

package com.TKCCOPL.effect;

import com.TKCCOPL.init.ModEffects;
import net.minecraft.server.TickTask;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

public class SynapticOverclockEffect extends MobEffect {
    private static final UUID ATTACK_SPEED_UUID = UUID.fromString("5b7d8f1d-24fc-4f4e-8f7a-14a1ec752c9e");

    public SynapticOverclockEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x44F7FF);
        // 不在构造函数中注册固定属性修饰符，改为在 applyEffectTick 中动态计算
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // 每秒刷新一次属性和抗性效果
        return duration % 20 == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // 动态攻速：0.15 + amp * 0.05 (MULTIPLY_TOTAL)
        double attackSpeed = 0.15 + amplifier * 0.05;
        var attackAttr = entity.getAttribute(Attributes.ATTACK_SPEED);
        if (attackAttr != null) {
            attackAttr.removeModifier(ATTACK_SPEED_UUID);
            attackAttr.addTransientModifier(new AttributeModifier(
                    ATTACK_SPEED_UUID, "synaptic_attack_speed", attackSpeed,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
        }

        // 力量效果（上限由 amplifier 决定，0=力量I, 1=力量II, ...）
        entity.addEffect(new MobEffectInstance(
                MobEffects.DAMAGE_BOOST, 30, amplifier, true, false, true));

        // 抗性效果（上限 III = amplifier 2）
        int resistanceAmp = Math.min(amplifier, 2);
        entity.addEffect(new MobEffectInstance(
                MobEffects.DAMAGE_RESISTANCE, 30, resistanceAmp, true, false, true));
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
        super.removeAttributeModifiers(entity, attributeMap, amplifier);

        // 清理攻速 transient modifier
        var attackAttr = attributeMap.getInstance(Attributes.ATTACK_SPEED);
        if (attackAttr != null) {
            attackAttr.removeModifier(ATTACK_SPEED_UUID);
        }

        // 神经过载延迟施加（保留现有逻辑）
        if (!entity.level().isClientSide) {
            // 只在效果自然过期时施加副作用，叠加替换时跳过
            if (entity.getEffect(this) == null) {
                entity.level().getServer().tell(new TickTask(
                        entity.level().getServer().getTickCount() + 1,
                        () -> {
                            // 设置来源为 S-01，amplifier 保持实际效果等级
                            NeuralOverloadEffect.setSource(entity, 1);
                            entity.addEffect(new MobEffectInstance(
                                    ModEffects.NEURAL_OVERLOAD.get(),
                                    20 * (12 + amplifier * 4),
                                    amplifier));
                        }
                ));
            }
        }
    }
}

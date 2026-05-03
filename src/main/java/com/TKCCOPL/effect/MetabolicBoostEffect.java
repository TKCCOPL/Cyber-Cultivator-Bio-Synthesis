package com.TKCCOPL.effect;

import com.TKCCOPL.init.ModEffects;
import net.minecraft.server.TickTask;
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

public class MetabolicBoostEffect extends MobEffect {

    private static final UUID MOVE_SPEED_UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");

    public MetabolicBoostEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFF6644);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 10 == 0; // Every 0.5 seconds
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // Heal: 0.5 + amp * 0.5, every 0.5s
        entity.heal(0.5F + amplifier * 0.5F);

        // Move speed transient modifier: 0.05 + amp * 0.05 (MULTIPLY_TOTAL)
        double moveSpeed = 0.05 + amplifier * 0.05;
        AttributeInstance moveAttr = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (moveAttr != null) {
            moveAttr.removeModifier(MOVE_SPEED_UUID);
            moveAttr.addTransientModifier(new AttributeModifier(
                    MOVE_SPEED_UUID, "metabolic_move_speed", moveSpeed,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
        }

        // Jump boost (cap at amplifier 3 = level IV)
        int jumpAmp = Math.min(amplifier, 3);
        entity.addEffect(new MobEffectInstance(MobEffects.JUMP, 30, jumpAmp, true, false, true));
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
        super.removeAttributeModifiers(entity, attributeMap, amplifier);

        // Clean up move speed transient modifier
        AttributeInstance moveAttr = attributeMap.getInstance(Attributes.MOVEMENT_SPEED);
        if (moveAttr != null) {
            moveAttr.removeModifier(MOVE_SPEED_UUID);
        }

        // NeuralOverload delay application (preserve existing logic)
        if (!entity.level().isClientSide) {
            if (entity.getEffect(this) == null) {
                entity.level().getServer().tell(new TickTask(
                        entity.level().getServer().getTickCount() + 1,
                        () -> {
                            // 设置来源为 S-03，amplifier 保持实际效果等级
                            NeuralOverloadEffect.setSource(entity, 3);
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

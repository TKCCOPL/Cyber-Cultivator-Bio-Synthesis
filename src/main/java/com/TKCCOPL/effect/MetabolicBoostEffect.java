package com.TKCCOPL.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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
        return duration % 20 == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide) {
            float healAmount = 1.0F + amplifier * 0.5F;
            if (entity instanceof Player player) {
                // 代谢治疗需要可用食物；低饥饿时仍保留机动增益，但暂停治疗与额外消耗。
                if (player.getFoodData().getFoodLevel() > 6) {
                    player.heal(healAmount);
                    player.causeFoodExhaustion(0.15F + amplifier * 0.05F);
                }
            } else {
                entity.heal(healAmount);
            }
        }

        // 跳跃提升上限 IV，避免高等级跳跃失控；治疗与移速仍完整覆盖 I~VIII。
        int jumpAmp = Math.min(amplifier, 3);
        entity.addEffect(new MobEffectInstance(MobEffects.JUMP, 21, jumpAmp, true, false, true));
    }

    @Override
    public void addAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
        super.addAttributeModifiers(entity, attributeMap, amplifier);

        // Move speed transient modifier: 0.05 + amp * 0.05 (MULTIPLY_TOTAL)
        double moveSpeed = 0.05 + amplifier * 0.05;
        AttributeInstance moveAttr = attributeMap.getInstance(Attributes.MOVEMENT_SPEED);
        if (moveAttr != null) {
            moveAttr.removeModifier(MOVE_SPEED_UUID);
            moveAttr.addTransientModifier(new AttributeModifier(
                    MOVE_SPEED_UUID, "metabolic_move_speed", moveSpeed,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
        super.removeAttributeModifiers(entity, attributeMap, amplifier);

        // Clean up move speed transient modifier
        AttributeInstance moveAttr = attributeMap.getInstance(Attributes.MOVEMENT_SPEED);
        if (moveAttr != null) {
            moveAttr.removeModifier(MOVE_SPEED_UUID);
        }

    }
}

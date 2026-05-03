package com.TKCCOPL.effect;

import com.TKCCOPL.init.ModEffects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

public class SynapticOverclockEffect extends MobEffect {
    private static final UUID ATTACK_SPEED_UUID = UUID.fromString("5b7d8f1d-24fc-4f4e-8f7a-14a1ec752c9e");
    private static final UUID MOVE_SPEED_UUID = UUID.fromString("b273ca3f-f6cc-4e49-a405-a56f5a9f90d8");

    public SynapticOverclockEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x44F7FF);
        addAttributeModifier(Attributes.ATTACK_SPEED, ATTACK_SPEED_UUID.toString(), 0.25D, AttributeModifier.Operation.MULTIPLY_TOTAL);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, MOVE_SPEED_UUID.toString(), 0.15D, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // Keep haste refreshed while this custom effect is active.
        entity.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.DIG_SPEED, 40, 1 + amplifier, true, false, true));
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, net.minecraft.world.entity.ai.attributes.AttributeMap attributeMap, int amplifier) {
        super.removeAttributeModifiers(entity, attributeMap, amplifier);
        if (!entity.level().isClientSide) {
            // 延迟到当前 tick 结束后施加 NeuralOverload，避免 curePotionEffects 遍历时 ConcurrentModificationException
            entity.level().getServer().tell(new net.minecraft.server.TickTask(
                entity.level().getServer().getTickCount() + 1,
                () -> entity.addEffect(new MobEffectInstance(ModEffects.NEURAL_OVERLOAD.get(), 20 * (12 + amplifier * 4), amplifier))
            ));
        }
    }
}

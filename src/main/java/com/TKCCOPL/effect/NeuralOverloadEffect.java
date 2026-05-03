package com.TKCCOPL.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NeuralOverloadEffect extends MobEffect {
    /**
     * 来源映射：entity UUID → source ID
     * 1 = S-01, 2 = S-02, 3 = S-03, 0 = 默认
     * 使用 ConcurrentHashMap 保证多线程安全。
     */
    private static final Map<UUID, Integer> SOURCE_MAP = new ConcurrentHashMap<>();

    public NeuralOverloadEffect() {
        super(MobEffectCategory.HARMFUL, 0xD94848);
    }

    /**
     * 设置来源信息。在施加 NeuralOverload 前调用。
     * @param entity 目标实体
     * @param sourceId 来源 ID：1=S-01, 2=S-02, 3=S-03, 0=默认
     */
    public static void setSource(LivingEntity entity, int sourceId) {
        SOURCE_MAP.put(entity.getUUID(), sourceId);
    }

    /**
     * 获取来源信息（只读，不删除）。效果持续期间每次 tick 都可读取。
     */
    private static int getSource(LivingEntity entity) {
        Integer source = SOURCE_MAP.get(entity.getUUID());
        return source != null ? source : 0;
    }

    /**
     * 清理来源信息。在效果自然结束时调用，防止内存泄漏。
     */
    public static void clearSource(LivingEntity entity) {
        SOURCE_MAP.remove(entity.getUUID());
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        int source = getSource(entity);

        // 子效果不可见（visible=false, showIcon=false），避免与神经过载本体在效果面板重叠
        switch (source) {
            case 1 -> {
                // S-01: 凋零 + 饥饿
                entity.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 1 + Math.min(amplifier, 2), true, false, false));
                entity.addEffect(new MobEffectInstance(MobEffects.HUNGER, 40, amplifier, true, false, false));
            }
            case 2 -> {
                // S-02: 失明 + 饥饿
                entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 1 + Math.min(amplifier, 2), true, false, false));
                entity.addEffect(new MobEffectInstance(MobEffects.HUNGER, 40, amplifier, true, false, false));
            }
            case 3 -> {
                // S-03: 缓慢 + 中毒
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1 + amplifier, true, false, false));
                entity.addEffect(new MobEffectInstance(MobEffects.POISON, 40, Math.min(amplifier, 2), true, false, false));
            }
            default -> {
                // 默认：缓慢 + 饥饿（兼容旧数据 / 未知来源）
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1 + amplifier, true, false, false));
                entity.addEffect(new MobEffectInstance(MobEffects.HUNGER, 40, amplifier, true, false, false));
            }
        }
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
        super.removeAttributeModifiers(entity, attributeMap, amplifier);
        // 效果自然结束时清理来源映射，防止内存泄漏
        clearSource(entity);
    }
}

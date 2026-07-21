package com.TKCCOPL.effect;

import com.TKCCOPL.Config;
import com.TKCCOPL.init.ModEffects;
import com.TKCCOPL.network.ModNetwork;
import com.TKCCOPL.network.S02DetectionSyncPacket;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.PacketDistributor;

import java.util.Comparator;
import java.util.List;

public class VisualEnhancementEffect extends MobEffect {

    /** 单次同步最多携带的目标实体数。客户端每帧渲染 256 个轮廓已足够明显。 */
    private static final int MAX_TARGETS_PER_SYNC = 256;

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

        // 抗火（上限 IV = amplifier 3）
        int fireResAmp = Math.min(amplifier, 3);
        entity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 100, fireResAmp, true, false, true));

        // 私有轮廓：服务端每 60 tick 扫描附近 LivingEntity，只向饮用者发送目标列表
        if (!entity.level().isClientSide && entity instanceof ServerPlayer serverPlayer) {
            double scanRange = Math.min(16.0 + amplifier * 8.0, (double) Config.glowScanRangeCap);
            AABB area = entity.getBoundingBox().inflate(scanRange);
            List<LivingEntity> nearby = entity.level().getEntitiesOfClass(LivingEntity.class, area,
                    e -> e != entity && e.isAlive() && !e.isRemoved());

            // 按距离升序排序，保留最近的 MAX_TARGETS_PER_SYNC 个目标
            nearby.sort(Comparator.comparingDouble(e -> e.distanceToSqr(entity)));
            int count = Math.min(MAX_TARGETS_PER_SYNC, nearby.size());
            int[] entityIds = new int[count];
            for (int i = 0; i < count; i++) {
                entityIds[i] = nearby.get(i).getId();
            }

            S02DetectionSyncPacket packet = new S02DetectionSyncPacket(entityIds);
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), packet);
        }
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
        super.removeAttributeModifiers(entity, attributeMap, amplifier);
        if (!entity.level().isClientSide) {
            // 效果结束时清除客户端残留的轮廓目标，避免饮用者退出 S-02 后仍看到旧目标
            if (entity instanceof ServerPlayer serverPlayer) {
                S02DetectionSyncPacket packet = new S02DetectionSyncPacket(new int[0]);
                ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), packet);
            }
            // 只在效果自然过期时施加副作用，叠加替换时跳过
            if (entity.getEffect(this) == null) {
                entity.level().getServer().tell(new TickTask(
                    entity.level().getServer().getTickCount() + 1,
                    () -> {
                        if (entity.isRemoved() || !entity.isAlive()) return;
                        // 直接使用 S-02 独立效果，避免旧 SOURCE_MAP 串线
                        entity.addEffect(new MobEffectInstance(ModEffects.NEURAL_OVERLOAD_S02.get(),
                                20 * (12 + amplifier * 4), amplifier));
                    }
                ));
            }
        }
    }
}

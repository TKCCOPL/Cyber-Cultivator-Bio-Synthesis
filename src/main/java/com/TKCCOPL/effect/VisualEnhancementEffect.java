package com.TKCCOPL.effect;

import com.TKCCOPL.Config;
import com.TKCCOPL.network.ModNetwork;
import com.TKCCOPL.network.S02DetectionSyncPacket;
import net.minecraft.server.MinecraftServer;
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
    /** 原版夜视在剩余 200 tick 内开始闪烁；刷新值必须始终高于这个区间。 */
    private static final int NIGHT_VISION_FLICKER_THRESHOLD = 200;
    private static final int NIGHT_VISION_REFRESH_MARGIN = 5;

    public VisualEnhancementEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x88FFAA);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % getScanInterval(amplifier) == 0;
    }

    /** I~VIII: 60, 56, 52, 48, 44, 40, 36, 32 tick. */
    public static int getScanInterval(int amplifier) {
        return Math.max(30, 60 - Math.max(0, amplifier) * 4);
    }

    /** I~VIII: 16, 23, 30, 37, 44, 51, 58, 64 blocks before the configured cap. */
    public static double getScanRange(int amplifier, int configuredCap) {
        return Math.min(16.0 + Math.max(0, amplifier) * 7.0, (double) configuredCap);
    }

    /** 刚好覆盖一次扫描间隔，并始终保留超过原版闪烁阈值的安全余量。 */
    public static int getNightVisionRefreshDuration(int amplifier) {
        return getScanInterval(amplifier) + NIGHT_VISION_FLICKER_THRESHOLD + NIGHT_VISION_REFRESH_MARGIN;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        refreshNightVision(entity, amplifier);

        // 私有轮廓：随 amplifier 提升刷新速度，只向饮用者发送目标列表。
        if (!entity.level().isClientSide && entity instanceof ServerPlayer serverPlayer) {
            double scanRange = getScanRange(amplifier, Config.glowScanRangeCap);
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
    public void addAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
        super.addAttributeModifiers(entity, attributeMap, amplifier);
        // 效果刚加入时立即获得夜视，不必等待第一次扫描取模命中。
        refreshNightVision(entity, amplifier);
    }

    private static void refreshNightVision(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) return;
        entity.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,
                getNightVisionRefreshDuration(amplifier), 0, true, false, true));
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, AttributeMap attributeMap, int amplifier) {
        super.removeAttributeModifiers(entity, attributeMap, amplifier);
        if (!entity.level().isClientSide) {
            // amplifier/时长更新也会调用本方法；延迟确认主效果确实已经离开效果表。
            MinecraftServer server = entity.level().getServer();
            if (server != null) {
                server.tell(new TickTask(server.getTickCount() + 1, () -> {
                    if (entity.getEffect(this) != null) return;
                    MobEffectInstance nightVision = entity.getEffect(MobEffects.NIGHT_VISION);
                    int ownedDurationCap = getNightVisionRefreshDuration(amplifier);
                    if (nightVision != null
                            && nightVision.getAmplifier() == 0
                            && nightVision.isAmbient()
                            && !nightVision.isVisible()
                            && nightVision.showIcon()
                            && nightVision.getDuration() <= ownedDurationCap) {
                        entity.removeEffect(MobEffects.NIGHT_VISION);
                    }
                    if (!(entity instanceof ServerPlayer serverPlayer)) return;
                    ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                            new S02DetectionSyncPacket(new int[0]));
                }));
            }
        }
    }
}

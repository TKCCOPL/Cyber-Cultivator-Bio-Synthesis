package com.TKCCOPL.client.s02;

import com.mojang.blaze3d.vertex.PoseStack;
import com.TKCCOPL.cybercultivator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端缓存的 S-02 侦测目标实体 ID 集合。
 * <p>
 * 由 {@link com.TKCCOPL.network.S02DetectionSyncPacket} 更新。当玩家断线、
 * 换维度或服务端清空目标时，集合会被替换为空集合。
 * <p>
 * 渲染逻辑在 {@link RenderLevelStageEvent#AFTER_TRANSLUCENT_BLOCKS} 阶段绘制轮廓：
 * 重新调用 {@code EntityRenderDispatcher.render} 并把目标输出到
 * {@link OutlineBufferSource}。这样绘制效果与原版发光一致，但只对本客户端可见，
 * 不会通过 {@code MobEffects.GLOWING} 同步给其他玩家。
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = cybercultivator.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class S02DetectionClientState {
    /** 当前已同步的目标实体 ID 集合。使用 ConcurrentHashMap 以便渲染线程与网络线程安全访问。 */
    private static final Set<Integer> TARGETS = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private S02DetectionClientState() {
    }

    public static void setTargets(int[] entityIds) {
        TARGETS.clear();
        if (entityIds == null) return;
        for (int id : entityIds) {
            TARGETS.add(id);
        }
    }

    public static void clear() {
        TARGETS.clear();
    }

    public static boolean isEmpty() {
        return TARGETS.isEmpty();
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (TARGETS.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        OutlineBufferSource outlineBuffer = mc.renderBuffers().outlineBufferSource();
        if (outlineBuffer == null) return;

        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        float partialTick = event.getPartialTick();

        // 渲染同时清理无效 ID（已离开当前 level 或已死亡）。ConcurrentHashMap 允许迭代时删除。
        TARGETS.removeIf(id -> {
            Entity entity = mc.level.getEntity(id);
            if (entity == null || !entity.isAlive() || entity.isRemoved()) {
                return true;
            }
            // 排除饮用者本人 — 服务端不会发送自己 ID，但作为防御性兜底
            if (entity == mc.player) return true;

            // S-02 主题色 #88FFAA
            outlineBuffer.setColor(0x88, 0xFF, 0xAA, 0xFF);
            try {
                mc.getEntityRenderDispatcher().render(
                        entity,
                        entity.getX() - camera.x,
                        entity.getY() - camera.y,
                        entity.getZ() - camera.z,
                        entity.getYRot(),
                        partialTick,
                        poseStack,
                        outlineBuffer,
                        LightTexture.FULL_BRIGHT
                );
            } catch (Exception ignored) {
                // 单帧渲染失败不应中断整批轮廓绘制
            }
            return false;
        });

        outlineBuffer.endOutlineBatch();
    }
}

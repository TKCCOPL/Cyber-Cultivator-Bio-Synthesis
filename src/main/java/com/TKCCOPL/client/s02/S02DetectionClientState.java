package com.TKCCOPL.client.s02;

import com.TKCCOPL.cybercultivator;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
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
 * {@code MinecraftMixin} 只在饮用者客户端把集合中的实体加入原版 glowing 判断，
 * 不修改实体状态，也不重复渲染模型。服务端只同步实体 ID，不施加
 * {@code MobEffects.GLOWING}，因此轮廓仍然是 S-02 私有的。
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
        if (entityIds != null) {
            for (int id : entityIds) {
                TARGETS.add(id);
            }
        }
    }

    public static void clear() {
        TARGETS.clear();
    }

    public static boolean isEmpty() {
        return TARGETS.isEmpty();
    }

    /**
     * 返回实体是否应仅在当前饮用者客户端进入原版发光轮廓管线。
     */
    public static boolean shouldAppearGlowing(Entity entity) {
        return entity != null && TARGETS.contains(entity.getId());
    }

    @SubscribeEvent
    public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        clear();
        com.TKCCOPL.client.ClientGameplayConfig.reset();
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
        com.TKCCOPL.client.ClientGameplayConfig.reset();
    }
}

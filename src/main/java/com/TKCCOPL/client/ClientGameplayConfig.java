package com.TKCCOPL.client;

import com.TKCCOPL.network.GameplayConfigSnapshot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 客户端缓存的服务端玩法配置快照。
 * <p>
 * 由 {@link com.TKCCOPL.network.GameplayConfigSyncPacket} 在玩家登录、换维度
 * 或服务端配置重载时更新。Tooltip 与 JEI 应从此处读取显示值，
 * 服务端业务逻辑始终读取 {@link com.TKCCOPL.Config} 上的 volatile 字段。
 * <p>
 * 在第一次收到服务端快照之前使用与 {@link com.TKCCOPL.Config} 默认值一致的回退值，
 * 使单人开发会话仍能正确显示。
 */
@OnlyIn(Dist.CLIENT)
public final class ClientGameplayConfig {
    private static volatile GameplayConfigSnapshot snapshot = GameplayConfigSnapshot.empty();

    private ClientGameplayConfig() {
    }

    public static void setSnapshot(GameplayConfigSnapshot newSnapshot) {
        if (newSnapshot != null) {
            snapshot = newSnapshot;
        }
    }

    public static GameplayConfigSnapshot getSnapshot() {
        return snapshot;
    }
}

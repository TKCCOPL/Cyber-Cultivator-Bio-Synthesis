package com.TKCCOPL.network;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

/**
 * 推送玩法配置快照到客户端的统一入口。
 *
 * <p>触发时机：
 * <ul>
 *   <li>玩家登录：仅向该玩家发送</li>
 *   <li>玩家换维度：仅向该玩家发送（不同维度可能存在 Config 加载差异）</li>
 *   <li>配置重载：广播给所有在线玩家</li>
 * </ul>
 */
public final class GameplayConfigSync {
    private GameplayConfigSync() {
    }

    public static void sendTo(ServerPlayer player) {
        GameplayConfigSnapshot snapshot = GameplayConfigSnapshot.fromServerConfig();
        GameplayConfigSyncPacket packet = new GameplayConfigSyncPacket(snapshot);
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void broadcastToAll(MinecraftServer server) {
        if (server == null) return;
        GameplayConfigSnapshot snapshot = GameplayConfigSnapshot.fromServerConfig();
        GameplayConfigSyncPacket packet = new GameplayConfigSyncPacket(snapshot);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
        }
    }
}

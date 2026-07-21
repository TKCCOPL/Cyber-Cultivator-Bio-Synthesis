package com.TKCCOPL.network;

import com.TKCCOPL.cybercultivator;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * 模组网络通道。所有客户端 ↔ 服务端数据包都在此通道上注册。
 * <p>
 * 协议版本号：当前为 "1"。客户端与服务端协议版本不一致时连接会被拒绝，
 * 因为这些数据包影响玩家可见的玩法表现（S-02 侦测、配置同步等）。
 */
public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(cybercultivator.MODID, "main"),
            () -> PROTOCOL_VERSION,
            // 客户端连服务端时校验：服务端协议版本必须匹配（或服务端未安装本模组）
            remoteVersion -> remoteVersion.equals(PROTOCOL_VERSION) || !remoteVersion.equals("MISSING"),
            // 服务端连客户端时校验：客户端协议版本必须匹配（或客户端未安装本模组）
            localVersion -> localVersion.equals(PROTOCOL_VERSION) || !localVersion.equals("MISSING")
    );

    private static int nextId = 0;

    private ModNetwork() {
    }

    public static void register() {
        int id = nextId++;
        CHANNEL.registerMessage(id,
                GameplayConfigSyncPacket.class,
                GameplayConfigSyncPacket::encode,
                GameplayConfigSyncPacket::decode,
                GameplayConfigSyncPacket::handle);
        // S-02 侦测同步包将在后续提交中注册
    }
}

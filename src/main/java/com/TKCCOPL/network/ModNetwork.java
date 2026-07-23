package com.TKCCOPL.network;

import com.TKCCOPL.cybercultivator;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * 模组网络通道。所有客户端 ↔ 服务端数据包都在此通道上注册。
 * <p>
 * 协议版本号：当前为 "3"。客户端与服务端协议版本不一致时连接会被拒绝，
 * 因为这些数据包影响玩家可见的玩法表现（S-02 侦测、配置同步等）。
 */
public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "3";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(cybercultivator.MODID, "main"),
            () -> PROTOCOL_VERSION,
            // 配置快照与 S-02 目标包都依赖固定字段布局；版本不一致必须拒绝连接。
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
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
        id = nextId++;
        CHANNEL.registerMessage(id,
                S02DetectionSyncPacket.class,
                S02DetectionSyncPacket::encode,
                S02DetectionSyncPacket::decode,
                S02DetectionSyncPacket::handle);
    }
}

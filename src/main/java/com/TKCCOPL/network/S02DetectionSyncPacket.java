package com.TKCCOPL.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 服务端 → 客户端同步 S-02 侦测目标的实体 ID 列表。
 * <p>
 * 服务端按效果等级每 60～32 tick 扫描附近 LivingEntity，
 * 排除饮用者本人、死亡/已移除实体，按距离排序最多 256 个，
 * 通过此包只发给饮用者。饮用者客户端据此扩展原版的 glowing 渲染判断，
 * 不修改实体状态；服务端不使用 {@code MobEffects.GLOWING}，其他玩家因此
 * 不会看到 S-02 轮廓。
 * <p>
 * 当效果结束、换维度或玩家断线时，服务端发送空列表让客户端清理。
 */
public class S02DetectionSyncPacket {
    /** 目标实体 ID 列表。饮用者本人 ID 不包含在此列表中。 */
    private final int[] entityIds;

    public S02DetectionSyncPacket(int[] entityIds) {
        this.entityIds = entityIds == null ? new int[0] : entityIds.clone();
    }

    public int[] getEntityIds() {
        return entityIds.clone();
    }

    public static void encode(S02DetectionSyncPacket pkt, FriendlyByteBuf buf) {
        buf.writeVarIntArray(pkt.entityIds);
    }

    public static S02DetectionSyncPacket decode(FriendlyByteBuf buf) {
        return new S02DetectionSyncPacket(buf.readVarIntArray());
    }

    public static void handle(S02DetectionSyncPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // DistExecutor 避免在专用服务端加载 client.s02.S02DetectionClientState
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> com.TKCCOPL.client.s02.S02DetectionClientState.setTargets(pkt.entityIds));
        });
        ctx.get().setPacketHandled(true);
    }
}

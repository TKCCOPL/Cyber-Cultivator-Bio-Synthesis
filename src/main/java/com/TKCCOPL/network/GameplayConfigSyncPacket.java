package com.TKCCOPL.network;

import com.TKCCOPL.client.ClientGameplayConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * 服务端 → 客户端推送 {@link GameplayConfigSnapshot}。
 * 客户端收到后更新 {@link ClientGameplayConfig}，供 Tooltip / JEI 读取。
 */
public class GameplayConfigSyncPacket {
    private final GameplayConfigSnapshot snapshot;

    public GameplayConfigSyncPacket(GameplayConfigSnapshot snapshot) {
        this.snapshot = Objects.requireNonNull(snapshot);
    }

    public GameplayConfigSnapshot getSnapshot() {
        return snapshot;
    }

    public static void encode(GameplayConfigSyncPacket pkt, FriendlyByteBuf buf) {
        GameplayConfigSnapshot s = pkt.snapshot;
        // genes
        buf.writeVarInt(s.mutationRange());
        buf.writeDouble(s.mutationChanceBase());
        buf.writeDouble(s.mutationChancePerGen());
        buf.writeDouble(s.mutationChancePerGeneDiff());
        buf.writeVarInt(s.mutationGenerationCap());
        buf.writeDouble(s.mutationChanceCap());
        buf.writeDouble(s.twinChanceBase());
        buf.writeDouble(s.twinChancePerGen());
        buf.writeDouble(s.twinChanceCap());
        buf.writeVarInt(s.geneMin());
        buf.writeVarInt(s.geneMax());
        // serum
        buf.writeVarInt(s.s01BaseDuration());
        buf.writeVarInt(s.s02BaseDuration());
        buf.writeVarInt(s.s03BaseDuration());
        buf.writeVarInt(s.stackAmplifierCap());
        buf.writeVarInt(s.stackDurationCap());
        buf.writeVarInt(s.s01StackDurationCap());
        buf.writeVarInt(s.s02StackDurationCap());
        buf.writeVarInt(s.s03StackDurationCap());
        buf.writeVarInt(s.activityThresholdForBonus());
        buf.writeDouble(s.durationMultiplierBase());
        buf.writeDouble(s.durationMultiplierPerActivity());
        buf.writeVarInt(s.glowScanRangeCap());
        // incubator
        buf.writeVarInt(s.maturationThreshold());
        buf.writeVarInt(s.resourceThreshold());
        buf.writeVarInt(s.nutritionDecayInterval());
        buf.writeVarInt(s.purityDecayInterval());
        buf.writeVarInt(s.dataSignalDecayInterval());
        buf.writeVarInt(s.nutritionInjectAmount());
        buf.writeVarInt(s.purityInjectAmount());
        buf.writeVarInt(s.dataSignalInjectAmount());
        buf.writeVarInt(s.matureNutritionCost());
        buf.writeVarInt(s.maturePurityCost());
        // curios
        buf.writeVarInt(s.beltScanRange());
        buf.writeVarInt(s.beltNutritionThreshold());
        buf.writeVarInt(s.beltPurityThreshold());
        buf.writeVarInt(s.beltDataSignalThreshold());
        buf.writeVarInt(s.packEffectReductionRate());
        buf.writeFloat(s.packHealThreshold());
        buf.writeVarInt(s.packHealCooldown());
    }

    public static GameplayConfigSyncPacket decode(FriendlyByteBuf buf) {
        return new GameplayConfigSyncPacket(new GameplayConfigSnapshot(
                // genes
                buf.readVarInt(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readVarInt(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readVarInt(),
                buf.readVarInt(),
                // serum
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readVarInt(),
                // incubator
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                // curios
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readFloat(),
                buf.readVarInt()
        ));
    }

    public static void handle(GameplayConfigSyncPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // DistExecutor 避免在专用服务端加载 ClientGameplayConfig 类
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ClientGameplayConfig.setSnapshot(pkt.snapshot));
        });
        ctx.get().setPacketHandled(true);
    }
}

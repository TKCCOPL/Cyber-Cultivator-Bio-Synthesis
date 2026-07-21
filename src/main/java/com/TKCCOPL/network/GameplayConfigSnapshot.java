package com.TKCCOPL.network;

import com.TKCCOPL.Config;

/**
 * 服务端玩法配置的不可变快照。
 * <p>
 * 客户端（Tooltip / JEI）必须从此快照读取显示所需的配置值，
 * 不能直接读取 {@link Config} 上的 volatile 字段——客户端的 Config 实例
 * 不一定是服务端权威值。服务端业务逻辑始终读取 {@link Config}。
 * <p>
 * 服务端在玩家登录、换维度或配置重载时通过 {@link GameplayConfigSyncPacket}
 * 将最新快照推送到对应客户端。
 */
public record GameplayConfigSnapshot(
        // genes
        int mutationRange,
        double mutationChanceBase,
        double mutationChancePerGen,
        double mutationChancePerGeneDiff,
        int geneMin,
        int geneMax,
        // serum
        int s01BaseDuration,
        int s02BaseDuration,
        int s03BaseDuration,
        int stackAmplifierCap,
        int stackDurationCap,
        int activityThresholdForBonus,
        double durationMultiplierBase,
        double durationMultiplierPerActivity,
        int glowScanRangeCap,
        // incubator
        int maturationThreshold,
        int resourceThreshold,
        int nutritionDecayInterval,
        int purityDecayInterval,
        int dataSignalDecayInterval,
        int nutritionInjectAmount,
        int purityInjectAmount,
        int dataSignalInjectAmount,
        int matureNutritionCost,
        int maturePurityCost,
        // curios
        int beltScanRange,
        int beltNutritionThreshold,
        int beltPurityThreshold,
        int beltDataSignalThreshold,
        int packEffectReductionRate,
        float packHealThreshold,
        int packHealCooldown
) {
    /**
     * 从当前服务端 {@link Config} 构造快照。
     * 应在 {@code ModConfigEvent.Loading/Reloading} 触发后调用，
     * 否则 Config 上的 volatile 字段尚未从 disk 加载。
     */
    public static GameplayConfigSnapshot fromServerConfig() {
        return new GameplayConfigSnapshot(
                Config.mutationRange,
                Config.mutationChanceBase,
                Config.mutationChancePerGen,
                Config.mutationChancePerGeneDiff,
                Config.geneMin,
                Config.geneMax,
                Config.s01BaseDuration,
                Config.s02BaseDuration,
                Config.s03BaseDuration,
                Config.stackAmplifierCap,
                Config.stackDurationCap,
                Config.activityThresholdForBonus,
                Config.durationMultiplierBase,
                Config.durationMultiplierPerActivity,
                Config.glowScanRangeCap,
                Config.maturationThreshold,
                Config.resourceThreshold,
                Config.nutritionDecayInterval,
                Config.purityDecayInterval,
                Config.dataSignalDecayInterval,
                Config.nutritionInjectAmount,
                Config.purityInjectAmount,
                Config.dataSignalInjectAmount,
                Config.matureNutritionCost,
                Config.maturePurityCost,
                Config.beltScanRange,
                Config.beltNutritionThreshold,
                Config.beltPurityThreshold,
                Config.beltDataSignalThreshold,
                Config.packEffectReductionRate,
                Config.packHealThreshold,
                Config.packHealCooldown
        );
    }

    /**
     * 客户端尚未收到任何快照时使用的默认值，与 {@link Config} 的出厂默认一致。
     * 用于单人开发环境或服务端尚未推送时的回退显示。
     */
    public static GameplayConfigSnapshot empty() {
        return new GameplayConfigSnapshot(
                2, 0.05, 0.02, 0.01, 1, 10,
                500, 600, 300, 7, 6000, 8, 0.5, 0.1, 48,
                200, 10, 20, 40, 60, 25, 20, 15, 5, 5,
                3, 50, 50, 25, 2, 6.0F, 1200
        );
    }
}

package com.TKCCOPL;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = cybercultivator.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // === [genes] 基因参数 ===
    static {
        BUILDER.push("genes");
    }
    private static final ForgeConfigSpec.IntValue MUTATION_RANGE = BUILDER
            .comment("随机变异范围 ±N")
            .defineInRange("mutationRange", 2, 1, 5);
    private static final ForgeConfigSpec.DoubleValue MUTATION_CHANCE_BASE = BUILDER
            .comment("基础突变概率")
            .defineInRange("mutationChanceBase", 0.05, 0.01, 0.20);
    private static final ForgeConfigSpec.DoubleValue MUTATION_CHANCE_PER_GEN = BUILDER
            .comment("每代增加突变概率")
            .defineInRange("mutationChancePerGen", 0.02, 0.0, 0.10);
    private static final ForgeConfigSpec.DoubleValue MUTATION_CHANCE_PER_GENE_DIFF = BUILDER
            .comment("每点基因差异增加概率")
            .defineInRange("mutationChancePerGeneDiff", 0.01, 0.0, 0.05);
    private static final ForgeConfigSpec.IntValue GENE_MIN = BUILDER
            .comment("基因下限")
            .defineInRange("geneMin", 1, 1, 5);
    private static final ForgeConfigSpec.IntValue GENE_MAX = BUILDER
            .comment("基因上限")
            .defineInRange("geneMax", 10, 5, 10);
    static {
        BUILDER.pop();
    }

    // === [serum] 血清参数 ===
    static {
        BUILDER.push("serum");
    }
    private static final ForgeConfigSpec.IntValue S01_BASE_DURATION = BUILDER
            .comment("S-01 基础持续时间 tick (25s = 500)")
            .defineInRange("s01BaseDuration", 500, 100, 12000);
    private static final ForgeConfigSpec.IntValue S02_BASE_DURATION = BUILDER
            .comment("S-02 基础持续时间 tick (30s = 600)")
            .defineInRange("s02BaseDuration", 600, 100, 12000);
    private static final ForgeConfigSpec.IntValue S03_BASE_DURATION = BUILDER
            .comment("S-03 基础持续时间 tick (15s = 300)")
            .defineInRange("s03BaseDuration", 300, 100, 12000);
    private static final ForgeConfigSpec.IntValue STACK_AMPLIFIER_CAP = BUILDER
            .comment("叠加 amplifier 上限")
            .defineInRange("stackAmplifierCap", 7, 1, 10);
    private static final ForgeConfigSpec.IntValue STACK_DURATION_CAP = BUILDER
            .comment("持续时间累加上限 tick (5min = 6000)")
            .defineInRange("stackDurationCap", 6000, 600, 60000);
    private static final ForgeConfigSpec.IntValue ACTIVITY_THRESHOLD_FOR_BONUS = BUILDER
            .comment("高品质起步 amplifier=1 的阈值")
            .defineInRange("activityThresholdForBonus", 8, 1, 10);
    private static final ForgeConfigSpec.DoubleValue DURATION_MULTIPLIER_BASE = BUILDER
            .comment("持续时间倍率基数")
            .defineInRange("durationMultiplierBase", 0.5, 0.1, 2.0);
    private static final ForgeConfigSpec.DoubleValue DURATION_MULTIPLIER_PER_ACTIVITY = BUILDER
            .comment("每点活性增加倍率")
            .defineInRange("durationMultiplierPerActivity", 0.1, 0.01, 0.5);
    private static final ForgeConfigSpec.IntValue GLOW_SCAN_RANGE_CAP = BUILDER
            .comment("S-02 发光扫描范围上限 (格)")
            .defineInRange("glowScanRangeCap", 48, 16, 64);
    static {
        BUILDER.pop();
    }

    // === [incubator] 培养槽参数 ===
    static {
        BUILDER.push("incubator");
    }
    private static final ForgeConfigSpec.IntValue MATURATION_THRESHOLD = BUILDER
            .comment("成熟所需生长进度")
            .defineInRange("maturationThreshold", 200, 50, 1000);
    private static final ForgeConfigSpec.IntValue RESOURCE_THRESHOLD = BUILDER
            .comment("资源消耗阈值")
            .defineInRange("resourceThreshold", 10, 0, 50);
    private static final ForgeConfigSpec.IntValue NUTRITION_DECAY_INTERVAL = BUILDER
            .comment("营养衰减间隔 tick")
            .defineInRange("nutritionDecayInterval", 20, 5, 200);
    private static final ForgeConfigSpec.IntValue PURITY_DECAY_INTERVAL = BUILDER
            .comment("纯净衰减间隔 tick")
            .defineInRange("purityDecayInterval", 40, 5, 200);
    private static final ForgeConfigSpec.IntValue DATA_SIGNAL_DECAY_INTERVAL = BUILDER
            .comment("信号衰减间隔 tick")
            .defineInRange("dataSignalDecayInterval", 60, 5, 200);
    private static final ForgeConfigSpec.IntValue NUTRITION_INJECT_AMOUNT = BUILDER
            .comment("生化原液注入量")
            .defineInRange("nutritionInjectAmount", 25, 1, 100);
    private static final ForgeConfigSpec.IntValue PURITY_INJECT_AMOUNT = BUILDER
            .comment("纯净水注入量")
            .defineInRange("purityInjectAmount", 20, 1, 100);
    private static final ForgeConfigSpec.IntValue DATA_SIGNAL_INJECT_AMOUNT = BUILDER
            .comment("硅碎片注入量")
            .defineInRange("dataSignalInjectAmount", 15, 1, 100);
    private static final ForgeConfigSpec.IntValue MATURE_NUTRITION_COST = BUILDER
            .comment("成熟时营养消耗")
            .defineInRange("matureNutritionCost", 5, 0, 50);
    private static final ForgeConfigSpec.IntValue MATURE_PURITY_COST = BUILDER
            .comment("成熟时纯净消耗")
            .defineInRange("maturePurityCost", 5, 0, 50);
    static {
        BUILDER.pop();
    }

    // === [curios] 饰品参数 ===
    static {
        BUILDER.push("curios");
    }
    private static final ForgeConfigSpec.IntValue BELT_SCAN_RANGE = BUILDER
            .comment("腰带扫描范围 (格，上限 5 以控制方块实体扫描开销)")
            .defineInRange("scanRange", 3, 1, 5);
    private static final ForgeConfigSpec.IntValue BELT_NUTRITION_THRESHOLD = BUILDER
            .comment("营养度注入阈值")
            .defineInRange("nutritionThreshold", 50, 0, 100);
    private static final ForgeConfigSpec.IntValue BELT_PURITY_THRESHOLD = BUILDER
            .comment("纯净度注入阈值")
            .defineInRange("purityThreshold", 50, 0, 100);
    private static final ForgeConfigSpec.IntValue BELT_DATA_SIGNAL_THRESHOLD = BUILDER
            .comment("数据信号注入阈值")
            .defineInRange("dataSignalThreshold", 25, 0, 100);
    private static final ForgeConfigSpec.IntValue PACK_EFFECT_REDUCTION_RATE = BUILDER
            .comment("支持箱副作用消减速率")
            .defineInRange("effectReductionRate", 2, 1, 10);
    private static final ForgeConfigSpec.DoubleValue PACK_HEAL_THRESHOLD = BUILDER
            .comment("支持箱治疗阈值")
            .defineInRange("healThreshold", 6.0, 1.0, 20.0);
    private static final ForgeConfigSpec.IntValue PACK_HEAL_COOLDOWN = BUILDER
            .comment("支持箱治疗冷却 tick (1200 = 60s)")
            .defineInRange("healCooldown", 1200, 200, 6000);
    private static final ForgeConfigSpec.IntValue MONOCLE_HUD_RANGE = BUILDER
            .comment("单片镜 HUD 距离 (格)")
            .defineInRange("hudRange", 8, 3, 16);
    static {
        BUILDER.pop();
    }

    static final ForgeConfigSpec SPEC = BUILDER.build();

    // === Runtime values ===
    // genes
    public static volatile int mutationRange;
    public static volatile double mutationChanceBase;
    public static volatile double mutationChancePerGen;
    public static volatile double mutationChancePerGeneDiff;
    public static volatile int geneMin;
    public static volatile int geneMax;
    // serum
    public static volatile int s01BaseDuration;
    public static volatile int s02BaseDuration;
    public static volatile int s03BaseDuration;
    public static volatile int stackAmplifierCap;
    public static volatile int stackDurationCap;
    public static volatile int activityThresholdForBonus;
    public static volatile double durationMultiplierBase;
    public static volatile double durationMultiplierPerActivity;
    public static volatile int glowScanRangeCap;
    // incubator
    public static volatile int maturationThreshold;
    public static volatile int resourceThreshold;
    public static volatile int nutritionDecayInterval;
    public static volatile int purityDecayInterval;
    public static volatile int dataSignalDecayInterval;
    public static volatile int nutritionInjectAmount;
    public static volatile int purityInjectAmount;
    public static volatile int dataSignalInjectAmount;
    public static volatile int matureNutritionCost;
    public static volatile int maturePurityCost;
    // curios
    public static volatile int beltScanRange;
    public static volatile int beltNutritionThreshold;
    public static volatile int beltPurityThreshold;
    public static volatile int beltDataSignalThreshold;
    public static volatile int packEffectReductionRate;
    public static volatile float packHealThreshold;
    public static volatile int packHealCooldown;
    public static volatile int monocleHudRange;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) return; // 只处理本模组配置
        // genes
        mutationRange = MUTATION_RANGE.get();
        mutationChanceBase = MUTATION_CHANCE_BASE.get();
        mutationChancePerGen = MUTATION_CHANCE_PER_GEN.get();
        mutationChancePerGeneDiff = MUTATION_CHANCE_PER_GENE_DIFF.get();
        geneMin = GENE_MIN.get();
        geneMax = GENE_MAX.get();
        // serum
        s01BaseDuration = S01_BASE_DURATION.get();
        s02BaseDuration = S02_BASE_DURATION.get();
        s03BaseDuration = S03_BASE_DURATION.get();
        stackAmplifierCap = STACK_AMPLIFIER_CAP.get();
        stackDurationCap = STACK_DURATION_CAP.get();
        activityThresholdForBonus = ACTIVITY_THRESHOLD_FOR_BONUS.get();
        durationMultiplierBase = DURATION_MULTIPLIER_BASE.get();
        durationMultiplierPerActivity = DURATION_MULTIPLIER_PER_ACTIVITY.get();
        glowScanRangeCap = GLOW_SCAN_RANGE_CAP.get();
        // incubator
        maturationThreshold = MATURATION_THRESHOLD.get();
        resourceThreshold = RESOURCE_THRESHOLD.get();
        nutritionDecayInterval = NUTRITION_DECAY_INTERVAL.get();
        purityDecayInterval = PURITY_DECAY_INTERVAL.get();
        dataSignalDecayInterval = DATA_SIGNAL_DECAY_INTERVAL.get();
        nutritionInjectAmount = NUTRITION_INJECT_AMOUNT.get();
        purityInjectAmount = PURITY_INJECT_AMOUNT.get();
        dataSignalInjectAmount = DATA_SIGNAL_INJECT_AMOUNT.get();
        matureNutritionCost = MATURE_NUTRITION_COST.get();
        maturePurityCost = MATURE_PURITY_COST.get();
        // curios
        beltScanRange = BELT_SCAN_RANGE.get();
        beltNutritionThreshold = BELT_NUTRITION_THRESHOLD.get();
        beltPurityThreshold = BELT_PURITY_THRESHOLD.get();
        beltDataSignalThreshold = BELT_DATA_SIGNAL_THRESHOLD.get();
        packEffectReductionRate = PACK_EFFECT_REDUCTION_RATE.get();
        packHealThreshold = PACK_HEAL_THRESHOLD.get().floatValue();
        packHealCooldown = PACK_HEAL_COOLDOWN.get();
        monocleHudRange = MONOCLE_HUD_RANGE.get();
    }
}

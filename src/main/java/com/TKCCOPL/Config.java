package com.TKCCOPL;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = cybercultivator.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // Bio-Pulse Belt
    private static final ForgeConfigSpec.IntValue BELT_SCAN_RANGE = BUILDER
            .comment("Scan range (blocks) for Bio-Pulse Belt to find incubators")
            .defineInRange("beltScanRange", 3, 1, 8);

    private static final ForgeConfigSpec.IntValue BELT_NUTRITION_THRESHOLD = BUILDER
            .comment("Below this value, belt will auto-inject nutrients into incubator")
            .defineInRange("beltNutritionThreshold", 50, 0, 100);

    private static final ForgeConfigSpec.IntValue BELT_PURITY_THRESHOLD = BUILDER
            .comment("Below this value, belt will auto-inject purified water into incubator")
            .defineInRange("beltPurityThreshold", 50, 0, 100);

    private static final ForgeConfigSpec.IntValue BELT_DATA_SIGNAL_THRESHOLD = BUILDER
            .comment("Below this value, belt will auto-inject data signal into incubator")
            .defineInRange("beltDataSignalThreshold", 25, 0, 100);

    // Life Support Pack
    private static final ForgeConfigSpec.IntValue PACK_EFFECT_REDUCTION_RATE = BUILDER
            .comment("NeuralOverload duration reduced per tick when wearing Life Support Pack")
            .defineInRange("packEffectReductionRate", 2, 1, 10);

    private static final ForgeConfigSpec.DoubleValue PACK_HEAL_THRESHOLD = BUILDER
            .comment("Health threshold to trigger emergency heal from Life Support Pack")
            .defineInRange("packHealThreshold", 6.0, 1.0, 20.0);

    private static final ForgeConfigSpec.IntValue PACK_HEAL_COOLDOWN = BUILDER
            .comment("Cooldown ticks between emergency heals (default 1200 = 60s)")
            .defineInRange("packHealCooldown", 1200, 200, 6000);

    // Spectrum Monocle
    private static final ForgeConfigSpec.IntValue MONOCLE_HUD_RANGE = BUILDER
            .comment("Max distance to show incubator HUD overlay when wearing Spectrum Monocle")
            .defineInRange("monocleHudRange", 8, 3, 16);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    // Runtime values
    public static int beltScanRange;
    public static int beltNutritionThreshold;
    public static int beltPurityThreshold;
    public static int beltDataSignalThreshold;
    public static int packEffectReductionRate;
    public static float packHealThreshold;
    public static int packHealCooldown;
    public static int monocleHudRange;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
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

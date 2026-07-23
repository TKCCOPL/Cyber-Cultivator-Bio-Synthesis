package com.TKCCOPL.recipe;

import com.TKCCOPL.Config;

/** Shared, deterministic probability rules used by machines, APIs, screens, and JEI. */
public final class GeneSpliceRules {
    public static final int MIN_OFFSPRING_COUNT = 1;
    public static final int MAX_OFFSPRING_COUNT = 2;

    private GeneSpliceRules() {
    }

    public static double mutationChance(int generation, int geneDifference) {
        return mutationChance(generation, geneDifference,
                Config.mutationChanceBase, Config.mutationChancePerGen,
                Config.mutationGenerationCap, Config.mutationChancePerGeneDiff,
                Config.mutationChanceCap);
    }

    public static double mutationChance(int generation, int geneDifference,
                                        double base, double perGeneration, int generationCap,
                                        double perGeneDifference, double chanceCap) {
        int effectiveGeneration = Math.min(Math.max(0, generation), Math.max(0, generationCap));
        int effectiveDifference = Math.max(0, geneDifference);
        double configuredCap = clampProbability(chanceCap);
        double chance = base + effectiveGeneration * perGeneration + effectiveDifference * perGeneDifference;
        return Math.max(0.0D, Math.min(configuredCap, chance));
    }

    public static double normalTwinChance(int generation) {
        return normalTwinChance(generation,
                Config.twinChanceBase, Config.twinChancePerGen, Config.twinChanceCap);
    }

    public static double normalTwinChance(int generation, double base, double perGeneration, double chanceCap) {
        double configuredCap = clampProbability(chanceCap);
        double chance = base + Math.max(0, generation) * perGeneration;
        return Math.max(0.0D, Math.min(configuredCap, chance));
    }

    public static double totalTwinChance(double mutationChance, double normalTwinChance) {
        double mutation = clampProbability(mutationChance);
        double normalTwin = clampProbability(normalTwinChance);
        return mutation + (1.0D - mutation) * normalTwin;
    }

    public static int clampOffspringCount(int count) {
        return Math.max(MIN_OFFSPRING_COUNT, Math.min(MAX_OFFSPRING_COUNT, count));
    }

    /** Resolves event-modified output count while preserving the mutation-guaranteed twin rule. */
    public static int resolveOffspringCount(boolean mutation, int requestedCount) {
        return mutation ? MAX_OFFSPRING_COUNT : clampOffspringCount(requestedCount);
    }

    private static double clampProbability(double chance) {
        return Math.max(0.0D, Math.min(1.0D, chance));
    }
}

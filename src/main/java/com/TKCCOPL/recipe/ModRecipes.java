package com.TKCCOPL.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 静态注册表：基因拼接机和培养槽的配方/产出规则。
 * 非 JSON 驱动（算法驱动），供 JEI 和第三方 mod 查询。
 */
public final class ModRecipes {

    /** 基因拼接配方接口 */
    public interface IGeneSpliceRecipe {
        /** 计算子代基因，返回 [speed, yield, potency] */
        int[] calculateOffspring(int speedA, int yieldA, int potencyA,
                                 int speedB, int yieldB, int potencyB,
                                 RandomSource random);
        /** 突变概率计算 */
        double getMutationChance(int generation, int geneDifference);
    }

    /** 培养槽产出接口 */
    public interface IIncubatorOutput {
        /** 种子类型标识（用于 JEI 展示） */
        String getSeedType();
        /** 默认基因值 [speed, yield, potency]（用于 JEI 展示） */
        int[] getDefaultGenes();
        /** 基因对生长速率的倍率 */
        double getGrowthMultiplier(int geneSpeed);
    }

    private static final List<IGeneSpliceRecipe> SPLICE_RECIPES = new ArrayList<>();
    private static final List<IIncubatorOutput> INCUBATOR_OUTPUTS = new ArrayList<>();

    /** 默认拼接配方实现 */
    private static final IGeneSpliceRecipe DEFAULT_SPLICE = new IGeneSpliceRecipe() {
        @Override
        public int[] calculateOffspring(int speedA, int yieldA, int potencyA,
                                        int speedB, int yieldB, int potencyB,
                                        RandomSource random) {
            int newSpeed = clampGene((speedA + speedB) / 2 + random.nextInt(5) - 2);
            int newYield = clampGene((yieldA + yieldB) / 2 + random.nextInt(5) - 2);
            int newPotency = clampGene((potencyA + potencyB) / 2 + random.nextInt(5) - 2);
            return new int[]{newSpeed, newYield, newPotency};
        }

        @Override
        public double getMutationChance(int generation, int geneDifference) {
            return 0.05 + generation * 0.02 + geneDifference * 0.01;
        }

        private int clampGene(int value) {
            return Math.max(1, Math.min(10, value));
        }
    };

    static {
        SPLICE_RECIPES.add(DEFAULT_SPLICE);
    }

    private ModRecipes() {
    }

    public static List<IGeneSpliceRecipe> getSPLICE_RECIPES() {
        return Collections.unmodifiableList(SPLICE_RECIPES);
    }

    public static List<IIncubatorOutput> getINCUBATOR_OUTPUTS() {
        return Collections.unmodifiableList(INCUBATOR_OUTPUTS);
    }

    public static IGeneSpliceRecipe getDefaultSpliceRecipe() {
        return DEFAULT_SPLICE;
    }

    /** 注册自定义拼接配方（供其他 mod 调用） */
    public static void registerSpliceRecipe(IGeneSpliceRecipe recipe) {
        SPLICE_RECIPES.add(recipe);
    }

    /** 注册培养槽产出（供其他 mod 调用） */
    public static void registerIncubatorOutput(IIncubatorOutput output) {
        INCUBATOR_OUTPUTS.add(output);
    }
}

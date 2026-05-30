package com.TKCCOPL.recipe;

import com.TKCCOPL.Config;
import com.TKCCOPL.init.ModItems;
import com.TKCCOPL.item.GeneticSeedItem;
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
        /** 培养槽产出的物品（用于 JEI 输出槽） */
        ItemStack getOutput();
        /** 作物显示名称（用于 JEI 文字） */
        String getDisplayName();
    }

    private static final List<IGeneSpliceRecipe> SPLICE_RECIPES = new ArrayList<>();
    private static final List<IIncubatorOutput> INCUBATOR_OUTPUTS = new ArrayList<>();

    /** 默认拼接配方实现 */
    private static final IGeneSpliceRecipe DEFAULT_SPLICE = new IGeneSpliceRecipe() {
        @Override
        public int[] calculateOffspring(int speedA, int yieldA, int potencyA,
                                        int speedB, int yieldB, int potencyB,
                                        RandomSource random) {
            int range = Config.mutationRange;
            int newSpeed = GeneticSeedItem.clampGene((speedA + speedB) / 2 + random.nextInt(range * 2 + 1) - range);
            int newYield = GeneticSeedItem.clampGene((yieldA + yieldB) / 2 + random.nextInt(range * 2 + 1) - range);
            int newPotency = GeneticSeedItem.clampGene((potencyA + potencyB) / 2 + random.nextInt(range * 2 + 1) - range);
            return new int[]{newSpeed, newYield, newPotency};
        }

        @Override
        public double getMutationChance(int generation, int geneDifference) {
            return Config.mutationChanceBase + generation * Config.mutationChancePerGen + geneDifference * Config.mutationChancePerGeneDiff;
        }
    };

    static {
        SPLICE_RECIPES.add(DEFAULT_SPLICE);

        // 纤维草: Speed=4, Yield=7, Potency=3 → 植物纤维
        INCUBATOR_OUTPUTS.add(new IIncubatorOutput() {
            @Override public String getSeedType() { return "fiber_reed"; }
            @Override public int[] getDefaultGenes() { return new int[]{4, 7, 3}; }
            @Override public double getGrowthMultiplier(int geneSpeed) { return 0.5 + geneSpeed / 10.0 * 1.5; }
            @Override public ItemStack getOutput() { return new ItemStack(ModItems.PLANT_FIBER.get()); }
            @Override public String getDisplayName() { return "纤维草"; }
        });
        // 蛋白质豆: Speed=5, Yield=4, Potency=7 → 生化原液
        INCUBATOR_OUTPUTS.add(new IIncubatorOutput() {
            @Override public String getSeedType() { return "protein_soy"; }
            @Override public int[] getDefaultGenes() { return new int[]{5, 4, 7}; }
            @Override public double getGrowthMultiplier(int geneSpeed) { return 0.5 + geneSpeed / 10.0 * 1.5; }
            @Override public ItemStack getOutput() { return new ItemStack(ModItems.BIOCHEMICAL_SOLUTION.get()); }
            @Override public String getDisplayName() { return "蛋白质豆"; }
        });
        // 酒精花: Speed=6, Yield=3, Potency=5 → 工业乙醇
        INCUBATOR_OUTPUTS.add(new IIncubatorOutput() {
            @Override public String getSeedType() { return "alcohol_bloom"; }
            @Override public int[] getDefaultGenes() { return new int[]{6, 3, 5}; }
            @Override public double getGrowthMultiplier(int geneSpeed) { return 0.5 + geneSpeed / 10.0 * 1.5; }
            @Override public ItemStack getOutput() { return new ItemStack(ModItems.INDUSTRIAL_ETHANOL.get()); }
            @Override public String getDisplayName() { return "酒精花"; }
        });
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

    /**
     * 根据种子类型标识查找对应的种子物品。
     * 供 JEI 基因拼接展示使用。
     */
    public static ItemStack getSeedItemForType(String seedType) {
        return switch (seedType) {
            case "fiber_reed" -> new ItemStack(ModItems.FIBER_REED_SEEDS.get());
            case "protein_soy" -> new ItemStack(ModItems.PROTEIN_SOY_SEEDS.get());
            case "alcohol_bloom" -> new ItemStack(ModItems.ALCOHOL_BLOOM_SEEDS.get());
            default -> ItemStack.EMPTY;
        };
    }
}

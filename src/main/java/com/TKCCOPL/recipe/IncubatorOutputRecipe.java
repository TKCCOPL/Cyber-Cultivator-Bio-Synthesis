package com.TKCCOPL.recipe;

import com.TKCCOPL.item.GeneticSeedItem;
import com.mojang.logging.LogUtils;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

/**
 * 培养槽产出配方（JSON 数据驱动）。
 * 定义种子类型 → 作物产出的映射关系。
 */
public class IncubatorOutputRecipe implements net.minecraft.world.item.crafting.Recipe<Container> {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final ResourceLocation id;
    private final ItemStack seedItem;      // 匹配用的种子物品
    private final ItemStack outputItem;    // 输出物品模板
    private final String countFormula;     // "2 + yield / 3"
    private final String qualityTag;       // "Potency" / "Purity" / "Concentration"
    private final int[] defaultGenes;      // [speed, yield, potency] — JEI 展示用
    private final String cropName;         // 作物显示名称 — JEI 展示用

    public IncubatorOutputRecipe(ResourceLocation id, ItemStack seedItem, ItemStack outputItem,
                                  String countFormula, String qualityTag, int[] defaultGenes,
                                  String cropName) {
        this.id = id;
        this.seedItem = seedItem;
        this.outputItem = outputItem;
        this.countFormula = countFormula;
        this.qualityTag = qualityTag;
        this.defaultGenes = defaultGenes;
        this.cropName = cropName;
    }

    /** 匹配种子物品（基于 Item 类型，不比较 NBT） */
    public boolean matches(ItemStack seed) {
        return seed.getItem() == seedItem.getItem();
    }

    @Override
    public boolean matches(Container container, Level level) {
        return !container.getItem(0).isEmpty() && matches(container.getItem(0));
    }

    /** 根据种子基因值计算产出 */
    public ItemStack assemble(ItemStack seedStack) {
        int yield = GeneticSeedItem.getGene(seedStack, GeneticSeedItem.GENE_YIELD);
        int potency = GeneticSeedItem.getGene(seedStack, GeneticSeedItem.GENE_POTENCY);
        int generation = GeneticSeedItem.getGeneration(seedStack);
        int synergy = GeneticSeedItem.getSynergy(seedStack);
        int count = evaluateCountFormula(yield);

        ItemStack result = new ItemStack(outputItem.getItem(), count);
        if (!qualityTag.isEmpty()) {
            result.getOrCreateTag().putInt(qualityTag, potency);
        }
        // 传递 Generation 标签
        result.getOrCreateTag().putInt("Generation", generation);
        // 传递 Gene_Synergy 标签（确保后续莓合成和血清合成能读取到该值）
        if (synergy > 0) {
            result.getOrCreateTag().putInt(GeneticSeedItem.GENE_SYNERGY, synergy);
        }
        // 传递 Mutation 标签（整数类型码 + 详情）
        CompoundTag seedTag = seedStack.getTag();
        if (seedTag != null && seedTag.contains("Mutation")) {
            result.getOrCreateTag().putInt("Mutation", seedTag.getInt("Mutation"));
            if (seedTag.contains("MutationDetail")) {
                result.getOrCreateTag().putString("MutationDetail", seedTag.getString("MutationDetail"));
            }
        }
        return result;
    }

    /**
     * 简单公式求值: "2 + yield / 3" → 2 + yieldValue / 3
     * 支持格式: "N", "N + yield / M", "N + yield * M", "N + yield / M - 1"
     * 整数除法，向下取整
     */
    private int evaluateCountFormula(int yieldValue) {
        String formula = countFormula.trim();

        // 纯常量
        try {
            return Integer.parseInt(formula);
        } catch (NumberFormatException ignored) {}

        // 替换 yield 为实际值
        formula = formula.replace("yield", String.valueOf(yieldValue));

        try {
            // 先处理乘除（从左到右，保持优先级）
            java.util.regex.Pattern mulDiv = java.util.regex.Pattern.compile("(-?\\d+)\\s*([*/])\\s*(-?\\d+)");
            java.util.regex.Matcher matcher;
            String current = formula;
            while ((matcher = mulDiv.matcher(current)).find()) {
                int a = Integer.parseInt(matcher.group(1));
                String op = matcher.group(2);
                int b = Integer.parseInt(matcher.group(3));
                int result = "*".equals(op) ? a * b : (b == 0 ? 0 : a / b);
                current = current.substring(0, matcher.start()) + result + current.substring(matcher.end());
            }

            // 再处理加减法
            java.util.regex.Pattern addSub = java.util.regex.Pattern.compile("(-?\\d+)\\s*([+-])\\s*(-?\\d+)");
            while ((matcher = addSub.matcher(current)).find()) {
                int a = Integer.parseInt(matcher.group(1));
                String op = matcher.group(2);
                int b = Integer.parseInt(matcher.group(3));
                int result = "+".equals(op) ? a + b : a - b;
                current = current.substring(0, matcher.start()) + result + current.substring(matcher.end());
            }

            return Integer.parseInt(current.trim());
        } catch (Exception e) {
            LOGGER.error("[IncubatorOutputRecipe] Failed to evaluate count_formula '{}' (yield={}): {}",
                    countFormula, yieldValue, e.getMessage());
            return 2; // 保底
        }
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess registryAccess) {
        ItemStack seedStack = container.getItem(0);
        if (seedStack.isEmpty()) return outputItem.copy();
        return assemble(seedStack);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return false;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return outputItem.copy();
    }

    @Override
    public ResourceLocation getId() { return id; }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.INCUBATOR_OUTPUT_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.INCUBATOR_OUTPUT.get();
    }

    // Accessors
    public ItemStack getSeedItem() { return seedItem.copy(); }
    public ItemStack getOutputItem() { return outputItem.copy(); }
    public String getCountFormula() { return countFormula; }
    public String getQualityTag() { return qualityTag; }
    public int[] getDefaultGenes() { return defaultGenes; }
    public String getCropName() { return cropName; }
}

package com.TKCCOPL.api;

import com.TKCCOPL.Config;
import com.TKCCOPL.block.entity.*;
import com.TKCCOPL.cybercultivator;
import com.TKCCOPL.item.GeneticSeedItem;
import com.TKCCOPL.item.SynapticSerumItem;
import com.TKCCOPL.recipe.ModRecipeTypes;
import com.TKCCOPL.recipe.RecipeOrdering;
import com.TKCCOPL.recipe.SerumRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/**
 * Cyber-Cultivator 公开 API 门面类。
 * 所有方法 null 安全，供第三方模组调用。
 */
public final class CyberCultivatorAPI {
    private CyberCultivatorAPI() {}

    // === 基因数据 API ===

    /** 读取种子基因值 (1-10)，无数据返回 1 */
    public static int getGene(ItemStack seed, String geneKey) {
        return GeneticSeedItem.getGene(seed, geneKey);
    }

    /** 设置种子基因值（clamp 至 1-10） */
    public static void setGene(ItemStack seed, String geneKey, int value) {
        GeneticSeedItem.setGene(seed, geneKey, value);
    }

    /** 读取种子代数，无数据返回 0 */
    public static int getGeneration(ItemStack seed) {
        return GeneticSeedItem.getGeneration(seed);
    }

    /** 读取协同基因值 (0-10)，无数据返回 0 */
    public static int getSynergy(ItemStack seed) {
        return GeneticSeedItem.getSynergy(seed);
    }

    // === 机器状态 API ===

    /** 获取培养槽状态快照，位置无效返回 null */
    public static IncubatorInfo getIncubatorInfo(Level level, BlockPos pos) {
        if (level == null || pos == null) return null;
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof BioIncubatorBlockEntity incubator)) return null;
        return new IncubatorInfo(
                incubator.getNutrition(),
                incubator.getPurity(),
                incubator.getDataSignal(),
                incubator.getGrowthPercent(),
                incubator.getEstimatedSecondsRemaining(),
                incubator.hasSeed(),
                incubator.getSeed()
        );
    }

    /** 查询培养槽精确生长速率，1000 表示每 tick 推进 1 点；位置无效或无法生长返回 0。 */
    public static int getIncubatorGrowthRateMilli(Level level, BlockPos pos) {
        if (level == null || pos == null) return 0;
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof BioIncubatorBlockEntity incubator
                ? incubator.getCurrentGrowthRateMilli() : 0;
    }

    /** 获取灌装机状态快照 */
    public static BottlerInfo getBottlerInfo(Level level, BlockPos pos) {
        if (level == null || pos == null) return null;
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof SerumBottlerBlockEntity bottler)) return null;
        return new BottlerInfo(
                bottler.getProgress(),
                bottler.getMaxProgress(),
                bottler.getActiveRecipeId(),
                bottler.getOutput(),
                SerumBottlerBlockEntity.getActivity(bottler.getOutput())
        );
    }

    /** 获取冷凝器状态快照 */
    public static CondenserInfo getCondenserInfo(Level level, BlockPos pos) {
        if (level == null || pos == null) return null;
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof AtmosphericCondenserBlockEntity condenser)) return null;
        return new CondenserInfo(
                condenser.getProgress(),
                condenser.getMaxProgress(),
                condenser.getStock(),
                condenser.getMaxStock(),
                condenser.getStock() >= condenser.getMaxStock()
        );
    }

    /** 获取拼接机状态快照 */
    public static SplicerInfo getSplicerInfo(Level level, BlockPos pos) {
        if (level == null || pos == null) return null;
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof GeneSplicerBlockEntity splicer)) return null;
        return new SplicerInfo(
                splicer.getSeedA(),
                splicer.getSeedB(),
                splicer.getOutput(),
                splicer.hasOutput(),
                splicer.getInputCount()
        );
    }

    // === 血清配方 API ===

    /** 查询所有血清配方 */
    public static List<SerumRecipe> getSerumRecipes(Level level) {
        if (level == null) return List.of();
        return RecipeOrdering.sorted(level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.SERUM_BOTTLING.get()));
    }

    /** 计算 Activity 值 */
    public static int calculateActivity(ItemStack[] inputs) {
        return SerumBottlerBlockEntity.calculateActivity(inputs);
    }

    /** 查询血清效果参数 */
    public static SerumEffectInfo getSerumEffectInfo(ItemStack serum) {
        if (serum == null || serum.isEmpty() || !(serum.getItem() instanceof SynapticSerumItem serumItem)) return null;
        int activity = SynapticSerumItem.getActivity(serum);
        double multiplier = Config.durationMultiplierBase + activity * Config.durationMultiplierPerActivity;
        int baseAmp = Math.min(SynapticSerumItem.getBaseAmplifier(activity)
                + SynapticSerumItem.getActivityBonusAmplifier(activity), Config.stackAmplifierCap);
        int baseDuration = SynapticSerumItem.getBaseDuration(serum);
        var effectId = ForgeRegistries.MOB_EFFECTS.getKey(serumItem.getSerumEffect());
        if (effectId == null) return null;
        return new SerumEffectInfo(
                effectId.toString(),
                baseDuration,
                baseAmp,
                multiplier,
                activity
        );
    }

    // === 版本/兼容信息 ===

    public static String getModVersion() {
        return ModList.get().getModContainerById(cybercultivator.MODID)
                .map(c -> c.getModInfo().getVersion().toString())
                .orElse("unknown");
    }

    public static boolean isCuriosLoaded() {
        return ModList.get().isLoaded("curios");
    }
}

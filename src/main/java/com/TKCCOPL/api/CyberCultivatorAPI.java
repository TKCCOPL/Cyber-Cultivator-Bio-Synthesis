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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Cyber-Cultivator 公开 API 门面类。
 * 所有方法 null 安全，供第三方模组调用。
 */
public final class CyberCultivatorAPI {
    private static final Logger LOGGER = LoggerFactory.getLogger("CyberCultivator/API");
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

    // === v1.1.7 红石控制 API ===

    /**
     * 获取机器红石控制状态快照。位置无效或非机器 BE 返回 {@code null}。
     *
     * <p>v1.1.7 hotfix：仅服务端可用。{@code powered} 状态不持久化、不在客户端采样，
     * 客户端调用会返回 {@code null} 避免返回陈旧状态。</p>
     *
     * @param level 世界（仅服务端）
     * @param pos   方块位置
     * @return 控制信息快照；位置无效或客户端调用返回 {@code null}
     */
    public static MachineControlInfo getMachineControlInfo(Level level, BlockPos pos) {
        if (level == null || pos == null) return null;
        if (!(level instanceof ServerLevel)) return null;
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof MachineRedstoneBlockEntity machine)) return null;
        MachineRedstoneState state = machine.getRedstoneState();
        return new MachineControlInfo(
                state.getMode(),
                state.isPowered(),
                state.isProcessingAllowed(),
                machine.getComparatorSignal()
        );
    }

    /**
     * 设置机器红石模式。走与 GUI 相同的服务端验证、持久化、同步路径。
     *
     * <p>约束（§5.3）：
     * <ul>
     *   <li>仅逻辑服务端可执行，客户端调用返回 {@code false}</li>
     *   <li>非主线程调用返回 {@code false} 并记录 debug 日志（M7）</li>
     *   <li>参数或目标无效返回 {@code false}</li>
     * </ul>
     *
     * @param level 世界
     * @param pos   方块位置
     * @param mode  目标模式；{@code null} 视为无效
     * @return 操作是否成功
     */
    public static boolean setMachineRedstoneMode(Level level, BlockPos pos, RedstoneControlMode mode) {
        if (level == null || pos == null || mode == null) return false;
        // v1.2.0 §10 采纳：使用 ServerLevel + isSameThread() 替代 getRunningThread() 比较
        if (!(level instanceof ServerLevel serverLevel)) return false;
        if (!serverLevel.getServer().isSameThread()) {
            LOGGER.debug("setMachineRedstoneMode rejected: non-main-thread call at {}", pos);
            return false;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof MachineRedstoneBlockEntity machine)) return false;
        if (!machine.getRedstoneState().setMode(mode)) {
            // 模式未变化，仍视为成功（幂等）
            return true;
        }
        // 持久化 + 同步给客户端（与 GUI clickMenuButton 路径一致）
        be.setChanged();
        level.sendBlockUpdated(pos, be.getBlockState(), be.getBlockState(), 2);
        return true;
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

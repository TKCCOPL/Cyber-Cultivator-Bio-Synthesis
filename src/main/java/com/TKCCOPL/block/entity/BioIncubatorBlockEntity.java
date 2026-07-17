package com.TKCCOPL.block.entity;

import com.TKCCOPL.Config;
import com.TKCCOPL.init.ModBlockEntities;
import com.TKCCOPL.item.GeneticSeedItem;
import com.TKCCOPL.event.CropMatureEvent;
import com.TKCCOPL.recipe.ModRecipeTypes;
import com.TKCCOPL.recipe.RecipeOrdering;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class BioIncubatorBlockEntity extends BlockEntity {
    private static final String TAG_NUTRITION = "Nutrition";
    private static final String TAG_PURITY = "Purity";
    private static final String TAG_DATA_SIGNAL = "DataSignal";
    private static final String TAG_GROWTH_PROGRESS = "GrowthProgress";
    private static final String TAG_SEED = "Seed";

    private static final int SYNC_INTERVAL = 10;

    private int nutrition;
    private int purity;
    private int dataSignal;
    private int growthProgress;
    private ItemStack seed = ItemStack.EMPTY;

    // 基因缓存（避免每 tick 解析 NBT）
    private int cachedSpeed = 1;
    private int syncCounter = 0;

    public BioIncubatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BIO_INCUBATOR.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BioIncubatorBlockEntity blockEntity) {
        if (level.isClientSide || blockEntity.seed.isEmpty()) {
            return;
        }

        boolean changed = false;
        boolean forceSync = false;

        // 资源自然衰减
        if (level.getGameTime() % (long) Config.nutritionDecayInterval == 0L && blockEntity.nutrition > 0) {
            blockEntity.nutrition -= 1;
            changed = true;
        }
        if (level.getGameTime() % (long) Config.purityDecayInterval == 0L && blockEntity.purity > 0) {
            blockEntity.purity -= 1;
            changed = true;
        }
        if (level.getGameTime() % (long) Config.dataSignalDecayInterval == 0L && blockEntity.dataSignal > 0) {
            blockEntity.dataSignal -= 1;
            changed = true;
        }

        // 生长推进：需要三项资源均高于阈值
        if (blockEntity.nutrition > Config.resourceThreshold
                && blockEntity.purity > Config.resourceThreshold
                && blockEntity.dataSignal > 0) {

            // 计算生长速率：基础速率 * 基因倍率 * 环境倍率
            int geneSpeed = blockEntity.cachedSpeed;
            double geneMultiplier = 0.5 + (geneSpeed / 10.0) * 1.5; // 范围 0.5 - 2.0
            double envMultiplier = (blockEntity.nutrition + blockEntity.purity + blockEntity.dataSignal) / 300.0;
            int growthRate = Math.max(1, (int) Math.round(geneMultiplier * envMultiplier));

            blockEntity.growthProgress += growthRate;
            changed = true;

            // 成熟判定
            if (blockEntity.growthProgress >= Config.maturationThreshold) {
                // 产出作物物品
                ItemStack cropOutput = getCropOutput(level, blockEntity.seed);

                // 触发 CropMatureEvent，允许其他 mod 修改产出
                CropMatureEvent cropEvent = new CropMatureEvent(level, pos, blockEntity.seed, cropOutput);
                boolean cancelled = net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(cropEvent);

                // A cancelled maturity attempt starts a fresh growth cycle while
                // preserving the seed and resources. Resetting progress prevents
                // the event from firing again every tick.
                blockEntity.growthProgress = 0;
                changed = true;
                forceSync = true;

                if (!cancelled) {
                    blockEntity.seed = ItemStack.EMPTY;
                    // 事件未取消，正常产出
                    cropOutput = cropEvent.getOutput();
                    if (!cropOutput.isEmpty()) {
                        Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, cropOutput);
                    }
                    // 消耗资源
                    blockEntity.nutrition = Math.max(0, blockEntity.nutrition - Config.matureNutritionCost);
                    blockEntity.purity = Math.max(0, blockEntity.purity - Config.maturePurityCost);
                }
            }
        }

        if (changed) {
            blockEntity.syncCounter++;
            if (forceSync || blockEntity.syncCounter >= SYNC_INTERVAL) {
                blockEntity.syncToClient();
                blockEntity.syncCounter = 0;
            }
        }
    }

    /**
     * 根据种子类型返回对应的作物产出物品。
     * 从 RecipeManager 查询 IncubatorOutputRecipe，使用 JSON 数据驱动。
     * 未知种子类型返回种子本身作为保底产出。
     */
    private static ItemStack getCropOutput(Level level, ItemStack seed) {
        if (level == null || seed.isEmpty()) return seed.copy();

        return RecipeOrdering.sorted(level.getRecipeManager()
                .getAllRecipesFor(ModRecipeTypes.INCUBATOR_OUTPUT.get()))
                .stream()
                .filter(r -> r.matches(seed))
                .findFirst()
                .map(r -> r.assemble(seed))
                .orElse(seed.copy()); // 未知种子保底产出种子本身
    }

    public boolean hasSeed() {
        return !seed.isEmpty();
    }

    public ItemStack getSeed() {
        return seed;
    }

    public boolean tryInsertSeed(ItemStack stack) {
        if (!seed.isEmpty()) {
            return false;
        }
        // 确保种子携带基因数据，防止缺少 NBT 导致生长计算异常
        if (stack.getItem() instanceof GeneticSeedItem geneticSeed) {
            geneticSeed.ensureGeneData(stack);
        }
        seed = stack.copy();
        cachedSpeed = GeneticSeedItem.getGene(stack, GeneticSeedItem.GENE_SPEED);
        growthProgress = 0;
        syncCounter = 0;
        syncToClient();
        return true;
    }

    public ItemStack extractSeed() {
        if (seed.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack out = seed;
        seed = ItemStack.EMPTY;
        cachedSpeed = 1;
        growthProgress = 0;
        syncCounter = 0;
        syncToClient();
        return out;
    }

    public void addNutrition(int value) {
        nutrition = clampStat(nutrition + value);
        syncToClient();
    }

    public void addPurity(int value) {
        purity = clampStat(purity + value);
        syncToClient();
    }

    public void addDataSignal(int value) {
        dataSignal = clampStat(dataSignal + value);
        syncToClient();
    }

    private void syncToClient() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    public int getNutrition() {
        return nutrition;
    }

    public int getPurity() {
        return purity;
    }

    public int getDataSignal() {
        return dataSignal;
    }

    /** 获取生长进度百分比 (0-100) */
    public int getGrowthPercent() {
        if (seed.isEmpty() || Config.maturationThreshold <= 0) return 0;
        return Math.min(100, (int) ((long) growthProgress * 100 / Config.maturationThreshold));
    }

    /** 获取当前生长速率（每 tick 推进量），用于外部估算 */
    public int getCurrentGrowthRate() {
        if (seed.isEmpty()) return 0;
        if (nutrition <= Config.resourceThreshold || purity <= Config.resourceThreshold || dataSignal <= 0) return 0;
        int geneSpeed = cachedSpeed;
        double geneMultiplier = 0.5 + (geneSpeed / 10.0) * 1.5;
        double envMultiplier = (nutrition + purity + dataSignal) / 300.0;
        return Math.max(1, (int) Math.round(geneMultiplier * envMultiplier));
    }

    /** 估算剩余成熟时间（秒），-1 表示无法生长 */
    public int getEstimatedSecondsRemaining() {
        int rate = getCurrentGrowthRate();
        if (rate <= 0) return -1;
        int remaining = Config.maturationThreshold - growthProgress;
        if (remaining <= 0) return 0;
        // 每秒 20 tick
        return (int) Math.ceil(remaining / (double) rate / 20.0);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        nutrition = clampStat(tag.getInt(TAG_NUTRITION));
        purity = clampStat(tag.getInt(TAG_PURITY));
        dataSignal = clampStat(tag.getInt(TAG_DATA_SIGNAL));
        growthProgress = Math.max(0, tag.getInt(TAG_GROWTH_PROGRESS));
        seed = tag.contains(TAG_SEED) ? ItemStack.of(tag.getCompound(TAG_SEED)) : ItemStack.EMPTY;
        // 初始化基因缓存
        if (!seed.isEmpty()) {
            cachedSpeed = GeneticSeedItem.getGene(seed, GeneticSeedItem.GENE_SPEED);
        } else {
            cachedSpeed = 1;
        }
        syncCounter = 0;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(TAG_NUTRITION, nutrition);
        tag.putInt(TAG_PURITY, purity);
        tag.putInt(TAG_DATA_SIGNAL, dataSignal);
        tag.putInt(TAG_GROWTH_PROGRESS, growthProgress);
        if (!seed.isEmpty()) {
            tag.put(TAG_SEED, seed.save(new CompoundTag()));
        } else {
            tag.put(TAG_SEED, new CompoundTag()); // sentinel: ensure tag is non-empty for client sync
        }
    }

    private static int clampStat(int value) {
        return Math.max(0, Math.min(100, value));
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }
}

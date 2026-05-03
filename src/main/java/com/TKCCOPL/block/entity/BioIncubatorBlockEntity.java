package com.TKCCOPL.block.entity;

import com.TKCCOPL.init.ModBlockEntities;
import com.TKCCOPL.init.ModItems;
import com.TKCCOPL.item.GeneticSeedItem;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.item.Item;
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

    /** 成熟所需的基础生长进度 */
    private static final int MATURATION_THRESHOLD = 200;
    /** 资源消耗阈值：低于此值不生长 */
    private static final int RESOURCE_THRESHOLD = 10;

    private int nutrition;
    private int purity;
    private int dataSignal;
    private int growthProgress;
    private ItemStack seed = ItemStack.EMPTY;

    public BioIncubatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BIO_INCUBATOR.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BioIncubatorBlockEntity blockEntity) {
        if (level.isClientSide || blockEntity.seed.isEmpty()) {
            return;
        }

        boolean changed = false;

        // 资源自然衰减
        if (level.getGameTime() % 20L == 0L && blockEntity.nutrition > 0) {
            blockEntity.nutrition -= 1;
            changed = true;
        }
        if (level.getGameTime() % 40L == 0L && blockEntity.purity > 0) {
            blockEntity.purity -= 1;
            changed = true;
        }
        if (level.getGameTime() % 60L == 0L && blockEntity.dataSignal > 0) {
            blockEntity.dataSignal -= 1;
            changed = true;
        }

        // 生长推进：需要三项资源均高于阈值
        if (blockEntity.nutrition > RESOURCE_THRESHOLD
                && blockEntity.purity > RESOURCE_THRESHOLD
                && blockEntity.dataSignal > 0) {

            // 计算生长速率：基础速率 * 基因倍率 * 环境倍率
            int geneSpeed = GeneticSeedItem.getGene(blockEntity.seed, GeneticSeedItem.GENE_SPEED);
            double geneMultiplier = 0.5 + (geneSpeed / 10.0) * 1.5; // 范围 0.5 - 2.0
            double envMultiplier = (blockEntity.nutrition + blockEntity.purity + blockEntity.dataSignal) / 300.0;
            int growthRate = Math.max(1, (int) Math.round(geneMultiplier * envMultiplier));

            blockEntity.growthProgress += growthRate;
            changed = true;

            // 成熟判定
            if (blockEntity.growthProgress >= MATURATION_THRESHOLD) {
                // 产出作物物品
                ItemStack cropOutput = getCropOutput(blockEntity.seed);
                if (!cropOutput.isEmpty()) {
                    // 尝试弹出到世界（类似漏斗/箱子溢出机制）
                    Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, cropOutput);
                }

                // 消耗资源、重置生长进度、清除种子（防止无限产出）
                blockEntity.growthProgress = 0;
                blockEntity.seed = ItemStack.EMPTY;
                blockEntity.nutrition = Math.max(0, blockEntity.nutrition - 5);
                blockEntity.purity = Math.max(0, blockEntity.purity - 5);
                changed = true;
            }
        }

        if (changed) {
            blockEntity.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }

    /**
     * 根据种子类型返回对应的作物产出物品。
     * 基因 Yield 属性影响产出数量（基础 2 + yield/3 额外）。
     * 未知种子类型返回种子本身作为保底产出。
     */
    private static ItemStack getCropOutput(ItemStack seed) {
        Item seedItem = seed.getItem();
        int geneYield = GeneticSeedItem.getGene(seed, GeneticSeedItem.GENE_YIELD);
        int genePotency = GeneticSeedItem.getGene(seed, GeneticSeedItem.GENE_POTENCY);
        int generation = GeneticSeedItem.getGeneration(seed);
        int genePurity = GeneticSeedItem.getPurity(seed);
        int count = 2 + geneYield / 3; // yield 1-10 → count 2-5

        ItemStack output;
        if (seedItem == ModItems.FIBER_REED_SEEDS.get()) {
            output = new ItemStack(ModItems.PLANT_FIBER.get(), count);
            output.getOrCreateTag().putInt("Potency", genePotency);
            output.getOrCreateTag().putInt("Generation", generation);
        } else if (seedItem == ModItems.PROTEIN_SOY_SEEDS.get()) {
            output = new ItemStack(ModItems.BIOCHEMICAL_SOLUTION.get(), count);
            output.getOrCreateTag().putInt("Concentration", genePotency);
            output.getOrCreateTag().putInt("Generation", generation);
        } else if (seedItem == ModItems.ALCOHOL_BLOOM_SEEDS.get()) {
            output = new ItemStack(ModItems.INDUSTRIAL_ETHANOL.get(), count);
            output.getOrCreateTag().putInt("Purity", genePotency);
            output.getOrCreateTag().putInt("Generation", generation);
        } else {
            output = seed.copy();
        }

        // 将种子的 Gene_Purity 传递到产出物，确保后续莓合成和血清合成能读取到该值
        if (genePurity > 0) {
            output.getOrCreateTag().putInt(GeneticSeedItem.GENE_PURITY, genePurity);
        }

        // 如果种子携带 Mutation 标签，传递到产出物
        CompoundTag seedTag = seed.getTag();
        if (seedTag != null && seedTag.getBoolean("Mutation")) {
            output.getOrCreateTag().putBoolean("Mutation", true);
        }

        return output;
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
        seed = stack;
        growthProgress = 0;
        syncToClient();
        return true;
    }

    public ItemStack extractSeed() {
        if (seed.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack out = seed;
        seed = ItemStack.EMPTY;
        growthProgress = 0;
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
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
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
        if (seed.isEmpty() || MATURATION_THRESHOLD <= 0) return 0;
        return Math.min(100, (int) ((long) growthProgress * 100 / MATURATION_THRESHOLD));
    }

    /** 获取当前生长速率（每 tick 推进量），用于外部估算 */
    public int getCurrentGrowthRate() {
        if (seed.isEmpty()) return 0;
        if (nutrition <= RESOURCE_THRESHOLD || purity <= RESOURCE_THRESHOLD || dataSignal <= 0) return 0;
        int geneSpeed = GeneticSeedItem.getGene(seed, GeneticSeedItem.GENE_SPEED);
        double geneMultiplier = 0.5 + (geneSpeed / 10.0) * 1.5;
        double envMultiplier = (nutrition + purity + dataSignal) / 300.0;
        return Math.max(1, (int) Math.round(geneMultiplier * envMultiplier));
    }

    /** 估算剩余成熟时间（秒），-1 表示无法生长 */
    public int getEstimatedSecondsRemaining() {
        int rate = getCurrentGrowthRate();
        if (rate <= 0) return -1;
        int remaining = MATURATION_THRESHOLD - growthProgress;
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

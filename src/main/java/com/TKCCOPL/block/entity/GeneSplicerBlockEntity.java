package com.TKCCOPL.block.entity;

import com.TKCCOPL.init.ModBlockEntities;
import com.TKCCOPL.item.GeneticSeedItem;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class GeneSplicerBlockEntity extends BlockEntity {
    private static final String TAG_SEED_A = "SeedA";
    private static final String TAG_SEED_B = "SeedB";
    private static final String TAG_OUTPUT = "Output";

    private ItemStack seedA = ItemStack.EMPTY;
    private ItemStack seedB = ItemStack.EMPTY;
    private ItemStack output = ItemStack.EMPTY;

    /** 防止同 tick 双次 use() 导致放入两颗种子 */
    private long lastInsertTick = -1;

    public GeneSplicerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GENE_SPLICER.get(), pos, state);
    }

    public boolean tryInsertSeed(ItemStack stack, RandomSource random) {
        if (!output.isEmpty()) {
            return false;
        }
        if (!(stack.getItem() instanceof GeneticSeedItem)) {
            return false;
        }

        // Forge 1.20.1 单次右键可能触发两次 use()，防抖：同一 tick 只允许插入一次
        long currentTick = level != null ? level.getGameTime() : -1;
        if (currentTick == lastInsertTick) {
            return false;
        }

        if (seedA.isEmpty()) {
            seedA = stack;
            lastInsertTick = currentTick;
            syncToClient();
            return true;
        }
        if (seedB.isEmpty()) {
            seedB = stack;
            lastInsertTick = currentTick;
            craftOutput(random);
            syncToClient();
            return true;
        }
        return false;
    }

    public ItemStack extractOutput() {
        if (output.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack out = output;
        output = ItemStack.EMPTY;
        // 取出 output 后清除父本种子
        seedA = ItemStack.EMPTY;
        seedB = ItemStack.EMPTY;
        syncToClient();
        return out;
    }

    public ItemStack extractLastInput() {
        if (!seedB.isEmpty()) {
            ItemStack out = seedB;
            seedB = ItemStack.EMPTY;
            syncToClient();
            return out;
        }
        if (!seedA.isEmpty()) {
            ItemStack out = seedA;
            seedA = ItemStack.EMPTY;
            syncToClient();
            return out;
        }
        return ItemStack.EMPTY;
    }

    public ItemStack getSeedA() {
        return seedA;
    }

    public ItemStack getSeedB() {
        return seedB;
    }

    public ItemStack getOutput() {
        return output;
    }

    public boolean hasOutput() {
        return !output.isEmpty();
    }

    public int getInputCount() {
        int count = 0;
        if (!seedA.isEmpty()) {
            count++;
        }
        if (!seedB.isEmpty()) {
            count++;
        }
        return count;
    }

    private void craftOutput(RandomSource random) {
        if (seedA.isEmpty() || seedB.isEmpty()) {
            return;
        }

        // 1. 读取亲本基因
        int speedA = GeneticSeedItem.getGene(seedA, GeneticSeedItem.GENE_SPEED);
        int speedB = GeneticSeedItem.getGene(seedB, GeneticSeedItem.GENE_SPEED);
        int yieldA = GeneticSeedItem.getGene(seedA, GeneticSeedItem.GENE_YIELD);
        int yieldB = GeneticSeedItem.getGene(seedB, GeneticSeedItem.GENE_YIELD);
        int potencyA = GeneticSeedItem.getGene(seedA, GeneticSeedItem.GENE_POTENCY);
        int potencyB = GeneticSeedItem.getGene(seedB, GeneticSeedItem.GENE_POTENCY);

        // 2. 计算突变概率: base 5% + 代数 2%/代 + 基因差异 1%/点
        int genA = GeneticSeedItem.getGeneration(seedA);
        int genB = GeneticSeedItem.getGeneration(seedB);
        int maxGen = Math.max(genA, genB);
        int maxDiff = Math.max(
                Math.abs(speedA - speedB),
                Math.max(Math.abs(yieldA - yieldB), Math.abs(potencyA - potencyB))
        );
        double mutationChance = 0.05 + maxGen * 0.02 + maxDiff * 0.01;
        boolean isMutation = random.nextDouble() < mutationChance;

        // 3. 计算子代基因（标准公式 ±2）
        int newSpeed = GeneticSeedItem.clampGene((speedA + speedB) / 2 + random.nextInt(5) - 2);
        int newYield = GeneticSeedItem.clampGene((yieldA + yieldB) / 2 + random.nextInt(5) - 2);
        int newPotency = GeneticSeedItem.clampGene((potencyA + potencyB) / 2 + random.nextInt(5) - 2);

        // 4. 如果突变触发，应用突变结果
        int mutationType = 0; // 0=未突变, 1=数值突破, 2=协同基因
        String mutationDetail = "";
        if (isMutation) {
            double roll = random.nextDouble();
            if (roll < 0.80) {
                // 数值突破（80%）：随机一个基因变异 ±4（覆盖标准公式结果）
                int target = random.nextInt(3); // 0=Speed, 1=Yield, 2=Potency
                int bonus = random.nextInt(9) - 4; // -4 to +4

                String geneName;
                if (target == 0) {
                    newSpeed = GeneticSeedItem.clampGene((speedA + speedB) / 2 + bonus);
                    geneName = "Speed";
                } else if (target == 1) {
                    newYield = GeneticSeedItem.clampGene((yieldA + yieldB) / 2 + bonus);
                    geneName = "Yield";
                } else {
                    newPotency = GeneticSeedItem.clampGene((potencyA + potencyB) / 2 + bonus);
                    geneName = "Potency";
                }
                mutationType = 1;
                mutationDetail = geneName + (bonus >= 0 ? "+" : "") + bonus;
            } else {
                // Gene_Synergy 获得（20%）在步骤 6 处理
                mutationType = 2;
            }
        }

        // 5. 设置基因到输出种子
        ItemStack result = new ItemStack(seedA.getItem());
        GeneticSeedItem.setGenes(result, newSpeed, newYield, newPotency);

        // 6. 如果是 Synergy 突变，写入 Gene_Synergy（累加，上限 10）
        if (mutationType == 2) {
            int currentSynergy = GeneticSeedItem.getSynergy(result);
            int synergyGain = 1 + random.nextInt(3); // 1-3
            int newSynergy = Math.min(10, currentSynergy + synergyGain);
            result.getOrCreateTag().putInt(GeneticSeedItem.GENE_SYNERGY, newSynergy);
            mutationDetail = "Synergy+" + synergyGain;
        }

        // 7. 标记突变（整数类型码 + 详情）
        if (mutationType > 0) {
            result.getOrCreateTag().putInt("Mutation", mutationType);
            result.getOrCreateTag().putString("MutationDetail", mutationDetail);
        }

        // 8. 设置 Generation
        int childGen = maxGen + 1;
        result.getOrCreateTag().putInt(GeneticSeedItem.GENE_GENERATION, childGen);

        output = result;

        // 保留种子显示，直到玩家取出 output 后由 extractOutput() 清除
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        seedA = tag.contains(TAG_SEED_A) ? ItemStack.of(tag.getCompound(TAG_SEED_A)) : ItemStack.EMPTY;
        seedB = tag.contains(TAG_SEED_B) ? ItemStack.of(tag.getCompound(TAG_SEED_B)) : ItemStack.EMPTY;
        output = tag.contains(TAG_OUTPUT) ? ItemStack.of(tag.getCompound(TAG_OUTPUT)) : ItemStack.EMPTY;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!seedA.isEmpty()) {
            tag.put(TAG_SEED_A, seedA.save(new CompoundTag()));
        } else {
            tag.put(TAG_SEED_A, new CompoundTag()); // sentinel: ensure tag is non-empty for sync
        }
        if (!seedB.isEmpty()) {
            tag.put(TAG_SEED_B, seedB.save(new CompoundTag()));
        } else {
            tag.put(TAG_SEED_B, new CompoundTag());
        }
        if (!output.isEmpty()) {
            tag.put(TAG_OUTPUT, output.save(new CompoundTag()));
        } else {
            tag.put(TAG_OUTPUT, new CompoundTag());
        }
    }

    private void syncToClient() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
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

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        this.load(tag);
    }
}

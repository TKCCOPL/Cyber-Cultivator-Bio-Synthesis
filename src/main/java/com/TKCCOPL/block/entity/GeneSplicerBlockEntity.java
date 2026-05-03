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

        int speed = breedGene(seedA, seedB, GeneticSeedItem.GENE_SPEED, random);
        int yield = breedGene(seedA, seedB, GeneticSeedItem.GENE_YIELD, random);
        int potency = breedGene(seedA, seedB, GeneticSeedItem.GENE_POTENCY, random);

        ItemStack result = new ItemStack(seedA.getItem());
        GeneticSeedItem.setGenes(result, speed, yield, potency);
        output = result;

        // 保留种子显示，直到玩家取出 output 后由 extractOutput() 清除
    }

    private static int breedGene(ItemStack a, ItemStack b, String key, RandomSource random) {
        int parentA = GeneticSeedItem.getGene(a, key);
        int parentB = GeneticSeedItem.getGene(b, key);
        int mutation = random.nextInt(5) - 2; // -2..+2, 期望值=0
        int value = ((parentA + parentB) / 2) + mutation;
        return GeneticSeedItem.clampGene(value);
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
        }
        if (!seedB.isEmpty()) {
            tag.put(TAG_SEED_B, seedB.save(new CompoundTag()));
        }
        if (!output.isEmpty()) {
            tag.put(TAG_OUTPUT, output.save(new CompoundTag()));
        }
    }

    private void syncToClient() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
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
}

package com.TKCCOPL.block.entity;

import com.TKCCOPL.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
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

        if (blockEntity.nutrition > 10 && blockEntity.purity > 10 && blockEntity.dataSignal > 0) {
            blockEntity.growthProgress += 1;
            changed = true;
            if (blockEntity.growthProgress >= 200) {
                // MVP stage: keep cycle simple and only consume machine resources.
                blockEntity.growthProgress = 0;
                if (blockEntity.nutrition > 0) {
                    blockEntity.nutrition -= 1;
                }
                if (blockEntity.purity > 0) {
                    blockEntity.purity -= 1;
                }
                changed = true;
            }
        }

        if (changed) {
            blockEntity.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }

    public boolean hasSeed() {
        return !seed.isEmpty();
    }

    public boolean tryInsertSeed(ItemStack stack) {
        if (!seed.isEmpty()) {
            return false;
        }
        seed = stack;
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

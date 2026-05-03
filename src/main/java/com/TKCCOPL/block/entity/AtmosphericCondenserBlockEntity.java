package com.TKCCOPL.block.entity;

import com.TKCCOPL.init.ModBlockEntities;
import com.TKCCOPL.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class AtmosphericCondenserBlockEntity extends BlockEntity implements WorldlyContainer {
    private static final String TAG_PROGRESS = "Progress";
    private static final String TAG_OUTPUT = "Output";

    private static final int PRODUCTION_TIME = 600; // 30 seconds
    private static final int MAX_STACK = 32; // was 16

    private int progress;
    private ItemStack output = ItemStack.EMPTY;

    public AtmosphericCondenserBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ATMOSPHERIC_CONDENSER.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, AtmosphericCondenserBlockEntity blockEntity) {
        if (level.isClientSide) return;

        boolean changed = false;

        // Try to produce purified water bottle
        if (blockEntity.output.getCount() < MAX_STACK) {
            blockEntity.progress++;
            if (blockEntity.progress >= PRODUCTION_TIME) {
                blockEntity.progress = 0;
                if (blockEntity.output.isEmpty()) {
                    blockEntity.output = new ItemStack(ModItems.PURIFIED_WATER_BOTTLE.get());
                } else {
                    blockEntity.output.grow(1);
                }
                changed = true;
            }
        }

        // Auto-transfer purity to incubator below
        if (level.getGameTime() % 20L == 0L) {
            BlockPos below = pos.below();
            if (level.getBlockEntity(below) instanceof BioIncubatorBlockEntity incubator) {
                if (blockEntity.output.getCount() > 0 && incubator.getPurity() < 80) {
                    incubator.addPurity(20);
                    blockEntity.output.shrink(1);
                    changed = true;
                }
            }
        }

        if (changed) {
            blockEntity.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }

    public ItemStack getOutput() {
        return output;
    }

    public int getProgress() {
        return progress;
    }

    public boolean hasOutput() {
        return !output.isEmpty();
    }

    public ItemStack extractOutput() {
        if (output.isEmpty()) return ItemStack.EMPTY;
        ItemStack out = output;
        output = ItemStack.EMPTY;
        progress = 0;
        setChanged();
        return out;
    }

    // WorldlyContainer implementation for hopper compatibility
    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return output.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot == 0 ? output : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot != 0 || output.isEmpty()) return ItemStack.EMPTY;
        int taken = Math.min(amount, output.getCount());
        ItemStack result = output.split(taken);
        if (output.isEmpty()) output = ItemStack.EMPTY;
        setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot != 0) return ItemStack.EMPTY;
        ItemStack out = output;
        output = ItemStack.EMPTY;
        setChanged();
        return out;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        // Output only, no input
    }

    @Override
    public boolean stillValid(net.minecraft.world.entity.player.Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        output = ItemStack.EMPTY;
        setChanged();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return new int[]{0};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false; // No input through hopper
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == 0 && !output.isEmpty();
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        progress = Math.max(0, tag.getInt(TAG_PROGRESS));
        output = tag.contains(TAG_OUTPUT) ? ItemStack.of(tag.getCompound(TAG_OUTPUT)) : ItemStack.EMPTY;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(TAG_PROGRESS, progress);
        if (!output.isEmpty()) {
            tag.put(TAG_OUTPUT, output.save(new CompoundTag()));
        }
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }
}

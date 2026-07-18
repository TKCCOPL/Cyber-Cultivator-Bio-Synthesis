package com.TKCCOPL.block.entity;

import com.TKCCOPL.Config;
import com.TKCCOPL.init.ModBlockEntities;
import com.TKCCOPL.init.ModItems;
import com.TKCCOPL.menu.AtmosphericCondenserMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class AtmosphericCondenserBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {
    private static final String TAG_PROGRESS = "Progress";
    private static final String TAG_OUTPUT = "Output";
    private static final String TAG_AUTO_INJECT = "AutoInject";
    private static final String TAG_PAUSED = "Paused";

    private static final int PRODUCTION_TIME = 600; // 30 seconds
    private static final int MAX_STACK = 32; // was 16

    private int progress;
    private ItemStack output = ItemStack.EMPTY;
    private boolean autoInject = true;
    private boolean paused;
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> PRODUCTION_TIME;
                case 2 -> output.getCount();
                case 3 -> autoInject ? 1 : 0;
                case 4 -> level != null
                        && level.getBlockEntity(worldPosition.below()) instanceof BioIncubatorBlockEntity ? 1 : 0;
                case 5 -> paused ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 6;
        }
    };

    public AtmosphericCondenserBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ATMOSPHERIC_CONDENSER.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, AtmosphericCondenserBlockEntity blockEntity) {
        if (level.isClientSide) return;

        boolean changed = false;

        // Try to produce purified water bottle
        if (!blockEntity.paused && blockEntity.output.getCount() < MAX_STACK) {
            blockEntity.progress++;
            // 每 20 tick 同步一次进度，用于客户端 HUD 进度条动画
            if (blockEntity.progress % 20 == 0) {
                changed = true;
            }
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
        if (blockEntity.autoInject && level.getGameTime() % 20L == 0L) {
            BlockPos below = pos.below();
            if (level.getBlockEntity(below) instanceof BioIncubatorBlockEntity incubator) {
                if (blockEntity.output.getCount() > 0 && incubator.getPurity() < 80) {
                    incubator.addPurity(Config.purityInjectAmount);
                    blockEntity.output.shrink(1);
                    changed = true;
                }
            }
        }

        if (changed) {
            blockEntity.syncToClient();
        }
    }

    private void syncToClient() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    public ItemStack getOutput() {
        return output.copy();
    }

    public int getProgress() {
        return progress;
    }

    public int getMaxProgress() {
        return PRODUCTION_TIME;
    }

    public int getStock() {
        return output.getCount();
    }

    public int getMaxStock() {
        return MAX_STACK;
    }

    public boolean hasOutput() {
        return !output.isEmpty();
    }

    public boolean isAutoInject() {
        return autoInject;
    }

    public void toggleAutoInject() {
        autoInject = !autoInject;
        syncToClient();
    }

    public boolean isPaused() {
        return paused;
    }

    public void togglePaused() {
        paused = !paused;
        syncToClient();
    }

    public ItemStack extractOutput() {
        if (output.isEmpty()) return ItemStack.EMPTY;
        ItemStack out = output.split(output.getCount());
        if (output.isEmpty()) output = ItemStack.EMPTY;
        progress = 0;
        syncToClient();
        return out;
    }

    /** Keep menu quick-move extraction consistent with buttons, sneak-use, and hopper extraction. */
    public void completeMenuOutputExtraction() {
        if (progress == 0) return;
        progress = 0;
        syncToClient();
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
        progress = 0; // 与 extractOutput 行为一致
        syncToClient();
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
        if (slot == 0) {
            output = stack.copy();
            syncToClient();
        }
    }

    @Override
    public boolean stillValid(net.minecraft.world.entity.player.Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5) <= 64.0;
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
        return slot == 0 && !output.isEmpty() && side != Direction.UP;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        progress = Math.max(0, tag.getInt(TAG_PROGRESS));
        output = tag.contains(TAG_OUTPUT) ? ItemStack.of(tag.getCompound(TAG_OUTPUT)) : ItemStack.EMPTY;
        autoInject = !tag.contains(TAG_AUTO_INJECT) || tag.getBoolean(TAG_AUTO_INJECT);
        paused = tag.getBoolean(TAG_PAUSED);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(TAG_PROGRESS, progress);
        tag.putBoolean(TAG_AUTO_INJECT, autoInject);
        tag.putBoolean(TAG_PAUSED, paused);
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

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.cybercultivator.atmospheric_condenser");
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new AtmosphericCondenserMenu(containerId, inventory, this, menuData);
    }
}

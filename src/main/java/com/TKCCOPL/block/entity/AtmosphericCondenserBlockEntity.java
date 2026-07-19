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
import org.jetbrains.annotations.Nullable;

public class AtmosphericCondenserBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider, MachineRedstoneBlockEntity, MachineInventoryPolicy {
    private static final String TAG_PROGRESS = "Progress";
    private static final String TAG_OUTPUT = "Output";
    private static final String TAG_AUTO_INJECT = "AutoInject";
    private static final String TAG_PAUSED = "Paused";

    private static final int PRODUCTION_TIME = 600; // 30 seconds
    private static final int MAX_STACK = 32; // was 16

    // ContainerData 索引：0-5 为原有字段，6-8 为红石字段（mode/powered/processingAllowed）
    private static final int DATA_REDSTONE_BASE = 6;

    private int progress;
    private ItemStack output = ItemStack.EMPTY;
    private boolean autoInject = true;
    private boolean paused;

    /** v1.1.7 红石控制状态 */
    private final MachineRedstoneState redstone = new MachineRedstoneState();

    /** v1.1.7 比较器信号缓存，仅在变化时通知相邻比较器 */
    private int lastComparatorSignal = 0;

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
                // 红石字段（mode/powered/processingAllowed）
                case 6 -> redstone.getMenuData(MachineRedstoneState.DATA_MODE);
                case 7 -> redstone.getMenuData(MachineRedstoneState.DATA_POWERED);
                case 8 -> redstone.getMenuData(MachineRedstoneState.DATA_PROCESSING_ALLOWED);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 9;
        }
    };

    public AtmosphericCondenserBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ATMOSPHERIC_CONDENSER.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, AtmosphericCondenserBlockEntity blockEntity) {
        if (level.isClientSide) return;

        // v1.1.7 hotfix：执行 clearRemoved 推迟的红石重新采样
        if (blockEntity.redstone.consumePendingResample(level, pos)) {
            blockEntity.setChanged();
            level.sendBlockUpdated(pos, state, state, 2);
        }

        boolean changed = false;

        // v1.1.7 红石门控：产水推进受红石模式控制（与 paused 同级，互不覆盖）
        // Try to produce purified water bottle
        if (!blockEntity.paused && blockEntity.output.getCount() < MAX_STACK
                && blockEntity.redstone.isProcessingAllowed()) {
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

        // v1.1.7 比较器信号变化检测
        blockEntity.updateComparatorIfChanged(level, pos);
    }

    private void syncToClient() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
            // v1.1.7 hotfix：库存变化时立即刷新比较器
            updateComparatorIfChanged(level, worldPosition);
        }
    }

    /**
     * v1.1.7 比较器信号缓存检测：仅在数值变化时通知相邻比较器。
     * 避免每 tick 更新比较器网络造成的性能开销。
     */
    private void updateComparatorIfChanged(Level level, BlockPos pos) {
        int current = getComparatorSignal();
        if (current != lastComparatorSignal) {
            lastComparatorSignal = current;
            level.updateNeighbourForOutputSignal(pos, getBlockState().getBlock());
        }
    }

    // === v1.1.7 MachineRedstoneBlockEntity 接口实现 ===

    @Override
    public MachineRedstoneState getRedstoneState() {
        return redstone;
    }

    /**
     * v1.1.7 统一比较器三段语义（0/1-14/15）：
     * <ul>
     *   <li>{@code 15}：产水槽存在可抽取的纯净水瓶</li>
     *   <li>{@code 1..14}：产水中（{@code progress > 0}），按进度比例</li>
     *   <li>{@code 0}：无产物且未在产水</li>
     * </ul>
     */
    @Override
    public int getComparatorSignal() {
        if (!output.isEmpty()) return 15;
        if (progress <= 0) return 0;
        // 产水中：按 progress 比例映射到 1..14
        int max = PRODUCTION_TIME;
        if (max <= 0) return 1;
        int raw = (int) Math.ceil((double) progress * 14 / max);
        return Math.max(1, Math.min(14, raw));
    }

    /**
     * 区块加载时仅标记需要重新采样；实际采样在首次 tick 时执行。
     *
     * <p>v1.1.7 hotfix：在 post-load 阶段调用 {@code level.hasNeighborSignal}
     * 会触发相邻 chunk 加载，而 spawn area 生成期间 Server thread 自身被阻塞
     * 导致死锁。改为延迟到 tick，此时区块已完全加载。</p>
     */
    @Override
    public void clearRemoved() {
        super.clearRemoved();
        if (level != null && !level.isClientSide) {
            redstone.markPendingResample();
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
        return visibleSlots(side);
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canInsert(slot, stack, side);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return canExtract(slot, stack, side);
    }

    // === v1.1.7 §9 MachineInventoryPolicy 实现 ===
    // 分面矩阵（§9.1）：顶部=无能力槽；水平面/底部=纯净水只出。

    @Override
    public int[] visibleSlots(@Nullable Direction side) {
        // 顶部无能力槽；其他面（含 null）暴露唯一的输出槽
        if (side == Direction.UP) return new int[0];
        return new int[]{0};
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction side) {
        // 冷凝器无外部输入路径
        return false;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, @Nullable Direction side) {
        // null side 或非 UP 允许从输出槽抽取；UP 是生产侧拒绝抽取
        if (side == Direction.UP) return false;
        return slot == 0 && !output.isEmpty();
    }

    @Override
    public ItemStack normalizeInsertedStack(int slot, ItemStack stack) {
        // 冷凝器无外部插入路径，规范化无意义；仍返回 copy 以保持契约
        return stack.copy();
    }

    @Override
    public int getSlotLimit(int slot) {
        return MAX_STACK;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        progress = Math.max(0, tag.getInt(TAG_PROGRESS));
        output = tag.contains(TAG_OUTPUT) ? ItemStack.of(tag.getCompound(TAG_OUTPUT)) : ItemStack.EMPTY;
        autoInject = !tag.contains(TAG_AUTO_INJECT) || tag.getBoolean(TAG_AUTO_INJECT);
        paused = tag.getBoolean(TAG_PAUSED);
        // v1.1.7 红石状态（缺失字段默认 IGNORE，不崩溃）
        redstone.load(tag);
        // 比较器缓存重置，下次 tick 重新检测
        lastComparatorSignal = -1;
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
        // v1.1.7 红石状态持久化
        redstone.save(tag);
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

    // === v1.1.7 §9.5 IItemHandler capability（按 face→角色映射缓存，§9.6） ===

    private net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler> capUp =
            net.minecraftforge.common.util.LazyOptional.empty();
    private net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler> capHorizontal =
            net.minecraftforge.common.util.LazyOptional.empty();
    private net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler> capDown =
            net.minecraftforge.common.util.LazyOptional.empty();
    private net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler> capNull =
            net.minecraftforge.common.util.LazyOptional.empty();

    @Override
    public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(
            net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER) {
            net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler> handler;
            if (side == null) {
                if (!capNull.isPresent()) {
                    capNull = net.minecraftforge.common.util.LazyOptional.of(
                            () -> new SidedMachineItemHandler(this, this, null));
                }
                handler = capNull;
            } else if (side == Direction.UP) {
                if (!capUp.isPresent()) {
                    capUp = net.minecraftforge.common.util.LazyOptional.of(
                            () -> new SidedMachineItemHandler(this, this, Direction.UP));
                }
                handler = capUp;
            } else if (side == Direction.DOWN) {
                if (!capDown.isPresent()) {
                    capDown = net.minecraftforge.common.util.LazyOptional.of(
                            () -> new SidedMachineItemHandler(this, this, Direction.DOWN));
                }
                handler = capDown;
            } else {
                if (!capHorizontal.isPresent()) {
                    capHorizontal = net.minecraftforge.common.util.LazyOptional.of(
                            () -> new SidedMachineItemHandler(this, this, side));
                }
                handler = capHorizontal;
            }
            return handler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        capUp.invalidate();
        capHorizontal.invalidate();
        capDown.invalidate();
        capNull.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
    }
}

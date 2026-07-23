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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class AtmosphericCondenserBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider,
        MachineRedstoneBlockEntity, MachineInventoryPolicy {
    private static final String TAG_PROGRESS = "Progress";
    private static final String TAG_BOTTLE_INPUT = "BottleInput";
    private static final String TAG_OUTPUT = "Output";

    private static final int PRODUCTION_TIME = 600; // 30 seconds
    private static final int MAX_STACK = 16;
    public static final int BOTTLE_INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    private static final int[] INPUT_SLOTS = {BOTTLE_INPUT_SLOT};
    private static final int[] OUTPUT_SLOTS = {OUTPUT_SLOT};
    private static final int[] SIDE_SLOTS = {BOTTLE_INPUT_SLOT, OUTPUT_SLOT};

    private int progress;
    private ItemStack bottleInput = ItemStack.EMPTY;
    private ItemStack output = ItemStack.EMPTY;
    private final MachineRedstoneState redstone = new MachineRedstoneState();
    private int lastComparatorSignal;
    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> PRODUCTION_TIME;
                case 2 -> output.getCount();
                case 3 -> level != null
                        && level.getBlockEntity(worldPosition.below()) instanceof BioIncubatorBlockEntity ? 1 : 0;
                case 4 -> bottleInput.getCount();
                case 5 -> redstone.getMenuData(MachineRedstoneState.DATA_MODE);
                case 6 -> redstone.getMenuData(MachineRedstoneState.DATA_POWERED);
                case 7 -> redstone.getMenuData(MachineRedstoneState.DATA_PROCESSING_ALLOWED);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 5 + MachineRedstoneState.getMenuDataCount();
        }
    };

    public AtmosphericCondenserBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ATMOSPHERIC_CONDENSER.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, AtmosphericCondenserBlockEntity blockEntity) {
        if (level.isClientSide) return;

        if (blockEntity.redstone.consumePendingResample(level, pos)) {
            blockEntity.syncToClient();
        }

        boolean changed = false;

        // 只有空玻璃瓶可用且输出库存未满时才推进冷凝周期。
        if (blockEntity.redstone.isProcessingAllowed()
                && blockEntity.hasBottleInput()
                && blockEntity.output.getCount() < MAX_STACK) {
            blockEntity.progress++;
            // 每 20 tick 同步一次进度，用于客户端 HUD 进度条动画
            if (blockEntity.progress % 20 == 0) {
                changed = true;
            }
            if (blockEntity.progress >= PRODUCTION_TIME) {
                blockEntity.progress = 0;
                blockEntity.bottleInput.shrink(1);
                if (blockEntity.bottleInput.isEmpty()) {
                    blockEntity.bottleInput = ItemStack.EMPTY;
                }
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
                    incubator.addPurity(Config.purityInjectAmount);
                    blockEntity.output.shrink(1);
                    changed = true;
                }
            }
        }

        if (changed) {
            blockEntity.syncToClient();
        }
        blockEntity.updateComparatorIfChanged(level, pos);
    }

    private void syncToClient() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
            updateComparatorIfChanged(level, worldPosition);
        }
    }

    private void updateComparatorIfChanged(Level level, BlockPos pos) {
        int current = getComparatorSignal();
        if (current != lastComparatorSignal) {
            lastComparatorSignal = current;
            level.updateNeighbourForOutputSignal(pos, getBlockState().getBlock());
        }
    }

    @Override
    public MachineRedstoneState getRedstoneState() {
        return redstone;
    }

    @Override
    public int getComparatorSignal() {
        if (!output.isEmpty()) return 15;
        if (progress <= 0) return 0;
        return Math.max(1, Math.min(14,
                (int) Math.ceil((double) progress * 14 / PRODUCTION_TIME)));
    }

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

    public ItemStack getBottleInput() {
        return bottleInput.copy();
    }

    public int getBottleCount() {
        return bottleInput.getCount();
    }

    public boolean hasBottleInput() {
        return bottleInput.is(Items.GLASS_BOTTLE) && !bottleInput.isEmpty();
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

    public ItemStack extractOutput() {
        if (output.isEmpty()) return ItemStack.EMPTY;
        ItemStack out = output.split(output.getCount());
        if (output.isEmpty()) output = ItemStack.EMPTY;
        syncToClient();
        return out;
    }

    public ItemStack extractBottleInput() {
        if (bottleInput.isEmpty()) return ItemStack.EMPTY;
        ItemStack out = bottleInput.split(bottleInput.getCount());
        bottleInput = ItemStack.EMPTY;
        syncToClient();
        return out;
    }

    /** Keep menu quick-move extraction consistent with buttons, sneak-use, and hopper extraction. */
    public void completeMenuOutputExtraction() {
        // Output extraction no longer interrupts the next condensation cycle.
    }

    // WorldlyContainer implementation for hopper compatibility
    @Override
    public int getContainerSize() {
        return 2;
    }

    @Override
    public boolean isEmpty() {
        return bottleInput.isEmpty() && output.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return switch (slot) {
            case BOTTLE_INPUT_SLOT -> bottleInput;
            case OUTPUT_SLOT -> output;
            default -> ItemStack.EMPTY;
        };
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = getItem(slot);
        if (stack.isEmpty() || amount <= 0) return ItemStack.EMPTY;
        ItemStack result = stack.split(Math.min(amount, stack.getCount()));
        if (slot == BOTTLE_INPUT_SLOT && bottleInput.isEmpty()) {
            bottleInput = ItemStack.EMPTY;
        } else if (slot == OUTPUT_SLOT) {
            if (output.isEmpty()) output = ItemStack.EMPTY;
        }
        syncToClient();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack out;
        if (slot == BOTTLE_INPUT_SLOT) {
            out = bottleInput;
            bottleInput = ItemStack.EMPTY;
        } else if (slot == OUTPUT_SLOT) {
            out = output;
            output = ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }
        setChanged();
        return out;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot == BOTTLE_INPUT_SLOT) {
            bottleInput = stack.copy();
            if (bottleInput.getCount() > bottleInput.getMaxStackSize()) {
                bottleInput.setCount(bottleInput.getMaxStackSize());
            }
            syncToClient();
        } else if (slot == OUTPUT_SLOT) {
            output = stack.copy();
            if (output.getCount() > MAX_STACK) output.setCount(MAX_STACK);
            syncToClient();
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return canInsert(slot, stack, null);
    }

    @Override
    public boolean stillValid(net.minecraft.world.entity.player.Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clearContent() {
        bottleInput = ItemStack.EMPTY;
        output = ItemStack.EMPTY;
        setChanged();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return visibleSlots(side);
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return canInsert(slot, stack, side);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return canExtract(slot, stack, side);
    }

    @Override
    public int[] visibleSlots(@org.jetbrains.annotations.Nullable Direction side) {
        if (side == null) return SIDE_SLOTS;
        if (side == Direction.UP) return INPUT_SLOTS;
        if (side == Direction.DOWN) return OUTPUT_SLOTS;
        return SIDE_SLOTS;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @org.jetbrains.annotations.Nullable Direction side) {
        return slot == BOTTLE_INPUT_SLOT
                && stack.is(Items.GLASS_BOTTLE)
                && side != Direction.DOWN;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, @org.jetbrains.annotations.Nullable Direction side) {
        return slot == OUTPUT_SLOT && !output.isEmpty() && side != Direction.UP;
    }

    @Override
    public ItemStack normalizeInsertedStack(int slot, ItemStack stack) {
        return stack.copy();
    }

    @Override
    public int getSlotLimit(int slot) {
        return slot == OUTPUT_SLOT ? MAX_STACK : 64;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        progress = Math.max(0, tag.getInt(TAG_PROGRESS));
        bottleInput = tag.contains(TAG_BOTTLE_INPUT)
                ? ItemStack.of(tag.getCompound(TAG_BOTTLE_INPUT)) : ItemStack.EMPTY;
        output = tag.contains(TAG_OUTPUT) ? ItemStack.of(tag.getCompound(TAG_OUTPUT)) : ItemStack.EMPTY;
        redstone.load(tag);
        lastComparatorSignal = -1;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(TAG_PROGRESS, progress);
        if (!bottleInput.isEmpty()) {
            tag.put(TAG_BOTTLE_INPUT, bottleInput.save(new CompoundTag()));
        }
        if (!output.isEmpty()) {
            tag.put(TAG_OUTPUT, output.save(new CompoundTag()));
        }
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
            net.minecraftforge.common.capabilities.Capability<T> cap,
            @org.jetbrains.annotations.Nullable Direction side) {
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
}

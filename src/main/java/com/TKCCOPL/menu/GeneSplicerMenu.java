package com.TKCCOPL.menu;

import com.TKCCOPL.block.entity.GeneSplicerBlockEntity;
import com.TKCCOPL.init.ModBlocks;
import com.TKCCOPL.init.ModMenuTypes;
import com.TKCCOPL.item.GeneticSeedItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class GeneSplicerMenu extends MachineMenu implements RedstoneMenuAccess {
    /** v1.1.7 红石模式循环按钮 ID */
    public static final int BUTTON_CYCLE_REDSTONE = 0;
    private final ContainerData data;
    private final ContainerLevelAccess access;

    public GeneSplicerMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        this(containerId, inventory, resolve(inventory, buffer), new SimpleContainerData(8),
                ContainerLevelAccess.NULL);
    }

    public GeneSplicerMenu(int containerId, Inventory inventory, GeneSplicerBlockEntity blockEntity,
                           ContainerData data) {
        this(containerId, inventory, blockEntity, data,
                ContainerLevelAccess.create(inventory.player.level(), blockEntity.getBlockPos()));
    }

    private GeneSplicerMenu(int containerId, Inventory inventory, Container machine, ContainerData data,
                            ContainerLevelAccess access) {
        super(ModMenuTypes.GENE_SPLICER.get(), containerId, machine, 3);
        this.data = data;
        this.access = access;
        addSlot(seedSlot(machine, GeneSplicerBlockEntity.SEED_A_SLOT, 38, 48));
        addSlot(seedSlot(machine, GeneSplicerBlockEntity.SEED_B_SLOT, 74, 48));
        addSlot(new Slot(machine, GeneSplicerBlockEntity.OUTPUT_SLOT, 146, 48) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        addPlayerInventory(inventory);
        addDataSlots(data);
    }

    private static Slot seedSlot(Container container, int index, int x, int y) {
        return new Slot(container, index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof GeneticSeedItem && container.canPlaceItem(index, stack);
            }

            @Override public int getMaxStackSize() { return 1; }
        };
    }

    private static Container resolve(Inventory inventory, FriendlyByteBuf buffer) {
        if (inventory.player.level().getBlockEntity(buffer.readBlockPos()) instanceof GeneSplicerBlockEntity blockEntity) {
            return blockEntity;
        }
        return new SimpleContainer(3);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.GENE_SPLICER.get());
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_CYCLE_REDSTONE && machine instanceof GeneSplicerBlockEntity blockEntity) {
            if (blockEntity.getRedstoneState().cycleMode()) {
                blockEntity.setChanged();
                if (blockEntity.getLevel() != null) {
                    blockEntity.getLevel().sendBlockUpdated(blockEntity.getBlockPos(),
                            blockEntity.getBlockState(), blockEntity.getBlockState(), 2);
                }
            }
            return true;
        }
        return false;
    }

    public int getSpliceProgress() { return data.get(0); }
    public int getSpliceDuration() {
        int duration = data.get(1);
        return duration > 0 ? duration : GeneSplicerBlockEntity.SPLICE_DURATION_TICKS;
    }
    public boolean isSplicing() { return data.get(2) != 0; }
    public int getPredictedGeneration() { return data.get(3); }
    public int getPredictedMutationPermille() { return data.get(4); }

    public int getRemainingSeconds() {
        if (!isSplicing()) return 0;
        return Math.max(0, (getSpliceDuration() - getSpliceProgress() + 19) / 20);
    }

    // v1.1.7 红石字段 getter（ContainerData 索引 5/6/7）
    public int getRedstoneModeOrdinal() { return data.get(5); }
    public boolean isRedstonePowered() { return data.get(6) != 0; }
    public boolean isRedstoneProcessingAllowed() { return data.get(7) != 0; }

    @Override
    public int getRedstoneButtonId() { return BUTTON_CYCLE_REDSTONE; }
}

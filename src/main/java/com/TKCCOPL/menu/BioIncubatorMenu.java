package com.TKCCOPL.menu;

import com.TKCCOPL.block.entity.BioIncubatorBlockEntity;
import com.TKCCOPL.init.ModBlocks;
import com.TKCCOPL.init.ModItems;
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

public class BioIncubatorMenu extends MachineMenu implements RedstoneMenuAccess {
    /** v1.1.7 红石模式循环按钮 ID */
    public static final int BUTTON_CYCLE_REDSTONE = 0;
    private final ContainerData data;
    private final ContainerLevelAccess access;

    public BioIncubatorMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        this(containerId, inventory, resolve(inventory, buffer), new SimpleContainerData(8));
    }

    public BioIncubatorMenu(int containerId, Inventory inventory, BioIncubatorBlockEntity blockEntity,
                            ContainerData data) {
        this(containerId, inventory, blockEntity, data,
                ContainerLevelAccess.create(inventory.player.level(), blockEntity.getBlockPos()));
    }

    private BioIncubatorMenu(int containerId, Inventory inventory, Container machine, ContainerData data) {
        this(containerId, inventory, machine, data, ContainerLevelAccess.NULL);
    }

    private BioIncubatorMenu(int containerId, Inventory inventory, Container machine, ContainerData data,
                             ContainerLevelAccess access) {
        super(ModMenuTypes.BIO_INCUBATOR.get(), containerId, machine, 6);
        this.data = data;
        this.access = access;
        addSlot(new Slot(machine, BioIncubatorBlockEntity.SEED_SLOT, 30, 50) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof GeneticSeedItem; }
            @Override public int getMaxStackSize() { return 1; }
        });
        addSlot(new Slot(machine, BioIncubatorBlockEntity.NUTRITION_SLOT, 64, 50) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.is(ModItems.BIOCHEMICAL_SOLUTION.get()); }
        });
        addSlot(new Slot(machine, BioIncubatorBlockEntity.PURITY_SLOT, 94, 50) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.is(ModItems.PURIFIED_WATER_BOTTLE.get()); }
        });
        addSlot(new Slot(machine, BioIncubatorBlockEntity.SIGNAL_SLOT, 124, 50) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.is(ModItems.SILICON_SHARD.get()); }
        });
        addSlot(new Slot(machine, BioIncubatorBlockEntity.RESOURCE_OUTPUT_SLOT, 154, 50) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        addSlot(new Slot(machine, BioIncubatorBlockEntity.BOTTLE_OUTPUT_SLOT, 94, 74) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        addPlayerInventory(inventory);
        addDataSlots(data);
    }

    private static Container resolve(Inventory inventory, FriendlyByteBuf buffer) {
        if (inventory.player.level().getBlockEntity(buffer.readBlockPos()) instanceof BioIncubatorBlockEntity blockEntity) {
            return blockEntity;
        }
        return new SimpleContainer(6);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.BIO_INCUBATOR.get());
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_CYCLE_REDSTONE && machine instanceof BioIncubatorBlockEntity blockEntity) {
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

    public int getNutrition() { return data.get(0); }
    public int getPurity() { return data.get(1); }
    public int getDataSignal() { return data.get(2); }
    public int getGrowthPercent() { return data.get(3); }
    public int getEtaSeconds() { return data.get(4) - 1; }
    public boolean hasSeed() { return !machine.getItem(BioIncubatorBlockEntity.SEED_SLOT).isEmpty(); }
    public boolean hasResourceOutput() {
        return !machine.getItem(BioIncubatorBlockEntity.RESOURCE_OUTPUT_SLOT).isEmpty();
    }

    // v1.1.7 红石字段 getter（ContainerData 索引 5/6/7）
    public int getRedstoneModeOrdinal() { return data.get(5); }
    public boolean isRedstonePowered() { return data.get(6) != 0; }
    public boolean isRedstoneProcessingAllowed() { return data.get(7) != 0; }

    @Override
    public int getRedstoneButtonId() { return BUTTON_CYCLE_REDSTONE; }
}

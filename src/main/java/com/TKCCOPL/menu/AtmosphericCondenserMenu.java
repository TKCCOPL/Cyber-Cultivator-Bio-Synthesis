package com.TKCCOPL.menu;

import com.TKCCOPL.block.entity.AtmosphericCondenserBlockEntity;
import com.TKCCOPL.init.ModBlocks;
import com.TKCCOPL.init.ModMenuTypes;
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

public class AtmosphericCondenserMenu extends MachineMenu implements RedstoneMenuAccess {
    public static final int BUTTON_CYCLE_REDSTONE = 0;
    private final ContainerData data;
    private final ContainerLevelAccess access;

    public AtmosphericCondenserMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        this(containerId, inventory, resolve(inventory, buffer), new SimpleContainerData(8), ContainerLevelAccess.NULL);
    }

    public AtmosphericCondenserMenu(int containerId, Inventory inventory, AtmosphericCondenserBlockEntity blockEntity,
                                    ContainerData data) {
        this(containerId, inventory, blockEntity, data,
                ContainerLevelAccess.create(inventory.player.level(), blockEntity.getBlockPos()));
    }

    private AtmosphericCondenserMenu(int containerId, Inventory inventory, Container machine, ContainerData data,
                                     ContainerLevelAccess access) {
        super(ModMenuTypes.ATMOSPHERIC_CONDENSER.get(), containerId, machine, 2);
        this.data = data;
        this.access = access;
        addSlot(new Slot(machine, AtmosphericCondenserBlockEntity.BOTTLE_INPUT_SLOT, 18, 50) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return machine.canPlaceItem(AtmosphericCondenserBlockEntity.BOTTLE_INPUT_SLOT, stack);
            }
        });
        addSlot(outputSlot(machine, 158, 50));
        addPlayerInventory(inventory);
        addDataSlots(data);
    }

    private static Slot outputSlot(Container container, int x, int y) {
        return new Slot(container, AtmosphericCondenserBlockEntity.OUTPUT_SLOT, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }

            @Override
            public void onTake(Player player, ItemStack stack) {
                if (container instanceof AtmosphericCondenserBlockEntity blockEntity) {
                    blockEntity.completeMenuOutputExtraction();
                }
                super.onTake(player, stack);
            }
        };
    }

    private static Container resolve(Inventory inventory, FriendlyByteBuf buffer) {
        if (inventory.player.level().getBlockEntity(buffer.readBlockPos()) instanceof AtmosphericCondenserBlockEntity blockEntity) {
            return blockEntity;
        }
        return new SimpleContainer(2);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.ATMOSPHERIC_CONDENSER.get());
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_CYCLE_REDSTONE && machine instanceof AtmosphericCondenserBlockEntity blockEntity) {
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

    public int getProgress() { return data.get(0); }
    public int getMaxProgress() { return data.get(1); }
    public int getStock() { return data.get(2); }
    public boolean isDownstreamConnected() { return data.get(3) != 0; }
    public int getBottleCount() { return data.get(4); }
    public boolean hasBottle() { return getBottleCount() > 0; }
    public int getRedstoneModeOrdinal() { return data.get(5); }
    public boolean isRedstonePowered() { return data.get(6) != 0; }
    public boolean isRedstoneProcessingAllowed() { return data.get(7) != 0; }

    @Override
    public int getRedstoneButtonId() { return BUTTON_CYCLE_REDSTONE; }
}

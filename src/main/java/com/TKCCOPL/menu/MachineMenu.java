package com.TKCCOPL.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public abstract class MachineMenu extends AbstractContainerMenu {
    protected static final int PLAYER_INVENTORY_X = 16;
    protected static final int PLAYER_INVENTORY_Y = 128;
    protected final Container machine;
    private final int machineSlotCount;

    protected MachineMenu(@Nullable MenuType<?> type, int containerId, Container machine, int machineSlotCount) {
        super(type, containerId);
        this.machine = machine;
        this.machineSlotCount = machineSlotCount;
    }

    protected void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9,
                        PLAYER_INVENTORY_X + column * 18, PLAYER_INVENTORY_Y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, PLAYER_INVENTORY_X + column * 18, PLAYER_INVENTORY_Y + 58));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) {
            return ItemStack.EMPTY;
        }

        ItemStack original = slot.getItem();
        ItemStack copy = original.copy();
        if (index < machineSlotCount) {
            if (!moveItemStackTo(original, machineSlotCount, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(original, 0, machineSlotCount, false)) {
            return ItemStack.EMPTY;
        }

        if (original.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (original.getCount() == copy.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, original);
        return copy;
    }
}

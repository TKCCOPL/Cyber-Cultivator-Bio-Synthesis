package com.TKCCOPL.curios;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

final class AccessoryInventoryHelper {
    private AccessoryInventoryHelper() {
    }

    static boolean consumeOne(Player player, Item item, @Nullable Item remainder) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack slotStack = inventory.getItem(slot);
            if (slotStack.isEmpty() || !slotStack.is(item)) continue;

            slotStack.shrink(1);
            if (slotStack.isEmpty()) {
                inventory.setItem(slot, ItemStack.EMPTY);
            }

            if (remainder != null) {
                ItemStack remainderStack = new ItemStack(remainder);
                if (!inventory.add(remainderStack)) {
                    player.drop(remainderStack, false);
                }
            }
            inventory.setChanged();
            return true;
        }
        return false;
    }
}

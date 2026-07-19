package com.TKCCOPL.menu;

import com.TKCCOPL.block.entity.SerumBottlerBlockEntity;
import com.TKCCOPL.init.ModBlocks;
import com.TKCCOPL.init.ModMenuTypes;
import com.TKCCOPL.recipe.ModRecipeTypes;
import com.TKCCOPL.recipe.RecipeOrdering;
import com.TKCCOPL.recipe.SerumRecipe;
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
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class SerumBottlerMenu extends MachineMenu {
    private final ContainerData data;
    private final ContainerLevelAccess access;
    private final Level level;

    public SerumBottlerMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        this(containerId, inventory, resolve(inventory, buffer), new SimpleContainerData(3), ContainerLevelAccess.NULL);
    }

    public SerumBottlerMenu(int containerId, Inventory inventory, SerumBottlerBlockEntity blockEntity,
                            ContainerData data) {
        this(containerId, inventory, blockEntity, data,
                ContainerLevelAccess.create(inventory.player.level(), blockEntity.getBlockPos()));
    }

    private SerumBottlerMenu(int containerId, Inventory inventory, Container machine, ContainerData data,
                             ContainerLevelAccess access) {
        super(ModMenuTypes.SERUM_BOTTLER.get(), containerId, machine, 4);
        this.data = data;
        this.access = access;
        this.level = inventory.player.level();
        addSlot(inputSlot(machine, 0, 30, 50));
        addSlot(inputSlot(machine, 1, 54, 50));
        addSlot(inputSlot(machine, 2, 78, 50));
        addSlot(new Slot(machine, 3, 158, 50) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        addPlayerInventory(inventory);
        addDataSlots(data);
    }

    private static Slot inputSlot(Container container, int index, int x, int y) {
        return new Slot(container, index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return container.canPlaceItem(index, stack);
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                if (container instanceof SerumBottlerBlockEntity blockEntity
                        && blockEntity.getMaxProgress() > 0) {
                    blockEntity.cancelProcessing();
                }
                super.onTake(player, stack);
            }
        };
    }

    private static Container resolve(Inventory inventory, FriendlyByteBuf buffer) {
        if (inventory.player.level().getBlockEntity(buffer.readBlockPos()) instanceof SerumBottlerBlockEntity blockEntity) {
            return blockEntity;
        }
        return new SimpleContainer(4);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.SERUM_BOTTLER.get());
    }

    public int getProgress() { return data.get(0); }
    public int getMaxProgress() { return data.get(1); }
    public int getPredictedActivity() { return data.get(2); }
    public boolean isProcessing() { return getMaxProgress() > 0; }
    public boolean hasOutput() { return !machine.getItem(3).isEmpty(); }

    public int getOccupiedInputCount() {
        int occupied = 0;
        for (int slot = 0; slot < 3; slot++) {
            if (!machine.getItem(slot).isEmpty()) occupied++;
        }
        return occupied;
    }

    @Nullable
    public SerumRecipe getDisplayRecipe() {
        if (machine instanceof SerumBottlerBlockEntity blockEntity && blockEntity.getActiveRecipeId() != null) {
            var recipe = level.getRecipeManager().byKey(blockEntity.getActiveRecipeId()).orElse(null);
            if (recipe instanceof SerumRecipe serumRecipe) return serumRecipe;
        }
        SimpleContainer inputs = new SimpleContainer(3);
        for (int slot = 0; slot < 3; slot++) inputs.setItem(slot, machine.getItem(slot));
        return RecipeOrdering.sorted(level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.SERUM_BOTTLING.get()))
                .stream()
                .filter(recipe -> recipe.matches(inputs, level))
                .findFirst()
                .orElse(null);
    }
}

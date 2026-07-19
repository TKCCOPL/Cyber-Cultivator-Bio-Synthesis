package com.TKCCOPL.block.entity;

import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * v1.1.7 §9 通用工业自动化：分面 IItemHandler 包装器。
 *
 * <p>包装 {@link WorldlyContainer} + {@link MachineInventoryPolicy} + side，
 * 让 Forge 自动化模组通过统一 capability 接口访问机器库存。</p>
 *
 * <p>设计约束：
 * <ul>
 *   <li>§9.4 模拟操作（{@code simulate=true}）完全无副作用，不调用 setItem/removeItem，
 *       不触发 cancelProcessing/syncToClient，不调用 normalizeInsertedStack</li>
 *   <li>§9.4 实际操作（{@code simulate=false}）通过 {@link WorldlyContainer#setItem} /
 *       {@link WorldlyContainer#removeItem} 走机器标准入口（含同步、配方取消等副作用）</li>
 *   <li>§9.7 {@link #getStackInSlot(int)} 返回 {@link ItemStack#copy()}（防御性只读）</li>
 *   <li>§9.8 不检查调用线程，capability 层不拒绝非主线程调用</li>
 *   <li>§9.9 共享 {@link MachineInventoryPolicy} 谓词，与 WorldlyContainer 的
 *       {@code canPlaceItemThroughFace}/{@code canTakeItemThroughFace} 行为一致</li>
 * </ul>
 * </p>
 */
public class SidedMachineItemHandler implements IItemHandler {

    private final WorldlyContainer container;
    private final MachineInventoryPolicy policy;
    @Nullable
    private final Direction side;

    public SidedMachineItemHandler(WorldlyContainer container, MachineInventoryPolicy policy, @Nullable Direction side) {
        this.container = container;
        this.policy = policy;
        this.side = side;
    }

    @Override
    public int getSlots() {
        return policy.visibleSlots(side).length;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        int actual = resolveSlot(slot);
        if (actual < 0) return ItemStack.EMPTY;
        return container.getItem(actual).copy(); // §9.7 防御性只读
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        int actual = resolveSlot(slot);
        if (actual < 0) return stack;
        if (!policy.canInsert(actual, stack, side)) return stack;

        ItemStack existing = container.getItem(actual);
        // 非空槽必须物品+标签一致才允许合并，否则会覆盖原物品（物品丢失/复制）
        if (!existing.isEmpty() && !ItemStack.isSameItemSameTags(existing, stack)) {
            return stack;
        }
        int limit = Math.min(policy.getSlotLimit(actual), stack.getMaxStackSize());
        int currentCount = existing.getCount();
        if (currentCount >= limit) return stack;

        int toInsert = Math.min(limit - currentCount, stack.getCount());
        ItemStack remaining = stack.copy();
        remaining.shrink(toInsert);

        if (simulate) {
            // §9.4 模拟路径不修改库存、不调用 normalizeInsertedStack、不触发 syncToClient/cancelProcessing
            return remaining;
        }

        // 空槽首次插入走 normalize（保留 instanceof GeneticSeedItem NBT 品质检查）；
        // 非空槽合并基于 existing.copy() 增长，避免覆盖原物品
        ItemStack target = existing.isEmpty()
                ? policy.normalizeInsertedStack(actual, stack)
                : existing.copy();
        target.setCount(currentCount + toInsert);
        // 走 BE 标准入口（含 syncToClient、cancelProcessing 等副作用）
        container.setItem(actual, target);
        return remaining;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0) return ItemStack.EMPTY;
        int actual = resolveSlot(slot);
        if (actual < 0) return ItemStack.EMPTY;

        ItemStack existing = container.getItem(actual);
        if (existing.isEmpty()) return ItemStack.EMPTY;
        if (!policy.canExtract(actual, existing, side)) return ItemStack.EMPTY;

        int toExtract = Math.min(Math.min(amount, existing.getCount()), existing.getMaxStackSize());
        ItemStack extracted = existing.copy();
        extracted.setCount(toExtract);

        if (simulate) {
            // §9.4 模拟路径不修改库存、不触发 syncToClient/cancelProcessing
            return extracted;
        }

        // 实际抽取通过 removeItem 走 BE 标准入口（含进度清零等机器特定副作用）
        return container.removeItem(actual, toExtract);
    }

    @Override
    public int getSlotLimit(int slot) {
        int actual = resolveSlot(slot);
        if (actual < 0) return 0;
        return policy.getSlotLimit(actual);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (stack.isEmpty()) return false;
        int actual = resolveSlot(slot);
        if (actual < 0) return false;
        return policy.canInsert(actual, stack, side);
    }

    private int resolveSlot(int externalSlot) {
        int[] slots = policy.visibleSlots(side);
        if (externalSlot < 0 || externalSlot >= slots.length) return -1;
        return slots[externalSlot];
    }
}

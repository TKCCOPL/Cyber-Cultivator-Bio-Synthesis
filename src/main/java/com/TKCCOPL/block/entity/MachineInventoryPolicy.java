package com.TKCCOPL.block.entity;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * v1.1.7 §9 通用工业自动化：槽位级权限策略。
 *
 * <p>由支持自动化接入的机器 BE 实现，统一 {@link net.minecraft.world.WorldlyContainer}
 * 与 {@link net.minecraftforge.items.IItemHandler} 的插入/抽取权限判定（§9.9 一致性）。</p>
 *
 * <p>所有方法必须保持纯查询语义，不得修改库存、进度、配方缓存或红石状态。
 * 模拟操作（{@code simulate=true}）只读取谓词结果，不触发任何副作用。</p>
 *
 * <p>实现类应将 {@link net.minecraft.world.WorldlyContainer#canPlaceItemThroughFace}
 * 与 {@link net.minecraft.world.WorldlyContainer#canTakeItemThroughFace} 委托给
 * {@link #canInsert} 与 {@link #canExtract}，确保漏斗路径与 capability 路径行为一致。</p>
 */
public interface MachineInventoryPolicy {

    /**
     * 该 side 可见的槽位索引数组。
     *
     * <p>与 {@link net.minecraft.world.WorldlyContainer#getSlotsForFace} 一致；
     * 用于 {@link SidedMachineItemHandler#getSlots} 与外部槽位映射。</p>
     */
    int[] visibleSlots(@Nullable Direction side);

    /**
     * 通过 side 将 stack 插入到 slot 是否合法。
     *
     * <p>实现应包含物品类型校验（如 {@code GeneticSeedItem}）、机器状态校验
     * （如不在加工中、输出槽为空）等业务规则。</p>
     */
    boolean canInsert(int slot, ItemStack stack, @Nullable Direction side);

    /**
     * 通过 side 从 slot 抽取 stack 是否合法。
     *
     * <p>实现应包含输出/输入槽区分、方向限制（如仅 DOWN 抽取输出）等业务规则。</p>
     */
    boolean canExtract(int slot, ItemStack stack, @Nullable Direction side);

    /**
     * 规范化待插入物品。
     *
     * <p>例如种子补齐基因 NBT、强制 {@code count=1} 等。仅在 {@code simulate=false}
     * 的实际插入路径调用；模拟路径不应触发此方法以保持无副作用（§9.4）。</p>
     *
     * @return 规范化后的物品（count 已包含栈内现有数量与新增数量）
     */
    ItemStack normalizeInsertedStack(int slot, ItemStack stack);

    /**
     * 槽位容量上限。
     *
     * <p>与 {@link net.minecraft.world.Container#getMaxStackSize} 语义对齐，
     * 但允许按槽位差异化（如种子槽返回 1，输入槽返回 64）。</p>
     */
    int getSlotLimit(int slot);
}

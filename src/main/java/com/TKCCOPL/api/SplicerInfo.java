package com.TKCCOPL.api;

import net.minecraft.world.item.ItemStack;

/** 拼接机状态（只读快照） */
public record SplicerInfo(
    ItemStack seedA, ItemStack seedB,
    ItemStack output, boolean hasOutput,
    int inputCount
) {
    public SplicerInfo {
        seedA = seedA == null ? ItemStack.EMPTY : seedA.copy();
        seedB = seedB == null ? ItemStack.EMPTY : seedB.copy();
        output = output == null ? ItemStack.EMPTY : output.copy();
    }

    @Override
    public ItemStack seedA() { return seedA.copy(); }

    @Override
    public ItemStack seedB() { return seedB.copy(); }

    @Override
    public ItemStack output() { return output.copy(); }
}

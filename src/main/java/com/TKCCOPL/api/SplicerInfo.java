package com.TKCCOPL.api;

import net.minecraft.world.item.ItemStack;

/** 拼接机状态（只读快照） */
public record SplicerInfo(
    ItemStack seedA, ItemStack seedB,
    ItemStack output, boolean hasOutput,
    int inputCount
) {
    public SplicerInfo {
        if (seedA != null) seedA = seedA.copy();
        if (seedB != null) seedB = seedB.copy();
        if (output != null) output = output.copy();
    }
}

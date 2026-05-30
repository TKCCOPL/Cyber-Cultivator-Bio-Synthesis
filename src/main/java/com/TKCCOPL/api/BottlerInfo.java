package com.TKCCOPL.api;

import net.minecraft.world.item.ItemStack;

/** 灌装机状态（只读快照） */
public record BottlerInfo(
    int progress, int maxProgress,
    int activeRecipe, ItemStack output,
    int activity
) {
    public BottlerInfo {
        if (output != null) output = output.copy();
    }
}

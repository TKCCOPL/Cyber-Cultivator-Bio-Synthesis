package com.TKCCOPL.api;

import net.minecraft.world.item.ItemStack;

/** 培养槽状态（只读快照） */
public record IncubatorInfo(
    int nutrition, int purity, int dataSignal,
    int growthPercent, int estimatedSeconds,
    boolean hasSeed, ItemStack seed
) {
    public IncubatorInfo {
        if (seed != null) seed = seed.copy(); // defensive copy
    }
}

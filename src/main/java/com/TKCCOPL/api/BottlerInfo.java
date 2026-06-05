package com.TKCCOPL.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** 灌装机状态（只读快照） */
public record BottlerInfo(
    int progress, int maxProgress,
    ResourceLocation activeRecipeId, ItemStack output,
    int activity
) {
    public BottlerInfo {
        if (output != null) output = output.copy();
    }

    /** @deprecated 使用 {@link #activeRecipeId()} 替代 */
    @Deprecated
    public int activeRecipe() { return -1; }
}

package com.TKCCOPL.event;

import com.TKCCOPL.recipe.SerumRecipeIds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.Event;

/**
 * 血清灌装机配方完成时触发。
 * 监听此事件可修改输出物品或 Activity 值。
 */
public class SerumCraftEvent extends Event {
    private final ItemStack[] inputs;
    private ItemStack output;
    private int activity;
    private final ResourceLocation recipeId;

    public SerumCraftEvent(ItemStack[] inputs, ItemStack output, int activity, ResourceLocation recipeId) {
        this.inputs = copyStacks(inputs);
        this.output = output == null ? ItemStack.EMPTY : output;
        this.activity = activity;
        this.recipeId = recipeId;
    }

    public ItemStack[] getInputs() { return copyStacks(inputs); }

    public ItemStack getOutput() { return output; }
    public void setOutput(ItemStack output) { this.output = output == null ? ItemStack.EMPTY : output; }

    public int getActivity() { return activity; }
    public void setActivity(int activity) { this.activity = activity; }

    public ResourceLocation getRecipeId() { return recipeId; }

    /** @deprecated 使用 {@link #getRecipeId()} 替代 */
    @Deprecated
    public int getRecipeIndex() { return SerumRecipeIds.legacyIndex(recipeId); }

    private static ItemStack[] copyStacks(ItemStack[] stacks) {
        if (stacks == null) return new ItemStack[0];
        ItemStack[] copy = new ItemStack[stacks.length];
        for (int i = 0; i < stacks.length; i++) {
            copy[i] = stacks[i] == null ? ItemStack.EMPTY : stacks[i].copy();
        }
        return copy;
    }

    @Override
    public boolean isCancelable() { return true; }
}

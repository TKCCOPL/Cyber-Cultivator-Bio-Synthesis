package com.TKCCOPL.event;

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
        this.inputs = inputs;
        this.output = output;
        this.activity = activity;
        this.recipeId = recipeId;
    }

    public ItemStack[] getInputs() { return inputs; }

    public ItemStack getOutput() { return output; }
    public void setOutput(ItemStack output) { this.output = output; }

    public int getActivity() { return activity; }
    public void setActivity(int activity) { this.activity = activity; }

    public ResourceLocation getRecipeId() { return recipeId; }

    /** @deprecated 使用 {@link #getRecipeId()} 替代 */
    @Deprecated
    public int getRecipeIndex() { return -1; }

    @Override
    public boolean isCancelable() { return true; }
}

package com.TKCCOPL.event;

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
    private final int recipeIndex;

    public SerumCraftEvent(ItemStack[] inputs, ItemStack output, int activity, int recipeIndex) {
        this.inputs = inputs;
        this.output = output;
        this.activity = activity;
        this.recipeIndex = recipeIndex;
    }

    public ItemStack[] getInputs() { return inputs; }

    public ItemStack getOutput() { return output; }
    public void setOutput(ItemStack output) { this.output = output; }

    public int getActivity() { return activity; }
    public void setActivity(int activity) { this.activity = activity; }

    public int getRecipeIndex() { return recipeIndex; }

    @Override
    public boolean isCancelable() { return true; }
}

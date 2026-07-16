package com.TKCCOPL.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.Event;

/**
 * 作物成熟时触发。
 * 监听此事件可修改产出物品。
 */
public class CropMatureEvent extends Event {
    private final Level level;
    private final BlockPos pos;
    private final ItemStack seed;
    private ItemStack output;

    public CropMatureEvent(Level level, BlockPos pos, ItemStack seed, ItemStack output) {
        this.level = level;
        this.pos = pos;
        this.seed = seed.copy(); // 防御性拷贝，防止监听器污染内部状态
        this.output = output == null ? ItemStack.EMPTY : output;
    }

    public Level getLevel() { return level; }
    public BlockPos getPos() { return pos; }
    public ItemStack getSeed() { return seed; }

    public ItemStack getOutput() { return output; }
    public void setOutput(ItemStack output) { this.output = output == null ? ItemStack.EMPTY : output; }

    @Override
    public boolean isCancelable() { return true; }
}

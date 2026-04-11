package com.TKCCOPL.block;

import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Supplier;

public class BasicCropBlock extends CropBlock {
    private final Supplier<? extends ItemLike> seedItem;

    public BasicCropBlock(BlockBehaviour.Properties properties, Supplier<? extends ItemLike> seedItem) {
        super(properties);
        this.seedItem = seedItem;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return seedItem.get();
    }
}

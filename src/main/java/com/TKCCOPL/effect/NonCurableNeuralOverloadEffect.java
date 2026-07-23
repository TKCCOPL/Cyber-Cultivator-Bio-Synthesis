package com.TKCCOPL.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Shared base for overload effects that ordinary curative items such as milk cannot remove. */
public abstract class NonCurableNeuralOverloadEffect extends MobEffect {
    protected NonCurableNeuralOverloadEffect(int color) {
        super(MobEffectCategory.HARMFUL, color);
    }

    @Override
    public List<ItemStack> getCurativeItems() {
        return List.of();
    }
}

package com.TKCCOPL.event;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.Event;

/**
 * 血清饮用时触发（在效果施加之前）。
 * 监听此事件可修改效果参数或取消效果施加。
 */
public class SerumConsumeEvent extends Event {
    private final LivingEntity entity;
    private final ItemStack serum;
    private final MobEffect effect;
    private int activity;
    private int duration;
    private int amplifier;

    public SerumConsumeEvent(LivingEntity entity, ItemStack serum, MobEffect effect,
                             int activity, int duration, int amplifier) {
        this.entity = entity;
        this.serum = serum.copy();
        this.effect = effect;
        this.activity = activity;
        this.duration = duration;
        this.amplifier = amplifier;
    }

    public LivingEntity getEntity() { return entity; }
    public ItemStack getSerum() { return serum; }
    public MobEffect getEffect() { return effect; }

    public int getActivity() { return activity; }
    public void setActivity(int activity) { this.activity = activity; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public int getAmplifier() { return amplifier; }
    public void setAmplifier(int amplifier) { this.amplifier = amplifier; }

    @Override
    public boolean isCancelable() { return true; }
}

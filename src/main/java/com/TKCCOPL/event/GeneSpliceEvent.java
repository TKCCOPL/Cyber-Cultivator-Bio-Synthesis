package com.TKCCOPL.event;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.Event;

/**
 * 基因拼接完成时触发。
 * 监听此事件可修改子代基因、突变结果等。
 */
public class GeneSpliceEvent extends Event {
    private final ItemStack seedA;
    private final ItemStack seedB;
    private int speed;
    private int yield;
    private int potency;
    private int synergy;
    private int generation;
    private boolean isMutation;
    private int mutationType;
    private String mutationDetail;
    private int offspringCount;

    public GeneSpliceEvent(ItemStack seedA, ItemStack seedB,
                           int speed, int yield, int potency,
                           int synergy, int generation,
                           boolean isMutation, int mutationType, String mutationDetail) {
        this(seedA, seedB, speed, yield, potency, synergy, generation,
                isMutation, mutationType, mutationDetail, 1);
    }

    public GeneSpliceEvent(ItemStack seedA, ItemStack seedB,
                           int speed, int yield, int potency,
                           int synergy, int generation,
                           boolean isMutation, int mutationType, String mutationDetail,
                           int offspringCount) {
        this.seedA = seedA == null ? ItemStack.EMPTY : seedA.copy();
        this.seedB = seedB == null ? ItemStack.EMPTY : seedB.copy();
        this.speed = speed;
        this.yield = yield;
        this.potency = potency;
        this.synergy = synergy;
        this.generation = generation;
        this.isMutation = isMutation;
        this.mutationType = mutationType;
        this.mutationDetail = mutationDetail;
        this.offspringCount = offspringCount;
    }

    public ItemStack getSeedA() { return seedA.copy(); }
    public ItemStack getSeedB() { return seedB.copy(); }

    public int getSpeed() { return speed; }
    public void setSpeed(int speed) { this.speed = speed; }

    public int getYield() { return yield; }
    public void setYield(int yield) { this.yield = yield; }

    public int getPotency() { return potency; }
    public void setPotency(int potency) { this.potency = potency; }

    public int getSynergy() { return synergy; }
    public void setSynergy(int synergy) { this.synergy = synergy; }

    public int getGeneration() { return generation; }
    public void setGeneration(int generation) { this.generation = generation; }

    public boolean isMutation() { return isMutation; }
    public void setMutation(boolean mutation) { isMutation = mutation; }

    public int getMutationType() { return mutationType; }
    public void setMutationType(int mutationType) { this.mutationType = mutationType; }

    public String getMutationDetail() { return mutationDetail; }
    public void setMutationDetail(String mutationDetail) { this.mutationDetail = mutationDetail; }

    public int getOffspringCount() { return offspringCount; }
    public void setOffspringCount(int offspringCount) { this.offspringCount = offspringCount; }

    @Override
    public boolean isCancelable() { return true; }
}

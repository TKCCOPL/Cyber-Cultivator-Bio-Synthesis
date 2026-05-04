package com.TKCCOPL.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public class GeneticSeedItem extends ItemNameBlockItem {
    public static final String GENE_SPEED = "Gene_Speed";
    public static final String GENE_YIELD = "Gene_Yield";
    public static final String GENE_POTENCY = "Gene_Potency";
    public static final String GENE_GENERATION = "Gene_Generation";
    public static final String GENE_SYNERGY = "Gene_Synergy";

    private final int defaultSpeed;
    private final int defaultYield;
    private final int defaultPotency;

    public GeneticSeedItem(Block block, Properties properties, int defaultSpeed, int defaultYield, int defaultPotency) {
        super(block, properties);
        this.defaultSpeed = clampGene(defaultSpeed);
        this.defaultYield = clampGene(defaultYield);
        this.defaultPotency = clampGene(defaultPotency);
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        ensureGeneData(stack);
        return stack;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        ensureGeneData(stack);
        super.inventoryTick(stack, level, entity, slotId, isSelected);
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        ensureGeneData(stack);
        super.onCraftedBy(stack, level, player);
    }

    @Override
    public net.minecraft.world.InteractionResult useOn(UseOnContext context) {
        ensureGeneData(context.getItemInHand());
        return super.useOn(context);
    }

    public void ensureGeneData(ItemStack stack) {
        CompoundTag existing = stack.getTag();
        if (existing != null && existing.contains(GENE_SPEED) && existing.contains(GENE_GENERATION)) return;
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains(GENE_SPEED)) {
            tag.putInt(GENE_SPEED, defaultSpeed);
        }
        if (!tag.contains(GENE_YIELD)) {
            tag.putInt(GENE_YIELD, defaultYield);
        }
        if (!tag.contains(GENE_POTENCY)) {
            tag.putInt(GENE_POTENCY, defaultPotency);
        }
        if (!tag.contains(GENE_GENERATION)) {
            tag.putInt(GENE_GENERATION, 0);
        }
    }

    public static int getGene(ItemStack stack, String key) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(key)) {
            return 1;
        }
        return clampGene(tag.getInt(key));
    }

    public static void setGene(ItemStack stack, String key, int value) {
        stack.getOrCreateTag().putInt(key, clampGene(value));
    }

    public static void setGenes(ItemStack stack, int speed, int yield, int potency) {
        setGene(stack, GENE_SPEED, speed);
        setGene(stack, GENE_YIELD, yield);
        setGene(stack, GENE_POTENCY, potency);
    }

    public static int clampGene(int value) {
        return Math.max(1, Math.min(10, value));
    }

    public static int getGeneration(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(GENE_GENERATION)) return 0;
        return tag.getInt(GENE_GENERATION);
    }

    public static int getSynergy(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(GENE_SYNERGY)) return 0;
        return Math.max(0, Math.min(10, tag.getInt(GENE_SYNERGY)));
    }
}
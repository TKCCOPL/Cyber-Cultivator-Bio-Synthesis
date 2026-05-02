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
        if (stack.hasTag() && stack.getTag().contains(GENE_SPEED)) return;
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
}
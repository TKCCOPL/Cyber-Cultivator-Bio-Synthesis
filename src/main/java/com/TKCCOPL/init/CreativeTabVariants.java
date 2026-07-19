package com.TKCCOPL.init;

import com.TKCCOPL.item.GeneticSeedItem;
import com.TKCCOPL.item.SynapticSerumItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class CreativeTabVariants {
    private static final int MIN_MATERIAL_QUALITY = 1;
    private static final int MAX_MATERIAL_QUALITY = 10;
    private static final int DEFAULT_MATERIAL_QUALITY = 5;

    private CreativeTabVariants() {
    }

    static void addDefaultQualityItems(CreativeModeTab.Output output) {
        output.accept(withIntTag(ModItems.PLANT_FIBER.get(), "Potency", DEFAULT_MATERIAL_QUALITY));
        output.accept(withIntTag(ModItems.INDUSTRIAL_ETHANOL.get(), "Purity", DEFAULT_MATERIAL_QUALITY));
        output.accept(withIntTag(ModItems.BIOCHEMICAL_SOLUTION.get(), "Concentration", DEFAULT_MATERIAL_QUALITY));
        output.accept(activityStack(ModItems.SYNAPTIC_NEURAL_BERRY.get(), SynapticSerumItem.DEFAULT_ACTIVITY));
        output.accept(activityStack(ModItems.SYNAPTIC_SERUM_S01.get(), SynapticSerumItem.DEFAULT_ACTIVITY));
        output.accept(activityStack(ModItems.SYNAPTIC_SERUM_S02.get(), SynapticSerumItem.DEFAULT_ACTIVITY));
        output.accept(activityStack(ModItems.SYNAPTIC_SERUM_S03.get(), SynapticSerumItem.DEFAULT_ACTIVITY));
    }

    public static void addBalancedSeedVariants(CreativeModeTab.Output output) {
        for (int geneLevel = MIN_MATERIAL_QUALITY; geneLevel <= MAX_MATERIAL_QUALITY; geneLevel++) {
            output.accept(balancedSeedStack(ModItems.FIBER_REED_SEEDS.get(), geneLevel));
            output.accept(balancedSeedStack(ModItems.PROTEIN_SOY_SEEDS.get(), geneLevel));
            output.accept(balancedSeedStack(ModItems.ALCOHOL_BLOOM_SEEDS.get(), geneLevel));
        }
    }

    public static void addMaterialVariants(CreativeModeTab.Output output) {
        for (int quality = MIN_MATERIAL_QUALITY; quality <= MAX_MATERIAL_QUALITY; quality++) {
            output.accept(withIntTag(ModItems.PLANT_FIBER.get(), "Potency", quality));
            output.accept(withIntTag(ModItems.INDUSTRIAL_ETHANOL.get(), "Purity", quality));
            output.accept(withIntTag(ModItems.BIOCHEMICAL_SOLUTION.get(), "Concentration", quality));
        }
    }

    public static void addActivityVariants(CreativeModeTab.Output output) {
        for (int activity = SynapticSerumItem.MIN_ACTIVITY; activity <= SynapticSerumItem.MAX_ACTIVITY; activity++) {
            output.accept(activityStack(ModItems.SYNAPTIC_NEURAL_BERRY.get(), activity));
        }
        for (int activity = SynapticSerumItem.MIN_ACTIVITY; activity <= SynapticSerumItem.MAX_ACTIVITY; activity++) {
            output.accept(activityStack(ModItems.SYNAPTIC_SERUM_S01.get(), activity));
            output.accept(activityStack(ModItems.SYNAPTIC_SERUM_S02.get(), activity));
            output.accept(activityStack(ModItems.SYNAPTIC_SERUM_S03.get(), activity));
        }
    }

    static ItemStack activityStack(Item item, int activity) {
        return withIntTag(item, "SynapticActivity", SynapticSerumItem.clampActivity(activity));
    }

    private static ItemStack balancedSeedStack(Item item, int geneLevel) {
        ItemStack stack = item.getDefaultInstance();
        GeneticSeedItem.setGenes(stack, geneLevel, geneLevel, geneLevel);
        return stack;
    }

    private static ItemStack withIntTag(Item item, String key, int value) {
        ItemStack stack = new ItemStack(item);
        stack.getOrCreateTag().putInt(key, value);
        return stack;
    }
}

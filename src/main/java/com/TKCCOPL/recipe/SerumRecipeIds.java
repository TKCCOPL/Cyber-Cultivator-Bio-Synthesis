package com.TKCCOPL.recipe;

import com.TKCCOPL.cybercultivator;
import net.minecraft.resources.ResourceLocation;

/** Stable IDs and legacy indices for the four built-in serum recipes. */
public final class SerumRecipeIds {
    public static final ResourceLocation BERRY_SYNTHESIS = id("serum/berry_synthesis");
    public static final ResourceLocation S01_BOTTLING = id("serum/s01_bottling");
    public static final ResourceLocation S02_BOTTLING = id("serum/s02_bottling");
    public static final ResourceLocation S03_BOTTLING = id("serum/s03_bottling");

    private SerumRecipeIds() {}

    public static int legacyIndex(ResourceLocation recipeId) {
        if (BERRY_SYNTHESIS.equals(recipeId)) return 0;
        if (S01_BOTTLING.equals(recipeId)) return 1;
        if (S02_BOTTLING.equals(recipeId)) return 2;
        if (S03_BOTTLING.equals(recipeId)) return 3;
        return -1;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(cybercultivator.MODID, path);
    }
}

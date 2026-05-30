package com.TKCCOPL.recipe;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.crafting.CraftingHelper;

import javax.annotation.Nullable;

public class IncubatorOutputSerializer implements net.minecraft.world.item.crafting.RecipeSerializer<IncubatorOutputRecipe> {
    @Override
    public IncubatorOutputRecipe fromJson(ResourceLocation id, JsonObject json) {
        JsonObject seedJson = GsonHelper.getAsJsonObject(json, "seed");
        ItemStack seedItem = CraftingHelper.getItemStack(seedJson, true);

        JsonObject outputJson = GsonHelper.getAsJsonObject(json, "output");
        ItemStack outputItem = CraftingHelper.getItemStack(outputJson, true);

        String countFormula = GsonHelper.getAsString(json, "count_formula", "2 + yield / 3");
        String qualityTag = GsonHelper.getAsString(json, "quality_tag", "");
        String cropName = GsonHelper.getAsString(json, "crop_name", "");

        JsonObject genesJson = GsonHelper.getAsJsonObject(json, "default_genes");
        int[] defaultGenes = {
            GsonHelper.getAsInt(genesJson, "speed", 5),
            GsonHelper.getAsInt(genesJson, "yield", 5),
            GsonHelper.getAsInt(genesJson, "potency", 5)
        };

        return new IncubatorOutputRecipe(id, seedItem, outputItem, countFormula, qualityTag, defaultGenes, cropName);
    }

    @Nullable
    @Override
    public IncubatorOutputRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
        ItemStack seedItem = buf.readItem();
        ItemStack outputItem = buf.readItem();
        String countFormula = buf.readUtf();
        String qualityTag = buf.readUtf();
        String cropName = buf.readUtf();
        int[] defaultGenes = { buf.readVarInt(), buf.readVarInt(), buf.readVarInt() };
        return new IncubatorOutputRecipe(id, seedItem, outputItem, countFormula, qualityTag, defaultGenes, cropName);
    }

    @Override
    public void toNetwork(FriendlyByteBuf buf, IncubatorOutputRecipe recipe) {
        buf.writeItem(recipe.getSeedItem());
        buf.writeItem(recipe.getOutputItem());
        buf.writeUtf(recipe.getCountFormula());
        buf.writeUtf(recipe.getQualityTag());
        buf.writeUtf(recipe.getCropName());
        for (int gene : recipe.getDefaultGenes()) {
            buf.writeVarInt(gene);
        }
    }
}

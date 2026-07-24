package com.TKCCOPL.recipe;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.crafting.CraftingHelper;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.Set;

public class IncubatorOutputSerializer implements net.minecraft.world.item.crafting.RecipeSerializer<IncubatorOutputRecipe> {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** 合法 quality_tag 白名单（空字符串表示不写入品质标签） */
    private static final Set<String> VALID_QUALITY_TAGS = Set.of("", "Potency", "Purity", "Concentration");

    @Override
    public IncubatorOutputRecipe fromJson(ResourceLocation id, JsonObject json) {
        Ingredient seed = Ingredient.fromJson(json.get("seed"));

        JsonObject outputJson = GsonHelper.getAsJsonObject(json, "output");
        ItemStack outputItem = CraftingHelper.getItemStack(outputJson, true);

        String countFormula = GsonHelper.getAsString(json, "count_formula", "2 + yield / 3");
        String qualityTag = GsonHelper.getAsString(json, "quality_tag", "");
        String cropName = GsonHelper.getAsString(json, "crop_name", "");

        JsonObject genesJson = GsonHelper.getAsJsonObject(json, "default_genes", new JsonObject());
        // 基因值合法范围 1..10，与 GeneticSeedItem.clampGene 一致；越界值在此处夹紧并记录警告
        int[] defaultGenes = {
            clampGene(id, "speed", GsonHelper.getAsInt(genesJson, "speed", 5)),
            clampGene(id, "yield", GsonHelper.getAsInt(genesJson, "yield", 5)),
            clampGene(id, "potency", GsonHelper.getAsInt(genesJson, "potency", 5))
        };

        if (!VALID_QUALITY_TAGS.contains(qualityTag)) {
            LOGGER.warn("[IncubatorOutputSerializer] Recipe {} declares unknown quality_tag '{}'; expected one of {}, fallback to empty (no quality tag written)",
                    id, qualityTag, VALID_QUALITY_TAGS);
            qualityTag = "";
        }

        int priority = GsonHelper.getAsInt(json, "priority", 0);
        return new IncubatorOutputRecipe(id, seed, outputItem, countFormula, qualityTag, defaultGenes, cropName, priority);
    }

    private static int clampGene(ResourceLocation id, String name, int value) {
        if (value < 1 || value > 10) {
            LOGGER.warn("[IncubatorOutputSerializer] Recipe {} default_genes.{}={} out of range [1,10]; clamped", id, name, value);
            return Math.max(1, Math.min(10, value));
        }
        return value;
    }

    @Nullable
    @Override
    public IncubatorOutputRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
        Ingredient seed = Ingredient.fromNetwork(buf);
        ItemStack outputItem = buf.readItem();
        String countFormula = buf.readUtf();
        String qualityTag = buf.readUtf();
        String cropName = buf.readUtf();
        int[] defaultGenes = { buf.readVarInt(), buf.readVarInt(), buf.readVarInt() };
        int priority = buf.readVarInt();
        return new IncubatorOutputRecipe(id, seed, outputItem, countFormula, qualityTag, defaultGenes, cropName, priority);
    }

    @Override
    public void toNetwork(FriendlyByteBuf buf, IncubatorOutputRecipe recipe) {
        recipe.getSeedIngredient().toNetwork(buf);
        buf.writeItem(recipe.getOutputItem());
        buf.writeUtf(recipe.getCountFormula());
        buf.writeUtf(recipe.getQualityTag());
        buf.writeUtf(recipe.getCropName());
        for (int gene : recipe.getDefaultGenes()) {
            buf.writeVarInt(gene);
        }
        buf.writeVarInt(recipe.getPriority());
    }
}

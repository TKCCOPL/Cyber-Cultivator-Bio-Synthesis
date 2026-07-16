package com.TKCCOPL.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.common.crafting.CraftingHelper;

import javax.annotation.Nullable;

/**
 * SerumRecipe 的 JSON 序列化器。
 * 支持从 JSON 文件和网络数据包反序列化配方。
 */
public class SerumRecipeSerializer implements RecipeSerializer<SerumRecipe> {
    @Override
    public SerumRecipe fromJson(ResourceLocation id, JsonObject json) {
        JsonArray ingredientsJson = GsonHelper.getAsJsonArray(json, "ingredients");
        if (ingredientsJson.isEmpty() || ingredientsJson.size() > 3) {
            throw new com.google.gson.JsonSyntaxException("Serum bottling recipes require 1 to 3 ingredients");
        }
        Ingredient[] inputs = new Ingredient[ingredientsJson.size()];
        for (int i = 0; i < ingredientsJson.size(); i++) {
            inputs[i] = Ingredient.fromJson(ingredientsJson.get(i));
        }

        JsonObject resultJson = GsonHelper.getAsJsonObject(json, "result");
        ItemStack output = CraftingHelper.getItemStack(resultJson, true);

        int processingTime = GsonHelper.getAsInt(json, "processing_time", 300);
        if (processingTime < 1) {
            throw new com.google.gson.JsonSyntaxException("processing_time must be at least 1 tick");
        }
        boolean inheritActivity = GsonHelper.getAsBoolean(json, "inherit_activity", false);
        boolean inheritMutation = GsonHelper.getAsBoolean(json, "inherit_mutation", false);

        return new SerumRecipe(id, inputs, output, processingTime, inheritActivity, inheritMutation);
    }

    @Nullable
    @Override
    public SerumRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
        int inputCount = buf.readVarInt();
        if (inputCount < 1 || inputCount > 3) {
            throw new IllegalArgumentException("Invalid serum bottling ingredient count: " + inputCount);
        }
        Ingredient[] inputs = new Ingredient[inputCount];
        for (int i = 0; i < inputCount; i++) {
            inputs[i] = Ingredient.fromNetwork(buf);
        }
        ItemStack output = buf.readItem();
        int processingTime = Math.max(1, buf.readVarInt());
        boolean inheritActivity = buf.readBoolean();
        boolean inheritMutation = buf.readBoolean();
        return new SerumRecipe(id, inputs, output, processingTime, inheritActivity, inheritMutation);
    }

    @Override
    public void toNetwork(FriendlyByteBuf buf, SerumRecipe recipe) {
        buf.writeVarInt(recipe.getInputs().length);
        for (Ingredient input : recipe.getInputs()) {
            input.toNetwork(buf);
        }
        buf.writeItem(recipe.getBaseOutput());
        buf.writeVarInt(recipe.getProcessingTime());
        buf.writeBoolean(recipe.isInheritActivity());
        buf.writeBoolean(recipe.isInheritMutation());
    }
}

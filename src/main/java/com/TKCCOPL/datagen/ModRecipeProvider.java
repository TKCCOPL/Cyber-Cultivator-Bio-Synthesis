package com.TKCCOPL.datagen;

import com.TKCCOPL.init.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Items;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

@SuppressWarnings("null")
public class ModRecipeProvider extends RecipeProvider {
    public ModRecipeProvider(PackOutput output) {
        super(output);
    }

    @Override
        protected void buildRecipes(@Nonnull Consumer<FinishedRecipe> writer) {
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModItems.BIO_INCUBATOR_ITEM.get())
                .pattern("GSG")
                .pattern("IBI")
                .pattern("IRI")
                .define('G', Items.GLASS)
                .define('S', ModItems.SILICON_SHARD.get())
                .define('I', Items.IRON_INGOT)
                .define('B', Items.WATER_BUCKET)
                .define('R', ModItems.RARE_EARTH_DUST.get())
                .unlockedBy(getHasName(ModItems.SILICON_SHARD.get()), has(ModItems.SILICON_SHARD.get()))
                .save(writer);

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModItems.GENE_SPLICER_ITEM.get())
                .pattern("SRS")
                .pattern("IRI")
                .pattern("SCS")
                .define('S', ModItems.SILICON_SHARD.get())
                .define('R', Items.REDSTONE)
                .define('I', Items.IRON_INGOT)
                .define('C', ModItems.RARE_EARTH_DUST.get())
                .unlockedBy(getHasName(ModItems.RARE_EARTH_DUST.get()), has(ModItems.RARE_EARTH_DUST.get()))
                .save(writer);

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModItems.ATMOSPHERIC_CONDENSER_ITEM.get())
                .pattern("IGI")
                .pattern("GSG")
                .pattern("IRI")
                .define('I', Items.IRON_INGOT)
                .define('G', Items.GLASS)
                .define('S', ModItems.SILICON_SHARD.get())
                .define('R', ModItems.RARE_EARTH_DUST.get())
                .unlockedBy(getHasName(ModItems.SILICON_SHARD.get()), has(ModItems.SILICON_SHARD.get()))
                .save(writer);

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModItems.SERUM_BOTTLER_ITEM.get())
                .pattern("IRI")
                .pattern("SRS")
                .pattern("IBI")
                .define('I', Items.IRON_INGOT)
                .define('R', Items.REDSTONE)
                .define('S', ModItems.SILICON_SHARD.get())
                .define('B', Items.GLASS_BOTTLE)
                .unlockedBy(getHasName(ModItems.RARE_EARTH_DUST.get()), has(ModItems.RARE_EARTH_DUST.get()))
                .save(writer);

        // Curios accessories are now obtained through loot chests, not crafting
    }
}
package com.TKCCOPL.datagen;

import com.TKCCOPL.cybercultivator;
import com.TKCCOPL.init.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

@SuppressWarnings("null")
public class ModRecipeProvider extends RecipeProvider {
    public ModRecipeProvider(PackOutput output) {
        super(output);
    }

    @Override
    protected void buildRecipes(@Nonnull Consumer<FinishedRecipe> writer) {
        storageBlockRecipes(writer, ModItems.RAW_SILICON_CRYSTAL.get(), ModItems.RAW_SILICON_BLOCK_ITEM.get(),
                "raw_silicon_crystal_from_block");
        storageBlockRecipes(writer, ModItems.RAW_RARE_EARTH.get(), ModItems.RAW_RARE_EARTH_BLOCK_ITEM.get(),
                "raw_rare_earth_from_block");
        storageBlockRecipes(writer, ModItems.SILICON_SHARD.get(), ModItems.SILICON_BLOCK_ITEM.get(),
                "silicon_shard_from_block");
        storageBlockRecipes(writer, ModItems.RARE_EARTH_DUST.get(), ModItems.RARE_EARTH_BLOCK_ITEM.get(),
                "rare_earth_dust_from_block");
        oreRefiningRecipes(writer, ModItems.RAW_SILICON_CRYSTAL.get(), ModItems.SILICON_SHARD.get(),
                "silicon_shard");
        oreRefiningRecipes(writer, ModItems.RAW_RARE_EARTH.get(), ModItems.RARE_EARTH_DUST.get(),
                "rare_earth_dust");

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
                .pattern("SCS")
                .pattern("IBI")
                .define('I', Items.IRON_INGOT)
                .define('R', Items.REDSTONE)
                .define('C', ModItems.RARE_EARTH_DUST.get())
                .define('S', ModItems.SILICON_SHARD.get())
                .define('B', Items.GLASS_BOTTLE)
                .unlockedBy(getHasName(ModItems.RARE_EARTH_DUST.get()), has(ModItems.RARE_EARTH_DUST.get()))
                .save(writer);

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.SPECTRUM_MONOCLE.get())
                .pattern("IPI")
                .pattern("SRS")
                .pattern(" I ")
                .define('I', Items.IRON_NUGGET)
                .define('P', Items.GLASS_PANE)
                .define('S', ModItems.SILICON_SHARD.get())
                .define('R', ModItems.RARE_EARTH_DUST.get())
                .unlockedBy(getHasName(ModItems.RARE_EARTH_DUST.get()), has(ModItems.RARE_EARTH_DUST.get()))
                .save(writer);
    }

    private static void storageBlockRecipes(Consumer<FinishedRecipe> writer,
                                            Item material,
                                            Item block,
                                            String unpackRecipeName) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, block)
                .pattern("MMM")
                .pattern("MMM")
                .pattern("MMM")
                .define('M', material)
                .unlockedBy(getHasName(material), has(material))
                .save(writer);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, material, 9)
                .requires(block)
                .unlockedBy(getHasName(block), has(block))
                .save(writer, ResourceLocation.fromNamespaceAndPath(cybercultivator.MODID, unpackRecipeName));
    }

    private static void oreRefiningRecipes(Consumer<FinishedRecipe> writer,
                                           Item rawMaterial,
                                           Item refinedMaterial,
                                           String refinedName) {
        String rawName = getItemName(rawMaterial);
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(rawMaterial), RecipeCategory.MISC,
                        refinedMaterial, 0.7F, 200)
                .unlockedBy(getHasName(rawMaterial), has(rawMaterial))
                .save(writer, ResourceLocation.fromNamespaceAndPath(cybercultivator.MODID,
                        refinedName + "_from_smelting_" + rawName));

        SimpleCookingRecipeBuilder.blasting(Ingredient.of(rawMaterial), RecipeCategory.MISC,
                        refinedMaterial, 0.7F, 100)
                .unlockedBy(getHasName(rawMaterial), has(rawMaterial))
                .save(writer, ResourceLocation.fromNamespaceAndPath(cybercultivator.MODID,
                        refinedName + "_from_blasting_" + rawName));
    }
}

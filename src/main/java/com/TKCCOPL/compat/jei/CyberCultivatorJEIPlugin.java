package com.TKCCOPL.compat.jei;

import com.TKCCOPL.cybercultivator;
import com.TKCCOPL.init.ModItems;
import com.TKCCOPL.recipe.ModRecipeTypes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

@JeiPlugin
public class CyberCultivatorJEIPlugin implements IModPlugin {
    private static final ResourceLocation UID = new ResourceLocation(cybercultivator.MODID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new SerumBottlingCategory(guiHelper),
                new GeneSplicingCategory(guiHelper),
                new IncubatorOutputCategory(guiHelper)
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;

        // 血清灌装配方
        registration.addRecipes(
                SerumBottlingCategory.RECIPE_TYPE,
                level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.SERUM_BOTTLING.get())
        );

        // 基因拼接配方
        registration.addRecipes(
                GeneSplicingCategory.RECIPE_TYPE,
                GeneSplicingCategory.buildRecipes()
        );

        // 培养槽产出
        registration.addRecipes(
                IncubatorOutputCategory.RECIPE_TYPE,
                IncubatorOutputCategory.buildRecipes(level)
        );

        // 物品信息页面
        registration.addIngredientInfo(
                new ItemStack(ModItems.SYNAPTIC_SERUM_S01.get()),
                VanillaTypes.ITEM_STACK,
                Component.translatable("jei.cybercultivator.info.s01")
        );
        registration.addIngredientInfo(
                new ItemStack(ModItems.SYNAPTIC_SERUM_S02.get()),
                VanillaTypes.ITEM_STACK,
                Component.translatable("jei.cybercultivator.info.s02")
        );
        registration.addIngredientInfo(
                new ItemStack(ModItems.SYNAPTIC_SERUM_S03.get()),
                VanillaTypes.ITEM_STACK,
                Component.translatable("jei.cybercultivator.info.s03")
        );
        registration.addIngredientInfo(
                new ItemStack(ModItems.SPECTRUM_MONOCLE.get()),
                VanillaTypes.ITEM_STACK,
                Component.translatable("jei.cybercultivator.info.monocle")
        );
        registration.addIngredientInfo(
                new ItemStack(ModItems.BIO_PULSE_BELT.get()),
                VanillaTypes.ITEM_STACK,
                Component.translatable("jei.cybercultivator.info.belt")
        );
        registration.addIngredientInfo(
                new ItemStack(ModItems.LIFE_SUPPORT_PACK.get()),
                VanillaTypes.ITEM_STACK,
                Component.translatable("jei.cybercultivator.info.pack")
        );
        registration.addIngredientInfo(
                new ItemStack(ModItems.FIBER_REED_SEEDS.get()),
                VanillaTypes.ITEM_STACK,
                Component.translatable("jei.cybercultivator.info.seeds.fiber")
        );
        registration.addIngredientInfo(
                new ItemStack(ModItems.PROTEIN_SOY_SEEDS.get()),
                VanillaTypes.ITEM_STACK,
                Component.translatable("jei.cybercultivator.info.seeds.soy")
        );
        registration.addIngredientInfo(
                new ItemStack(ModItems.ALCOHOL_BLOOM_SEEDS.get()),
                VanillaTypes.ITEM_STACK,
                Component.translatable("jei.cybercultivator.info.seeds.bloom")
        );

        // 基础材料
        registration.addIngredientInfo(
                new ItemStack(ModItems.SILICON_SHARD.get()),
                VanillaTypes.ITEM_STACK,
                Component.translatable("jei.cybercultivator.info.silicon_shard")
        );
        registration.addIngredientInfo(
                new ItemStack(ModItems.RARE_EARTH_DUST.get()),
                VanillaTypes.ITEM_STACK,
                Component.translatable("jei.cybercultivator.info.rare_earth_dust")
        );
        registration.addIngredientInfo(
                new ItemStack(ModItems.PLANT_FIBER.get()),
                VanillaTypes.ITEM_STACK,
                Component.translatable("jei.cybercultivator.info.plant_fiber")
        );
        registration.addIngredientInfo(
                new ItemStack(ModItems.BIOCHEMICAL_SOLUTION.get()),
                VanillaTypes.ITEM_STACK,
                Component.translatable("jei.cybercultivator.info.biochemical_solution")
        );
        registration.addIngredientInfo(
                new ItemStack(ModItems.INDUSTRIAL_ETHANOL.get()),
                VanillaTypes.ITEM_STACK,
                Component.translatable("jei.cybercultivator.info.industrial_ethanol")
        );
        registration.addIngredientInfo(
                new ItemStack(ModItems.PURIFIED_WATER_BOTTLE.get()),
                VanillaTypes.ITEM_STACK,
                Component.translatable("jei.cybercultivator.info.purified_water")
        );
        registration.addIngredientInfo(
                new ItemStack(ModItems.SYNAPTIC_NEURAL_BERRY.get()),
                VanillaTypes.ITEM_STACK,
                Component.translatable("jei.cybercultivator.info.neural_berry")
        );

        // 机器方块
        registration.addIngredientInfo(
                new ItemStack(ModItems.BIO_INCUBATOR_ITEM.get()),
                VanillaTypes.ITEM_STACK,
                Component.translatable("jei.cybercultivator.info.bio_incubator")
        );
        registration.addIngredientInfo(
                new ItemStack(ModItems.GENE_SPLICER_ITEM.get()),
                VanillaTypes.ITEM_STACK,
                Component.translatable("jei.cybercultivator.info.gene_splicer")
        );
        registration.addIngredientInfo(
                new ItemStack(ModItems.ATMOSPHERIC_CONDENSER_ITEM.get()),
                VanillaTypes.ITEM_STACK,
                Component.translatable("jei.cybercultivator.info.atmospheric_condenser")
        );
        registration.addIngredientInfo(
                new ItemStack(ModItems.SERUM_BOTTLER_ITEM.get()),
                VanillaTypes.ITEM_STACK,
                Component.translatable("jei.cybercultivator.info.serum_bottler")
        );

        // 矿石
        registration.addIngredientInfo(
                new ItemStack(ModItems.SILICON_ORE_ITEM.get()),
                VanillaTypes.ITEM_STACK,
                Component.translatable("jei.cybercultivator.info.silicon_ore")
        );
        registration.addIngredientInfo(
                new ItemStack(ModItems.RARE_EARTH_ORE_ITEM.get()),
                VanillaTypes.ITEM_STACK,
                Component.translatable("jei.cybercultivator.info.rare_earth_ore")
        );
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(
                new ItemStack(ModItems.SERUM_BOTTLER_ITEM.get()),
                SerumBottlingCategory.RECIPE_TYPE
        );
        registration.addRecipeCatalyst(
                new ItemStack(ModItems.GENE_SPLICER_ITEM.get()),
                GeneSplicingCategory.RECIPE_TYPE
        );
        registration.addRecipeCatalyst(
                new ItemStack(ModItems.BIO_INCUBATOR_ITEM.get()),
                IncubatorOutputCategory.RECIPE_TYPE
        );
    }
}

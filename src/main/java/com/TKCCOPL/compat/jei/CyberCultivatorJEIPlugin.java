package com.TKCCOPL.compat.jei;

import com.TKCCOPL.cybercultivator;
import com.TKCCOPL.init.ModItems;
import com.TKCCOPL.recipe.ModRecipeTypes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
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
                IncubatorOutputCategory.buildRecipes()
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

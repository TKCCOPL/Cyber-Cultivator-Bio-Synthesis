package com.TKCCOPL.compat.jei;

import com.TKCCOPL.cybercultivator;
import com.TKCCOPL.init.ModItems;
import com.TKCCOPL.recipe.SerumRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.List;

public class SerumBottlingCategory implements IRecipeCategory<SerumRecipe> {
    public static final RecipeType<SerumRecipe> RECIPE_TYPE =
            new RecipeType<>(new ResourceLocation(cybercultivator.MODID, "serum_bottling"), SerumRecipe.class);

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(cybercultivator.MODID, "textures/gui/jei_serum_bottling.png");

    private final IDrawable background;
    private final IDrawable icon;

    public SerumBottlingCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(TEXTURE, 0, 0, 140, 60);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.SERUM_BOTTLER_ITEM.get()));
    }

    @Override
    public RecipeType<SerumRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.cybercultivator.serum_bottling");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, SerumRecipe recipe, IFocusGroup focuses) {
        Ingredient[] inputs = recipe.getInputs();
        for (int i = 0; i < inputs.length; i++) {
            builder.addSlot(RecipeIngredientRole.INPUT, 1 + i * 18, 11)
                    .addIngredients(inputs[i]);
        }

        // 输出物品（带 Activity NBT 用于展示）
        ItemStack output = recipe.getBaseOutput();
        boolean isBerry = output.is(ModItems.SYNAPTIC_NEURAL_BERRY.get());
        if (!isBerry) {
            output.getOrCreateTag().putInt("SynapticActivity", 8);
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, 113, 11)
                .addItemStack(output);
    }

    @Override
    public void draw(SerumRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics,
                     double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        ItemStack output = recipe.getBaseOutput();
        boolean isBerry = output.is(ModItems.SYNAPTIC_NEURAL_BERRY.get());

        // 加工时间（输入槽上方）
        int seconds = recipe.getProcessingTime() / 20;
        guiGraphics.drawString(font,
                Component.translatable("jei.cybercultivator.processing_time", seconds),
                1, 2, 0x808080, false);

        if (isBerry) {
            // 活性值（右上角）
            guiGraphics.drawString(font,
                    Component.translatable("jei.cybercultivator.activity", "?"),
                    60, 1, 0xFFAA00, false);

            // 品质标签行（槽位下方，y=31 充分脱离 y:11~27 槽位区）
            // 效价 — 对应输入槽 0 (植物纤维)
            guiGraphics.drawString(font,
                    Component.translatable("jei.cybercultivator.tag_potency"),
                    1, 31, 0x55FF55, false);
            // 纯度 — 对应输入槽 1 (工业乙醇)
            guiGraphics.drawString(font,
                    Component.translatable("jei.cybercultivator.tag_purity"),
                    45, 31, 0x55FFFF, false);
            // 浓度 — 对应输入槽 2 (生化原液)
            guiGraphics.drawString(font,
                    Component.translatable("jei.cybercultivator.tag_concentration"),
                    83, 31, 0xFF55FF, false);

            // 链路说明（底部）
            guiGraphics.drawString(font,
                    Component.translatable("jei.cybercultivator.berry_chain"),
                    1, 44, 0xAAAAAA, false);
        } else {
            // 血清配方：显示活性
            guiGraphics.drawString(font,
                    Component.translatable("jei.cybercultivator.activity", "8"),
                    60, 2, 0xFFAA00, false);

            // 血清等级信息
            guiGraphics.drawString(font,
                    Component.translatable("jei.cybercultivator.serum_level"),
                    1, 35, 0xFFFF55, false);

            // 血清效果简介
            if (output.is(ModItems.SYNAPTIC_SERUM_S01.get())) {
                guiGraphics.drawString(font,
                        Component.translatable("jei.cybercultivator.serum_effect.s01"),
                        1, 48, 0xAAAAAA, false);
            } else if (output.is(ModItems.SYNAPTIC_SERUM_S02.get())) {
                guiGraphics.drawString(font,
                        Component.translatable("jei.cybercultivator.serum_effect.s02"),
                        1, 48, 0xAAAAAA, false);
            } else if (output.is(ModItems.SYNAPTIC_SERUM_S03.get())) {
                guiGraphics.drawString(font,
                        Component.translatable("jei.cybercultivator.serum_effect.s03"),
                        1, 48, 0xAAAAAA, false);
            }
        }
    }

    @Override
    public List<Component> getTooltipStrings(SerumRecipe recipe, IRecipeSlotsView recipeSlotsView,
                                             double mouseX, double mouseY) {
        List<Component> tooltip = new ArrayList<>();
        ItemStack output = recipe.getBaseOutput();
        boolean isBerry = output.is(ModItems.SYNAPTIC_NEURAL_BERRY.get());

        if (isBerry) {
            // 效价 tooltip 热区 (x:0~27, y:29~41)
            if (mouseX >= 0 && mouseX <= 27 && mouseY >= 29 && mouseY <= 41) {
                tooltip.add(Component.translatable("jei.cybercultivator.tag_potency")
                        .withStyle(net.minecraft.ChatFormatting.GREEN));
                tooltip.add(Component.translatable("jei.cybercultivator.tooltip.quality_tags")
                        .withStyle(net.minecraft.ChatFormatting.GRAY));
            }
            // 纯度 tooltip 热区 (x:43~71, y:29~41)
            if (mouseX >= 43 && mouseX <= 71 && mouseY >= 29 && mouseY <= 41) {
                tooltip.add(Component.translatable("jei.cybercultivator.tag_purity")
                        .withStyle(net.minecraft.ChatFormatting.AQUA));
                tooltip.add(Component.translatable("jei.cybercultivator.tooltip.quality_tags")
                        .withStyle(net.minecraft.ChatFormatting.GRAY));
            }
            // 浓度 tooltip 热区 (x:81~109, y:29~41)
            if (mouseX >= 81 && mouseX <= 109 && mouseY >= 29 && mouseY <= 41) {
                tooltip.add(Component.translatable("jei.cybercultivator.tag_concentration")
                        .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
                tooltip.add(Component.translatable("jei.cybercultivator.tooltip.quality_tags")
                        .withStyle(net.minecraft.ChatFormatting.GRAY));
            }
            // 链路说明 tooltip 热区 (底部)
            if (mouseX >= 0 && mouseX <= 140 && mouseY >= 42 && mouseY <= 53) {
                tooltip.add(Component.translatable("jei.cybercultivator.tooltip.berry_chain_detail")
                        .withStyle(net.minecraft.ChatFormatting.GRAY));
                tooltip.add(Component.translatable("jei.cybercultivator.tooltip.activity_formula")
                        .withStyle(net.minecraft.ChatFormatting.GOLD));
            }
            // 活性值 tooltip 热区
            if (mouseX >= 58 && mouseX <= 105 && mouseY >= 0 && mouseY <= 11) {
                tooltip.add(Component.translatable("jei.cybercultivator.tooltip.activity_formula")
                        .withStyle(net.minecraft.ChatFormatting.GRAY));
            }
        } else {
            // 血清配方
            // Activity 区域提示
            if (mouseX >= 60 && mouseX <= 120 && mouseY >= 0 && mouseY <= 12) {
                tooltip.add(Component.translatable("jei.cybercultivator.tooltip.activity_formula")
                        .withStyle(net.minecraft.ChatFormatting.GRAY));
            }
            // 等级区域提示
            if (mouseX >= 1 && mouseX <= 80 && mouseY >= 33 && mouseY <= 45) {
                tooltip.add(Component.translatable("jei.cybercultivator.tooltip.level_scaling")
                        .withStyle(net.minecraft.ChatFormatting.GRAY));
            }
            // 血清效果区域提示
            if (mouseX >= 1 && mouseX <= 140 && mouseY >= 46 && mouseY <= 58) {
                tooltip.add(Component.translatable("jei.cybercultivator.tooltip.serum_chain")
                        .withStyle(net.minecraft.ChatFormatting.GRAY));
                tooltip.add(Component.translatable("jei.cybercultivator.tooltip.serum_stackable")
                        .withStyle(net.minecraft.ChatFormatting.GRAY));
                tooltip.add(Component.translatable("jei.cybercultivator.tooltip.serum_side_effect")
                        .withStyle(net.minecraft.ChatFormatting.RED));
            }
        }
        return tooltip;
    }
}

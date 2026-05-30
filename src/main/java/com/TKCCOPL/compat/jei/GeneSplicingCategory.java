package com.TKCCOPL.compat.jei;

import com.TKCCOPL.cybercultivator;
import com.TKCCOPL.init.ModItems;
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

import java.util.ArrayList;
import java.util.List;

public class GeneSplicingCategory implements IRecipeCategory<GeneSplicingCategory.DisplayRecipe> {
    public static final RecipeType<DisplayRecipe> RECIPE_TYPE =
            new RecipeType<>(new ResourceLocation(cybercultivator.MODID, "gene_splicing"), DisplayRecipe.class);

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(cybercultivator.MODID, "textures/gui/jei_gene_splicing.png");

    private final IDrawable background;
    private final IDrawable icon;

    /** JEI 展示用配方数据 */
    public record DisplayRecipe(
            ItemStack seedA, ItemStack seedB,
            int speedA, int yieldA, int potencyA,
            int speedB, int yieldB, int potencyB,
            double mutationChance
    ) {}

    public GeneSplicingCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(TEXTURE, 0, 0, 140, 60);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.GENE_SPLICER_ITEM.get()));
    }

    @Override
    public RecipeType<DisplayRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.cybercultivator.gene_splicing");
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
    public void setRecipe(IRecipeLayoutBuilder builder, DisplayRecipe recipe, IFocusGroup focuses) {
        // 输入槽 A（带基因值的种子）
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 21)
                .addItemStack(recipe.seedA());
        // 输入槽 B（带基因值的种子）
        builder.addSlot(RecipeIngredientRole.INPUT, 37, 21)
                .addItemStack(recipe.seedB());
        // 输出槽：显示带默认基因值的种子（平均值）
        int avgSpeed = (recipe.speedA() + recipe.speedB()) / 2;
        int avgYield = (recipe.yieldA() + recipe.yieldB()) / 2;
        int avgPotency = (recipe.potencyA() + recipe.potencyB()) / 2;
        ItemStack output = seedWithGenes(new ItemStack(recipe.seedA().getItem()), avgSpeed, avgYield, avgPotency);
        builder.addSlot(RecipeIngredientRole.OUTPUT, 113, 21)
                .addItemStack(output);
    }

    @Override
    public void draw(DisplayRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics,
                     double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;

        // 父本 A 基因（槽 A 上方）
        guiGraphics.drawString(font,
                Component.translatable("jei.cybercultivator.gene_info_a",
                        recipe.speedA(), recipe.yieldA(), recipe.potencyA()),
                1, 2, 0x808080, false);

        // 父本 B 基因（槽 B 上方）
        guiGraphics.drawString(font,
                Component.translatable("jei.cybercultivator.gene_info_b",
                        recipe.speedB(), recipe.yieldB(), recipe.potencyB()),
                1, 12, 0x808080, false);

        // 突变概率（输出槽上方）
        guiGraphics.drawString(font,
                Component.translatable("jei.cybercultivator.mutation_chance",
                        String.format("%.0f", recipe.mutationChance() * 100)),
                75, 2, 0xFF55FF, false);

        // 子代基因范围（输出槽下方）
        int minS = Math.max(1, (recipe.speedA() + recipe.speedB()) / 2 - 2);
        int maxS = Math.min(10, (recipe.speedA() + recipe.speedB()) / 2 + 2);
        int minY = Math.max(1, (recipe.yieldA() + recipe.yieldB()) / 2 - 2);
        int maxY = Math.min(10, (recipe.yieldA() + recipe.yieldB()) / 2 + 2);
        int minP = Math.max(1, (recipe.potencyA() + recipe.potencyB()) / 2 - 2);
        int maxP = Math.min(10, (recipe.potencyA() + recipe.potencyB()) / 2 + 2);
        guiGraphics.drawString(font,
                Component.translatable("jei.cybercultivator.gene_range",
                        minS, maxS, minY, maxY, minP, maxP),
                1, 48, 0x55FF55, false);
    }

    @Override
    public List<Component> getTooltipStrings(DisplayRecipe recipe, IRecipeSlotsView recipeSlotsView,
                                             double mouseX, double mouseY) {
        List<Component> tooltip = new ArrayList<>();
        // 突变区域提示
        if (mouseX >= 75 && mouseX <= 140 && mouseY >= 0 && mouseY <= 12) {
            tooltip.add(Component.translatable("jei.cybercultivator.tooltip.mutation_formula")
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
        }
        // 子代范围区域提示
        if (mouseX >= 1 && mouseX <= 140 && mouseY >= 46 && mouseY <= 58) {
            tooltip.add(Component.translatable("jei.cybercultivator.tooltip.gene_formula")
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
        }
        return tooltip;
    }

    /** 设置种子基因值（用于 JEI 展示） */
    private static ItemStack seedWithGenes(ItemStack seed, int speed, int yield, int potency) {
        ItemStack stack = seed.copy();
        stack.getOrCreateTag().putInt("Gene_Speed", speed);
        stack.getOrCreateTag().putInt("Gene_Yield", yield);
        stack.getOrCreateTag().putInt("Gene_Potency", potency);
        return stack;
    }

    /** 构建展示用配方列表 */
    public static List<DisplayRecipe> buildRecipes() {
        List<DisplayRecipe> recipes = new ArrayList<>();

        // 同类拼接（默认基因值）
        recipes.add(new DisplayRecipe(
                seedWithGenes(new ItemStack(ModItems.FIBER_REED_SEEDS.get()), 4, 7, 3),
                seedWithGenes(new ItemStack(ModItems.FIBER_REED_SEEDS.get()), 4, 7, 3),
                4, 7, 3, 4, 7, 3, 0.05
        ));
        recipes.add(new DisplayRecipe(
                seedWithGenes(new ItemStack(ModItems.PROTEIN_SOY_SEEDS.get()), 5, 4, 7),
                seedWithGenes(new ItemStack(ModItems.PROTEIN_SOY_SEEDS.get()), 5, 4, 7),
                5, 4, 7, 5, 4, 7, 0.05
        ));
        recipes.add(new DisplayRecipe(
                seedWithGenes(new ItemStack(ModItems.ALCOHOL_BLOOM_SEEDS.get()), 6, 3, 5),
                seedWithGenes(new ItemStack(ModItems.ALCOHOL_BLOOM_SEEDS.get()), 6, 3, 5),
                6, 3, 5, 6, 3, 5, 0.05
        ));

        // 跨类型拼接（展示基因差异导致的突变概率变化）
        recipes.add(new DisplayRecipe(
                seedWithGenes(new ItemStack(ModItems.FIBER_REED_SEEDS.get()), 4, 7, 3),
                seedWithGenes(new ItemStack(ModItems.PROTEIN_SOY_SEEDS.get()), 5, 4, 7),
                4, 7, 3, 5, 4, 7, 0.09
        ));
        recipes.add(new DisplayRecipe(
                seedWithGenes(new ItemStack(ModItems.FIBER_REED_SEEDS.get()), 4, 7, 3),
                seedWithGenes(new ItemStack(ModItems.ALCOHOL_BLOOM_SEEDS.get()), 6, 3, 5),
                4, 7, 3, 6, 3, 5, 0.09
        ));
        recipes.add(new DisplayRecipe(
                seedWithGenes(new ItemStack(ModItems.PROTEIN_SOY_SEEDS.get()), 5, 4, 7),
                seedWithGenes(new ItemStack(ModItems.ALCOHOL_BLOOM_SEEDS.get()), 6, 3, 5),
                5, 4, 7, 6, 3, 5, 0.09
        ));

        return recipes;
    }
}

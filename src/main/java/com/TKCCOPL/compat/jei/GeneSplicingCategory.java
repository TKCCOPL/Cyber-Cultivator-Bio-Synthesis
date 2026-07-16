package com.TKCCOPL.compat.jei;

import com.TKCCOPL.Config;
import com.TKCCOPL.cybercultivator;
import com.TKCCOPL.init.ModItems;
import com.TKCCOPL.recipe.ModRecipes;
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
        int range = Config.mutationRange;
        int minS = Math.max(Config.geneMin, (recipe.speedA() + recipe.speedB()) / 2 - range);
        int maxS = Math.min(Config.geneMax, (recipe.speedA() + recipe.speedB()) / 2 + range);
        int minY = Math.max(Config.geneMin, (recipe.yieldA() + recipe.yieldB()) / 2 - range);
        int maxY = Math.min(Config.geneMax, (recipe.yieldA() + recipe.yieldB()) / 2 + range);
        int minP = Math.max(Config.geneMin, (recipe.potencyA() + recipe.potencyB()) / 2 - range);
        int maxP = Math.min(Config.geneMax, (recipe.potencyA() + recipe.potencyB()) / 2 + range);
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

    /** 从 ModRecipes 静态注册表自动构建同类+跨类拼接配方（第三方 mod 注册后自动显示） */
    public static List<DisplayRecipe> buildRecipes() {
        List<DisplayRecipe> recipes = new ArrayList<>();
        var outputs = ModRecipes.getINCUBATOR_OUTPUTS();
        var splice = ModRecipes.getDefaultSpliceRecipe();

        // 同类拼接
        for (var output : outputs) {
            ItemStack seed = ModRecipes.getSeedItemForType(output.getSeedType());
            if (seed.isEmpty()) continue;
            int[] genes = output.getDefaultGenes();
            double mutation = splice.getMutationChance(0, 0);
            recipes.add(new DisplayRecipe(
                    seedWithGenes(seed.copy(), genes[0], genes[1], genes[2]),
                    seedWithGenes(seed.copy(), genes[0], genes[1], genes[2]),
                    genes[0], genes[1], genes[2],
                    genes[0], genes[1], genes[2],
                    mutation
            ));
        }

        // 跨类拼接（所有种子对组合）
        for (int i = 0; i < outputs.size(); i++) {
            for (int j = i + 1; j < outputs.size(); j++) {
                var outA = outputs.get(i);
                var outB = outputs.get(j);
                ItemStack seedA = ModRecipes.getSeedItemForType(outA.getSeedType());
                ItemStack seedB = ModRecipes.getSeedItemForType(outB.getSeedType());
                if (seedA.isEmpty() || seedB.isEmpty()) continue;
                int[] genesA = outA.getDefaultGenes();
                int[] genesB = outB.getDefaultGenes();
                int geneDiff = Math.max(
                        Math.abs(genesA[0] - genesB[0]),
                        Math.max(Math.abs(genesA[1] - genesB[1]),
                                Math.abs(genesA[2] - genesB[2])));
                double mutation = splice.getMutationChance(0, geneDiff);
                recipes.add(new DisplayRecipe(
                        seedWithGenes(seedA, genesA[0], genesA[1], genesA[2]),
                        seedWithGenes(seedB, genesB[0], genesB[1], genesB[2]),
                        genesA[0], genesA[1], genesA[2],
                        genesB[0], genesB[1], genesB[2],
                        mutation
                ));
            }
        }
        return recipes;
    }
}

# JEI 集成完善实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复 JEI 配方展示问题，添加 Activity、基因值、公式和概率显示。

**Architecture:** 修改 3 个 JEI Category 类的 setRecipe() 和 draw() 方法，扩展 DisplayRecipe record 以携带更多数据。在 ModLangProvider 中添加翻译键。

**Tech Stack:** Minecraft Forge 1.20.1, JEI API, Java 17

**Spec:** `docs/superpowers/specs/2026-05-30-jei-improvement-design.md`

---

## 文件修改清单

| 文件 | 修改类型 | 职责 |
|------|---------|------|
| `compat/jei/GeneSplicingCategory.java` | 重写 | DisplayRecipe 扩展、setRecipe、draw、buildRecipes |
| `compat/jei/SerumBottlingCategory.java` | 修改 | 添加 draw 方法、输出 Activity 显示 |
| `compat/jei/IncubatorOutputCategory.java` | 重写 | DisplayRecipe 扩展、setRecipe、draw、buildRecipes |
| `datagen/ModLangProvider.java` | 修改 | 添加 JEI 翻译键 |

---

## Task 1: 语言文件

**Files:**
- Modify: `src/main/java/com/TKCCOPL/datagen/ModLangProvider.java`

- [ ] **Step 1: 添加 JEI 翻译键**

在 `ModLangProvider.java` 的 `addTranslations()` 方法末尾添加：

```java
// JEI 配方类别
add("jei.cybercultivator.serum_bottling", "血清灌装");
add("jei.cybercultivator.gene_splicing", "基因拼接");
add("jei.cybercultivator.incubator_output", "培养槽产出");

// JEI 信息
add("jei.cybercultivator.activity", "活性: %s");
add("jei.cybercultivator.processing_time", "%ss");
add("jei.cybercultivator.mutation_chance", "突变: %s%%");
add("jei.cybercultivator.gene_info_a", "A: S:%s Y:%s P:%s");
add("jei.cybercultivator.gene_info_b", "B: S:%s Y:%s P:%s");
add("jei.cybercultivator.gene_range", "子代: S:%s-%s Y:%s-%s P:%s-%s");
add("jei.cybercultivator.gene_default", "基因: S:%s Y:%s P:%s");
add("jei.cybercultivator.output_range", "产出: %s-%s");
add("jei.cybercultivator.growth_rate", "速率: %sx");
```

- [ ] **Step 2: 运行 datagen 生成语言文件**

```bash
./gradlew runData
```

预期：`src/generated/resources/assets/cybercultivator/lang/zh_cn.json` 包含新增翻译键

- [ ] **Step 3: 编译验证**

```bash
./gradlew compileJava
```

预期：BUILD SUCCESSFUL

- [ ] **Step 4: 提交**

```bash
git add src/main/java/com/TKCCOPL/datagen/ModLangProvider.java src/generated/resources/assets/cybercultivator/lang/zh_cn.json
git commit -m "feat(lang): 添加 JEI 配方类别翻译键"
```

---

## Task 2: GeneSplicingCategory 改造

**Files:**
- Modify: `src/main/java/com/TKCCOPL/compat/jei/GeneSplicingCategory.java`

- [ ] **Step 1: 重写 GeneSplicingCategory.java**

完整替换文件内容：

```java
package com.TKCCOPL.compat.jei;

import com.TKCCOPL.cybercultivator;
import com.TKCCOPL.init.ModItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
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
        // 输出槽（与 seedA 同类型的种子）
        builder.addSlot(RecipeIngredientRole.OUTPUT, 113, 21)
                .addItemStack(new ItemStack(recipe.seedA().getItem()));
    }

    @Override
    public void draw(DisplayRecipe recipe, net.minecraft.client.gui.GuiGraphics guiGraphics,
                     double mouseX, double mouseY) {
        // 父本 A 基因信息
        String geneA = String.format("A: S:%d Y:%d P:%d",
                recipe.speedA(), recipe.yieldA(), recipe.potencyA());
        guiGraphics.drawString(net.minecraft.client.Minecraft.getInstance().font,
                geneA, 1, 4, 0x808080);

        // 父本 B 基因信息
        String geneB = String.format("B: S:%d Y:%d P:%d",
                recipe.speedB(), recipe.yieldB(), recipe.potencyB());
        guiGraphics.drawString(net.minecraft.client.Minecraft.getInstance().font,
                geneB, 37, 4, 0x808080);

        // 突变概率
        String mutation = String.format("%.0f%%", recipe.mutationChance() * 100);
        guiGraphics.drawString(net.minecraft.client.Minecraft.getInstance().font,
                mutation, 70, 10, 0xFF55FF);

        // 子代基因范围
        int minS = Math.max(1, (recipe.speedA() + recipe.speedB()) / 2 - 2);
        int maxS = Math.min(10, (recipe.speedA() + recipe.speedB()) / 2 + 2);
        int minY = Math.max(1, (recipe.yieldA() + recipe.yieldB()) / 2 - 2);
        int maxY = Math.min(10, (recipe.yieldA() + recipe.yieldB()) / 2 + 2);
        int minP = Math.max(1, (recipe.potencyA() + recipe.potencyB()) / 2 - 2);
        int maxP = Math.min(10, (recipe.potencyA() + recipe.potencyB()) / 2 + 2);
        String range = String.format("S:%d-%d Y:%d-%d P:%d-%d", minS, maxS, minY, maxY, minP, maxP);
        guiGraphics.drawString(net.minecraft.client.Minecraft.getInstance().font,
                range, 70, 40, 0x55FF55);
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
```

- [ ] **Step 2: 编译验证**

```bash
./gradlew compileJava
```

预期：BUILD SUCCESSFUL

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/TKCCOPL/compat/jei/GeneSplicingCategory.java
git commit -m "feat(jei): GeneSplicing 显示基因值、突变概率、子代范围"
```

---

## Task 2: SerumBottlingCategory 改造

**Files:**
- Modify: `src/main/java/com/TKCCOPL/compat/jei/SerumBottlingCategory.java`

- [ ] **Step 1: 重写 SerumBottlingCategory.java**

```java
package com.TKCCOPL.compat.jei;

import com.TKCCOPL.cybercultivator;
import com.TKCCOPL.init.ModItems;
import com.TKCCOPL.recipe.SerumRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public class SerumBottlingCategory implements IRecipeCategory<SerumRecipe> {
    public static final RecipeType<SerumRecipe> RECIPE_TYPE =
            new RecipeType<>(new ResourceLocation(cybercultivator.MODID, "serum_bottling"), SerumRecipe.class);

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(cybercultivator.MODID, "textures/gui/jei_serum_bottling.png");

    private final IDrawable background;
    private final IDrawable icon;

    public SerumBottlingCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(TEXTURE, 0, 0, 140, 50);
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
        if (!output.is(ModItems.SYNAPTIC_NEURAL_BERRY.get())) {
            output.getOrCreateTag().putInt("SynapticActivity", 8);
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, 113, 11)
                .addItemStack(output);
    }

    @Override
    public void draw(SerumRecipe recipe, net.minecraft.client.gui.GuiGraphics guiGraphics,
                     double mouseX, double mouseY) {
        // 加工时间
        int seconds = recipe.getProcessingTime() / 20;
        String time = seconds + "s";
        guiGraphics.drawString(Minecraft.getInstance().font, time, 60, 2, 0x808080);

        // Activity 值（仅血清配方）
        ItemStack output = recipe.getBaseOutput();
        if (!output.is(ModItems.SYNAPTIC_NEURAL_BERRY.get())) {
            guiGraphics.drawString(Minecraft.getInstance().font, "活性: 8", 60, 38, 0xFFAA00);
        }
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
./gradlew compileJava
```

预期：BUILD SUCCESSFUL

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/TKCCOPL/compat/jei/SerumBottlingCategory.java
git commit -m "feat(jei): SerumBottling 显示加工时间和 Activity 值"
```

---

## Task 3: IncubatorOutputCategory 改造

**Files:**
- Modify: `src/main/java/com/TKCCOPL/compat/jei/IncubatorOutputCategory.java`

- [ ] **Step 1: 重写 IncubatorOutputCategory.java**

```java
package com.TKCCOPL.compat.jei;

import com.TKCCOPL.cybercultivator;
import com.TKCCOPL.init.ModItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class IncubatorOutputCategory implements IRecipeCategory<IncubatorOutputCategory.DisplayRecipe> {
    public static final RecipeType<DisplayRecipe> RECIPE_TYPE =
            new RecipeType<>(new ResourceLocation(cybercultivator.MODID, "incubator_output"), DisplayRecipe.class);

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(cybercultivator.MODID, "textures/gui/jei_incubator_output.png");

    private final IDrawable background;
    private final IDrawable icon;

    /** JEI 展示用配方数据 */
    public record DisplayRecipe(
            ItemStack seed, ItemStack output,
            String cropName,
            int defaultSpeed, int defaultYield, int defaultPotency
    ) {}

    public IncubatorOutputCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(TEXTURE, 0, 0, 140, 50);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.BIO_INCUBATOR_ITEM.get()));
    }

    @Override
    public RecipeType<DisplayRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.cybercultivator.incubator_output");
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
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 11)
                .addItemStack(recipe.seed());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 113, 11)
                .addItemStack(recipe.output());
    }

    @Override
    public void draw(DisplayRecipe recipe, net.minecraft.client.gui.GuiGraphics guiGraphics,
                     double mouseX, double mouseY) {
        // 默认基因值
        String genes = String.format("S:%d Y:%d P:%d",
                recipe.defaultSpeed(), recipe.defaultYield(), recipe.defaultPotency());
        guiGraphics.drawString(Minecraft.getInstance().font, genes, 30, 2, 0x808080);

        // 产出数量范围（基于 Yield：2 + yield/3）
        int minOutput = 2;
        int maxOutput = 2 + recipe.defaultYield() / 3;
        String outputRange = String.format("产出: %d-%d", minOutput, maxOutput);
        guiGraphics.drawString(Minecraft.getInstance().font, outputRange, 30, 14, 0x55FF55);

        // 生长速率（基于 Speed：0.5 + speed/10*1.5）
        double growthRate = 0.5 + recipe.defaultSpeed() / 10.0 * 1.5;
        String rate = String.format("速率: %.1fx", growthRate);
        guiGraphics.drawString(Minecraft.getInstance().font, rate, 30, 26, 0xFFFF55);
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

        // Fiber Reed: Speed=4, Yield=7, Potency=3
        recipes.add(new DisplayRecipe(
                seedWithGenes(new ItemStack(ModItems.FIBER_REED_SEEDS.get()), 4, 7, 3),
                new ItemStack(ModItems.PLANT_FIBER.get()),
                "纤维草", 4, 7, 3
        ));

        // Protein Soy: Speed=5, Yield=4, Potency=7
        recipes.add(new DisplayRecipe(
                seedWithGenes(new ItemStack(ModItems.PROTEIN_SOY_SEEDS.get()), 5, 4, 7),
                new ItemStack(ModItems.BIOCHEMICAL_SOLUTION.get()),
                "蛋白质豆", 5, 4, 7
        ));

        // Alcohol Bloom: Speed=6, Yield=3, Potency=5
        recipes.add(new DisplayRecipe(
                seedWithGenes(new ItemStack(ModItems.ALCOHOL_BLOOM_SEEDS.get()), 6, 3, 5),
                new ItemStack(ModItems.INDUSTRIAL_ETHANOL.get()),
                "酒精花", 6, 3, 5
        ));

        return recipes;
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
./gradlew compileJava
```

预期：BUILD SUCCESSFUL

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/TKCCOPL/compat/jei/IncubatorOutputCategory.java
git commit -m "feat(jei): IncubatorOutput 显示基因值、产出范围、生长速率"
```

---

## Task 4: 运行数据生成器

**Files:**
- 生成: `src/generated/resources/assets/cybercultivator/lang/zh_cn.json`

- [ ] **Step 1: 运行 datagen**

```bash
./gradlew runData
```

预期：生成包含新增 JEI 翻译键的 zh_cn.json

- [ ] **Step 2: 验证翻译文件**

检查 `src/generated/resources/assets/cybercultivator/lang/zh_cn.json` 包含：
- `jei.cybercultivator.serum_bottling`
- `jei.cybercultivator.gene_splicing`
- `jei.cybercultivator.incubator_output`
- 其他 JEI 信息翻译键

- [ ] **Step 3: 提交**

```bash
git add src/generated/resources/assets/cybercultivator/lang/zh_cn.json
git commit -m "feat(lang): 添加 JEI 配方类别翻译键"
```

---

## Task 5: 最终验证

- [ ] **Step 1: 编译检查**

```bash
./gradlew compileJava
```

预期：BUILD SUCCESSFUL

- [ ] **Step 2: 构建 jar**

```bash
./gradlew build
```

预期：BUILD SUCCESSFUL

- [ ] **Step 3: 提交**

```bash
git add -A
git commit -m "feat(jei): 完善 JEI 集成——基因值/Activity/公式/概率显示"
```

# JEI 集成全面修复设计规范

**日期：** 2026-05-30
**分支：** feature/mod-compatibility-api
**目标：** 修复所有 JEI 显示问题，完善信息展示

## 问题清单

| # | 问题 | 类型 | 影响 |
|---|------|------|------|
| 1 | en_us.json 缺少 JEI 翻译键 | BUG | 英文环境显示原始翻译键 |
| 2 | draw() 中硬编码中文字符串 | BUG | 非中文环境显示中文 |
| 3 | 无 addIngredientInfo 页面 | 缺失 | 复杂物品无说明 |
| 4 | Activity 值硬编码为 8 | 不准确 | 显示值与实际不符 |
| 5 | 拼接输出无基因 NBT | 不准确 | 输出种子无法预览 |
| 6 | 无 getTooltip 悬停提示 | 缺失 | 配方元素无详细说明 |

## 修改方案

### 1. 修复 en_us.json 缺失的 JEI 翻译键

**文件：** `src/main/resources/assets/cybercultivator/lang/en_us.json`

添加所有 `jei.cybercultivator.*` 键：

```json
"jei.cybercultivator.serum_bottling": "Serum Bottling",
"jei.cybercultivator.gene_splicing": "Gene Splicing",
"jei.cybercultivator.incubator_output": "Incubator Output",
"jei.cybercultivator.activity": "Activity: %s",
"jei.cybercultivator.processing_time": "%ss",
"jei.cybercultivator.mutation_chance": "Mutation: %s%%",
"jei.cybercultivator.gene_info_a": "A: S:%s Y:%s P:%s",
"jei.cybercultivator.gene_info_b": "B: S:%s Y:%s P:%s",
"jei.cybercultivator.gene_range": "Offspring: S:%s-%s Y:%s-%s P:%s-%s",
"jei.cybercultivator.gene_default": "Genes: S:%s Y:%s P:%s",
"jei.cybercultivator.output_range": "Output: %s-%s",
"jei.cybercultivator.growth_rate": "Rate: %sx"
```

### 2. 修复 draw() 中硬编码的中文字符串

#### 2.1 SerumBottlingCategory.java

```java
// 修改前
guiGraphics.drawString(font, "活性: 8", 60, 38, 0xFFAA00, false);

// 修改后
guiGraphics.drawString(font, Component.translatable("jei.cybercultivator.activity", "8"), 60, 38, 0xFFAA00, false);
```

#### 2.2 GeneSplicingCategory.java

```java
// 修改前
String geneA = String.format("A: S:%d Y:%d P:%d", ...);
guiGraphics.drawString(font, geneA, 1, 4, 0x808080, false);

// 修改后
guiGraphics.drawString(font,
    Component.translatable("jei.cybercultivator.gene_info_a",
        recipe.speedA(), recipe.yieldA(), recipe.potencyA()),
    1, 4, 0x808080, false);
```

突变概率和子代范围同理。

#### 2.3 IncubatorOutputCategory.java

```java
// 修改前
String outputRange = String.format("产出: %d-%d", minOutput, maxOutput);
String rate = String.format("速率: %.1fx", growthRate);

// 修改后
guiGraphics.drawString(font,
    Component.translatable("jei.cybercultivator.output_range", minOutput, maxOutput),
    30, 14, 0x55FF55, false);
guiGraphics.drawString(font,
    Component.translatable("jei.cybercultivator.growth_rate",
        String.format("%.1f", growthRate)),
    30, 26, 0xFFFF55, false);
```

### 3. 添加 addIngredientInfo 页面

**文件：** `CyberCultivatorJEIPlugin.java`

在 `registerRecipes()` 中添加物品信息页面：

```java
@Override
public void registerRecipes(IRecipeRegistration registration) {
    // ... 现有配方注册 ...

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
        Component.translatable("jei.cybercultivator.info.seeds")
    );
    registration.addIngredientInfo(
        new ItemStack(ModItems.PROTEIN_SOY_SEEDS.get()),
        VanillaTypes.ITEM_STACK,
        Component.translatable("jei.cybercultivator.info.seeds")
    );
    registration.addIngredientInfo(
        new ItemStack(ModItems.ALCOHOL_BLOOM_SEEDS.get()),
        VanillaTypes.ITEM_STACK,
        Component.translatable("jei.cybercultivator.info.seeds")
    );
}
```

### 4. 添加翻译键（ModLangProvider + en_us.json）

#### 中文（ModLangProvider.java）

```java
// JEI 物品信息页面
add("jei.cybercultivator.info.s01", "S-01 突触超频血清：饮用后获得攻击速度和力量加成，持续时间取决于突触活性。效果结束后会产生神经过载副作用（凋零+饥饿）。可叠加饮用提升效果等级，上限 VIII 级。");
add("jei.cybercultivator.info.s02", "S-02 视觉强化血清：饮用后获得夜视和发光能力，发光范围随活性增长。效果结束后会产生神经过载副作用（失明+饥饿）。可叠加饮用提升效果等级，上限 VIII 级。");
add("jei.cybercultivator.info.s03", "S-03 代谢加速血清：饮用后获得生命恢复和移动速度加成。效果结束后会产生神经过载副作用（缓慢+中毒）。可叠加饮用提升效果等级，上限 VIII 级。");
add("jei.cybercultivator.info.monocle", "光谱单片镜：佩戴后可以解析培养槽的状态信息（N/P/D 数值、生长进度、预计成熟时间）和种子的基因值。在 HUD 上显示浮窗信息。");
add("jei.cybercultivator.info.belt", "生化脉冲腰带：自动扫描附近的培养槽，消耗背包中的材料自动注入营养液、纯净水和数据信号。");
add("jei.cybercultivator.info.pack", "生命支持箱：加速血清副作用（神经过载）的消退。当生命值过低时自动注射治疗（冷却 60 秒）。");
add("jei.cybercultivator.info.seeds", "基因种子：放入培养槽中培育，产出对应作物。通过基因拼接机可以改变种子的基因值（速度、产量、效价），影响产出数量和生长速率。");
```

#### 英文（en_us.json）

```json
"jei.cybercultivator.info.s01": "S-01 Synaptic Overclock Serum: Grants attack speed and strength boosts. Duration depends on Synaptic Activity. Causes Neural Overload (Wither + Hunger) when effect ends. Stackable up to level VIII.",
"jei.cybercultivator.info.s02": "S-02 Visual Enhancement Serum: Grants night vision and glowing ability. Glow range increases with Activity. Causes Neural Overload (Blindness + Hunger) when effect ends. Stackable up to level VIII.",
"jei.cybercultivator.info.s03": "S-03 Metabolic Boost Serum: Grants regeneration and movement speed boosts. Causes Neural Overload (Slowness + Poison) when effect ends. Stackable up to level VIII.",
"jei.cybercultivator.info.monocle": "Spectrum Monocle: Wear to analyze incubator status (N/P/D values, growth progress, ETA) and seed gene values. Displays HUD overlay information.",
"jei.cybercultivator.info.belt": "Bio-Pulse Belt: Automatically scans nearby incubators and injects nutrients, purified water, and data signal from your inventory.",
"jei.cybercultivator.info.pack": "Life Support Pack: Accelerates serum side effect (Neural Overload) recovery. Auto-heals at low HP (60s cooldown).",
"jei.cybercultivator.info.seeds": "Gene Seeds: Place in Bio-Incubator to cultivate. Use Gene Splicer to modify gene values (Speed, Yield, Potency), affecting output count and growth rate."
```

### 5. 改进拼接输出显示

**文件：** `GeneSplicingCategory.java`

在 `setRecipe()` 中为输出种子设置默认基因值：

```java
@Override
public void setRecipe(IRecipeLayoutBuilder builder, DisplayRecipe recipe, IFocusGroup focuses) {
    builder.addSlot(RecipeIngredientRole.INPUT, 1, 21)
            .addItemStack(recipe.seedA());
    builder.addSlot(RecipeIngredientRole.INPUT, 37, 21)
            .addItemStack(recipe.seedB());

    // 输出槽：显示带默认基因值的种子
    int avgSpeed = (recipe.speedA() + recipe.speedB()) / 2;
    int avgYield = (recipe.yieldA() + recipe.yieldB()) / 2;
    int avgPotency = (recipe.potencyA() + recipe.potencyB()) / 2;
    ItemStack output = seedWithGenes(new ItemStack(recipe.seedA().getItem()), avgSpeed, avgYield, avgPotency);
    builder.addSlot(RecipeIngredientRole.OUTPUT, 113, 21)
            .addItemStack(output);
}
```

### 6. 添加 getTooltip 悬停提示

为每个配方类别添加鼠标悬停提示，解释公式和机制。

#### GeneSplicingCategory

```java
@Override
public void getTooltip(ITooltipBuilder tooltip, DisplayRecipe recipe,
        IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
    // 突变区域提示
    if (mouseX >= 70 && mouseX <= 120 && mouseY >= 5 && mouseY <= 18) {
        tooltip.add(Component.translatable("jei.cybercultivator.tooltip.mutation_formula"));
    }
    // 子代范围区域提示
    if (mouseX >= 70 && mouseX <= 140 && mouseY >= 35 && mouseY <= 50) {
        tooltip.add(Component.translatable("jei.cybercultivator.tooltip.gene_formula"));
    }
}
```

#### IncubatorOutputCategory

```java
@Override
public void getTooltip(ITooltipBuilder tooltip, DisplayRecipe recipe,
        IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
    // 产出范围提示
    if (mouseX >= 30 && mouseX <= 100 && mouseY >= 10 && mouseY <= 22) {
        tooltip.add(Component.translatable("jei.cybercultivator.tooltip.output_formula"));
    }
    // 生长速率提示
    if (mouseX >= 30 && mouseX <= 100 && mouseY >= 22 && mouseY <= 34) {
        tooltip.add(Component.translatable("jei.cybercultivator.tooltip.rate_formula"));
    }
}
```

### 7. 新增翻译键汇总

| 键 | 中文 | 英文 |
|---|------|------|
| `jei.cybercultivator.info.s01` | S-01 突触超频血清说明 | S-01 Synaptic Overclock Serum info |
| `jei.cybercultivator.info.s02` | S-02 视觉强化血清说明 | S-02 Visual Enhancement Serum info |
| `jei.cybercultivator.info.s03` | S-03 代谢加速血清说明 | S-03 Metabolic Boost Serum info |
| `jei.cybercultivator.info.monocle` | 光谱单片镜说明 | Spectrum Monocle info |
| `jei.cybercultivator.info.belt` | 生化脉冲腰带说明 | Bio-Pulse Belt info |
| `jei.cybercultivator.info.pack` | 生命支持箱说明 | Life Support Pack info |
| `jei.cybercultivator.info.seeds` | 基因种子说明 | Gene Seeds info |
| `jei.cybercultivator.tooltip.mutation_formula` | 突变概率公式说明 | Mutation formula tooltip |
| `jei.cybercultivator.tooltip.gene_formula` | 子代基因范围公式说明 | Gene range formula tooltip |
| `jei.cybercultivator.tooltip.output_formula` | 产出数量公式说明 | Output formula tooltip |
| `jei.cybercultivator.tooltip.rate_formula` | 生长速率公式说明 | Growth rate formula tooltip |

## 文件修改清单

| 文件 | 修改内容 |
|------|---------|
| `compat/jei/CyberCultivatorJEIPlugin.java` | 添加 addIngredientInfo 调用 |
| `compat/jei/SerumBottlingCategory.java` | 修复硬编码字符串，改用 Component.translatable() |
| `compat/jei/GeneSplicingCategory.java` | 修复硬编码字符串，改进输出显示，添加 getTooltip |
| `compat/jei/IncubatorOutputCategory.java` | 修复硬编码字符串，添加 getTooltip |
| `datagen/ModLangProvider.java` | 添加 JEI info 和 tooltip 翻译键 |
| `src/main/resources/assets/cybercultivator/lang/en_us.json` | 添加所有 JEI 翻译键 |

## 验证步骤

1. `./gradlew compileJava` — 编译通过
2. `./gradlew runData` — 生成语言文件
3. `./gradlew build` — 构建成功
4. `./gradlew runClient` — 运行时验证
5. 打开 JEI → 检查 3 个配方类别
6. 切换英文语言 → 确认所有文字正确显示
7. 检查物品信息页面（右键物品查看）
8. 鼠标悬停检查 Tooltip 显示

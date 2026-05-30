# JEI 集成完善设计规范

**日期：** 2026-05-30
**分支：** feature/mod-compatibility-api
**目标：** 修复 JEI 配方展示问题，添加关键信息显示

## 范围

**本次范围：**
- 修复 GeneSplicingCategory 输出错误
- 显示 Activity 值和加工时间（SerumBottlingCategory）
- 显示公式和概率（GeneSplicingCategory + IncubatorOutputCategory）

**不在范围：**
- 进度条动画
- 悬停 Tooltip

## 1. GeneSplicingCategory 改造

### 1.1 问题

当前输出固定为 `fiber_reed_seeds`，实际拼接输出类型跟随 seedA。

### 1.2 DisplayRecipe 扩展

```java
public record DisplayRecipe(
    ItemStack seedA, ItemStack seedB,
    int speedA, int yieldA, int potencyA,
    int speedB, int yieldB, int potencyB,
    double mutationChance
) {}
```

### 1.3 setRecipe() 修改

- 输入槽显示带基因值的种子（通过 NBT 设置默认基因值）
- 输出槽显示与 seedA 同类型的种子 + 基因范围文字

### 1.4 draw() 新增

在配方区域绘制文字信息：
- 父本 A 基因：`A: S:4 Y:7 P:3`
- 父本 B 基因：`B: S:5 Y:4 P:7`
- 突变概率：`突变: 7%`
- 子代基因范围：`子代: S:4-6 Y:5-7 P:4-6`

### 1.5 buildRecipes() 扩展

展示 6 个配方：
- 同类拼接：fiber+fiber, soy+soy, bloom+bloom
- 跨类型拼接：fiber+soy, fiber+bloom, soy+bloom

每个配方使用种子的默认基因值。

### 1.6 基因值设置

```java
private static ItemStack seedWithGenes(ItemStack seed, int speed, int yield, int potency) {
    ItemStack stack = seed.copy();
    stack.getOrCreateTag().putInt("Gene_Speed", speed);
    stack.getOrCreateTag().putInt("Gene_Yield", yield);
    stack.getOrCreateTag().putInt("Gene_Potency", potency);
    return stack;
}
```

## 2. SerumBottlingCategory 改造

### 2.1 setRecipe() 修改

输出槽显示带 Activity NBT 的实际产出物品：
```java
ItemStack output = recipe.getBaseOutput();
// 对于血清配方，设置默认 Activity 值用于展示
if (!output.is(ModItems.SYNAPTIC_NEURAL_BERRY.get())) {
    output.getOrCreateTag().putInt("SynapticActivity", 8); // 展示用默认值
}
builder.addSlot(RecipeIngredientRole.OUTPUT, 91, 11)
        .addItemStack(output);
```

### 2.2 draw() 新增

在配方区域绘制文字信息：
- Activity 值：`活性: 8`（金色文字）
- 加工时间：`15s`

### 2.3 配方名称

在 JEI 标题中显示配方类型名称（通过 getTitle() 返回通用名称，具体配方名在 draw() 中绘制）。

## 3. IncubatorOutputCategory 改造

### 3.1 DisplayRecipe 扩展

```java
public record DisplayRecipe(
    ItemStack seed, ItemStack output,
    String cropName,
    int defaultSpeed, int defaultYield, int defaultPotency
) {}
```

### 3.2 draw() 新增

在配方区域绘制文字信息：
- 默认基因值：`基因: S:4 Y:7 P:3`
- 产出数量范围：`产出: 2-4`（基于 Yield：`2 + yield/3`）
- 生长速率范围：`速率: 0.8x-1.6x`（基于 Speed：`0.5 + speed/10*1.5`）

### 3.3 buildRecipes() 改进

每个种子使用默认基因值：
- Fiber Reed: Speed=4, Yield=7, Potency=3 → 产出 2-4, 速率 1.1x
- Protein Soy: Speed=5, Yield=4, Potency=7 → 产出 2-3, 速率 1.25x
- Alcohol Bloom: Speed=6, Yield=3, Potency=5 → 产出 2-3, 速率 1.4x

## 4. 语言文件

在 ModLangProvider.java 中添加：

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

## 5. 文件修改清单

| 文件 | 修改内容 |
|------|---------|
| `compat/jei/GeneSplicingCategory.java` | 重写：DisplayRecipe 扩展、setRecipe、draw、buildRecipes |
| `compat/jei/SerumBottlingCategory.java` | 添加：draw 方法、输出 Activity 显示 |
| `compat/jei/IncubatorOutputCategory.java` | 重写：DisplayRecipe 扩展、setRecipe、draw、buildRecipes |
| `datagen/ModLangProvider.java` | 添加 JEI 翻译键 |

## 6. 验证

1. `./gradlew compileJava` — 编译通过
2. `./gradlew runClient` — 进入世界
3. 打开 JEI → 检查 3 个配方类别
4. 确认 GeneSplicing 输出不再固定为 fiber_reed_seeds
5. 确认 Activity 和加工时间正确显示
6. 确认基因值和公式正确显示

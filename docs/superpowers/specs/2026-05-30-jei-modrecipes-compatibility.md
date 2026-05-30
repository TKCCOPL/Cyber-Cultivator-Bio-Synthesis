# JEI ↔ ModRecipes 兼容改造计划

**日期：** 2026-05-30
**目标：** 将 JEI 硬编码配方改为从 `ModRecipes` 静态注册表读取，实现第三方 mod 扩展自动显示

## 差距分析

| # | 差距 | 当前 | 目标 |
|---|------|------|------|
| 1 | `IIncubatorOutput` 缺少 JEI 必需字段 | 只有 `getSeedType/getDefaultGenes/getGrowthMultiplier` | +`getOutput()` +`getDisplayName()` |
| 2 | `INCUBATOR_OUTPUTS` 列表为空 | static 块未注册任何默认产出 | 注册 3 个默认实现 |
| 3 | `IncubatorOutputCategory.buildRecipes()` 硬编码 | 3 个配方直接 new | 从 `ModRecipes.getINCUBATOR_OUTPUTS()` 遍历 |
| 4 | `GeneSplicingCategory.buildRecipes()` 硬编码 | 6 个配方直接 new | 从 `INCUBATOR_OUTPUTS` 自动生成同类+跨类 |
| 5 | 突变概率硬编码 0.05/0.09 | 耦合算法细节 | 调用 `getMutationChance()` 动态计算 |

## 修改方案（3 个文件）

### 文件 1：`recipe/ModRecipes.java`

**1a. 扩展 `IIncubatorOutput` 接口（+2 方法）：**
```java
public interface IIncubatorOutput {
    String getSeedType();
    int[] getDefaultGenes();
    double getGrowthMultiplier(int geneSpeed);
    ItemStack getOutput();       // 新增：培养槽产出物品
    String getDisplayName();     // 新增：作物显示名称
}
```

**1b. 在 `static {}` 块注册 3 个默认实现：**

| 种子类型 | 默认基因 | 产出物品 | 显示名称 |
|---------|---------|---------|---------|
| fiber_reed | [4,7,3] | PLANT_FIBER | 纤维草 |
| protein_soy | [5,4,7] | BIOCHEMICAL_SOLUTION | 蛋白质豆 |
| alcohol_bloom | [6,3,5] | INDUSTRIAL_ETHANOL | 酒精花 |

**1c. 新增辅助方法：**
```java
public static ItemStack getSeedItemForType(String seedType)
```
根据 seedType 返回对应种子 ItemStack（供 JEI 构建 DisplayRecipe 使用）。

### 文件 2：`compat/jei/IncubatorOutputCategory.java`

**修改 `buildRecipes()` 方法：**
- 遍历 `ModRecipes.getINCUBATOR_OUTPUTS()`
- 用 `getSeedItemForType()` 获取种子物品
- 用 `getDefaultGenes()` 获取基因值
- 用 `getOutput()` / `getDisplayName()` 构建 DisplayRecipe

### 文件 3：`compat/jei/GeneSplicingCategory.java`

**修改 `buildRecipes()` 方法：**
- 同类拼接：遍历 `INCUBATOR_OUTPUTS`，每种种子与自身拼接
- 跨类拼接：双重循环，所有种子对组合
- 突变概率：调用 `splice.getMutationChance(0, geneDiff)` 动态计算
- geneDiff = 三维度基因差绝对值之和

## 风险评估

| 风险 | 等级 | 说明 |
|------|------|------|
| 存档兼容 | 无 | 纯接口扩展，不涉及序列化/NBT |
| 编译错误 | 低 | 当前无外部 IIncubatorOutput 实现 |
| JEI 显示变化 | 无 | 生成内容与硬编码完全一致 |
| 第三方 mod 兼容 | 正向 | registerIncubatorOutput() 后 JEI 自动显示 |

## 实现顺序

1. `ModRecipes.java` — 接口扩展 + 默认注册 + 辅助方法
2. `IncubatorOutputCategory.java` — buildRecipes() 改读 ModRecipes
3. `GeneSplicingCategory.java` — buildRecipes() 自动生成
4. `./gradlew compileJava` — 编译验证
5. `./gradlew build` — 构建验证

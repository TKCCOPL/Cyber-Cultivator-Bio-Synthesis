# 代码审查修复计划

**日期：** 2026-05-30
**分支：** fix/code-review-findings
**目标：** 修复第二轮代码审查发现的 15 个问题

---

## 问题分类

### 🔴 阻断性问题（2 个）

1. **`cachedRecipe` 未持久化** - 世界重载后配方静默丢失
2. **`SerumConsumeEvent` 取消仍消耗物品** - 事件语义错误

### 🟡 确认的功能问题（5 个）

3. **`GeneSpliceEvent` setter 不回读** - API 契约被违反
4. **`GeneSpliceEvent` synergy 无法清零** - 事件修改被忽略
5. **`SerumCraftEvent` 浅拷贝** - 暴露可变引用
6. **`assemble(Container)` 忽略基因数据** - 标准接口返回错误结果
7. **`GeneSpliceEvent` 存储可变引用** - 污染输入种子

### 🟢 架构改进（8 个）

8-15. 硬编码索引、baseDuration=0、数据源重复等

---

## 修复方案

### Task 1: `cachedRecipe` 持久化

**文件：** `SerumBottlerBlockEntity.java`

**问题：** `cachedRecipe` 是运行时缓存，不持久化。世界重载后 `maxProgress > 0` 但 `cachedRecipe == null`，配方完成时不产出物品。

**方案：** 在 `saveAdditional()` 中保存配方 ID，在 `load()` 中恢复配方引用。

```java
// 新增常量
private static final String TAG_RECIPE_ID = "RecipeId";

// saveAdditional() 中
if (cachedRecipe != null) {
    tag.putString(TAG_RECIPE_ID, cachedRecipe.getId().toString());
}

// load() 中
if (tag.contains(TAG_RECIPE_ID)) {
    ResourceLocation recipeId = new ResourceLocation(tag.getString(TAG_RECIPE_ID));
    // 从 RecipeManager 恢复配方
    level.getRecipeManager().getRecipeFor(ModRecipeTypes.SERUM_BOTTLING.get(), ...)
}
```

**注意：** `load()` 时 `level` 可能为 null，需要延迟恢复。方案：在 `tick()` 中检查 `maxProgress > 0 && cachedRecipe == null` 时尝试恢复。

---

### Task 2: `SerumConsumeEvent` 取消时不消耗物品

**文件：** `SynapticSerumItem.java`

**问题：** 事件取消后仍执行 `stack.shrink(1)`。

**方案：** 将 `stack.shrink(1)` 移到事件检查之后。

```java
if (net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(consumeEvent)) {
    // 事件被取消，不施加效果，不消耗物品
    return stack;
}
scaledDuration = consumeEvent.getDuration();
amp = consumeEvent.getAmplifier();
entity.addEffect(new MobEffectInstance(effect.get(), scaledDuration, amp));

// 消耗物品（只在成功时）
if (entity instanceof Player player && !player.getAbilities().instabuild) {
    stack.shrink(1);
}
```

---

### Task 3: `GeneSpliceEvent` 回读所有 setter

**文件：** `GeneSplicerBlockEntity.java`

**问题：** 只回读 speed/yield/potency，忽略 mutationType/mutationDetail/generation。

**方案：** 在事件处理后回读所有可修改字段。

```java
// 使用事件修改后的值
newSpeed = event.getSpeed();
newYield = event.getYield();
newPotency = event.getPotency();
GeneticSeedItem.setGenes(result, newSpeed, newYield, newPotency);

// 回读 synergy（支持清零）
result.getOrCreateTag().putInt(GeneticSeedItem.GENE_SYNERGY, event.getSynergy());

// 回读 mutation 信息
if (event.isMutation()) {
    result.getOrCreateTag().putInt("Mutation", event.getMutationType());
    result.getOrCreateTag().putString("MutationDetail", event.getMutationDetail());
}

// 回读 generation
result.getOrCreateTag().putInt(GeneticSeedItem.GENE_GENERATION, event.getGeneration());
```

---

### Task 4: `SerumCraftEvent` 深拷贝 inputs

**文件：** `SerumBottlerBlockEntity.java`

**问题：** `inputs.clone()` 只拷贝数组，不拷贝 ItemStack。

**方案：** 深拷贝每个 ItemStack。

```java
// 深拷贝 inputs
ItemStack[] copiedInputs = new ItemStack[inputs.length];
for (int i = 0; i < inputs.length; i++) {
    copiedInputs[i] = inputs[i].copy();
}
SerumCraftEvent craftEvent = new SerumCraftEvent(copiedInputs, result, activity, recipeIndex);
```

---

### Task 5: `assemble(Container)` 使用基因数据

**文件：** `IncubatorOutputRecipe.java`

**问题：** 标准 `Recipe.assemble(Container, RegistryAccess)` 返回裸模板。

**方案：** 从 Container 中获取种子并调用自定义 assemble。

```java
@Override
public ItemStack assemble(Container container, RegistryAccess registryAccess) {
    ItemStack seedStack = container.getItem(0);
    if (seedStack.isEmpty()) return outputItem.copy();
    return assemble(seedStack);
}
```

---

### Task 6: `GeneSpliceEvent` 防御性拷贝

**文件：** `GeneSpliceEvent.java`

**问题：** 存储种子的可变引用。

**方案：** 构造时拷贝种子。

```java
public GeneSpliceEvent(ItemStack seedA, ItemStack seedB, ...) {
    this.seedA = seedA.copy();  // 防御性拷贝
    this.seedB = seedB.copy();
    // ...
}
```

---

### Task 7: `getActiveRecipe()` 使用 ResourceLocation

**文件：** `SerumBottlerBlockEntity.java`, `SerumCraftEvent.java`

**问题：** 硬编码物品类型映射，自定义配方返回 -1。

**方案：** 使用配方 ID 替代整数索引。

```java
// SerumCraftEvent 改为携带 ResourceLocation
private final ResourceLocation recipeId;

// getActiveRecipe() 返回配方 ID
public ResourceLocation getActiveRecipeId() {
    return cachedRecipe != null ? cachedRecipe.getId() : null;
}
```

---

## 实施顺序

| 优先级 | Task | 文件 | 复杂度 |
|---|---|---|---|
| 🔴 P0 | Task 1 | SerumBottlerBlockEntity | 中 |
| 🔴 P0 | Task 2 | SynapticSerumItem | 低 |
| 🟡 P1 | Task 3 | GeneSplicerBlockEntity | 低 |
| 🟡 P1 | Task 4 | SerumBottlerBlockEntity | 低 |
| 🟡 P1 | Task 5 | IncubatorOutputRecipe | 低 |
| 🟡 P1 | Task 6 | GeneSpliceEvent | 低 |
| 🟢 P2 | Task 7 | SerumBottlerBlockEntity, SerumCraftEvent | 中 |

---

## 验证标准

1. `./gradlew compileJava` 通过
2. `./gradlew build` 通过
3. 客户端冒烟测试：
   - 灌装机配方在世界重载后正常完成
   - 血清取消事件不消耗物品
   - 拼接机事件修改正确应用
   - JEI 显示正确

---

## 不修复的问题

| 问题 | 原因 |
|---|---|
| `CropMatureEvent` 取消销毁种子 | 有意为之，防止无限循环 |
| dataSignal 阈值不对称 | 有意为之，`dataSignalThreshold` 给腰带使用 |
| `evaluateCountFormula` 不支持减法 | YAGNI，当前配方不使用 |
| `ModRecipes` 数据源重复 | 向后兼容需要 |
| `getInputs()` 泄露数组 | 需要修改 `SerumRecipe` API，影响范围大 |
| `baseDuration=0` | 需要重构血清物品架构 |
| `cachedSpeed` 缓存陈旧 | 极端场景，实际影响小 |
| Config 字段非 volatile | Forge 单线程模型，实际安全 |

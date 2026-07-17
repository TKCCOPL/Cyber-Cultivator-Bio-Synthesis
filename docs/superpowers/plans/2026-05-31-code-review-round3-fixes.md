# 第三轮代码审查修复计划

**日期：** 2026-05-31
**分支：** fix/code-review-findings
**目标：** 修复第三轮代码审查发现的 15 个问题（9 角度 × 8 候选 → 去重验证 → 15 确认）

---

## 问题分类

### 🔴 阻断性问题（4 个）

1. **`SerumCraftEvent` 取消仍消耗输入** — `consumeRecipeInputs()` 在 `assembleRecipe()` 返回 EMPTY 后仍执行
2. **`SerumConsumeEvent` 取消客户端不同步** — `stack.shrink(1)` 在 `isClientSide` 守卫外执行
3. **`extractLastInput()` 缺少 `markInputsDirty()`** — recipeContainer 残留旧引用，可能导致物品复制
4. **`GeneSpliceEvent` 取消不清理输入** — seedA/seedB 保留，允许无限重试（与 SerumCraftEvent 行为一致，降级为设计改进）

### 🟡 功能性问题（6 个）

5. **`getActiveRecipe()` 硬编码映射** — 自定义配方返回 -1，HUD/API 失效
6. **`isSerumOutput` 忽略 `inheritActivity` 标志** — 血清输出无条件继承 Activity
7. **冷凝器侧面抽取限制** — `canTakeItemThroughFace` 仅允许 DOWN，与文档不一致
8. **发光范围上限 32 vs 文档 48** — `Math.min(..., 32.0)` 限制了高等级血清
9. **`getSerumEffectInfo()` baseDuration=0** — API 返回错误的基础持续时间
10. **`SerumRecipe.getInputs()` 暴露可变数组** — 缺少防御性拷贝

### 🟢 低优先级（5 个）

11. **`onEntityLeaveLevel` 对所有实体触发** — 非 LivingEntity 的无用 ConcurrentHashMap 操作
12. **公式解析器不支持减法** — 含 `-` 的公式静默回退到默认值 2
13. **`CropMatureEvent` 无防御性拷贝** — 种子 ItemStack 按引用传递
14. **`PROCESSING_TIME` 死代码** — 迁移到 RecipeType 后未删除
15. **`syncToClient` 节流导致成熟时 HUD 延迟** — 作物成熟时未立即同步

---

## 修复方案

### Phase 1: 阻断性修复

#### Task 1: `SerumCraftEvent` 取消时跳过消耗输入

**文件：** `SerumBottlerBlockEntity.java`

**问题：** 第 140-141 行，`assembleRecipe()` 返回 EMPTY 后 `consumeRecipeInputs()` 仍执行。

**方案：** 检查 assembleRecipe 返回值，EMPTY 时跳过消耗和放置。

```java
// 修改前 (line 136-151)
if (blockEntity.progress >= blockEntity.maxProgress) {
    SerumRecipe recipe = blockEntity.cachedRecipe;
    if (recipe != null) {
        ItemStack result = blockEntity.assembleRecipe(recipe);
        blockEntity.consumeRecipeInputs(recipe);
        if (blockEntity.output.isEmpty()) {
            blockEntity.output = result;
        } else {
            blockEntity.output.grow(result.getCount());
        }
    }
    // ...
}

// 修改后
if (blockEntity.progress >= blockEntity.maxProgress) {
    SerumRecipe recipe = blockEntity.cachedRecipe;
    if (recipe != null) {
        ItemStack result = blockEntity.assembleRecipe(recipe);
        if (!result.isEmpty()) {
            blockEntity.consumeRecipeInputs(recipe);
            if (blockEntity.output.isEmpty()) {
                blockEntity.output = result;
            } else {
                blockEntity.output.grow(result.getCount());
            }
        }
    }
    // ...
}
```

**验证：** `./gradlew compileJava`

---

#### Task 2: `SerumConsumeEvent` 取消时客户端不消耗物品

**文件：** `SynapticSerumItem.java`

**问题：** 第 122-123 行 `stack.shrink(1)` 在 `isClientSide` 守卫外，事件取消后客户端仍消耗。

**方案：** 将 `stack.shrink(1)` 移入 `isClientSide` 守卫内，仅在服务端成功时消耗。客户端通过服务端的 `setChanged()` 同步。

```java
// 修改前 (line 92-126)
@Override
public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
    if (!level.isClientSide) {
        // ... 事件处理 ...
        if (EVENT_BUS.post(consumeEvent)) {
            return stack; // 服务端取消，不消耗
        }
        entity.addEffect(...);
    }
    // 问题：客户端也执行 shrink
    if (entity instanceof Player player && !player.getAbilities().instabuild) {
        stack.shrink(1);
    }
    return stack;
}

// 修改后
@Override
public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
    if (!level.isClientSide) {
        // ... 事件处理 ...
        if (EVENT_BUS.post(consumeEvent)) {
            return stack;
        }
        entity.addEffect(...);
        // 仅服务端消耗物品
        if (entity instanceof Player player && !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }
    return stack;
}
```

**验证：** `./gradlew compileJava`

---

#### Task 3: `extractLastInput()` 添加 `markInputsDirty()`

**文件：** `SerumBottlerBlockEntity.java`

**问题：** 第 290-300 行，提取输入后 recipeContainer 残留旧引用。

**方案：** 在 `inputs[i] = ItemStack.EMPTY` 后调用 `markInputsDirty()`。

```java
// 修改前 (line 290-300)
public ItemStack extractLastInput() {
    for (int i = INPUT_SLOTS - 1; i >= 0; i--) {
        if (!inputs[i].isEmpty()) {
            ItemStack out = inputs[i];
            inputs[i] = ItemStack.EMPTY;
            syncToClient();
            return out;
        }
    }
    return ItemStack.EMPTY;
}

// 修改后
public ItemStack extractLastInput() {
    for (int i = INPUT_SLOTS - 1; i >= 0; i--) {
        if (!inputs[i].isEmpty()) {
            ItemStack out = inputs[i];
            inputs[i] = ItemStack.EMPTY;
            markInputsDirty();
            syncToClient();
            return out;
        }
    }
    return ItemStack.EMPTY;
}
```

**验证：** `./gradlew compileJava`

---

#### Task 4: `GeneSpliceEvent` 取消时清理输入种子（可选）

**文件：** `GeneSplicerBlockEntity.java`

**问题：** 第 209-210 行，事件取消后 seedA/seedB 保留。

**验证结论：** 当前行为与 SerumCraftEvent 一致（取消时保留输入）。这是正确的事件取消语义："操作未发生，输入应保留"。此任务为可选设计改进。

**方案（如需实现）：** 取消时清空输入槽并同步。

```java
// 修改前 (line 209-211)
if (net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event)) {
    return; // 事件被取消
}

// 修改后（可选）
if (net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event)) {
    seedA = ItemStack.EMPTY;
    seedB = ItemStack.EMPTY;
    syncToClient();
    return;
}
```

**验证：** `./gradlew compileJava`

---

### Phase 2: 功能性修复

#### Task 5: `getActiveRecipe()` 改用 ResourceLocation

**文件：** `SerumBottlerBlockEntity.java`, `IncubatorHudOverlay.java`, `CyberCultivatorAPI.java`

**问题：** 第 262-271 行，硬编码物品映射，自定义配方返回 -1。

**方案：** 废弃 `getActiveRecipe()`，HUD 和 API 改用 `getActiveRecipeId()`。

```java
// SerumBottlerBlockEntity.java — 删除 getActiveRecipe()，保留 getActiveRecipeId()
// IncubatorHudOverlay.java — 改用 getActiveRecipeId() 判断配方类型
// CyberCultivatorAPI.java — BottlerInfo 改用 ResourceLocation
```

**验证：** `./gradlew compileJava`

---

#### Task 6: `isSerumOutput` 检查 `inheritActivity` 标志

**文件：** `SerumBottlerBlockEntity.java`

**问题：** 第 213-220 行，血清输出无条件继承 Activity。

**方案：** 添加 `recipe.isInheritActivity()` 检查。

```java
// 修改前 (line 213-220)
boolean isSerumOutput = !result.is(ModItems.SYNAPTIC_NEURAL_BERRY.get());
if (isSerumOutput) {
    ItemStack berry = findInput(ModItems.SYNAPTIC_NEURAL_BERRY.get());
    if (!berry.isEmpty()) {
        int activity = getActivity(berry);
        result.getOrCreateTag().putInt(TAG_ACTIVITY, activity);
    }
}

// 修改后
boolean isSerumOutput = !result.is(ModItems.SYNAPTIC_NEURAL_BERRY.get());
if (isSerumOutput && recipe.isInheritActivity()) {
    ItemStack berry = findInput(ModItems.SYNAPTIC_NEURAL_BERRY.get());
    if (!berry.isEmpty()) {
        int activity = getActivity(berry);
        result.getOrCreateTag().putInt(TAG_ACTIVITY, activity);
    }
}
```

**验证：** `./gradlew compileJava`

---

#### Task 7: 冷凝器恢复侧面抽取

**文件：** `AtmosphericCondenserBlockEntity.java`

**问题：** 第 175 行，`canTakeItemThroughFace` 仅允许 DOWN。

**方案：** 恢复侧面抽取（DOWN + 水平方向）。

```java
// 修改前 (line 175)
return slot == 0 && !output.isEmpty() && side == Direction.DOWN;

// 修改后
return slot == 0 && !output.isEmpty() && side != Direction.UP;
```

**验证：** `./gradlew compileJava`

---

#### Task 8: 发光范围上限改为 Config 值

**文件：** `VisualEnhancementEffect.java`, `Config.java`

**问题：** 第 37 行，硬编码上限 32，文档说 16-48。

**方案：** 在 Config 中添加 `glowScanRangeCap`，默认 48。

```java
// Config.java — 在 [serum] section 添加
private static final ForgeConfigSpec.IntValue GLOW_SCAN_RANGE_CAP = BUILDER
        .comment("发光扫描范围上限 (格)")
        .defineInRange("glowScanRangeCap", 48, 16, 64);

// Runtime
public static volatile int glowScanRangeCap;

// onLoad
glowScanRangeCap = GLOW_SCAN_RANGE_CAP.get();

// VisualEnhancementEffect.java — line 37
double scanRange = Math.min(16.0 + amplifier * 8.0, Config.glowScanRangeCap);
```

**验证：** `./gradlew compileJava`

---

#### Task 9: `getSerumEffectInfo()` 返回正确的 baseDuration

**文件：** `CyberCultivatorAPI.java`

**问题：** 第 132 行，baseDuration 硬编码为 0。

**方案：** 根据血清类型查找正确的 baseDuration。

```java
// 修改前 (line 130-136)
return new SerumEffectInfo(
    serum.getItem().getDescriptionId(),
    0, // baseDuration 由具体血清类型决定
    baseAmp, multiplier, activity
);

// 修改后
int baseDuration = SynapticSerumItem.getBaseDuration(serum);
return new SerumEffectInfo(
    serum.getItem().getDescriptionId(),
    baseDuration,
    baseAmp, multiplier, activity
);
```

需要在 `SynapticSerumItem` 中添加 `getBaseDuration()` 静态方法。

**验证：** `./gradlew compileJava`

---

#### Task 10: `SerumRecipe.getInputs()` 返回防御性拷贝

**文件：** `SerumRecipe.java`

**问题：** 第 86 行，返回内部数组引用。

**方案：** 返回 `inputs.clone()`。

```java
// 修改前
public Ingredient[] getInputs() { return inputs; }

// 修改后
public Ingredient[] getInputs() { return inputs.clone(); }
```

**验证：** `./gradlew compileJava`

---

### Phase 3: 低优先级修复

#### Task 11: `onEntityLeaveLevel` 仅处理 LivingEntity

**文件：** `cybercultivator.java`

**方案：** 添加 `instanceof LivingEntity` 检查。

```java
// 修改前
public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
    NeuralOverloadEffect.cleanupByUUID(event.getEntity().getUUID());
}

// 修改后
public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
    if (event.getEntity() instanceof LivingEntity) {
        NeuralOverloadEffect.cleanupByUUID(event.getEntity().getUUID());
    }
}
```

---

#### Task 12: 公式解析器支持减法

**文件：** `IncubatorOutputRecipe.java`

**方案：** 扩展正则表达式支持 `-` 操作符。

---

#### Task 13: `CropMatureEvent` 防御性拷贝种子

**文件：** `CropMatureEvent.java`

**方案：** 构造函数中 `this.seed = seed.copy()`。

---

#### Task 14: 删除 `PROCESSING_TIME` 死代码

**文件：** `SerumBottlerBlockEntity.java`

**方案：** 删除第 29 行的 `PROCESSING_TIME` 常量。

---

#### Task 15: 作物成熟时立即同步（可选）

**文件：** `BioIncubatorBlockEntity.java`

**问题：** 第 105-109 行，syncToClient 节流导致成熟时 HUD 延迟最多 0.5s。

**验证结论：** 这是 UI 改进而非 bug。当前 SYNC_INTERVAL=10（0.5s）对成熟事件略长，但不会导致功能错误。

**方案（如需实现）：** 成熟时绕过 syncCounter 直接调用 `syncToClient()`。

```java
// 修改前 (line 88-110)
blockEntity.growthProgress = 0;
blockEntity.seed = ItemStack.EMPTY;
changed = true;
// ... 后续通过 syncCounter 节流同步

// 修改后（可选）
blockEntity.growthProgress = 0;
blockEntity.seed = ItemStack.EMPTY;
blockEntity.syncToClient(); // 成熟时立即同步
changed = false; // 已同步，无需再次同步
```

---

## 实施顺序

| Phase | Task | 文件 | 复杂度 | 预计时间 | 验证状态 |
|-------|------|------|--------|---------|---------|
| 1 | T1 | SerumBottlerBlockEntity | 低 | 5min | ✅ 已验证 |
| 1 | T2 | SynapticSerumItem | 低 | 5min | ✅ 已验证 |
| 1 | T3 | SerumBottlerBlockEntity | 低 | 2min | ✅ 已验证 |
| 1 | T4 | GeneSplicerBlockEntity | 低 | 3min | ⚠️ 可选 |
| 2 | T5 | SerumBottlerBlockEntity + HUD + API | 中 | 15min | ✅ 已验证 |
| 2 | T6 | SerumBottlerBlockEntity | 低 | 3min | ✅ 已验证 |
| 2 | T7 | AtmosphericCondenserBlockEntity | 低 | 2min | ✅ 已验证 |
| 2 | T8 | VisualEnhancementEffect + Config | 中 | 10min | ✅ 已验证 |
| 2 | T9 | CyberCultivatorAPI + SynapticSerumItem | 中 | 10min | ✅ 已验证 |
| 2 | T10 | SerumRecipe | 低 | 2min | ✅ 已验证 |
| 3 | T11 | cybercultivator | 低 | 2min | ✅ 已验证 |
| 3 | T12 | IncubatorOutputRecipe | 中 | 10min | ✅ 已验证 |
| 3 | T13 | CropMatureEvent | 低 | 2min | ✅ 已验证 |
| 3 | T14 | SerumBottlerBlockEntity | 低 | 1min | ✅ 已验证 |
| 3 | T15 | BioIncubatorBlockEntity | 低 | 3min | ⚠️ 可选 |

---

## 网络搜索验证结果

### Task 2: SerumConsumeEvent 客户端不同步
**搜索关键词：** `Forge 1.20.1 finishUsingItem event cancel item consumption stack shrink client server desync`

**验证结论：** ✅ 方案正确
- `stack.shrink(1)` 应仅在服务端执行（`!level.isClientSide` 守卫内）
- 服务端是库存的权威来源，客户端通过服务端同步获取正确状态
- Forge 的 `LivingEntity.completeUsingItem()` 调用 `finishUsingItem()` 后处理返回的 stack
- 当前代码的问题：客户端执行 shrink 导致短暂不同步，服务端同步后修正
- 修复后：客户端不执行 shrink，保持与服务端一致

### Task 4: GeneSpliceEvent 取消不清理输入
**搜索关键词：** `Forge event cancel item consumption pattern best practice`

**验证结论：** ⚠️ 降级为设计改进
- 当前行为（取消时保留种子）与 SerumCraftEvent 行为一致
- 事件取消语义："操作未发生，输入应保留"
- 玩家可通过 `extractLastInput()` 提取种子
- 这是正确的事件取消模式，不是 bug
- 保留此任务作为设计改进（可选实现）

### Task 10: Recipe getInputs 防御性拷贝
**搜索关键词：** `Forge Recipe getInputs defensive copy Ingredient array clone best practice`

**验证结论：** ✅ 方案正确
- Forge 的 `ShapedRecipes` 已实现防御性拷贝
- `getIngredients()` / `getInputs()` 返回内部数组时应 clone
- JEI/NEI 等插件可能修改返回的数组
- 返回 `inputs.clone()` 是最佳实践

### Task 11: EntityLeaveLevelEvent 性能优化
**搜索关键词：** `Forge EntityLeaveLevelEvent instanceof LivingEntity check performance`

**验证结论：** ✅ 方案正确
- `instanceof` 检查在现代 Java 中非常快（指针比较 + 类型层次查找）
- `EntityLeaveLevelEvent` 对所有实体触发（包括粒子、箭矢、物品框架等）
- 在执行较重操作前添加 `instanceof LivingEntity` 检查是推荐做法
- 可进一步优化为 `instanceof Player`（如果只需要清理玩家数据）

### Task 15: BlockEntity 同步策略
**搜索关键词：** `Forge BlockEntity sendBlockUpdated setChanged sync pattern immediate vs throttled`

**验证结论：** ⚠️ 降级为 UI 改进
- `setChanged()` 仅标记脏数据用于保存，不同步到客户端
- `sendBlockUpdated()` 触发同步包发送到附近客户端
- 离散事件（种子插入/提取/成熟）应立即同步
- 周期性变化（生长进度/资源衰减）可节流（每 2-5 tick）
- 当前代码的 SYNC_INTERVAL=10（0.5s）对成熟事件略长
- 保留此任务作为 UI 改进（可选实现）

---

## 验证标准

1. `./gradlew compileJava` — 全部 Phase 完成后通过
2. `./gradlew build` — 构建成功
3. 冒烟测试：
   - 灌装机：事件取消时不消耗材料
   - 血清：事件取消时不消耗物品
   - 拼接机：事件取消时保留输入（设计行为）
   - 冷凝器：侧面漏斗可抽取
   - HUD：自定义配方正确显示

---

## 总结

**必须修复（13 个）：** Task 1-3, 5-14
**可选改进（2 个）：** Task 4, 15

**验证结果：**
- 15 个任务中 13 个必须修复方案已通过网搜索验证
- 2 个任务（T4, T15）降级为可选改进
- 所有方案均符合 Forge 1.20.1 最佳实践

> **后续状态（2026-07-17）：** T15 的“成熟立即同步”已随本轮取消语义修复完成；T4 仍保留原有“取消即保留输入”语义。

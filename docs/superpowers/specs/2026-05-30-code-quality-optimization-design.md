# 代码质量优化设计文档

**日期:** 2026-05-30
**分支:** `refactor/code-quality-optimization`
**范围:** 高 + 中优先级共 10 项修复
**方案:** 每个修复一个 commit，按严重程度排序

---

## 修复清单

| # | 严重度 | 问题 | 涉及文件 |
|---|--------|------|---------|
| 1 | 🔴 | BlockEntity saveAdditional 缺少哨兵写入 | BioIncubatorBlockEntity, SerumBottlerBlockEntity |
| 2 | 🔴 | NeuralOverloadEffect SOURCE_MAP 内存泄漏 | NeuralOverloadEffect |
| 3 | 🔴 | TickTask 捕获实体引用导致潜在泄漏 | SynapticOverclockEffect, MetabolicBoostEffect, VisualEnhancementEffect |
| 4 | 🔴 | Config 字段非 volatile，多线程可见性问题 | Config |
| 5 | 🟠 | BioIncubatorBlock 硬编码注入量未使用 Config | BioIncubatorBlock |
| 6 | 🟠 | BioPulseBeltItem 每秒扫描 343 个方块 | BioPulseBeltItem |
| 7 | 🟠 | SerumBottlerBlockEntity 每 tick 创建 SimpleContainer | SerumBottlerBlockEntity |
| 8 | 🟠 | SerumRecipe.matches() 不验证输入槽多余物品 | SerumRecipe |
| 9 | 🟠 | VisualEnhancementEffect 高 amplifier 扫描范围过大 | VisualEnhancementEffect |
| 10 | 🟠 | IncubatorOutputRecipe 公式解析器不支持常见格式 | IncubatorOutputRecipe |

---

## Fix #1: BlockEntity saveAdditional 哨兵写入

**问题:** `BioIncubatorBlockEntity` 和 `SerumBottlerBlockEntity` 在字段为空时不写入哨兵 tag，导致 `getUpdateTag()` 返回的 tag 可能被 Minecraft 网络层视为无数据，客户端 `load()` 不被调用。

**根因:** `GeneSplicerBlockEntity` 和 `AtmosphericCondenserBlockEntity` 已正确实现哨兵模式，但另外两个 BlockEntity 遗漏了。

**修复方案:**

### BioIncubatorBlockEntity.java (line 263-265)

```java
// 修改前
if (!seed.isEmpty()) {
    tag.put(TAG_SEED, seed.save(new CompoundTag()));
}

// 修改后
if (!seed.isEmpty()) {
    tag.put(TAG_SEED, seed.save(new CompoundTag()));
} else {
    tag.put(TAG_SEED, new CompoundTag()); // sentinel for client sync
}
```

### SerumBottlerBlockEntity.java (line 405-413)

为每个输入槽和输出槽添加哨兵写入：

```java
for (int i = 0; i < INPUT_SLOTS; i++) {
    String key = TAG_INPUT + i;
    if (!inputs[i].isEmpty()) {
        tag.put(key, inputs[i].save(new CompoundTag()));
    } else {
        tag.put(key, new CompoundTag()); // sentinel
    }
}
if (!output.isEmpty()) {
    tag.put(TAG_OUTPUT, output.save(new CompoundTag()));
} else {
    tag.put(TAG_OUTPUT, new CompoundTag()); // sentinel
}
```

**Commit:** `fix(blockentity): saveAdditional 写入空 tag 哨兵确保客户端同步`

---

## Fix #2: NeuralOverloadEffect SOURCE_MAP 内存泄漏

**问题:** `SOURCE_MAP` 是静态 `ConcurrentHashMap<UUID, Integer>`，实体卸载/维度切换时 UUID 不会被清理，导致泄漏。

**根因:** `removeAttributeModifiers()` 只在效果自然过期时调用，实体卸载或命令清除时不触发。

**修复方案:**

在 `NeuralOverloadEffect` 中注册 `PlayerEvent.PlayerLoggedOutEvent` 和 `EntityLeaveLevelEvent` 清理：

```java
// 在 NeuralOverloadEffect 中添加静态方法
public static void cleanupEntity(UUID uuid) {
    SOURCE_MAP.remove(uuid);
}
```

在 `cybercultivator.java` 或专门的事件处理器中注册：

```java
@SubscribeEvent
public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
    NeuralOverloadEffect.cleanupEntity(event.getEntity().getUUID());
}
```

**替代方案（更简洁）:** 将 `ConcurrentHashMap` 改为带过期清理的 cache，或在 `applyEffectTick` 中检查实体是否仍有效，无效则清理。但事件驱动清理更可靠。

**Commit:** `fix(effect): 清理 NeuralOverload SOURCE_MAP 防止实体卸载后内存泄漏`

---

## Fix #3: TickTask 捕获实体引用

**问题:** 三个正面效果类在 `removeAttributeModifiers()` 中通过 TickTask lambda 捕获 `entity` 强引用。如果实体在下一 tick 被移除，lambda 阻止 GC。

**根因:** lambda 闭包持有 `LivingEntity` 引用。

**修复方案:**

在 TickTask 执行前检查实体是否仍然有效：

```java
entity.level().getServer().tell(new TickTask(
    entity.level().getServer().getTickCount() + 1,
    () -> {
        // 添加有效性检查
        if (entity.isRemoved() || !entity.isAlive()) return;
        NeuralOverloadEffect.setSource(entity, 1);
        entity.addEffect(new MobEffectInstance(
                ModEffects.NEURAL_OVERLOAD.get(),
                20 * (12 + amplifier * 4),
                amplifier));
    }
));
```

三个效果类（SynapticOverclockEffect, MetabolicBoostEffect, VisualEnhancementEffect）统一修改。

**Commit:** `fix(effect): TickTask 执行前检查实体有效性防止泄漏`

---

## Fix #4: Config 字段非 volatile

**问题:** Config 的 `public static` 字段在 mod 加载线程写入，在 server tick 线程读取。Java 内存模型不保证跨线程可见性。

**根因:** 缺少 `volatile` 修饰符。

**修复方案:**

将所有运行时 Config 字段添加 `volatile`：

```java
// 修改前
public static int mutationRange;
public static double mutationChanceBase;
// ...

// 修改后
public static volatile int mutationRange;
public static volatile double mutationChanceBase;
// ...
```

影响字段列表（Config.java:144-178）：共 28 个字段全部添加 `volatile`。

**Commit:** `fix(config): 运行时字段添加 volatile 保证多线程可见性`

---

## Fix #5: BioIncubatorBlock 硬编码注入量

**问题:** `BioIncubatorBlock.use()` 中 `addNutrition(25)` 和 `addDataSignal(15)` 硬编码，未使用 `Config.nutritionInjectAmount` 和 `Config.dataSignalInjectAmount`。

**根因:** 开发初期硬编码，后添加 Config 时遗漏更新。

**修复方案:**

```java
// 修改前 (line 76)
blockEntity.addNutrition(25);

// 修改后
blockEntity.addNutrition(Config.nutritionInjectAmount);

// 修改前 (line 85)
blockEntity.addDataSignal(15);

// 修改后
blockEntity.addDataSignal(Config.dataSignalInjectAmount);
```

同时检查 `addPurity(20)` 是否也应改为 `Config.purityInjectAmount`（虽然默认值相同，但保持一致性）。

**Commit:** `fix(block): BioIncubatorBlock 注入量改用 Config 值`

---

## Fix #6: BioPulseBeltItem 扫描性能优化

**问题:** 默认 range=3 时扫描 7×7×7=343 个 BlockPos，每秒执行一次，`level.getBlockEntity()` 调用次数过多。

**根因:** 暴力遍历所有方块位置。

**修复方案:**

**方案 A（推荐）:** 降低扫描频率 + 硬编码上限

```java
// 每 5 秒扫描一次（而非每秒）
if (level.getGameTime() % 100L != 0L) return;

// 硬编码 range 上限 5（11³=1331，仍可接受）
int range = Math.min(Config.beltScanRange, 5);
```

**方案 B:** 缓存已知培养槽位置。首次扫描后记住位置，后续只检查缓存位置。复杂度高，对单人项目不值得。

**Commit:** `perf(curios): BioPulseBelt 降低扫描频率并限制范围上限`

---

## Fix #7: SerumBottlerBlockEntity SimpleContainer 缓存

**问题:** `findRecipe()` 每 tick 创建新 `SimpleContainer`，空闲时造成不必要的对象分配。

**根因:** 容器在每次查询时重新创建。

**修复方案:**

将 `SimpleContainer` 提升为字段，在输入变更时重建：

```java
private final SimpleContainer recipeContainer = new SimpleContainer(INPUT_SLOTS);
private boolean inputsDirty = true; // 输入变更时标记

private void markInputsDirty() {
    inputsDirty = true;
}

private SerumRecipe findRecipe() {
    if (level == null) return null;
    if (inputsDirty) {
        for (int i = 0; i < INPUT_SLOTS; i++) {
            recipeContainer.setItem(i, inputs[i]);
        }
        inputsDirty = false;
    }
    return level.getRecipeManager()
            .getAllRecipesFor(ModRecipeTypes.SERUM_BOTTLING.get())
            .stream()
            .filter(r -> r.matches(recipeContainer, level))
            .findFirst()
            .orElse(null);
}
```

在 `setItem()`, `removeItem()`, `load()` 等修改输入的方法中调用 `markInputsDirty()`。

**Commit:** `perf(bottler): 缓存 SimpleContainer 避免每 tick 分配`

---

## Fix #8: SerumRecipe.matches() 输入数量验证

**问题:** 匹配算法只检查配方所需 inputs 是否能在容器中找到，不检查容器中是否有额外非空槽位。

**根因:** 贪心匹配只关注"能否找到"，不关注"是否有剩余"。

**修复方案:**

在匹配成功后，检查容器中是否还有未匹配的非空槽位。如果有，说明有多余物品，配方不应匹配（除非配方本身允许任意输入）。

```java
@Override
public boolean matches(Container container, Level level) {
    boolean[] matched = new boolean[inputs.length];
    int matchedCount = 0;
    for (int i = 0; i < container.getContainerSize(); i++) {
        ItemStack slotStack = container.getItem(i);
        if (slotStack.isEmpty()) continue;
        boolean slotMatched = false;
        for (int j = 0; j < inputs.length; j++) {
            if (!matched[j] && inputs[j].test(slotStack)) {
                matched[j] = true;
                matchedCount++;
                slotMatched = true;
                break;
            }
        }
        // 如果有非空槽位无法匹配任何配方输入，拒绝
        if (!slotMatched) return false;
    }
    return matchedCount == inputs.length;
}
```

**注意:** 需要验证现有 4 个配方（berry_synthesis 需 3 输入，s01/s02/s03 各需 3 输入）是否都填满了 3 个 Ingredient。如果都是 3 输入对 3 槽位，此修复影响为零（但防御性更好）。

**Commit:** `fix(recipe): SerumRecipe.matches() 拒绝含多余物品的容器`

---

## Fix #9: VisualEnhancementEffect 扫描范围上限

**问题:** amplifier=7 时扫描范围 72 格，AABB 体积巨大，`getEntitiesOfClass` 遍历开销大。

**根因:** `16.0 + amplifier * 8.0` 无上限。

**修复方案:**

硬编码扫描范围上限 32 格（amplifier=2 的水平）：

```java
double scanRange = Math.min(16.0 + amplifier * 8.0, 32.0);
```

或使用 Config 可配置上限（但增加 Config 复杂度不值得，硬编码即可）。

**Commit:** `fix(effect): VisualEnhancementEffect 扫描范围上限 32 格`

---

## Fix #10: IncubatorOutputRecipe 公式解析器增强

**问题:** 解析器只支持 `N + yield / M` 和 `N + yield * M` 格式，不支持 `yield / M + N`（操作数顺序反转）。

**根因:** 解析逻辑假设加法的左侧是常量，右侧包含 yield。

**修复方案:**

重构解析器，先替换 yield 为实际值，再按运算符优先级求值（先乘除后加减）：

```java
private int evaluateCountFormula(int yieldValue) {
    String formula = countFormula.trim();

    // 纯常量
    try {
        return Integer.parseInt(formula);
    } catch (NumberFormatException ignored) {}

    // 替换 yield 为实际值
    formula = formula.replace("yield", String.valueOf(yieldValue));

    try {
        // 先处理乘除（从左到右）
        // 正则匹配 "数字 运算符 数字" 的乘除表达式
        java.util.regex.Pattern mulDiv = java.util.regex.Pattern.compile("(\\d+)\\s*([*/])\\s*(\\d+)");
        java.util.regex.Matcher matcher;
        String current = formula;
        while ((matcher = mulDiv.matcher(current)).find()) {
            int a = Integer.parseInt(matcher.group(1));
            String op = matcher.group(2);
            int b = Integer.parseInt(matcher.group(3));
            int result = "*".equals(op) ? a * b : (b == 0 ? 0 : a / b);
            current = current.substring(0, matcher.start()) + result + current.substring(matcher.end());
        }

        // 再处理加减
        String[] addParts = current.split("\\+");
        int total = 0;
        for (String part : addParts) {
            total += Integer.parseInt(part.trim());
        }
        return total;
    } catch (Exception e) {
        return 2; // 保底
    }
}
```

**Commit:** `fix(recipe): IncubatorOutputRecipe 公式解析器支持操作数任意顺序`

---

## 实施顺序

按严重程度排序，每个修复一个 commit：

1. `fix(blockentity): saveAdditional 写入空 tag 哨兵确保客户端同步`
2. `fix(effect): 清理 NeuralOverload SOURCE_MAP 防止实体卸载后内存泄漏`
3. `fix(effect): TickTask 执行前检查实体有效性防止泄漏`
4. `fix(config): 运行时字段添加 volatile 保证多线程可见性`
5. `fix(block): BioIncubatorBlock 注入量改用 Config 值`
6. `perf(curios): BioPulseBelt 降低扫描频率并限制范围上限`
7. `perf(bottler): 缓存 SimpleContainer 避免每 tick 分配`
8. `fix(recipe): SerumRecipe.matches() 拒绝含多余物品的容器`
9. `fix(effect): VisualEnhancementEffect 扫描范围上限 32 格`
10. `fix(recipe): IncubatorOutputRecipe 公式解析器支持操作数任意顺序`

## 验证标准

每个 commit 完成后执行：
- `./gradlew compileJava` — 编译通过
- `./gradlew build` — 构建通过
- `./gradlew runClient` — 冒烟测试：放置/使用机器、饮用血清、佩戴饰品

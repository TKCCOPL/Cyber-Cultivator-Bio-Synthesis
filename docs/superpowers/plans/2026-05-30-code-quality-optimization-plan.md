# 代码质量优化实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复代码审查发现的 10 项高/中优先级问题，提升稳定性、性能和可维护性。

**Architecture:** 每个修复独立 commit，按严重程度排序。修改集中在 BlockEntity 同步、Effect 生命周期、Config 线程安全、性能优化四个维度。

**Tech Stack:** Minecraft Forge 1.20.1, Java 17, Parchment mappings

---

## 文件变更总览

| 文件 | 变更类型 | 关联 Task |
|------|---------|----------|
| `src/main/java/com/TKCCOPL/block/entity/BioIncubatorBlockEntity.java` | Modify | #1 |
| `src/main/java/com/TKCCOPL/block/entity/SerumBottlerBlockEntity.java` | Modify | #1, #7 |
| `src/main/java/com/TKCCOPL/effect/NeuralOverloadEffect.java` | Modify | #2 |
| `src/main/java/com/TKCCOPL/effect/SynapticOverclockEffect.java` | Modify | #3 |
| `src/main/java/com/TKCCOPL/effect/MetabolicBoostEffect.java` | Modify | #3 |
| `src/main/java/com/TKCCOPL/effect/VisualEnhancementEffect.java` | Modify | #3, #9 |
| `src/main/java/com/TKCCOPL/Config.java` | Modify | #4 |
| `src/main/java/com/TKCCOPL/block/BioIncubatorBlock.java` | Modify | #5 |
| `src/main/java/com/TKCCOPL/curios/BioPulseBeltItem.java` | Modify | #6 |
| `src/main/java/com/TKCCOPL/recipe/SerumRecipe.java` | Modify | #8 |
| `src/main/java/com/TKCCOPL/recipe/IncubatorOutputRecipe.java` | Modify | #10 |
| `src/main/java/com/TKCCOPL/cybercultivator.java` | Modify | #2 |

---

### Task 1: BlockEntity saveAdditional 哨兵写入

**Files:**
- Modify: `src/main/java/com/TKCCOPL/block/entity/BioIncubatorBlockEntity.java:257-266`
- Modify: `src/main/java/com/TKCCOPL/block/entity/SerumBottlerBlockEntity.java:400-413`

- [ ] **Step 1: 修改 BioIncubatorBlockEntity.saveAdditional()**

在 `src/main/java/com/TKCCOPL/block/entity/BioIncubatorBlockEntity.java` 的 `saveAdditional` 方法中，为 seed 字段添加哨兵写入：

```java
// 修改前 (line 263-265)
if (!seed.isEmpty()) {
    tag.put(TAG_SEED, seed.save(new CompoundTag()));
}

// 修改后
if (!seed.isEmpty()) {
    tag.put(TAG_SEED, seed.save(new CompoundTag()));
} else {
    tag.put(TAG_SEED, new CompoundTag()); // sentinel: ensure tag is non-empty for client sync
}
```

- [ ] **Step 2: 修改 SerumBottlerBlockEntity.saveAdditional()**

在 `src/main/java/com/TKCCOPL/block/entity/SerumBottlerBlockEntity.java` 的 `saveAdditional` 方法中，为输入槽和输出槽添加哨兵写入：

```java
// 修改前 (line 405-413)
for (int i = 0; i < INPUT_SLOTS; i++) {
    if (!inputs[i].isEmpty()) {
        tag.put(TAG_INPUT + i, inputs[i].save(new CompoundTag()));
    }
}
if (!output.isEmpty()) {
    tag.put(TAG_OUTPUT, output.save(new CompoundTag()));
}

// 修改后
for (int i = 0; i < INPUT_SLOTS; i++) {
    String key = TAG_INPUT + i;
    if (!inputs[i].isEmpty()) {
        tag.put(key, inputs[i].save(new CompoundTag()));
    } else {
        tag.put(key, new CompoundTag()); // sentinel: ensure tag is non-empty for client sync
    }
}
if (!output.isEmpty()) {
    tag.put(TAG_OUTPUT, output.save(new CompoundTag()));
} else {
    tag.put(TAG_OUTPUT, new CompoundTag()); // sentinel: ensure tag is non-empty for client sync
}
```

- [ ] **Step 3: 编译验证**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/TKCCOPL/block/entity/BioIncubatorBlockEntity.java \
        src/main/java/com/TKCCOPL/block/entity/SerumBottlerBlockEntity.java
git commit -m "fix(blockentity): saveAdditional 写入空 tag 哨兵确保客户端同步"
```

---

### Task 2: NeuralOverloadEffect SOURCE_MAP 内存泄漏修复

**Files:**
- Modify: `src/main/java/com/TKCCOPL/effect/NeuralOverloadEffect.java`
- Modify: `src/main/java/com/TKCCOPL/cybercultivator.java`

- [ ] **Step 1: 在 NeuralOverloadEffect 中添加清理方法**

在 `src/main/java/com/TKCCOPL/effect/NeuralOverloadEffect.java` 中，在 `clearSource(LivingEntity)` 方法后添加：

```java
/**
 * 按 UUID 清理来源信息。用于实体卸载时防止内存泄漏。
 */
public static void cleanupByUUID(UUID uuid) {
    SOURCE_MAP.remove(uuid);
}
```

- [ ] **Step 2: 在主类中注册 EntityLeaveLevelEvent**

在 `src/main/java/com/TKCCOPL/cybercultivator.java` 中，添加事件监听。在类末尾（`ClientModEvents` 内部类之前）添加：

```java
@SubscribeEvent
public static void onEntityLeaveLevel(net.minecraftforge.event.entity.EntityLeaveLevelEvent event) {
    NeuralOverloadEffect.cleanupByUUID(event.getEntity().getUUID());
}
```

需要在文件顶部添加 import：
```java
import com.TKCCOPL.effect.NeuralOverloadEffect;
```

- [ ] **Step 3: 编译验证**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/TKCCOPL/effect/NeuralOverloadEffect.java \
        src/main/java/com/TKCCOPL/cybercultivator.java
git commit -m "fix(effect): 清理 NeuralOverload SOURCE_MAP 防止实体卸载后内存泄漏"
```

---

### Task 3: TickTask 执行前检查实体有效性

**Files:**
- Modify: `src/main/java/com/TKCCOPL/effect/SynapticOverclockEffect.java:63-78`
- Modify: `src/main/java/com/TKCCOPL/effect/MetabolicBoostEffect.java:60-76`
- Modify: `src/main/java/com/TKCCOPL/effect/VisualEnhancementEffect.java:47-63`

- [ ] **Step 1: 修改 SynapticOverclockEffect.removeAttributeModifiers()**

在 `src/main/java/com/TKCCOPL/effect/SynapticOverclockEffect.java` 的 `removeAttributeModifiers` 方法中，在 TickTask lambda 内部添加实体有效性检查：

```java
// 修改前 (line 66-77)
entity.level().getServer().tell(new TickTask(
        entity.level().getServer().getTickCount() + 1,
        () -> {
            NeuralOverloadEffect.setSource(entity, 1);
            entity.addEffect(new MobEffectInstance(
                    ModEffects.NEURAL_OVERLOAD.get(),
                    20 * (12 + amplifier * 4),
                    amplifier));
        }
));

// 修改后
entity.level().getServer().tell(new TickTask(
        entity.level().getServer().getTickCount() + 1,
        () -> {
            if (entity.isRemoved() || !entity.isAlive()) return;
            NeuralOverloadEffect.setSource(entity, 1);
            entity.addEffect(new MobEffectInstance(
                    ModEffects.NEURAL_OVERLOAD.get(),
                    20 * (12 + amplifier * 4),
                    amplifier));
        }
));
```

- [ ] **Step 2: 修改 MetabolicBoostEffect.removeAttributeModifiers()**

在 `src/main/java/com/TKCCOPL/effect/MetabolicBoostEffect.java` 的 `removeAttributeModifiers` 方法中，同样的修改：

```java
// 修改后
entity.level().getServer().tell(new TickTask(
        entity.level().getServer().getTickCount() + 1,
        () -> {
            if (entity.isRemoved() || !entity.isAlive()) return;
            NeuralOverloadEffect.setSource(entity, 3);
            entity.addEffect(new MobEffectInstance(
                    ModEffects.NEURAL_OVERLOAD.get(),
                    20 * (12 + amplifier * 4),
                    amplifier));
        }
));
```

- [ ] **Step 3: 修改 VisualEnhancementEffect.removeAttributeModifiers()**

在 `src/main/java/com/TKCCOPL/effect/VisualEnhancementEffect.java` 的 `removeAttributeModifiers` 方法中，同样的修改：

```java
// 修改后
entity.level().getServer().tell(new net.minecraft.server.TickTask(
    entity.level().getServer().getTickCount() + 1,
    () -> {
        if (entity.isRemoved() || !entity.isAlive()) return;
        NeuralOverloadEffect.setSource(entity, 2);
        entity.addEffect(new MobEffectInstance(ModEffects.NEURAL_OVERLOAD.get(),
                20 * (12 + amplifier * 4), amplifier));
    }
));
```

- [ ] **Step 4: 编译验证**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/TKCCOPL/effect/SynapticOverclockEffect.java \
        src/main/java/com/TKCCOPL/effect/MetabolicBoostEffect.java \
        src/main/java/com/TKCCOPL/effect/VisualEnhancementEffect.java
git commit -m "fix(effect): TickTask 执行前检查实体有效性防止泄漏"
```

---

### Task 4: Config 字段添加 volatile

**Files:**
- Modify: `src/main/java/com/TKCCOPL/Config.java:144-178`

- [ ] **Step 1: 为所有运行时字段添加 volatile**

在 `src/main/java/com/TKCCOPL/Config.java` 中，将 `// === Runtime values ===` 注释下的所有 `public static` 字段添加 `volatile` 修饰符：

```java
// === Runtime values ===
// genes
public static volatile int mutationRange;
public static volatile double mutationChanceBase;
public static volatile double mutationChancePerGen;
public static volatile double mutationChancePerGeneDiff;
public static volatile int geneMin;
public static volatile int geneMax;
// serum
public static volatile int s01BaseDuration;
public static volatile int s02BaseDuration;
public static volatile int s03BaseDuration;
public static volatile int stackAmplifierCap;
public static volatile int stackDurationCap;
public static volatile int activityThresholdForBonus;
public static volatile double durationMultiplierBase;
public static volatile double durationMultiplierPerActivity;
// incubator
public static volatile int maturationThreshold;
public static volatile int resourceThreshold;
public static volatile int nutritionDecayInterval;
public static volatile int purityDecayInterval;
public static volatile int dataSignalDecayInterval;
public static volatile int nutritionInjectAmount;
public static volatile int purityInjectAmount;
public static volatile int dataSignalInjectAmount;
public static volatile int matureNutritionCost;
public static volatile int maturePurityCost;
// curios
public static volatile int beltScanRange;
public static volatile int beltNutritionThreshold;
public static volatile int beltPurityThreshold;
public static volatile int beltDataSignalThreshold;
public static volatile int packEffectReductionRate;
public static volatile float packHealThreshold;
public static volatile int packHealCooldown;
public static volatile int monocleHudRange;
```

- [ ] **Step 2: 编译验证**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/TKCCOPL/Config.java
git commit -m "fix(config): 运行时字段添加 volatile 保证多线程可见性"
```

---

### Task 5: BioIncubatorBlock 注入量改用 Config 值

**Files:**
- Modify: `src/main/java/com/TKCCOPL/block/BioIncubatorBlock.java:62-91`

- [ ] **Step 1: 添加 Config import**

在 `src/main/java/com/TKCCOPL/block/BioIncubatorBlock.java` 顶部添加：

```java
import com.TKCCOPL.Config;
```

- [ ] **Step 2: 替换硬编码值**

```java
// 修改前 (line 76)
blockEntity.addNutrition(25);

// 修改后
blockEntity.addNutrition(Config.nutritionInjectAmount);

// 修改前 (line 63)
blockEntity.addPurity(20);

// 修改后
blockEntity.addPurity(Config.purityInjectAmount);

// 修改前 (line 85)
blockEntity.addDataSignal(15);

// 修改后
blockEntity.addDataSignal(Config.dataSignalInjectAmount);
```

- [ ] **Step 3: 更新 sendMachineStatus 中的硬编码描述**

在 `sendMachineStatus` 方法的调用处，将硬编码数值改为动态：

```java
// 修改前
sendMachineStatus(player, blockEntity, "注入纯净水 +20");
sendMachineStatus(player, blockEntity, "注入营养液 +25");
sendMachineStatus(player, blockEntity, "注入数据信号 +15");

// 修改后
sendMachineStatus(player, blockEntity, "注入纯净水 +" + Config.purityInjectAmount);
sendMachineStatus(player, blockEntity, "注入营养液 +" + Config.nutritionInjectAmount);
sendMachineStatus(player, blockEntity, "注入数据信号 +" + Config.dataSignalInjectAmount);
```

- [ ] **Step 4: 编译验证**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/TKCCOPL/block/BioIncubatorBlock.java
git commit -m "fix(block): BioIncubatorBlock 注入量改用 Config 值"
```

---

### Task 6: BioPulseBelt 扫描性能优化

**Files:**
- Modify: `src/main/java/com/TKCCOPL/curios/BioPulseBeltItem.java:17-24`

- [ ] **Step 1: 降低扫描频率并限制范围上限**

在 `src/main/java/com/TKCCOPL/curios/BioPulseBeltItem.java` 的 `tick` 方法中：

```java
// 修改前 (line 19)
if (level.isClientSide || level.getGameTime() % 20L != 0L) return;

int range = Config.beltScanRange;

// 修改后
if (level.isClientSide || level.getGameTime() % 100L != 0L) return; // 每 5 秒扫描一次

int range = Math.min(Config.beltScanRange, 5); // 硬编码上限 5（11³=1331）
```

- [ ] **Step 2: 编译验证**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/TKCCOPL/curios/BioPulseBeltItem.java
git commit -m "perf(curios): BioPulseBelt 降低扫描频率并限制范围上限"
```

---

### Task 7: SerumBottlerBlockEntity SimpleContainer 缓存

**Files:**
- Modify: `src/main/java/com/TKCCOPL/block/entity/SerumBottlerBlockEntity.java`

- [ ] **Step 1: 添加缓存字段**

在 `SerumBottlerBlockEntity` 类中，在 `cachedRecipe` 字段后添加：

```java
private final SimpleContainer recipeContainer = new SimpleContainer(INPUT_SLOTS);
private boolean inputsDirty = true;
```

- [ ] **Step 2: 添加标记方法**

在 `cancelProcessing()` 方法后添加：

```java
private void markInputsDirty() {
    inputsDirty = true;
}
```

- [ ] **Step 3: 修改 findRecipe() 使用缓存容器**

```java
// 修改前 (line 144-156)
private SerumRecipe findRecipe() {
    if (level == null) return null;
    SimpleContainer container = new SimpleContainer(INPUT_SLOTS);
    for (int i = 0; i < INPUT_SLOTS; i++) {
        container.setItem(i, inputs[i]);
    }
    return level.getRecipeManager()
            .getAllRecipesFor(ModRecipeTypes.SERUM_BOTTLING.get())
            .stream()
            .filter(r -> r.matches(container, level))
            .findFirst()
            .orElse(null);
}

// 修改后
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

- [ ] **Step 4: 在修改输入的方法中标记 dirty**

在以下方法中添加 `markInputsDirty()` 调用：

`setItem()` 方法：
```java
// 修改前 (line 348-353)
public void setItem(int slot, ItemStack stack) {
    if (slot < INPUT_SLOTS) {
        inputs[slot] = stack;
        setChanged();
    }
}

// 修改后
public void setItem(int slot, ItemStack stack) {
    if (slot < INPUT_SLOTS) {
        inputs[slot] = stack;
        markInputsDirty();
        setChanged();
    }
}
```

`removeItem()` 方法（输入槽分支）：
```java
// 修改前 (line 320-326)
if (slot < INPUT_SLOTS && !inputs[slot].isEmpty()) {
    int taken = Math.min(amount, inputs[slot].getCount());
    ItemStack result = inputs[slot].split(taken);
    if (inputs[slot].isEmpty()) inputs[slot] = ItemStack.EMPTY;
    setChanged();
    return result;
}

// 修改后
if (slot < INPUT_SLOTS && !inputs[slot].isEmpty()) {
    int taken = Math.min(amount, inputs[slot].getCount());
    ItemStack result = inputs[slot].split(taken);
    if (inputs[slot].isEmpty()) inputs[slot] = ItemStack.EMPTY;
    markInputsDirty();
    setChanged();
    return result;
}
```

`removeItemNoUpdate()` 方法（输入槽分支）：
```java
// 修改前 (line 338-343)
if (slot < INPUT_SLOTS) {
    ItemStack out = inputs[slot];
    inputs[slot] = ItemStack.EMPTY;
    setChanged();
    return out;
}

// 修改后
if (slot < INPUT_SLOTS) {
    ItemStack out = inputs[slot];
    inputs[slot] = ItemStack.EMPTY;
    markInputsDirty();
    setChanged();
    return out;
}
```

`clearContent()` 方法：
```java
// 修改前 (line 361-367)
public void clearContent() {
    for (int i = 0; i < INPUT_SLOTS; i++) {
        inputs[i] = ItemStack.EMPTY;
    }
    output = ItemStack.EMPTY;
    setChanged();
}

// 修改后
public void clearContent() {
    for (int i = 0; i < INPUT_SLOTS; i++) {
        inputs[i] = ItemStack.EMPTY;
    }
    output = ItemStack.EMPTY;
    markInputsDirty();
    setChanged();
}
```

`load()` 方法末尾添加：
```java
// 在 load() 方法的最后添加
markInputsDirty();
```

`consumeRecipeInputs()` 方法末尾添加：
```java
// 在 consumeRecipeInputs() 方法的最后添加
markInputsDirty();
```

- [ ] **Step 5: 编译验证**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/TKCCOPL/block/entity/SerumBottlerBlockEntity.java
git commit -m "perf(bottler): 缓存 SimpleContainer 避免每 tick 分配"
```

---

### Task 8: SerumRecipe.matches() 拒绝多余物品

**Files:**
- Modify: `src/main/java/com/TKCCOPL/recipe/SerumRecipe.java:33-49`

- [ ] **Step 1: 修改匹配算法**

在 `src/main/java/com/TKCCOPL/recipe/SerumRecipe.java` 的 `matches` 方法中：

```java
// 修改前 (line 33-49)
@Override
public boolean matches(Container container, Level level) {
    boolean[] matched = new boolean[inputs.length];
    for (int i = 0; i < container.getContainerSize(); i++) {
        ItemStack slotStack = container.getItem(i);
        if (slotStack.isEmpty()) continue;
        for (int j = 0; j < inputs.length; j++) {
            if (!matched[j] && inputs[j].test(slotStack)) {
                matched[j] = true;
                break;
            }
        }
    }
    for (boolean m : matched) {
        if (!m) return false;
    }
    return true;
}

// 修改后
@Override
public boolean matches(Container container, Level level) {
    boolean[] matched = new boolean[inputs.length];
    for (int i = 0; i < container.getContainerSize(); i++) {
        ItemStack slotStack = container.getItem(i);
        if (slotStack.isEmpty()) continue;
        boolean slotMatched = false;
        for (int j = 0; j < inputs.length; j++) {
            if (!matched[j] && inputs[j].test(slotStack)) {
                matched[j] = true;
                slotMatched = true;
                break;
            }
        }
        // 非空槽位无法匹配任何配方输入 → 拒绝
        if (!slotMatched) return false;
    }
    for (boolean m : matched) {
        if (!m) return false;
    }
    return true;
}
```

- [ ] **Step 2: 编译验证**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/TKCCOPL/recipe/SerumRecipe.java
git commit -m "fix(recipe): SerumRecipe.matches() 拒绝含多余物品的容器"
```

---

### Task 9: VisualEnhancementEffect 扫描范围上限

**Files:**
- Modify: `src/main/java/com/TKCCOPL/effect/VisualEnhancementEffect.java:37`

- [ ] **Step 1: 添加范围上限**

在 `src/main/java/com/TKCCOPL/effect/VisualEnhancementEffect.java` 的 `applyEffectTick` 方法中：

```java
// 修改前 (line 37)
double scanRange = 16.0 + amplifier * 8.0;

// 修改后
double scanRange = Math.min(16.0 + amplifier * 8.0, 32.0);
```

- [ ] **Step 2: 编译验证**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/TKCCOPL/effect/VisualEnhancementEffect.java
git commit -m "fix(effect): VisualEnhancementEffect 扫描范围上限 32 格"
```

---

### Task 10: IncubatorOutputRecipe 公式解析器增强

**Files:**
- Modify: `src/main/java/com/TKCCOPL/recipe/IncubatorOutputRecipe.java:65-101`

- [ ] **Step 1: 重构 evaluateCountFormula 方法**

在 `src/main/java/com/TKCCOPL/recipe/IncubatorOutputRecipe.java` 中，替换整个 `evaluateCountFormula` 方法：

```java
// 修改前 (line 65-101)
private int evaluateCountFormula(int yieldValue) {
    String formula = countFormula.trim();

    // 纯常量
    try {
        return Integer.parseInt(formula);
    } catch (NumberFormatException ignored) {}

    // 替换 yield 为实际值
    formula = formula.replace("yield", String.valueOf(yieldValue));

    // 解析 "A + B / C" 或 "A + B * C" 格式
    try {
        // 按 + 分割
        String[] addParts = formula.split("\\+");
        int result = 0;
        for (String part : addParts) {
            part = part.trim();
            if (part.contains("/")) {
                String[] divParts = part.split("/");
                int dividend = Integer.parseInt(divParts[0].trim());
                int divisor = Integer.parseInt(divParts[1].trim());
                result += divisor == 0 ? 0 : dividend / divisor;
            } else if (part.contains("*")) {
                String[] mulParts = part.split("\\*");
                int a = Integer.parseInt(mulParts[0].trim());
                int b = Integer.parseInt(mulParts[1].trim());
                result += a * b;
            } else {
                result += Integer.parseInt(part);
            }
        }
        return result;
    } catch (Exception e) {
        return 2; // 保底
    }
}

// 修改后
private int evaluateCountFormula(int yieldValue) {
    String formula = countFormula.trim();

    // 纯常量
    try {
        return Integer.parseInt(formula);
    } catch (NumberFormatException ignored) {}

    // 替换 yield 为实际值
    formula = formula.replace("yield", String.valueOf(yieldValue));

    try {
        // 先处理乘除（从左到右，支持操作数任意顺序）
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

        // 再处理加法
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

- [ ] **Step 2: 编译验证**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/TKCCOPL/recipe/IncubatorOutputRecipe.java
git commit -m "fix(recipe): IncubatorOutputRecipe 公式解析器支持操作数任意顺序"
```

---

## 最终验证

全部 10 个 commit 完成后执行：

- [ ] **Step 1: 编译检查**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 构建检查**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 冒烟测试**

Run: `./gradlew runClient`
验证项：
1. 进入世界，放置培养槽、灌装机、冷凝器、拼接机
2. 培养槽：放入种子、注入纯净水/营养液/数据信号，观察 HUD
3. 灌装机：放入材料，等待加工完成，取出产物
4. 饮用血清 S-01/S-02/S-03，等待过期触发副作用
5. 佩戴腰带，观察自动注入
6. 拼接种子，取出结果

- [ ] **Step 4: 合并分支**

```bash
git checkout feature/mod-compatibility-api
git merge refactor/code-quality-optimization
```

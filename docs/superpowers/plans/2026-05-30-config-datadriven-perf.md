# Config 扩展 + 数据驱动 + KubeJS + 性能优化 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将硬编码数值提取为 Config TOML、种子产出改为 JSON RecipeType、集成 KubeJS、优化培养槽 tick 性能。

**Architecture:** 三个独立阶段按顺序实施。Config 先行（让数值可调），数据驱动次之（让内容可扩展），性能优化最后（不改变功能行为）。

**Tech Stack:** Forge 1.20.1, ForgeConfigSpec, RecipeType, KubeJS (compileOnly), JEI (compileOnly)

**规范文档：** `docs/superpowers/specs/2026-05-30-config-datadriven-perf-design.md`

---

## 阶段 1：Config 扩展

### Task 1: 重构 Config.java 为 push/pop 分组

**Files:**
- Modify: `src/main/java/com/TKCCOPL/Config.java`

- [ ] **Step 1: 重写 Config.java**

将扁平字段重构为 4 个 section（genes / serum / incubator / curios），使用 `push/pop` 分组。

```java
package com.TKCCOPL;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = cybercultivator.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // === [genes] 基因参数 ===
    static {
        BUILDER.push("genes");
    }
    private static final ForgeConfigSpec.IntValue MUTATION_RANGE = BUILDER
            .comment("随机变异范围 ±N")
            .defineInRange("mutationRange", 2, 1, 5);
    private static final ForgeConfigSpec.DoubleValue MUTATION_CHANCE_BASE = BUILDER
            .comment("基础突变概率")
            .defineInRange("mutationChanceBase", 0.05, 0.01, 0.20);
    private static final ForgeConfigSpec.DoubleValue MUTATION_CHANCE_PER_GEN = BUILDER
            .comment("每代增加突变概率")
            .defineInRange("mutationChancePerGen", 0.02, 0.0, 0.10);
    private static final ForgeConfigSpec.DoubleValue MUTATION_CHANCE_PER_GENE_DIFF = BUILDER
            .comment("每点基因差异增加概率")
            .defineInRange("mutationChancePerGeneDiff", 0.01, 0.0, 0.05);
    private static final ForgeConfigSpec.IntValue GENE_MIN = BUILDER
            .comment("基因下限")
            .defineInRange("geneMin", 1, 1, 5);
    private static final ForgeConfigSpec.IntValue GENE_MAX = BUILDER
            .comment("基因上限")
            .defineInRange("geneMax", 10, 5, 10);
    static {
        BUILDER.pop();
    }

    // === [serum] 血清参数 ===
    static {
        BUILDER.push("serum");
    }
    private static final ForgeConfigSpec.IntValue S01_BASE_DURATION = BUILDER
            .comment("S-01 基础持续时间 tick (25s = 500)")
            .defineInRange("s01BaseDuration", 500, 100, 12000);
    private static final ForgeConfigSpec.IntValue S02_BASE_DURATION = BUILDER
            .comment("S-02 基础持续时间 tick (30s = 600)")
            .defineInRange("s02BaseDuration", 600, 100, 12000);
    private static final ForgeConfigSpec.IntValue S03_BASE_DURATION = BUILDER
            .comment("S-03 基础持续时间 tick (15s = 300)")
            .defineInRange("s03BaseDuration", 300, 100, 12000);
    private static final ForgeConfigSpec.IntValue STACK_AMPLIFIER_CAP = BUILDER
            .comment("叠加 amplifier 上限")
            .defineInRange("stackAmplifierCap", 7, 1, 10);
    private static final ForgeConfigSpec.IntValue STACK_DURATION_CAP = BUILDER
            .comment("持续时间累加上限 tick (5min = 6000)")
            .defineInRange("stackDurationCap", 6000, 600, 60000);
    private static final ForgeConfigSpec.IntValue ACTIVITY_THRESHOLD_FOR_BONUS = BUILDER
            .comment("高品质起步 amplifier=1 的阈值")
            .defineInRange("activityThresholdForBonus", 8, 1, 10);
    private static final ForgeConfigSpec.DoubleValue DURATION_MULTIPLIER_BASE = BUILDER
            .comment("持续时间倍率基数")
            .defineInRange("durationMultiplierBase", 0.5, 0.1, 2.0);
    private static final ForgeConfigSpec.DoubleValue DURATION_MULTIPLIER_PER_ACTIVITY = BUILDER
            .comment("每点活性增加倍率")
            .defineInRange("durationMultiplierPerActivity", 0.1, 0.01, 0.5);
    static {
        BUILDER.pop();
    }

    // === [incubator] 培养槽参数 ===
    static {
        BUILDER.push("incubator");
    }
    private static final ForgeConfigSpec.IntValue MATURATION_THRESHOLD = BUILDER
            .comment("成熟所需生长进度")
            .defineInRange("maturationThreshold", 200, 50, 1000);
    private static final ForgeConfigSpec.IntValue RESOURCE_THRESHOLD = BUILDER
            .comment("资源消耗阈值")
            .defineInRange("resourceThreshold", 10, 0, 50);
    private static final ForgeConfigSpec.IntValue NUTRITION_DECAY_INTERVAL = BUILDER
            .comment("营养衰减间隔 tick")
            .defineInRange("nutritionDecayInterval", 20, 5, 200);
    private static final ForgeConfigSpec.IntValue PURITY_DECAY_INTERVAL = BUILDER
            .comment("纯净衰减间隔 tick")
            .defineInRange("purityDecayInterval", 40, 5, 200);
    private static final ForgeConfigSpec.IntValue DATA_SIGNAL_DECAY_INTERVAL = BUILDER
            .comment("信号衰减间隔 tick")
            .defineInRange("dataSignalDecayInterval", 60, 5, 200);
    private static final ForgeConfigSpec.IntValue NUTRITION_INJECT_AMOUNT = BUILDER
            .comment("生化原液注入量")
            .defineInRange("nutritionInjectAmount", 25, 1, 100);
    private static final ForgeConfigSpec.IntValue PURITY_INJECT_AMOUNT = BUILDER
            .comment("纯净水注入量")
            .defineInRange("purityInjectAmount", 20, 1, 100);
    private static final ForgeConfigSpec.IntValue DATA_SIGNAL_INJECT_AMOUNT = BUILDER
            .comment("硅碎片注入量")
            .defineInRange("dataSignalInjectAmount", 15, 1, 100);
    private static final ForgeConfigSpec.IntValue MATURE_NUTRITION_COST = BUILDER
            .comment("成熟时营养消耗")
            .defineInRange("matureNutritionCost", 5, 0, 50);
    private static final ForgeConfigSpec.IntValue MATURE_PURITY_COST = BUILDER
            .comment("成熟时纯净消耗")
            .defineInRange("maturePurityCost", 5, 0, 50);
    static {
        BUILDER.pop();
    }

    // === [curios] 饰品参数 ===
    static {
        BUILDER.push("curios");
    }
    private static final ForgeConfigSpec.IntValue BELT_SCAN_RANGE = BUILDER
            .comment("腰带扫描范围 (格)")
            .defineInRange("scanRange", 3, 1, 8);
    private static final ForgeConfigSpec.IntValue BELT_NUTRITION_THRESHOLD = BUILDER
            .comment("营养度注入阈值")
            .defineInRange("nutritionThreshold", 50, 0, 100);
    private static final ForgeConfigSpec.IntValue BELT_PURITY_THRESHOLD = BUILDER
            .comment("纯净度注入阈值")
            .defineInRange("purityThreshold", 50, 0, 100);
    private static final ForgeConfigSpec.IntValue BELT_DATA_SIGNAL_THRESHOLD = BUILDER
            .comment("数据信号注入阈值")
            .defineInRange("dataSignalThreshold", 25, 0, 100);
    private static final ForgeConfigSpec.IntValue PACK_EFFECT_REDUCTION_RATE = BUILDER
            .comment("支持箱副作用消减速率")
            .defineInRange("effectReductionRate", 2, 1, 10);
    private static final ForgeConfigSpec.DoubleValue PACK_HEAL_THRESHOLD = BUILDER
            .comment("支持箱治疗阈值")
            .defineInRange("healThreshold", 6.0, 1.0, 20.0);
    private static final ForgeConfigSpec.IntValue PACK_HEAL_COOLDOWN = BUILDER
            .comment("支持箱治疗冷却 tick (1200 = 60s)")
            .defineInRange("healCooldown", 1200, 200, 6000);
    private static final ForgeConfigSpec.IntValue MONOCLE_HUD_RANGE = BUILDER
            .comment("单片镜 HUD 距离 (格)")
            .defineInRange("hudRange", 8, 3, 16);
    static {
        BUILDER.pop();
    }

    static final ForgeConfigSpec SPEC = BUILDER.build();

    // === Runtime values ===
    // genes
    public static int mutationRange;
    public static double mutationChanceBase;
    public static double mutationChancePerGen;
    public static double mutationChancePerGeneDiff;
    public static int geneMin;
    public static int geneMax;
    // serum
    public static int s01BaseDuration;
    public static int s02BaseDuration;
    public static int s03BaseDuration;
    public static int stackAmplifierCap;
    public static int stackDurationCap;
    public static int activityThresholdForBonus;
    public static double durationMultiplierBase;
    public static double durationMultiplierPerActivity;
    // incubator
    public static int maturationThreshold;
    public static int resourceThreshold;
    public static int nutritionDecayInterval;
    public static int purityDecayInterval;
    public static int dataSignalDecayInterval;
    public static int nutritionInjectAmount;
    public static int purityInjectAmount;
    public static int dataSignalInjectAmount;
    public static int matureNutritionCost;
    public static int maturePurityCost;
    // curios
    public static int beltScanRange;
    public static int beltNutritionThreshold;
    public static int beltPurityThreshold;
    public static int beltDataSignalThreshold;
    public static int packEffectReductionRate;
    public static float packHealThreshold;
    public static int packHealCooldown;
    public static int monocleHudRange;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        // genes
        mutationRange = MUTATION_RANGE.get();
        mutationChanceBase = MUTATION_CHANCE_BASE.get();
        mutationChancePerGen = MUTATION_CHANCE_PER_GEN.get();
        mutationChancePerGeneDiff = MUTATION_CHANCE_PER_GENE_DIFF.get();
        geneMin = GENE_MIN.get();
        geneMax = GENE_MAX.get();
        // serum
        s01BaseDuration = S01_BASE_DURATION.get();
        s02BaseDuration = S02_BASE_DURATION.get();
        s03BaseDuration = S03_BASE_DURATION.get();
        stackAmplifierCap = STACK_AMPLIFIER_CAP.get();
        stackDurationCap = STACK_DURATION_CAP.get();
        activityThresholdForBonus = ACTIVITY_THRESHOLD_FOR_BONUS.get();
        durationMultiplierBase = DURATION_MULTIPLIER_BASE.get();
        durationMultiplierPerActivity = DURATION_MULTIPLIER_PER_ACTIVITY.get();
        // incubator
        maturationThreshold = MATURATION_THRESHOLD.get();
        resourceThreshold = RESOURCE_THRESHOLD.get();
        nutritionDecayInterval = NUTRITION_DECAY_INTERVAL.get();
        purityDecayInterval = PURITY_DECAY_INTERVAL.get();
        dataSignalDecayInterval = DATA_SIGNAL_DECAY_INTERVAL.get();
        nutritionInjectAmount = NUTRITION_INJECT_AMOUNT.get();
        purityInjectAmount = PURITY_INJECT_AMOUNT.get();
        dataSignalInjectAmount = DATA_SIGNAL_INJECT_AMOUNT.get();
        matureNutritionCost = MATURE_NUTRITION_COST.get();
        maturePurityCost = MATURE_PURITY_COST.get();
        // curios
        beltScanRange = BELT_SCAN_RANGE.get();
        beltNutritionThreshold = BELT_NUTRITION_THRESHOLD.get();
        beltPurityThreshold = BELT_PURITY_THRESHOLD.get();
        beltDataSignalThreshold = BELT_DATA_SIGNAL_THRESHOLD.get();
        packEffectReductionRate = PACK_EFFECT_REDUCTION_RATE.get();
        packHealThreshold = PACK_HEAL_THRESHOLD.get().floatValue();
        packHealCooldown = PACK_HEAL_COOLDOWN.get();
        monocleHudRange = MONOCLE_HUD_RANGE.get();
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/TKCCOPL/Config.java
git commit -m "refactor(config): 重构为 push/pop 分组 + 新增 genes/serum/incubator section"
```

---

### Task 2: 消费端读取 Config 值 — GeneSplicerBlockEntity

**Files:**
- Modify: `src/main/java/com/TKCCOPL/block/entity/GeneSplicerBlockEntity.java`

- [ ] **Step 1: 添加 Config import 并替换硬编码值**

在 `GeneSplicerBlockEntity.java` 中：

1. 添加 `import com.TKCCOPL.Config;`
2. `craftOutput()` 方法中替换硬编码值：

```java
// 替换: double mutationChance = 0.05 + maxGen * 0.02 + maxDiff * 0.01;
double mutationChance = Config.mutationChanceBase + maxGen * Config.mutationChancePerGen + maxDiff * Config.mutationChancePerGeneDiff;

// 替换: int newSpeed = GeneticSeedItem.clampGene((speedA + speedB) / 2 + random.nextInt(5) - 2);
int newSpeed = GeneticSeedItem.clampGene((speedA + speedB) / 2 + random.nextInt(Config.mutationRange * 2 + 1) - Config.mutationRange);
int newYield = GeneticSeedItem.clampGene((yieldA + yieldB) / 2 + random.nextInt(Config.mutationRange * 2 + 1) - Config.mutationRange);
int newPotency = GeneticSeedItem.clampGene((potencyA + potencyB) / 2 + random.nextInt(Config.mutationRange * 2 + 1) - Config.mutationRange);

// 替换: int bonus = random.nextInt(9) - 4;
int bonusRange = Config.mutationRange * 2 + 1;
int bonus = random.nextInt(bonusRange * 2 - 1) - (bonusRange - 1);

// 替换: newSpeed = GeneticSeedItem.clampGene(...)
// 使用 Config.geneMin / Config.geneMax 替代 GeneticSeedItem.clampGene 的硬编码 1-10
```

3. `GeneticSeedItem.clampGene()` 也需要改为读取 Config：

```java
// 在 GeneticSeedItem 中
public static int clampGene(int value) {
    return Math.max(Config.geneMin, Math.min(Config.geneMax, value));
}
```

- [ ] **Step 2: 编译验证**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/TKCCOPL/block/entity/GeneSplicerBlockEntity.java src/main/java/com/TKCCOPL/item/GeneticSeedItem.java
git commit -m "refactor(config): 拼接机读取 Config 基因参数替代硬编码"
```

---

### Task 3: 消费端读取 Config 值 — BioIncubatorBlockEntity

**Files:**
- Modify: `src/main/java/com/TKCCOPL/block/entity/BioIncubatorBlockEntity.java`

- [ ] **Step 1: 添加 Config import 并替换硬编码值**

1. 添加 `import com.TKCCOPL.Config;`
2. 删除硬编码常量，改用 Config：
```java
// 删除:
// private static final int MATURATION_THRESHOLD = 200;
// private static final int RESOURCE_THRESHOLD = 10;
```
3. `tick()` 中替换：
```java
// 替换: if (level.getGameTime() % 20L == 0L && blockEntity.nutrition > 0)
if (level.getGameTime() % (long) Config.nutritionDecayInterval == 0L && blockEntity.nutrition > 0) {
    blockEntity.nutrition -= 1;
    changed = true;
}
if (level.getGameTime() % (long) Config.purityDecayInterval == 0L && blockEntity.purity > 0) {
    blockEntity.purity -= 1;
    changed = true;
}
if (level.getGameTime() % (long) Config.dataSignalDecayInterval == 0L && blockEntity.dataSignal > 0) {
    blockEntity.dataSignal -= 1;
    changed = true;
}

// 替换: if (blockEntity.nutrition > RESOURCE_THRESHOLD ...)
if (blockEntity.nutrition > Config.resourceThreshold
        && blockEntity.purity > Config.resourceThreshold
        && blockEntity.dataSignal > 0) {

// 替换: if (blockEntity.growthProgress >= MATURATION_THRESHOLD)
if (blockEntity.growthProgress >= Config.maturationThreshold) {

// 替换: blockEntity.nutrition = Math.max(0, blockEntity.nutrition - 5);
blockEntity.nutrition = Math.max(0, blockEntity.nutrition - Config.matureNutritionCost);
blockEntity.purity = Math.max(0, blockEntity.purity - Config.maturePurityCost);
```
4. `addNutrition/addPurity/addDataSignal` 中替换注入量：
```java
// BioPulseBeltItem 中已硬编码 25/20/15，改为读取 Config
```
5. `getGrowthPercent()` 和 `getEstimatedSecondsRemaining()` 中替换 `MATURATION_THRESHOLD`：
```java
// 替换: if (seed.isEmpty() || MATURATION_THRESHOLD <= 0) return 0;
if (seed.isEmpty() || Config.maturationThreshold <= 0) return 0;
return Math.min(100, (int) ((long) growthProgress * 100 / Config.maturationThreshold));

// 替换: int remaining = MATURATION_THRESHOLD - growthProgress;
int remaining = Config.maturationThreshold - growthProgress;
```
6. `getCurrentGrowthRate()` 中替换 `RESOURCE_THRESHOLD`：
```java
if (nutrition <= Config.resourceThreshold || purity <= Config.resourceThreshold || dataSignal <= 0) return 0;
```

- [ ] **Step 2: 编译验证**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/TKCCOPL/block/entity/BioIncubatorBlockEntity.java
git commit -m "refactor(config): 培养槽读取 Config 参数替代硬编码"
```

---

### Task 4: 消费端读取 Config 值 — SynapticSerumItem

**Files:**
- Modify: `src/main/java/com/TKCCOPL/item/SynapticSerumItem.java`

- [ ] **Step 1: 添加 Config import 并替换硬编码值**

1. 添加 `import com.TKCCOPL.Config;`
2. `getScaledDuration()` 中替换：
```java
// 替换: double multiplier = 0.5 + activity * 0.1;
double multiplier = Config.durationMultiplierBase + activity * Config.durationMultiplierPerActivity;
```
3. `getBaseAmplifier()` 中替换：
```java
// 替换: return activity >= 8 ? 1 : 0;
return activity >= Config.activityThresholdForBonus ? 1 : 0;
```
4. `finishUsingItem()` 中替换：
```java
// 替换: amp = Math.min(existing.getAmplifier() + 1, MAX_AMPLIFIER);
amp = Math.min(existing.getAmplifier() + 1, Config.stackAmplifierCap);

// 替换: scaledDuration = Math.min(scaledDuration + existing.getDuration(), 20 * 300);
scaledDuration = Math.min(scaledDuration + existing.getDuration(), Config.stackDurationCap);
```

- [ ] **Step 2: 改造 SynapticSerumItem 构造函数为 Supplier 模式**

血清物品在 `ModItems.java` 中注册（DeferredRegister lambda），注册阶段 Config 可能尚未加载。需改用 `Supplier<Integer>` 延迟读取。

在 `SynapticSerumItem.java` 中：

```java
// 新增字段
private final Supplier<Integer> durationSupplier;

// 新增构造函数（Supplier 版本）
public SynapticSerumItem(Properties properties, Supplier<MobEffect> effect, Supplier<Integer> durationSupplier, int amplifier) {
    super(properties);
    this.effect = effect;
    this.durationSupplier = durationSupplier;
    this.amplifier = amplifier;
}

// 保留旧构造函数（向后兼容，固定值转为 Supplier）
public SynapticSerumItem(Properties properties, Supplier<MobEffect> effect, int durationTicks, int amplifier) {
    this(properties, effect, () -> durationTicks, amplifier);
}

// 默认构造函数也改为 Supplier
public SynapticSerumItem(Properties properties) {
    this(properties, ModEffects.SYNAPTIC_OVERCLOCK, () -> Config.s01BaseDuration, 0);
}

// finishUsingItem 中替换:
// 旧: int scaledDuration = getScaledDuration(durationTicks, activity);
// 新:
int scaledDuration = getScaledDuration(durationSupplier.get(), activity);
```

- [ ] **Step 3: 更新 ModItems.java 中血清注册**

在 `ModItems.java` 中，3 个血清物品注册改为 Supplier 模式：

```java
// S-01: 使用默认构造函数，已自动读取 Config.s01BaseDuration
public static final RegistryObject<Item> SYNAPTIC_SERUM_S01 = ITEMS.register("synaptic_serum_s01",
        () -> new SynapticSerumItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));

// S-02: 改用 Supplier（注意 RegistryObject 需要 .get()）
public static final RegistryObject<Item> SYNAPTIC_SERUM_S02 = ITEMS.register("synaptic_serum_s02",
        () -> new SynapticSerumItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE),
                () -> ModEffects.VISUAL_ENHANCEMENT.get(), () -> Config.s02BaseDuration, 0));

// S-03: 改用 Supplier
public static final RegistryObject<Item> SYNAPTIC_SERUM_S03 = ITEMS.register("synaptic_serum_s03",
        () -> new SynapticSerumItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE),
                () -> ModEffects.METABOLIC_BOOST.get(), () -> Config.s03BaseDuration, 0));
```

添加 import：
```java
import com.TKCCOPL.Config;
```

- [ ] **Step 4: 编译验证**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 提交**

```bash
git add src/main/java/com/TKCCOPL/item/SynapticSerumItem.java src/main/java/com/TKCCOPL/init/ModItems.java
git commit -m "refactor(config): 血清效果读取 Config 参数 — Supplier 延迟模式"
```

---

### Task 5: 消费端读取 Config 值 — CurioEventHandler + Belt + Pack

**Files:**
- Modify: `src/main/java/com/TKCCOPL/curios/BioPulseBeltItem.java`
- Modify: `src/main/java/com/TKCCOPL/curios/LifeSupportPackItem.java`

- [ ] **Step 1: BioPulseBeltItem 替换注入量硬编码**

```java
// 替换: incubator.addNutrition(25);
incubator.addNutrition(Config.nutritionInjectAmount);
// 替换: incubator.addPurity(20);
incubator.addPurity(Config.purityInjectAmount);
// 替换: incubator.addDataSignal(15);
incubator.addDataSignal(Config.dataSignalInjectAmount);
```

- [ ] **Step 2: LifeSupportPackItem 无需改动**

`LifeSupportPackItem` 已经读取 `Config.packEffectReductionRate` / `Config.packHealThreshold` / `Config.packHealCooldown`，无需修改。

- [ ] **Step 3: 编译验证**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 提交**

```bash
git add src/main/java/com/TKCCOPL/curios/BioPulseBeltItem.java
git commit -m "refactor(config): 腰带注入量读取 Config 参数"
```

---

### Task 6: Config 全阶段编译验证 + 运行时冒烟

- [ ] **Step 1: 完整编译**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 运行客户端冒烟测试**

Run: `./gradlew runClient`
验证：
1. 进入世界
2. 放置培养槽 → 注入营养/纯净/信号 → 确认衰减速度正常
3. 放入种子 → 确认生长进度推进
4. 使用拼接机 → 确认基因变异范围合理
5. 饮用血清 → 确认效果持续时间正确
6. 检查 `run/config/cybercultivator-common.toml` 是否生成了 4 个 section

- [ ] **Step 3: 提交**

```bash
git add -A && git commit -m "refactor(config): Config 扩展完成 — genes/serum/incubator/curios 四分组"
```

---

## 阶段 2：数据驱动 + KubeJS

### Task 7: 新增 IncubatorOutputRecipe + Serializer

**Files:**
- Create: `src/main/java/com/TKCCOPL/recipe/IncubatorOutputRecipe.java`
- Create: `src/main/java/com/TKCCOPL/recipe/IncubatorOutputSerializer.java`
- Modify: `src/main/java/com/TKCCOPL/recipe/ModRecipeTypes.java`

- [ ] **Step 1: 创建 IncubatorOutputRecipe**

```java
package com.TKCCOPL.recipe;

import com.TKCCOPL.item.GeneticSeedItem;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * 培养槽产出配方（JSON 数据驱动）。
 * 定义种子类型 → 作物产出的映射关系。
 */
public class IncubatorOutputRecipe implements net.minecraft.world.item.crafting.Recipe<Container> {
    private final ResourceLocation id;
    private final ItemStack seedItem;      // 匹配用的种子物品
    private final ItemStack outputItem;    // 输出物品模板
    private final String countFormula;     // "2 + yield / 3"
    private final String qualityTag;       // "Potency" / "Purity" / "Concentration"
    private final int[] defaultGenes;      // [speed, yield, potency] — JEI 展示用
    private final String cropName;         // 作物显示名称 — JEI 展示用

    public IncubatorOutputRecipe(ResourceLocation id, ItemStack seedItem, ItemStack outputItem,
                                  String countFormula, String qualityTag, int[] defaultGenes,
                                  String cropName) {
        this.id = id;
        this.seedItem = seedItem;
        this.outputItem = outputItem;
        this.countFormula = countFormula;
        this.qualityTag = qualityTag;
        this.defaultGenes = defaultGenes;
        this.cropName = cropName;
    }

    /** 匹配种子物品（基于 Item 类型，不比较 NBT） */
    public boolean matches(ItemStack seed) {
        return seed.getItem() == seedItem.getItem();
    }

    @Override
    public boolean matches(Container container, Level level) {
        return !container.getItem(0).isEmpty() && matches(container.getItem(0));
    }

    /** 根据种子基因值计算产出 */
    public ItemStack assemble(ItemStack seedStack) {
        int yield = GeneticSeedItem.getGene(seedStack, GeneticSeedItem.GENE_YIELD);
        int potency = GeneticSeedItem.getGene(seedStack, GeneticSeedItem.GENE_POTENCY);
        int count = evaluateCountFormula(yield);

        ItemStack result = new ItemStack(outputItem.getItem(), count);
        if (!qualityTag.isEmpty()) {
            result.getOrCreateTag().putInt(qualityTag, potency);
        }
        return result;
    }

    /**
     * 简单公式求值: "2 + yield / 3" → 2 + yieldValue / 3
     * 支持格式: "N", "N + yield / M", "N + yield * M"
     * 整数除法，向下取整
     */
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

    @Override
    public ItemStack assemble(Container container, RegistryAccess registryAccess) {
        return outputItem.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return false;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return outputItem.copy();
    }

    @Override
    public ResourceLocation getId() { return id; }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.INCUBATOR_OUTPUT_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.INCUBATOR_OUTPUT.get();
    }

    // Accessors
    public ItemStack getSeedItem() { return seedItem.copy(); }
    public ItemStack getOutputItem() { return outputItem.copy(); }
    public String getCountFormula() { return countFormula; }
    public String getQualityTag() { return qualityTag; }
    public int[] getDefaultGenes() { return defaultGenes; }
    public String getCropName() { return cropName; }
}
```

- [ ] **Step 2: 创建 IncubatorOutputSerializer**

```java
package com.TKCCOPL.recipe;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.crafting.CraftingHelper;

import javax.annotation.Nullable;

public class IncubatorOutputSerializer implements net.minecraft.world.item.crafting.RecipeSerializer<IncubatorOutputRecipe> {
    @Override
    public IncubatorOutputRecipe fromJson(ResourceLocation id, JsonObject json) {
        JsonObject seedJson = GsonHelper.getAsJsonObject(json, "seed");
        ItemStack seedItem = CraftingHelper.getItemStack(seedJson, true);

        JsonObject outputJson = GsonHelper.getAsJsonObject(json, "output");
        ItemStack outputItem = CraftingHelper.getItemStack(outputJson, true);

        String countFormula = GsonHelper.getAsString(json, "count_formula", "2 + yield / 3");
        String qualityTag = GsonHelper.getAsString(json, "quality_tag", "");
        String cropName = GsonHelper.getAsString(json, "crop_name", "");

        JsonObject genesJson = GsonHelper.getAsJsonObject(json, "default_genes");
        int[] defaultGenes = {
            GsonHelper.getAsInt(genesJson, "speed", 5),
            GsonHelper.getAsInt(genesJson, "yield", 5),
            GsonHelper.getAsInt(genesJson, "potency", 5)
        };

        return new IncubatorOutputRecipe(id, seedItem, outputItem, countFormula, qualityTag, defaultGenes, cropName);
    }

    @Nullable
    @Override
    public IncubatorOutputRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
        ItemStack seedItem = buf.readItem();
        ItemStack outputItem = buf.readItem();
        String countFormula = buf.readUtf();
        String qualityTag = buf.readUtf();
        String cropName = buf.readUtf();
        int[] defaultGenes = { buf.readVarInt(), buf.readVarInt(), buf.readVarInt() };
        return new IncubatorOutputRecipe(id, seedItem, outputItem, countFormula, qualityTag, defaultGenes, cropName);
    }

    @Override
    public void toNetwork(FriendlyByteBuf buf, IncubatorOutputRecipe recipe) {
        buf.writeItem(recipe.getSeedItem());
        buf.writeItem(recipe.getOutputItem());
        buf.writeUtf(recipe.getCountFormula());
        buf.writeUtf(recipe.getQualityTag());
        buf.writeUtf(recipe.getCropName());
        for (int gene : recipe.getDefaultGenes()) {
            buf.writeVarInt(gene);
        }
    }
}
```

- [ ] **Step 3: 注册 RecipeType 到 ModRecipeTypes**

在 `ModRecipeTypes.java` 中新增：
```java
public static final RegistryObject<RecipeType<IncubatorOutputRecipe>> INCUBATOR_OUTPUT =
        RECIPE_TYPES.register("incubator_output",
                () -> new RecipeType<>() {
                    @Override
                    public String toString() {
                        return "cybercultivator:incubator_output";
                    }
                });

public static final RegistryObject<RecipeSerializer<IncubatorOutputRecipe>> INCUBATOR_OUTPUT_SERIALIZER =
        SERIALIZERS.register("incubator_output", IncubatorOutputSerializer::new);
```

- [ ] **Step 4: 编译验证**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 提交**

```bash
git add src/main/java/com/TKCCOPL/recipe/IncubatorOutputRecipe.java src/main/java/com/TKCCOPL/recipe/IncubatorOutputSerializer.java src/main/java/com/TKCCOPL/recipe/ModRecipeTypes.java
git commit -m "feat(recipe): 新增 IncubatorOutputRecipe RecipeType + Serializer"
```

---

### Task 8: 创建 JSON 配方文件 + datagen

**Files:**
- Modify: `src/main/java/com/TKCCOPL/datagen/ModRecipeProvider.java`

- [ ] **Step 1: 在 ModRecipeProvider 中添加 IncubatorOutput 配方生成**

由于 IncubatorOutputRecipe 不是标准 `FinishedRecipe`，需手写 JSON 文件或创建自定义 FinishedRecipe 实现。

**方案：手写 JSON 文件（更简单直接）**

创建 3 个 JSON 文件：

`src/main/resources/data/cybercultivator/recipes/incubator/fiber_reed.json`:
```json
{
  "type": "cybercultivator:incubator_output",
  "seed": { "item": "cybercultivator:fiber_reed_seeds" },
  "output": { "item": "cybercultivator:plant_fiber" },
  "count_formula": "2 + yield / 3",
  "quality_tag": "Potency",
  "crop_name": "纤维草",
  "default_genes": { "speed": 4, "yield": 7, "potency": 3 }
}
```

`src/main/resources/data/cybercultivator/recipes/incubator/protein_soy.json`:
```json
{
  "type": "cybercultivator:incubator_output",
  "seed": { "item": "cybercultivator:protein_soy_seeds" },
  "output": { "item": "cybercultivator:biochemical_solution" },
  "count_formula": "2 + yield / 3",
  "quality_tag": "Concentration",
  "crop_name": "蛋白质豆",
  "default_genes": { "speed": 5, "yield": 4, "potency": 7 }
}
```

`src/main/resources/data/cybercultivator/recipes/incubator/alcohol_bloom.json`:
```json
{
  "type": "cybercultivator:incubator_output",
  "seed": { "item": "cybercultivator:alcohol_bloom_seeds" },
  "output": { "item": "cybercultivator:industrial_ethanol" },
  "count_formula": "2 + yield / 3",
  "quality_tag": "Purity",
  "crop_name": "酒精花",
  "default_genes": { "speed": 6, "yield": 3, "potency": 5 }
}
```

- [ ] **Step 2: 编译验证**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交**

```bash
git add src/main/resources/data/cybercultivator/recipes/incubator/
git commit -m "feat(recipe): 新增 3 个 IncubatorOutput JSON 配方"
```

---

### Task 9: BioIncubatorBlockEntity.getCropOutput() 改用 RecipeManager

**Files:**
- Modify: `src/main/java/com/TKCCOPL/block/entity/BioIncubatorBlockEntity.java`

- [ ] **Step 1: 重写 getCropOutput 方法**

```java
// 替换整个 getCropOutput 方法：
private static ItemStack getCropOutput(Level level, ItemStack seed) {
    if (level == null || seed.isEmpty()) return seed.copy();

    return level.getRecipeManager()
            .getAllRecipesFor(ModRecipeTypes.INCUBATOR_OUTPUT.get())
            .stream()
            .filter(r -> r.matches(seed))
            .findFirst()
            .map(r -> r.assemble(seed))
            .orElse(seed.copy()); // 未知种子保底产出种子本身
}
```

添加 import：
```java
import com.TKCCOPL.recipe.ModRecipeTypes;
```

更新 tick() 中的调用：
```java
// 替换: ItemStack cropOutput = getCropOutput(blockEntity.seed);
ItemStack cropOutput = getCropOutput(level, blockEntity.seed);
```

- [ ] **Step 2: 删除旧的硬编码 getCropOutput 方法**

删除整个旧的 `getCropOutput(ItemStack seed)` 方法（约 40 行 if/else 代码）。

- [ ] **Step 3: 编译验证**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 提交**

```bash
git add src/main/java/com/TKCCOPL/block/entity/BioIncubatorBlockEntity.java
git commit -m "refactor(incubator): getCropOutput 改用 RecipeManager 查询"
```

---

### Task 10: IncubatorOutputCategory 改用 RecipeManager

**Files:**
- Modify: `src/main/java/com/TKCCOPL/compat/jei/IncubatorOutputCategory.java`

- [ ] **Step 1: 重写 buildRecipes()**

```java
public static List<DisplayRecipe> buildRecipes(Level level) {
    if (level == null) return Collections.emptyList();

    List<DisplayRecipe> recipes = new ArrayList<>();
    for (var recipe : level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.INCUBATOR_OUTPUT.get())) {
        int[] genes = recipe.getDefaultGenes();
        recipes.add(new DisplayRecipe(
                seedWithGenes(recipe.getSeedItem(), genes[0], genes[1], genes[2]),
                recipe.getOutputItem(),
                recipe.getCropName(),    // ← 映射 cropName
                genes[0], genes[1], genes[2]));
    }
    return recipes;
}
```

- [ ] **Step 2: CyberCultivatorJEIPlugin 无需改动**

当前 `registerRecipes()` 已经通过 `Minecraft.getInstance().level` 获取 Level，直接调用 `buildRecipes(level)` 即可。`registerRecipes` 在客户端运行时调用，level 不为 null。

```java
// 当前代码已正确:
var level = Minecraft.getInstance().level;
if (level == null) return;
registration.addRecipes(
        IncubatorOutputCategory.RECIPE_TYPE,
        IncubatorOutputCategory.buildRecipes(level)  // ← 传入 level
);
```

- [ ] **Step 3: 编译验证**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 提交**

```bash
git add src/main/java/com/TKCCOPL/compat/jei/IncubatorOutputCategory.java src/main/java/com/TKCCOPL/compat/jei/CyberCultivatorJEIPlugin.java
git commit -m "refactor(jei): IncubatorOutputCategory 改用 RecipeManager 自动发现"
```

---

### Task 11: 添加 KubeJS compileOnly 依赖

**Files:**
- Modify: `build.gradle`
- Modify: `gradle.properties`
- Modify: `src/main/resources/META-INF/mods.toml`

- [ ] **Step 1: gradle.properties 新增**

```properties
kubejs_version=2001.6.5-build.16
```

- [ ] **Step 2: build.gradle 新增依赖**

```groovy
// 在 dependencies {} 块中添加：
compileOnly fg.deobf("dev.latvian.mods:kubejs-forge-${kubejs_version}:api")
runtimeOnly fg.deobf("dev.latvian.mods:kubejs-forge-${kubejs_version}")
```

在 repositories {} 块中添加：
```groovy
maven { url = 'https://maven.latvian.dev/releases' }
```

- [ ] **Step 3: mods.toml 新增可选依赖**

```toml
[[dependencies."${mod_id}"]]
modId = "kubejs"
mandatory = false
versionRange = "[2001,)"
ordering = "AFTER"
side = "BOTH"
```

- [ ] **Step 4: 编译验证**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL（KubeJS 为 compileOnly，运行时无 KubeJS 不崩溃）

- [ ] **Step 5: 提交**

```bash
git add build.gradle gradle.properties src/main/resources/META-INF/mods.toml
git commit -m "feat(kubejs): 添加 KubeJS compileOnly 依赖"
```

---

### Task 12: 数据驱动 + KubeJS 全阶段编译验证 + 运行时冒烟

- [ ] **Step 1: 完整编译**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 运行客户端冒烟测试**

Run: `./gradlew runClient`
验证：
1. 进入世界 → 放置培养槽 → 放入纤维草种子 → 成熟后确认产出植物纤维（数量受 yield 基因影响）
2. 放入蛋白质豆种子 → 确认产出生物原液（Concentration 标签）
3. 使用拼接机 → 确认正常工作（未在阶段 2 改动）
4. 使用灌装机 → 确认血清合成正常（RecipeType 已有，不受影响）
5. 打开 JEI → 确认培养槽产出类别自动显示 3 种配方
6. 检查 `run/config/` 目录无崩溃日志

- [ ] **Step 3: 提交**

```bash
git add -A && git commit -m "feat: 数据驱动 IncubatorOutput + KubeJS 依赖完成"
```

---

## 阶段 3：性能优化

### Task 13: syncToClient() 降频 + 基因缓存

**Files:**
- Modify: `src/main/java/com/TKCCOPL/block/entity/BioIncubatorBlockEntity.java`

- [ ] **Step 1: 添加基因缓存字段**

```java
// 新增字段
private int cachedSpeed = 1;
private int cachedYield = 1;
private int cachedPotency = 1;
private int syncCounter = 0;
private static final int SYNC_INTERVAL = 10;
```

- [ ] **Step 2: 在 tryInsertSeed 中初始化缓存**

```java
public boolean tryInsertSeed(ItemStack stack) {
    if (!seed.isEmpty()) {
        return false;
    }
    if (stack.getItem() instanceof GeneticSeedItem geneticSeed) {
        geneticSeed.ensureGeneData(stack);
    }
    seed = stack;
    cachedSpeed = GeneticSeedItem.getGene(stack, GeneticSeedItem.GENE_SPEED);
    cachedYield = GeneticSeedItem.getGene(stack, GeneticSeedItem.GENE_YIELD);
    cachedPotency = GeneticSeedItem.getGene(stack, GeneticSeedItem.GENE_POTENCY);
    growthProgress = 0;
    syncCounter = 0;
    syncToClient();
    return true;
}
```

- [ ] **Step 3: 在 extractSeed 中重置缓存**

```java
public ItemStack extractSeed() {
    if (seed.isEmpty()) {
        return ItemStack.EMPTY;
    }
    ItemStack out = seed;
    seed = ItemStack.EMPTY;
    cachedSpeed = 1;
    cachedYield = 1;
    cachedPotency = 1;
    growthProgress = 0;
    syncCounter = 0;
    syncToClient();
    return out;
}
```

- [ ] **Step 4: 在 load() 中初始化缓存**

```java
@Override
public void load(CompoundTag tag) {
    super.load(tag);
    nutrition = clampStat(tag.getInt(TAG_NUTRITION));
    purity = clampStat(tag.getInt(TAG_PURITY));
    dataSignal = clampStat(tag.getInt(TAG_DATA_SIGNAL));
    growthProgress = Math.max(0, tag.getInt(TAG_GROWTH_PROGRESS));
    seed = tag.contains(TAG_SEED) ? ItemStack.of(tag.getCompound(TAG_SEED)) : ItemStack.EMPTY;
    // 初始化基因缓存
    if (!seed.isEmpty()) {
        cachedSpeed = GeneticSeedItem.getGene(seed, GeneticSeedItem.GENE_SPEED);
        cachedYield = GeneticSeedItem.getGene(seed, GeneticSeedItem.GENE_YIELD);
        cachedPotency = GeneticSeedItem.getGene(seed, GeneticSeedItem.GENE_POTENCY);
    } else {
        cachedSpeed = 1;
        cachedYield = 1;
        cachedPotency = 1;
    }
    syncCounter = 0;
}
```

- [ ] **Step 5: tick() 中使用缓存 + syncToClient 降频**

替换 tick() 中的基因读取（生长速率计算）：
```java
// 替换: int geneSpeed = GeneticSeedItem.getGene(blockEntity.seed, GeneticSeedItem.GENE_SPEED);
int geneSpeed = blockEntity.cachedSpeed;
```

**注意：** `getCropOutput()` 保持 Task 9 的签名 `getCropOutput(Level, ItemStack)` 不变。该方法只在作物成熟时调用一次（非每 tick），内部 `IncubatorOutputRecipe.assemble()` 读取 NBT 的开销可忽略。

替换 tick() 末尾的同步逻辑：
```java
// 替换整个 if (changed) 块：
if (changed) {
    blockEntity.syncCounter++;
    if (blockEntity.syncCounter >= SYNC_INTERVAL) {
        blockEntity.syncToClient();
        blockEntity.syncCounter = 0;
    }
}
```

- [ ] **Step 6: 衰减逻辑合并（可选优化）**

```java
// 替换三段独立的衰减判断：
long time = level.getGameTime();
if (time % (long) Config.nutritionDecayInterval == 0L && blockEntity.nutrition > 0) {
    blockEntity.nutrition -= 1;
    changed = true;
}
if (time % (long) Config.purityDecayInterval == 0L && blockEntity.purity > 0) {
    blockEntity.purity -= 1;
    changed = true;
}
if (time % (long) Config.dataSignalDecayInterval == 0L && blockEntity.dataSignal > 0) {
    blockEntity.dataSignal -= 1;
    changed = true;
}
```

**注意：** 使用 Config 值后，衰减间隔不再是固定 20/40/60，无法用 GCD 合并。保持三段独立判断即可。

- [ ] **Step 7: 编译验证**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 8: 提交**

```bash
git add src/main/java/com/TKCCOPL/block/entity/BioIncubatorBlockEntity.java
git commit -m "perf(incubator): syncToClient 每 10 tick + 基因值缓存"
```

---

### Task 14: 性能优化编译验证 + 运行时冒烟

- [ ] **Step 1: 完整编译**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 运行客户端冒烟测试**

Run: `./gradlew runClient`
验证：
1. 放置培养槽 → 放入种子 → 确认 HUD 显示正常（延迟 ≤0.5s 可接受）
2. 确认生长进度正常推进
3. 成熟后确认产出正确
4. 取出种子 → 确认 HUD 立即更新
5. 拼接种子 → 确认基因值正确

- [ ] **Step 3: 最终提交**

```bash
git add -A && git commit -m "perf: 性能优化完成 — syncToClient 降频 + 基因缓存"
```

---

## 总览

| 阶段 | Task | 改动文件 | 核心改动 |
|---|---|---|---|
| 1. Config | T1-T6 | Config.java + 4 消费端 | push/pop 分组 + 新增 3 section |
| 2. 数据驱动 | T7-T12 | 新增 Recipe + Serializer + JSON + JEI + build.gradle | RecipeType 替代硬编码 + KubeJS 依赖 |
| 3. 性能 | T13-T14 | BioIncubatorBlockEntity | syncToClient 降频 + 基因缓存 |

**总新增文件：** 2 (IncubatorOutputRecipe, IncubatorOutputSerializer)
**总修改文件：** ~10
**总新增 JSON：** 3 (incubator recipes)

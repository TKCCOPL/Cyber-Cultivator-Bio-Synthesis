# Config 扩展 + 数据驱动 + KubeJS + 性能优化 设计规范

**日期：** 2026-05-30
**模组：** Cyber-Cultivator: Bio-Synthesis v1.1.2
**目标版本：** Forge 1.20.1

## 目标

1. 将硬编码数值提取为可配置参数（Config TOML）
2. 将种子→作物映射改为 JSON 数据驱动（RecipeType）
3. 集成 KubeJS 让整合包作者能脚本化自定义
4. 优化培养槽 tick 性能（syncToClient 降频 + 基因缓存）

## 范围

**规范 1：Config 扩展**
- 重构 Config.java 为 push/pop 分组
- 新增 genes / serum / incubator 三个 section
- 修改 4 个 BlockEntity/Item 读取 Config 值

**规范 2：数据驱动 + KubeJS**
- 新增 `RecipeType<IncubatorRecipe>` 替代硬编码 `getCropOutput()`
- JSON 配方文件定义种子→产出映射
- 添加 KubeJS compileOnly 依赖
- 4 个自定义事件暴露给 KubeJS

**规范 3：性能优化**
- `syncToClient()` 降频（每 10 tick 或状态突变时）
- 基因值缓存（种子插入时读取，tick 中直接使用）
- 衰减逻辑合并（可选）

**不在本次范围：**
- 培养槽营养/纯净/信号的 JSON 化（仍为 Config 控制）
- 新增 Curios 饰品参数（已有 8 项完整覆盖）

---

## 规范 1：Config 扩展

### 1.1 分组结构

```toml
# config/cybercultivator-common.toml

[genes]
    # 基因变异参数
    mutationRange = 2              # 随机变异范围 ±N (1-5)
    mutationChanceBase = 0.05      # 基础突变概率 (0.01-0.20)
    mutationChancePerGen = 0.02    # 每代增加突变概率 (0-0.10)
    mutationChancePerGeneDiff = 0.01 # 每点基因差异增加概率 (0-0.05)
    geneMin = 1                    # 基因下限 (1-5)
    geneMax = 10                   # 基因上限 (5-10)

[serum]
    # 血清效果参数
    s01BaseDuration = 500          # S-01 基础持续时间 tick (25s)
    s02BaseDuration = 600          # S-02 基础持续时间 tick (30s)
    s03BaseDuration = 300          # S-03 基础持续时间 tick (15s)
    stackAmplifierCap = 7          # 叠加 amplifier 上限 (VIII 级)
    stackDurationCap = 6000        # 持续时间累加上限 tick (5min)
    activityThresholdForBonus = 8  # 高品质起步 amplifier=1 的阈值
    durationMultiplierBase = 0.5   # 持续时间倍率基数
    durationMultiplierPerActivity = 0.1 # 每点活性增加倍率

[incubator]
    # 培养槽参数
    maturationThreshold = 200      # 成熟所需生长进度
    resourceThreshold = 10         # 资源消耗阈值
    nutritionDecayInterval = 20    # 营养衰减间隔 tick
    purityDecayInterval = 40       # 纯净衰减间隔 tick
    dataSignalDecayInterval = 60   # 信号衰减间隔 tick
    nutritionInjectAmount = 25     # 生化原液注入量
    purityInjectAmount = 20        # 纯净水注入量
    dataSignalInjectAmount = 15    # 硅碎片注入量
    matureNutritionCost = 5        # 成熟时营养消耗
    maturePurityCost = 5           # 成熟时纯净消耗

[curios]
    # 饰品参数（已有 8 项，重构为分组）
    scanRange = 3
    nutritionThreshold = 50
    purityThreshold = 50
    dataSignalThreshold = 25
    effectReductionRate = 2
    healThreshold = 6.0
    healCooldown = 1200
    hudRange = 8
```

### 1.2 Config.java 重构

使用 `push/pop` 分组替代扁平字段：

```java
BUILDER.push("genes");
MUTATION_RANGE = BUILDER.comment("随机变异范围 ±N").defineInRange("mutationRange", 2, 1, 5);
// ... 其余基因参数
BUILDER.pop();

BUILDER.push("serum");
S01_BASE_DURATION = BUILDER.comment("S-01 基础持续时间 tick").defineInRange("s01BaseDuration", 500, 100, 12000);
// ... 其余血清参数
BUILDER.pop();

BUILDER.push("incubator");
MATURATION_THRESHOLD = BUILDER.comment("成熟所需生长进度").defineInRange("maturationThreshold", 200, 50, 1000);
// ... 其余培养槽参数
BUILDER.pop();

BUILDER.push("curios");
SCAN_RANGE = BUILDER.comment("腰带扫描范围").defineInRange("scanRange", 3, 1, 8);
// ... 其余饰品参数（从现有字段迁移）
BUILDER.pop();
```

### 1.3 消费端改动

| 文件 | 改动 |
|---|---|
| `GeneSplicerBlockEntity` | 读取 `Config.mutationRange`, `Config.mutationChanceBase` 等 |
| `BioIncubatorBlockEntity` | 读取 `Config.maturationThreshold`, `Config.resourceThreshold` 等 |
| `SynapticSerumItem` | 读取 `Config.s01BaseDuration`, `Config.stackAmplifierCap` 等 |
| `CurioEventHandler` | 读取 `Config.scanRange` 等（从旧字段迁移） |
| `Config.java` | 重构 push/pop 分组 + 新增 3 个 section |

### 1.4 向后兼容

- 旧配置文件中的扁平字段名变更（如 `beltScanRange` → `curios.scanRange`）
- 首次加载时 Forge 会自动重新生成 TOML，旧值丢失
- 在更新日志中提醒用户重新配置

---

## 规范 2：数据驱动 + KubeJS

### 2.1 RecipeType：IncubatorOutput

新增 `RecipeType<IncubatorOutputRecipe>`，替代 `getCropOutput()` 硬编码。

**JSON 配方格式：**
```json
{
  "type": "cybercultivator:incubator_output",
  "seed": "cybercultivator:fiber_reed_seeds",
  "output": "cybercultivator:plant_fiber",
  "count_formula": "2 + yield / 3",  // 整数除法，yield=10 → count=5
  "quality_tag": "Potency",
  "default_genes": {
    "speed": 4,
    "yield": 7,
    "potency": 3
  }
}
```

**字段说明：**
- `seed`：输入种子物品 ID
- `output`：输出物品 ID
- `count_formula`：产出数量公式（基于 yield 基因）
- `quality_tag`：品质 NBT 标签名（Potency/Purity/Concentration）
- `default_genes`：默认基因值（JEI 展示用）

**3 个默认配方：**
- `fiber_reed.json`：纤维草种子 → 植物纤维（Potency）
- `protein_soy.json`：蛋白质豆种子 → 生化原液（Concentration）
- `alcohol_bloom.json`：酒精花种子 → 工业乙醇（Purity）

### 2.2 IncubatorOutputRecipe 实现

```java
public class IncubatorOutputRecipe implements Recipe<Container> {
    private final ResourceLocation id;
    private final Ingredient seed;
    private final ItemStack output;
    private final String countFormula;  // "2 + yield / 3"
    private final String qualityTag;    // "Potency"
    private final int[] defaultGenes;   // [speed, yield, potency]

    @Override
    public boolean matches(Container container, Level level) {
        return seed.test(container.getItem(0));
    }

    /** 根据基因值计算产出 */
    public ItemStack assemble(ItemStack seedStack) {
        int yield = GeneticSeedItem.getGene(seedStack, GENE_YIELD);
        int potency = GeneticSeedItem.getGene(seedStack, GENE_POTENCY);
        int count = evaluateFormula(countFormula, yield);
        ItemStack result = new ItemStack(output.getItem(), count);
        result.getOrCreateTag().putInt(qualityTag, potency);
        return result;
    }
}
```

### 2.3 BioIncubatorBlockEntity 改造

```java
// 当前：硬编码
private static ItemStack getCropOutput(ItemStack seed) { ... }

// 改造后：从 RecipeManager 查询
private static ItemStack getCropOutput(Level level, ItemStack seed) {
    return level.getRecipeManager()
        .getAllRecipesFor(ModRecipeTypes.INCUBATOR_OUTPUT.get())
        .stream()
        .filter(r -> r.matches(seed))
        .findFirst()
        .map(r -> r.assemble(seed))
        .orElse(seed.copy()); // 未知种子保底
}
```

### 2.4 KubeJS 集成

**build.gradle 新增：**
```groovy
compileOnly fg.deobf("dev.latvian.mods:kubejs-forge-${kubejs_version}:api")
runtimeOnly fg.deobf("dev.latvian.mods:kubejs-forge-${kubejs_version}")
```

**gradle.properties 新增：**
```properties
kubejs_version=2001.6.5-build.16
```

**mods.toml 新增：**
```toml
[[dependencies.cybercultivator]]
modId = "kubejs"
mandatory = false
ordering = "AFTER"
side = "BOTH"
```

**KubeJS 使用方式：**

KubeJS 通过 Forge 事件总线监听自定义事件。4 个事件类需添加 KubeJS 兼容注解：

```javascript
// kubejs/server_scripts/cybercultivator.js
// 监听基因拼接
ForgeEvents.onEvent('com.TKCCOPL.event.GeneSpliceEvent', event => {
    event.setSpeed(event.getSpeed() + 2);
})

// 监听血清饮用
ForgeEvents.onEvent('com.TKCCOPL.event.SerumConsumeEvent', event => {
    if (event.getActivity() >= 8) {
        event.setDuration(event.getDuration() * 2);
    }
})

// 监听作物成熟
ForgeEvents.onEvent('com.TKCCOPL.event.CropMatureEvent', event => {
    // 修改产出
})

// 监听血清灌装
ForgeEvents.onEvent('com.TKCCOPL.event.SerumCraftEvent', event => {
    // 修改活性/输出
})
```

### 2.5 改动范围

| 文件 | 改动 |
|---|---|
| `ModRecipeTypes.java` | +`INCUBATOR_OUTPUT` RecipeType + Serializer |
| 新增 `IncubatorOutputRecipe.java` | 配方实现 |
| 新增 `IncubatorOutputSerializer.java` | JSON 序列化器 |
| `BioIncubatorBlockEntity.java` | `getCropOutput()` 改用 RecipeManager |
| `IncubatorOutputCategory.java` | 改用 RecipeManager 自动发现 |
| `ModRecipes.java` | 保留接口，注册逻辑迁移到 JSON |
| `build.gradle` | +KubeJS compileOnly 依赖 |
| `gradle.properties` | +kubejs_version |
| `mods.toml` | +kubejs 可选依赖 |
| JSON 文件 × 3 | 种子配方 |
| `ModRecipeProvider.java` | datagen 生成配方 JSON |

---

## 规范 3：性能优化

### 3.1 syncToClient() 降频

**问题：** 生长期间每 tick 调用 `syncToClient()`（每秒 20 次 NBT 序列化 + 网络包）。

**方案：** 每 10 tick 同步一次 + 状态突变时立即同步。

```java
private int syncCounter = 0;
private static final int SYNC_INTERVAL = 10;

// tick() 末尾
if (changed) {
    syncCounter++;
    if (syncCounter >= SYNC_INTERVAL) {
        syncToClient();
        syncCounter = 0;
    }
}

// 状态突变时立即同步
public boolean tryInsertSeed(ItemStack stack) {
    ...
    syncToClient(); // 立即同步
    syncCounter = 0;
}

public ItemStack extractSeed() {
    ...
    syncToClient(); // 立即同步
    syncCounter = 0;
}
```

**效果：** 生长期间每秒 2 次同步（原来 20 次），降低 90% 网络/IO 开销。HUD 显示延迟最多 0.5 秒，可接受。

**注意：** `syncCounter` 为运行时计数器，不需 NBT 持久化。加载存档时重置为 0。

### 3.2 基因值缓存

**问题：** 每 tick 调用 `GeneticSeedItem.getGene()` 解析 NBT CompoundTag。

**方案：** 种子插入时缓存基因值，tick 中直接使用缓存。

```java
private int cachedSpeed = 1;
private int cachedYield = 1;
private int cachedPotency = 1;

public boolean tryInsertSeed(ItemStack stack) {
    seed = stack;
    cachedSpeed = GeneticSeedItem.getGene(stack, GENE_SPEED);
    cachedYield = GeneticSeedItem.getGene(stack, GENE_YIELD);
    cachedPotency = GeneticSeedItem.getGene(stack, GENE_POTENCY);
    ...
}

@Override
public void load(CompoundTag tag) {
    super.load(tag);
    ...
    // 加载存档后初始化缓存
    if (!seed.isEmpty()) {
        cachedSpeed = GeneticSeedItem.getGene(seed, GENE_SPEED);
        cachedYield = GeneticSeedItem.getGene(seed, GENE_YIELD);
        cachedPotency = GeneticSeedItem.getGene(seed, GENE_POTENCY);
    }
}
```

**效果：** 消除每 tick 的 NBT 解析开销。

### 3.3 衰减逻辑合并（可选）

**问题：** 每 tick 执行 3 次 `%` 运算判断衰减。

**方案：** 用最大公倍数 120 tick 合并判断。

```java
// 当前
if (time % 20 == 0) nutrition--;
if (time % 40 == 0) purity--;
if (time % 60 == 0) dataSignal--;

// 优化
if (time % 20 == 0) {
    nutrition--;
    if (time % 40 == 0) {
        purity--;
        if (time % 60 == 0) dataSignal--;
    }
}
```

**效果：** 微优化，减少 % 运算次数。可选实现。

### 3.4 改动范围

| 文件 | 改动 |
|---|---|
| `BioIncubatorBlockEntity.java` | syncToClient 降频 + 基因缓存 + 衰减合并 |

---

## 实现顺序

1. **规范 1：Config 扩展** — 先让所有数值可配置
2. **规范 2：数据驱动 + KubeJS** — JSON 化 + 脚本集成
3. **规范 3：性能优化** — 最后优化（依赖规范 1 的 Config 值）

## 风险与缓解

| 风险 | 缓解 |
|---|---|
| Config 字段名变更导致旧配置丢失 | 更新日志提醒重新配置 |
| syncToClient 降频导致 HUD 延迟 | 0.5 秒延迟可接受，状态突变时立即同步 |
| KubeJS API 版本变更 | compileOnly 模式隔离 |
| RecipeType 改造破坏现有 JEI | IncubatorOutputCategory 同步改造 |
| 基因缓存与种子实际值不一致 | tryInsertSeed/extractSeed/load 中同步刷新缓存 |

# 模组兼容 & API 设计规范

**日期：** 2026-05-30
**模组：** Cyber-Cultivator: Bio-Synthesis v1.1.2
**目标版本：** Forge 1.20.1

## 目标

将 Cyber-Cultivator 的配方、基因、机器系统暴露为公开 API，实现 JEI 配方查看、KubeJS 脚本化、第三方模组集成。

## 范围

**本次范围：**
- JEI 配方查看器集成（4 个配方类别）
- Forge RecipeType 血清灌装机配方（JSON 数据驱动）
- 公开门面类 `CyberCultivatorAPI` + 只读 DTO
- 自定义 Forge 事件：基因拼接、作物成熟、血清灌装、血清饮用

**后续范围（不在本次实现）：**
- KubeJS 插件（需额外 compileOnly 依赖）
- Patchouli 引导手册
- TOP/WTHIT/Jade 方块信息悬浮窗
- ~~Create 机械动力兼容~~（v1.1.7 已通过 Forge `IItemHandler` 分面能力实现标准能力预期兼容）
- ~~超出现有 WorldlyContainer 的自动化管道兼容~~（v1.1.7 已实现 Forge `IItemHandler` 能力，详见 `docs/superpowers/plans/2026-07-20-v1.1.7-redstone-industrial-compat.md`）

## 架构

```
┌─────────────────────────────────────────────────────┐
│                    Cyber-Cultivator                   │
├─────────────┬─────────────┬─────────────┬───────────┤
│  RecipeType │ 自定义 API   │  事件总线    │ compat/   │
│  (JSON驱动)  │ (DTO只读)    │ (Forge事件)  │(compileOnly)│
├─────────────┴─────────────┴─────────────┴───────────┤
│           核心游戏逻辑（BlockEntity/Item/Effect）       │
└─────────────────────────────────────────────────────┘
         │              │              │
    ┌────▼────┐   ┌────▼────┐   ┌────▼────┐
    │   JEI   │   │ KubeJS  │   │ 其他Mod  │
    │(compileOnly)│(compileOnly)│ (API调用) │
    └─────────┘   └─────────┘   └─────────┘
```

**分层策略：**
- **RecipeType 层：** 血清灌装机配方（JSON 数据驱动，JEI 自动发现）
- **自定义 API 层：** 基因拼接机 / 培养槽（算法驱动，通过静态注册表暴露）
- **事件总线层：** GeneSpliceEvent / CropMatureEvent / SerumCraftEvent / SerumConsumeEvent
- **兼容层：** `compat/jei/` 包，compileOnly 依赖

## 1. 配方系统

### 1.1 血清灌装机 → Forge RecipeType

新增 `RecipeType<SerumRecipe>`，通过 `ModRecipeTypes` 注册。

**SerumRecipe 结构：**
```java
public class SerumRecipe implements Recipe<SimpleContainer> {
    Ingredient[] inputs;     // 3 个输入（JSON 定义）
    ItemStack baseOutput;    // 基础输出物品（JSON 定义）
    int processingTime;      // 加工时间（JSON 定义，默认 300）
    boolean inheritActivity; // 是否从输入继承 Activity（JSON 定义）
    boolean inheritMutation; // 是否从输入继承 Mutation 标签（JSON 定义）
}
```

**JSON 配方示例** (`data/cybercultivator/recipes/serum/s01.json`)：
```json
{
  "type": "cybercultivator:serum_bottling",
  "ingredients": [
    { "item": "cybercultivator:synaptic_neural_berry" },
    { "item": "cybercultivator:biochemical_solution" },
    { "item": "minecraft:glass_bottle" }
  ],
  "result": { "item": "cybercultivator:synaptic_serum_s01" },
  "processing_time": 300,
  "inherit_activity": true,
  "inherit_mutation": false
}
```

**4 个配方定义：**
1. 莓合成：plant_fiber + industrial_ethanol + biochemical_solution → synaptic_neural_berry（inherit_activity=true, inherit_mutation=true）
2. S-01：synaptic_neural_berry + biochemical_solution + glass_bottle → synaptic_serum_s01（inherit_activity=true）
3. S-02：synaptic_neural_berry + rare_earth_dust + glass_bottle → synaptic_serum_s02（inherit_activity=true）
4. S-03：synaptic_neural_berry + industrial_ethanol + glass_bottle → synaptic_serum_s03（inherit_activity=true）

**NBT 转移逻辑**（保留在代码中，不走 JSON）：
- Activity 计算：莓合成时 `calculateActivity(inputs)`
- Activity 继承：血清合成时 `getActivity(berry)`
- Mutation 标签继承：扫描输入中 Mutation/MutationDetail，取最大类型

### 1.2 基因拼接机 → 自定义 API

非 JSON 驱动（算法驱动）。通过 `ModRecipes` 静态注册表暴露。

```java
public interface IGeneSpliceRecipe {
    /** 计算子代基因，返回 [speed, yield, potency] */
    int[] calculateOffspring(int speedA, int yieldA, int potencyA,
                             int speedB, int yieldB, int potencyB,
                             RandomSource random);
    /** 突变概率计算 */
    double getMutationChance(int generation, int geneDifference);
}
```

### 1.3 培养槽产出 → 自定义 API

非 JSON 驱动（基因相关）。通过 `ModRecipes` 静态注册表暴露。

```java
public interface IIncubatorOutput {
    /** 根据种子类型和基因值返回产出 ItemStack */
    ItemStack getOutput(ItemStack seed);
    /** 基因对生长速率的倍率 */
    double getGrowthMultiplier(int geneSpeed);
}
```

### 1.4 JEI 配方类别

| 类别 | 配方来源 | 展示内容 |
|------|---------|---------|
| 血清灌装 | `RecipeManager.getAllRecipesFor(SERUM_RECIPE)` | 3 输入 → 1 输出 + Activity 值 |
| 基因拼接 | `ModRecipes.getSPLICE_RECIPES()` | 父本 A + B → 子代 + 突变概率 |
| 培养槽产出 | `ModRecipes.getINCUBATOR_OUTPUTS()` | 种子 → 作物产出 + 基因倍率 |
| 机器合成 | 标准 `CraftingRecipe` | 已有配方，JEI 自动发现 |

## 2. 事件系统

### 2.1 GeneSpliceEvent（基因拼接事件）

**触发位置：** `GeneSplicerBlockEntity.craftOutput()` 步骤 7 之后
**可取消：** 是
**字段（均可修改）：**
- `seedA`, `seedB`（输入种子，只读）
- `speed`, `yield`, `potency`（计算出的子代基因，可修改）
- `synergy`（可修改）
- `generation`（可修改）
- `isMutation`（可修改）
- `mutationType`（0=无, 1=数值突破, 2=协同基因，可修改）
- `mutationDetail`（可修改）

### 2.2 CropMatureEvent（作物成熟事件）

**触发位置：** `BioIncubatorBlockEntity.tick()` 中 `getCropOutput()` 之后、`Containers.dropItemStack()` 之前
**可取消：** 是
**字段：**
- `seed`（输入种子，只读）
- `output`（计算出的作物产出，可修改）

### 2.3 SerumCraftEvent（血清灌装事件）

**触发位置：** `SerumBottlerBlockEntity.getRecipeOutput()` 中 Activity 计算之后
**可取消：** 是
**字段：**
- `inputs`（ItemStack[3]，只读）
- `output`（可修改）
- `activity`（可修改）
- `recipeIndex`（0=莓, 1=S01, 2=S02, 3=S03，只读）

### 2.4 SerumConsumeEvent（血清饮用事件）

**触发位置：** `SynapticSerumItem.finishUsingItem()` 中 `entity.addEffect()` 之前
**可取消：** 是（取消则不施加效果）
**字段：**
- `serum`（ItemStack，只读）
- `activity`（可修改）
- `duration`（可修改）
- `amplifier`（可修改）
- `effect`（MobEffect，只读）

### KubeJS 使用示例：
```javascript
// KubeJS 脚本示例
onEvent('cybercultivator.gene_splice', event => {
    // 所有拼接结果 Speed +2
    event.setSpeed(event.getSpeed() + 2);
})

onEvent('cybercultivator.serum_consume', event => {
    // 高品质血清效果时间翻倍
    if (event.getActivity() >= 8) {
        event.setDuration(event.getDuration() * 2);
    }
})
```

## 3. 公开 API

### 3.1 CyberCultivatorAPI 门面类

```java
public final class CyberCultivatorAPI {
    // === 基因数据 API ===
    /** 读取种子基因值 (1-10) */
    public static int getGene(ItemStack seed, String geneKey);
    /** 设置种子基因值 */
    public static void setGene(ItemStack seed, String geneKey, int value);
    /** 读取种子代数 */
    public static int getGeneration(ItemStack seed);
    /** 读取协同基因值 (0-10) */
    public static int getSynergy(ItemStack seed);

    // === 机器状态 API（返回只读 DTO）===
    public static IncubatorInfo getIncubatorInfo(Level level, BlockPos pos);
    public static BottlerInfo getBottlerInfo(Level level, BlockPos pos);
    public static CondenserInfo getCondenserInfo(Level level, BlockPos pos);
    public static SplicerInfo getSplicerInfo(Level level, BlockPos pos);

    // === 血清配方 API ===
    /** 查询所有血清配方（JEI/KubeJS 使用） */
    public static List<SerumRecipe> getSerumRecipes(Level level);
    /** 计算 Activity 值 */
    public static int calculateActivity(ItemStack[] inputs);
    /** 查询血清效果参数 */
    public static SerumEffectInfo getSerumEffectInfo(ItemStack serum);

    // === 版本/兼容信息 ===
    public static String getModVersion();
    public static boolean isCuriosLoaded();
}
```

### 3.2 只读 DTO（Java Record）

**注意：** `ItemStack` 字段在构造时使用 defensive copy（`stack.copy()`）防止外部修改。

```java
/** 培养槽状态 */
public record IncubatorInfo(
    int nutrition, int purity, int dataSignal,
    int growthPercent, int estimatedSeconds,
    boolean hasSeed, ItemStack seed
) {}

/** 灌装机状态 */
public record BottlerInfo(
    int progress, int maxProgress,
    int activeRecipe, ItemStack output,
    int activity
) {}

/** 冷凝器状态 */
public record CondenserInfo(
    int progress, int maxProgress,
    int stock, int maxStock,
    boolean isFull
) {}

/** 拼接机状态 */
public record SplicerInfo(
    ItemStack seedA, ItemStack seedB,
    ItemStack output, boolean hasOutput,
    int inputCount
) {}

/** 血清效果参数 */
public record SerumEffectInfo(
    String effectId,
    int baseDuration,
    int baseAmplifier,
    double durationMultiplier,
    int activity
) {}
```

### 3.3 API 设计规则

- 所有方法 null 安全（返回默认值或 Optional）
- API 包不依赖任何 compileOnly 依赖（纯 Forge API）
- DTO 为不可变 record（无 setter）
- 基因 key 常量通过 `GeneticSeedItem.GENE_SPEED` 等暴露

## 4. 文件结构

```
src/main/java/com/TKCCOPL/
├── api/                              # 新增：公开 API
│   ├── CyberCultivatorAPI.java       # 门面类
│   ├── IncubatorInfo.java            # 培养槽 DTO (record)
│   ├── BottlerInfo.java              # 灌装机 DTO (record)
│   ├── CondenserInfo.java            # 冷凝器 DTO (record)
│   ├── SplicerInfo.java              # 拼接机 DTO (record)
│   └── SerumEffectInfo.java          # 血清效果 DTO (record)
├── recipe/                           # 新增：配方系统
│   ├── ModRecipeTypes.java           # RecipeType 注册
│   ├── ModRecipes.java               # 静态注册表（拼接机/培养槽配方）
│   ├── SerumRecipe.java              # 血清配方（JSON 数据驱动）
│   └── SerumRecipeSerializer.java    # 序列化器
├── event/                            # 新增：自定义事件
│   ├── GeneSpliceEvent.java          # 拼接事件
│   ├── CropMatureEvent.java          # 成熟事件
│   ├── SerumCraftEvent.java          # 灌装事件
│   └── SerumConsumeEvent.java        # 饮用事件
├── compat/                           # 新增：兼容层
│   └── jei/
│       ├── CyberCultivatorJEIPlugin.java   # @JeiPlugin 入口
│       ├── SerumBottlingCategory.java       # 灌装机 JEI 类别
│       ├── GeneSplicingCategory.java        # 拼接机 JEI 类别
│       └── IncubatorOutputCategory.java     # 培养槽 JEI 类别
├── block/entity/                     # 修改：注入事件 + 配方查询
│   ├── BioIncubatorBlockEntity.java  # 成熟时触发 CropMatureEvent
│   ├── GeneSplicerBlockEntity.java   # 拼接时触发 GeneSpliceEvent
│   └── SerumBottlerBlockEntity.java  # matchRecipe() 改用 RecipeManager
├── item/
│   └── SynapticSerumItem.java        # 饮用时触发 SerumConsumeEvent
└── (现有文件不变)
```

**新增文件：** 16 个
**修改文件：** 4 个

## 5. 依赖配置

### build.gradle 新增：
```groovy
// JEI（与 Curios 相同的 compileOnly 模式）
compileOnly fg.deobf("mezz.jei:jei-${minecraft_version}-forge:${jei_version}:api")
runtimeOnly fg.deobf("mezz.jei:jei-${minecraft_version}-forge:${jei_version}")
```

### gradle.properties 新增：
```properties
jei_version=15.2.0.27
```

### mods.toml 新增：
```toml
[[dependencies.cybercultivator]]
modId = "jei"
mandatory = false
ordering = "AFTER"
side = "CLIENT"
```

## 6. 数据生成

新增 JSON 配方文件（`data/cybercultivator/recipes/serum/`）：
- `berry_synthesis.json`
- `s01_bottling.json`
- `s02_bottling.json`
- `s03_bottling.json`

可手写 JSON 或通过扩展 `ModRecipeProvider` 生成。

## 7. 实现顺序

1. **RecipeType 基础** — `ModRecipeTypes`, `SerumRecipe`, `SerumRecipeSerializer`, `ModRecipes`, JSON 配方
2. **BlockEntity 重构** — `SerumBottlerBlockEntity.matchRecipe()` 改用 RecipeManager（RecipeType 必须先就绪）
3. **事件系统** — 4 个事件类 + BlockEntity/Item 中的触发点
4. **API 门面** — `CyberCultivatorAPI` + DTO record
5. **JEI 集成** — `compat/jei/` 包（依赖 RecipeType + BlockEntity 重构）

## 8. 风险与缓解

| 风险 | 缓解措施 |
|------|---------|
| JEI API 版本变更 | compileOnly 模式隔离运行时依赖 |
| SerumRecipe NBT 转移逻辑出错 | 单元测试 Activity 计算 + Mutation 继承 |
| 事件顺序问题 | 事件在状态变更前触发，支持取消 |
| RecipeManager 性能 | 每 tick 缓存配方查询结果 |
| BlockEntity 重构破坏存档兼容 | 保持 NBT 格式向后兼容 |

# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概况

**Cyber-Cultivator: Bio-Synthesis** — Minecraft Forge 1.20.1 中型模组，核心玩法为遗传育种算法 + 生物强化血清系统。

- **Mod ID:** `cybercultivator`
- **包名:** `com.TKCCOPL`
- **Forge:** 47.4.18 | **Java:** 17
- **可选依赖:** Curios API 5.3.5+（饰品系统）、KubeJS 2001.6.5-build.16 至 build.26（脚本配方/事件）
- **兼容性:** 兼容 JEI；Curios、JEI、KubeJS 均非必需
- **Mappings:** Parchment 2023.09.03-1.20.1

## 常用构建命令

```bash
# 构建模组 jar
./gradlew build

# 编译检查（不打 jar）
./gradlew compileJava

# 运行数据生成器（产出在 src/generated/resources/）
./gradlew runData

# 启动客户端开发环境
./gradlew runClient

# 启动服务端开发环境
./gradlew runServer

# KubeJS 最低/最新验证端点（独立 build/kubejs-smoke 运行目录）
./gradlew -I .github/gradle/exclude-non-kubejs-runtime.init.gradle -PenableKubeJSRuntime=true runGameTestServer
./gradlew -I .github/gradle/exclude-non-kubejs-runtime.init.gradle -PenableKubeJSRuntime=true -Pkubejs_version=2001.6.5-build.26 runGameTestServer

# 重新混淆 jar（build 已自动包含）
./gradlew reobfJar
```

## 架构设计

### 核心系统链路

```
大气冷凝器(纯净水) → 种子(NBT基因) → 培养槽(营养/纯度/信号) → 成熟作物 → 血清灌装机 → 强化血清(药效+副作用)
         ↓                                    ↑                              ↑
    相邻自动注入                          基因拼接机(父本+母本→变异子代)      S-02/S-03
```

### 包结构与关键类

| 包 | 职责 |
|---|------|
| `init/` | 注册表入口：`ModBlocks`, `ModItems`, `ModBlockEntities`, `ModEffects`, `ModCreativeTabs` |
| `block/` | 方块类：`BioIncubatorBlock` (培养槽), `GeneSplicerBlock` (拼接机), `AtmosphericCondenserBlock` (冷凝器), `SerumBottlerBlock` (灌装机), `BasicCropBlock` (基础作物) |
| `block/entity/` | TileEntity：`BioIncubatorBlockEntity` (培养槽状态机), `GeneSplicerBlockEntity` (遗传算法), `AtmosphericCondenserBlockEntity` (纯净水生产+相邻传输), `SerumBottlerBlockEntity` (RecipeType 驱动加工+漏斗兼容) |
| `item/` | `GeneticSeedItem` (NBT基因标签), `SynapticSerumItem` (血清效果触发，支持构造函数注入不同效果) |
| `effect/` | `SynapticOverclockEffect` (突触超频), `NeuralOverloadEffect` (神经过载副作用), `VisualEnhancementEffect` (S-02 视觉强化), `MetabolicBoostEffect` (S-03 代谢加速) |
| `recipe/` | `ModRecipeTypes` (RecipeType 注册), `SerumRecipe` (JSON 配方), `SerumRecipeSerializer` (序列化器), `ModRecipes` (静态注册表：拼接机/培养槽配方，供第三方 mod 查询) |
| `api/` | `CyberCultivatorAPI` (门面类), 5 个只读 DTO record：`IncubatorInfo`, `BottlerInfo`, `CondenserInfo`, `SplicerInfo`, `SerumEffectInfo` |
| `event/` | 自定义 Forge 事件：`GeneSpliceEvent`, `CropMatureEvent`, `SerumCraftEvent`, `SerumConsumeEvent`（均支持取消+字段修改） |
| `curios/` | `CuriosCompat` — Curios API 饰品集成（compileOnly），`CurioAccessoryItem` 基类，`BioPulseBeltItem`（腰带），`LifeSupportPackItem`（支持箱），`CurioEventHandler`（事件驱动 tick） |
| `compat/kubejs/` | 可选 KubeJS 插件、两类 Recipe Schema 与四类可热重载事件包装；核心代码不得引用 KubeJS 类型 |
| `datagen/` | 数据生成器：配方、战利品表、方块状态、物品模型、语言文件、标签、进度引导 |
| `client/` | `ClientTooltipEvents` — 客户端渲染/Tooltip 逻辑，`IncubatorHudOverlay` — 单片镜 HUD 浮窗 |

### 关键机制

**基因系统 (GeneticSeedItem):**
- NBT 标签：`Gene_Speed`, `Gene_Yield`, `Gene_Potency`，范围 1-10
- 拼接公式：`新值 = (父本 + 母本) / 2 + 随机变异(-2..+2)`，clamp 至 1..10

**培养槽 (BioIncubatorBlockEntity):**
- 三项动态数值：Nutrition / Purity / Data Signal (0-100，随时间衰减)
- 交互：放入种子、纯净水瓶注入纯净度、生化原液注入营养液、硅碎片注入信号、潜行取回
- Tick 采用静态方法签名 `tick(Level, BlockPos, BlockState, BioIncubatorBlockEntity)`，通过 `BlockEntityTicker` 注册
- 客户端同步：所有状态变更通过 `syncToClient()` → `setChanged()` + `level.sendBlockUpdated(pos, state, state, 2)` 推送到客户端（flags=2 是 `Block.UPDATE_CLIENTS`）。注意：`saveAdditional()` 必须写入非空 tag（空字段写入哨兵 `new CompoundTag()`），否则 `ClientboundBlockEntityDataPacket` 会将 tag 设为 null 导致客户端不调用 `load()`

**血清效果重平衡 (v1.1.1):**
- S-01 突触超频：攻速+力量随 amplifier 动态增长（`applyEffectTick` 中 transient modifier + addEffect），抗性（上限 III）
- S-02 视觉强化：夜视 + 发光范围 16-48 格随 amplifier 增长 + 抗火（上限 III）
- S-03 代谢加速：回血 + 移速 transient modifier + 跳跃提升（上限 III）

**血清副作用链:**
- `SynapticOverclockEffect` / `VisualEnhancementEffect` / `MetabolicBoostEffect` 结束时均自动施加 `NeuralOverloadEffect`
- 副作用按来源差异化：S-01 凋零+饥饿，S-02 失明+饥饿，S-03 缓慢+中毒
- 来源通过 `NeuralOverloadEffect.setSource(entity, sourceId)` 静态 `ConcurrentHashMap` 传递

**Curios 饰品系统:**
- 饰品为纯 `Item` 子类（`CurioAccessoryItem`），不直接实现 `ICurioItem`（Curios 为 compileOnly）
- `CurioEventHandler` 通过 `PlayerTickEvent` + `CuriosApi.getCuriosInventory()` 检测装备状态并驱动逻辑
- 腰带：扫描范围内培养槽，自动消耗背包材料注入三项数值
- 支持箱：加速 NeuralOverload 消退 + 低血量应急治疗（冷却 60s）
- 单片镜 HUD：`IncubatorHudOverlay` 监听 `RenderGuiOverlayEvent`，准星对准机器时显示 HUD
  - 培养槽：N/P/D 进度条 + 生长进度(G) + 预计成熟时间(ETA)
  - 灌装机：配方名 + 加工进度条 + 突触活性值
  - 冷凝器：生产进度条 + 库存量 + 状态
  - 拼接机：父本种子基因 + 输出结果

**大气冷凝器 (AtmosphericCondenserBlockEntity):**
- 每 600 tick 生产 1 纯净水瓶，库存上限 32
- 相邻传输：下方为培养槽时自动注入 Purity +20（消耗 1 瓶）
- 实现 `WorldlyContainer`，漏斗可从侧面抽取

**血清灌装机 (SerumBottlerBlockEntity):**
- 3 输入槽 + 1 输出槽，配方通过 `RecipeType<SerumRecipe>` JSON 数据驱动（`data/cybercultivator/recipes/serum/`）
- 4 种配方：莓合成（纤维+乙醇+原液）、S-01（莓+原液+瓶）、S-02（莓+稀土+瓶）、S-03（莓+乙醇+瓶）
- 加工时间 300 tick，实现 `WorldlyContainer`（顶部/侧面注入，底部抽取）
- `matchRecipe()` 从 `RecipeManager` 查询 `SerumRecipe`，`consumeInputs()` 消耗材料
- Activity 公式：`clamp(round(Potency×0.25 + Purity×0.375 + Concentration×0.375), 1, 10)`，按物品种类查找输入
- `activeRecipe` 缓存配方索引避免 TOCTOU；加工开始时缓存，完成后使用缓存值

**血清品质链路 (v1.1.0):**
- 原料品质 NBT：培养槽产出时从种子 Potency 基因写入（纤维→Potency，乙醇→Purity，原液→Concentration）
- 莓合成：灌装机中三种原料合成，Activity = 加权平均
- 血清继承：灌装机合成血清时继承莓的 Activity
- 效果缩放：`duration = base × (0.5 + Activity × 0.1)`，`baseAmplifier = Activity >= 8 ? 1 : 0`
- 叠加升级：再次饮用 amplifier +1（上限 7，VIII 级），持续时间累加（上限 5 分钟）
- 副作用：`removeAttributeModifiers` 中检查 `entity.getEffect(this) == null`，仅自然过期时施加 NeuralOverload；用 TickTask 延迟避免 CME

**配方系统 (recipe/):**
- `ModRecipeTypes`：注册 `RecipeType<SerumRecipe>` + `RecipeSerializer`，DeferredRegister 模式
- `SerumRecipe`：JSON 数据驱动配方，支持 `inheritActivity` / `inheritMutation` 标签
- `ModRecipes`：静态注册表，暴露 `IGeneSpliceRecipe`（拼接算法）和 `IIncubatorOutput`（培养槽产出）接口，供第三方 mod 查询
- `getSeedItemForType(String)`：根据种子类型标识查找对应种子物品

**自定义事件 (event/):**
- `GeneSpliceEvent`：拼接完成后触发，可修改子代基因/突变类型（可取消）
- `CropMatureEvent`：作物成熟产出前触发，可修改产出（可取消）
- `SerumCraftEvent`：灌装完成后触发，可修改输出/活性（可取消）
- `SerumConsumeEvent`：血清饮用时触发，可修改效果参数（可取消）

**公开 API (api/):**
- `CyberCultivatorAPI`：门面类，提供基因读写、机器状态查询、血清配方查询
- DTO record：`IncubatorInfo`, `BottlerInfo`, `CondenserInfo`, `SplicerInfo`, `SerumEffectInfo`（ItemStack 字段 defensive copy）
- 所有方法 null 安全，API 包不依赖任何 compileOnly 依赖

### 数据生成

数据生成器输出在 `src/generated/resources/`，`build.gradle` 已配置 `sourceSets.main.resources { srcDir 'src/generated/resources' }`。修改配方/战利品表/语言/进度/模型后需重新运行 `./gradlew runData`。

datagen 覆盖范围：
- `ModBlockStateProvider`：9 个方块的 blockstate + block model + item model（cube_all 方块 + crop 方块）
- `ModItemModelProvider`：16 个物品的 item model（basicItem 生成）
- `ModLangProvider`：zh_cn 语言文件
- `ModBlockTagProvider`：方块标签
- `ModLootTableProvider`：战利品表
- `ModRecipeProvider`：配方
- `ModAdvancementProvider`：进度引导

方块状态和物品模型已全部由 datagen 生成，不再手写 JSON。

## 开发约定

- 注册表对象统一在 `init/` 包下各类中声明，使用 `DeferredRegister` + `RegistryObject` 模式
- 方块状态 JSON / 物品模型全部由 datagen 生成到 `src/generated/resources/`，不在 `src/main/resources/` 中手写
- 中文翻译通过 `ModLangProvider` datagen 生成到 `zh_cn.json`
- Curios 依赖为 `compileOnly`（API jar）+ `runtimeOnly`（完整模组），通过 `mixin.env.remapRefMap` 属性解决 mixin refmap 重映射问题。代码中需做兼容检查（`CuriosCompat.isCuriosLoaded()`）
- Curios 物品→槽位映射通过 `data/curios/tags/items/{slot}.json` 实现（数据驱动，无需运行时 Curios 类）
- Curios 槽位定义在 `data/cybercultivator/curios/slots/`，实体槽位绑定在 `data/cybercultivator/curios/entities/player.json`
- KubeJS、Rhino、Architectury 默认仅 `compileOnly`；只有显式传入 `-PenableKubeJSRuntime=true` 才进入开发运行时，发布 JAR 不捆绑这些依赖
- 自定义配方统一按 `priority` 降序、配方 ID 升序选择；机器、公开 API 与 JEI 必须复用 `RecipeOrdering`
- KubeJS 专用类型只允许位于 `compat/kubejs/`，通过 `kubejs.plugins.txt` 发现；`api/`、配方核心和机器逻辑保持零耦合
- 运行目录为 `run/`（客户端）和 `run-data/`（数据生成）
- `Config.java` 含 6 项可配置参数（腰带扫描范围、注入阈值、支持箱效果消减速率、治疗阈值/冷却、单片镜 HUD 距离）
- 贴图规范见 `docs/texture_generation_spec.md`（16x16 像素，扁平化高对比度，霓虹高光风格）

## 辅助工具

- `simulations/` 目录含 Python 脚本（`advanced_seed_sim.py`, `level10_probability_sim.py`），用于离线模拟基因育种概率与数值平衡，避免频繁改代码调参

## Phase Gate（强制）

每完成一个阶段，必须执行一次完整测试验证；未通过则禁止进入下一阶段。

1. 代码与资源静态验证：
   - `./gradlew compileJava`
   - 如本阶段涉及数据生成：`./gradlew runData`
2. 构建验证：
   - `./gradlew build`
3. 运行时验证（手工冒烟）：
   - `./gradlew runClient`
   - 至少完成：进入主界面 -> 创建/进入世界 -> 打开创造栏检查本阶段新增内容 -> 基础交互（放置/使用/提示）
4. 结果记录（必须）：
   - 记录通过/失败、失败日志路径、修复提交点。
   - 若失败，先修复并重跑本 Gate，直到通过再继续。
5. 文档同步（必须）：
   - 在 `README.md` 的 Roadmap 中勾选本阶段已完成项。
   - 同步更新本阶段相关说明（如算法、交互规则、前置依赖、验证步骤）。
   - 若代码行为与文档描述不一致，优先修正文档并在结果记录中注明变更点。


**Gate Through Criteria（通过标准）**
- 无阻断级错误：不允许崩溃、注册表加载失败、关键资源缺失导致功能不可用。
- 本阶段新增功能可复现：至少 1 条可重复操作路径从输入到输出成功闭环。
- 构建稳定：`compileJava`、`build` 必须通过；涉及 datagen 的阶段 `runData` 必须通过。

**Decisions**
- 推荐按 README 的三阶段走，但每阶段都要有“可运行 + 可验收”的最小闭环。
- MVP 允许用占位物品/交互先把系统串起来；后续再补流体/能量/多方块与美术。
- 每完成一个阶段都要做一次完整的测试验证，确保基础功能稳定后再迭代复杂度。
- 平衡调整（如突变概率、数值范围）先走离线模拟（如 Python 脚本），再落地游戏实测，避免频繁改代码调参。
- 每完成一个阶段后，必须在 `README.md` 的 Roadmap 中打勾，并更新相关文档说明（如新增基因算法细节时同步更新算法描述）。

## 版本发布流程（强制）

每次版本更新必须按以下顺序执行：

> 仅修改说明文档时不提升版本号，也不触发版本发布；只有代码、资源或用户可见行为发生变化时才发布新版本。
>
> 用户要求时，小型、低风险的文档、图片或维护性修改可直接提交并推送到 `main`，无需创建 PR；版本发布、玩法/API、依赖及其他高风险修改仍必须使用分支和 PR。

### 1. 更新版本号
- `gradle.properties` → `mod_version=X.Y.Z`
- `README.md` → 头部版本号；保持为当前功能说明，不维护历史更新日志
- `README_EN.md` → 头部版本号；保持与中文说明一致，不维护历史更新日志
- 检查 `CLAUDE.md` 是否需要更新（如有核心机制变更）

### 2. 构建验证
```bash
./gradlew build
```

### 3. 提交（按格式）
```bash
git add README.md README_EN.md gradle.properties
git commit -m "release: vX.Y.Z 更新与修复

- 更新：用户可见的更新内容
- 修复：用户可见的问题修复
- ..."
```

### 4. PR 合并 + 自动发布
- 推送版本分支并创建 PR，不直接推送 `main`。
- 版本 PR 正文必须使用下列格式；没有内容的章节直接省略，每一项必须为单行列表：
  ```markdown
  ## 更新

  - 用户可见的功能更新

  ## 修复

  - 用户可见的问题修复
  ```
- PR 正文不要写版本号提升、README 或其他文档同步，也不要加入审计过程、测试过程、内部计划或延期事项。CI 只接受上述格式，并将其写入 annotated tag 注释和 GitHub Release notes。
- PR 合并到 `main` 后，CI 在构建、datagen、Curios、无可选依赖、KubeJS 最低版和 KubeJS 最新版运行时测试全部通过后自动：
  1. 上传 `cybercultivator-X.Y.Z.jar` 为 workflow artifact（保留 30 天）。
  2. 创建 annotated tag `vX.Y.Z`。
  3. 创建同版本 GitHub Release 并附加 JAR。
- 若 `vX.Y.Z` Release 已存在，CI 只保留 workflow artifact；发布新版本前必须提升 `mod_version`。

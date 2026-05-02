# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概况

**Cyber-Cultivator: Bio-Synthesis** — Minecraft Forge 1.20.1 中型模组，核心玩法为遗传育种算法 + 生物强化血清系统。

- **Mod ID:** `cybercultivator`
- **包名:** `com.TKCCOPL`
- **Forge:** 47.4.18 | **Java:** 17
- **前置依赖:** Curios API 5.3.5 (饰品系统，compileOnly API + runtimeOnly 完整模组)
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
| `block/entity/` | TileEntity：`BioIncubatorBlockEntity` (培养槽状态机), `GeneSplicerBlockEntity` (遗传算法), `AtmosphericCondenserBlockEntity` (纯净水生产+相邻传输), `SerumBottlerBlockEntity` (配方驱动加工+漏斗兼容) |
| `item/` | `GeneticSeedItem` (NBT基因标签), `SynapticSerumItem` (血清效果触发，支持构造函数注入不同效果) |
| `effect/` | `SynapticOverclockEffect` (突触超频), `NeuralOverloadEffect` (神经过载副作用), `VisualEnhancementEffect` (S-02 视觉强化), `MetabolicBoostEffect` (S-03 代谢加速) |
| `curios/` | `CuriosCompat` — Curios API 饰品集成（compileOnly），`CurioAccessoryItem` 基类，`BioPulseBeltItem`（腰带），`LifeSupportPackItem`（支持箱），`CurioEventHandler`（事件驱动 tick） |
| `datagen/` | 数据生成器：配方、战利品表、方块状态、物品模型、语言文件、标签、进度引导 |
| `client/` | `ClientTooltipEvents` — 客户端渲染/Tooltip 逻辑，`IncubatorHudOverlay` — 单片镜 HUD 浮窗 |

### 关键机制

**基因系统 (GeneticSeedItem):**
- NBT 标签：`Gene_Speed`, `Gene_Yield`, `Gene_Potency`，范围 1-10
- 拼接公式：`新值 = (父本 + 母本) / 2 + 随机变异(-2..+2)`，clamp 至 1..10

**培养槽 (BioIncubatorBlockEntity):**
- 三项动态数值：Nutrition / Purity / Data Signal (0-100，随时间衰减)
- 交互：放入种子、水桶注入纯净水、生化原液注入营养液、硅碎片注入信号、潜行取回
- Tick 采用静态方法签名 `tick(Level, BlockPos, BlockState, BioIncubatorBlockEntity)`，通过 `BlockEntityTicker` 注册
- 客户端同步：所有状态变更通过 `syncToClient()` → `setChanged()` + `level.sendBlockUpdated()` 推送到客户端

**血清副作用链:**
- `SynapticOverclockEffect` / `VisualEnhancementEffect` / `MetabolicBoostEffect` 结束时均自动施加 `NeuralOverloadEffect`

**Curios 饰品系统:**
- 饰品为纯 `Item` 子类（`CurioAccessoryItem`），不直接实现 `ICurioItem`（Curios 为 compileOnly）
- `CurioEventHandler` 通过 `PlayerTickEvent` + `CuriosApi.getCuriosInventory()` 检测装备状态并驱动逻辑
- 腰带：扫描范围内培养槽，自动消耗背包材料注入三项数值
- 支持箱：加速 NeuralOverload 消退 + 低血量应急治疗（冷却 60s）
- 单片镜 HUD：`IncubatorHudOverlay` 监听 `RenderGuiOverlayEvent`，准星对准培养槽时显示 N/P/D 进度条

**大气冷凝器 (AtmosphericCondenserBlockEntity):**
- 每 200 tick 生产 1 纯净水瓶，库存上限 16
- 相邻传输：下方为培养槽时自动注入 Purity +20（消耗 1 瓶）
- 实现 `WorldlyContainer`，漏斗可从侧面抽取

**血清灌装机 (SerumBottlerBlockEntity):**
- 3 输入槽 + 1 输出槽，配方硬编码：S-02（莓+稀土+瓶）、S-03（莓+乙醇+瓶）
- 加工时间 300 tick，实现 `WorldlyContainer`（顶部/侧面注入，底部抽取）
- `matchRecipe()` 遍历输入槽匹配配方，`consumeInputs()` 消耗材料

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
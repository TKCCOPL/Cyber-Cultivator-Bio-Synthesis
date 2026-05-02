# Changelog

## Phase 6 — 平衡与发布准备

### 数值平衡
- 基因突变范围：`{-1,0,+1,+2}` → `{-2,-1,0,+1,+2}`（期望值从 +0.25 降为 0）
- `VisualEnhancementEffect` 补充副作用触发：S-02 结束后施加神经过载（15+amplifier*5 秒）

### 进度引导
- 新增 `ModAdvancementProvider`，8 个进度节点覆盖完整成长路径
- 进度树：赛博农夫(root) → 硅基起步 → 稀土之源 / 生化培育 → 基因密码 / 血清之路 → 视觉超越 + 代谢狂飙 / 赛博装备

### 多语言
- 新增 `en_us.json` 英文翻译（方块/物品/效果/提示/进度）

### 贴图系统重构
- **Datagen 启用**：`ModBlockStateProvider` + `ModItemModelProvider` 取消注释并补全覆盖范围
- **机器方块多面贴图**：从 `cubeAll`（单面）改为 `cube`（四面独立）
  - `_front`：各机器独立（4 张）
  - `_top`：各机器独立（4 张）
  - `machine_side.png`：统一侧面（所有机器共用）
  - `machine_bottom.png`：统一底面（所有机器共用）
- **机器方块朝向**：新增 `MachineBlock` 基类，`HORIZONTAL_FACING` 属性，放置时 front 面朝向玩家
- **作物生长模型**：从单 stage0 改为 4 阶段（age 0-1→stage0, 2-3→stage1, 4-5→stage2, 6-7→stage3）
- **缺失贴图补齐**：创建 4 个缺失贴图占位（bio_pulse_belt, life_support_pack, protein_soy_stage0, alcohol_bloom_stage0）
- **格式修复**：bio_incubator.png 从索引色转为 RGBA；synaptic_serum_s02.png 从 15x16 修复为 16x16
- **删除冗余**：移除所有手写 blockstate/model JSON（由 datagen 生成）

### Bug 修复
- `AtmosphericCondenserBlockEntity`：修复 `changed = true` 在 `if` 外导致每 tick 调用 `setChanged()` 的性能问题
- `SerumBottlerBlockEntity`：修复处理中每 tick 调用 `setChanged()` 的性能问题
- `GeneSplicerBlockEntity`：补充 `syncToClient()` + `getUpdatePacket()` + `getUpdateTag()`
- `SerumBottlerBlockEntity`：补充 `getUpdatePacket()` + `getUpdateTag()`
- `AtmosphericCondenserBlockEntity`：补充 `getUpdatePacket()` + `getUpdateTag()`
- `VisualEnhancementEffect`：补充 `removeAttributeModifiers` 触发神经过载副作用

### 已知问题
- 部分贴图为占位色块，待替换正式美术资源（详见 `docs/texture_generation_spec.md`）

---

## v1.0.0 — 发布前验收修复

### Bug 修复
- `AtmosphericCondenserBlockEntity`：补充 `sendBlockUpdated()` 实现客户端实时同步
- `SerumBottlerBlockEntity`：补充 `sendBlockUpdated()` 实现客户端实时同步
- `GeneSplicerBlock`：移除空 ticker lambda（瞬时操作无需 tick），返回 `null`
- `VisualEnhancementEffect`：修正发光逻辑，改为对 32 格范围内非玩家生物施加 `GLOWING`（符合"透视墙后生物轮廓"设计意图）

### 性能优化
- `GeneticSeedItem`：`ensureGeneData()` 增加 early return，避免每 tick 重复 NBT 操作
- `LifeSupportPackItem`：NeuralOverload 消退逻辑改为每 10 tick 执行一次，减少 90% 的 `MobEffectInstance` 对象创建

### 配置系统
- `Config.java` 新增 `beltPurityThreshold`（默认 50）和 `beltDataSignalThreshold`（默认 25），替代原 `beltNutritionThreshold / 2` 硬编码
- `BioPulseBeltItem` 改用独立阈值配置字段

### 资源完整性
- 补充 `head.json` Curios 槽位定义（单片镜装备必需）
- `gradle.properties`：版本号修正为 `1.0.0`（SemVer），补充 `mod_authors` 和 `mod_description`

---

## Phase 5 — Curios 深化与玩家交互

### 新增内容
- `CurioAccessoryItem` 基类：支持 `initCapabilities` + `CuriosCapability.ITEM`（右键装备）
- `BioPulseBeltItem`：Belt 槽位，每 20 tick 扫描范围内培养槽，自动注入营养/纯净水/信号
- `LifeSupportPackItem`：Back 槽位，加速 NeuralOverload 消退 + 低血量应急治疗
- `IncubatorHudOverlay`：佩戴单片镜 + 准星对准培养槽时显示 N/P/D 进度条
- `Config.java`：8 项可配置参数（含 beltPurityThreshold、beltDataSignalThreshold）

### Curios 配置
- `compileOnly` API + `runtimeOnly` 完整模组
- `mixin.env.remapRefMap` 修复 mixin refmap 重映射
- 槽位定义：`data/cybercultivator/curios/slots/` + `entities/player.json`
- 物品映射：`data/curios/tags/items/{head,belt,back}.json`

---

## Phase 4 — 设施扩展与自动化

### 新增内容
- `AtmosphericCondenserBlock` + `AtmosphericCondenserBlockEntity`：每 200 tick 生产纯净水瓶，相邻传输 Purity
- `SerumBottlerBlock` + `SerumBottlerBlockEntity`：3 输入 + 1 输出，配方驱动加工
- `VisualEnhancementEffect`（S-02）：夜视 + 发光
- `MetabolicBoostEffect`（S-03）：回血 + 急迫
- `purified_water_bottle`、`synaptic_serum_s02`、`synaptic_serum_s03` 物品

### 产线闭环
冷凝器产出纯净水 → 培养槽自动接收 → 作物成熟 → 灌装机加工高级血清

---

## Phase 3 — 强化与数值平衡

### 新增内容
- `SynapticSerumItem`：支持构造函数注入效果
- `SynapticOverclockEffect`：攻速+25%、移速+15%、急迫II
- `NeuralOverloadEffect`：减速 + 饥饿副作用
- 配方：`bio_incubator`、`gene_splicer`、`synaptic_neural_berry`、`synaptic_serum_s01`
- 战利品表覆盖：硅晶矿、稀土矿、培养槽、拼接机、三类基础作物

---

## Phase 2 — 核心系统实现

### 新增内容
- `BioIncubatorBlock` + `BioIncubatorBlockEntity`：培养槽状态机（Nutrition/Purity/DataSignal）
- `GeneticSeedItem`：NBT 基因标签（Gene_Speed/Gene_Yield/Gene_Potency，1-10）
- `GeneSplicerBlock` + `GeneSplicerBlockEntity`：双输入拼接，公式 `floor((A+B)/2) + random(-2..+2)`
- `BasicCropBlock`：基础作物方块
- 交互：放入种子、注入纯净水/营养液/信号、潜行取回

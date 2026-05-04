# Changelog

## v1.1.2 — Gene_Synergy 重命名 + Mutation 标签升级 + HUD 透明化 + BlockEntity 同步修复

### 变更
- **Gene_Purity → Gene_Synergy 重命名：** 基因标签从 `Gene_Purity` 改为 `Gene_Synergy`（协同基因），避免与乙醇品质 `Purity` 混淆。涉及 8 个文件全量替换
- **Mutation 标签升级：** 从布尔值升级为整数类型码（0=未突变, 1=数值突破, 2=协同基因），新增 `MutationDetail` 字符串标签记录突变详情（如 `Potency+3`、`Synergy+2`）
- **calculateActivity 合并：** 灌装机 Activity 计算从双循环合并为单循环，删除莓的 Gene_Purity 死标签
- **HUD 全透明背景：** 移除 4 个 HUD 面板的背景 fill 调用，只保留进度条背景
- **Tooltip/HUD 突变显示：** 按 Mutation 类型码显示"数值突破"或"协同基因"+具体详情

### Bug 修复
- **BlockEntity 同步修复：** `saveAdditional()` 空字段写入哨兵 CompoundTag，修复 `ClientboundBlockEntityDataPacket` 因 tag 为空导致客户端不调用 `load()` 的问题
- **死 key 清理：** 删除 3 个无引用的语言 key（`tooltip.cybercultivator.mutation`、`hud.cybercultivator.mutation`、`hud.cybercultivator.progress`）
- **语言文件同步：** `en_us.json` 和 `zh_cn.json` 通过 datagen 和手动更新保持一致

---

## v1.1.1 — 血清效果重平衡

### 血清效果重平衡
- **S-01 突触超频：** 攻速+力量全部随 amplifier 动态增长（每级 +5%），移除急迫效果，新增抗性提升（上限 III 级）
- **S-02 视觉强化：** 发光范围 16-48 格随 amplifier 增长（每级 +8 格），新增抗火效果（上限 III 级）
- **S-03 代谢加速：** 移除急迫效果，新增移速+跳跃提升（上限 III 级），回血保持 amplifier 缩放
- **副作用差异化：** 副作用按血清来源分支 — S-01 凋零+饥饿，S-02 失明+饥饿，S-03 缓慢+中毒
- **来源感知机制：** `NeuralOverloadEffect` 通过静态 `ConcurrentHashMap` 存储来源 ID，按血清类型分支施加不同副作用组合

### 变更
- `SynapticOverclockEffect`：属性修饰符改为 `applyEffectTick` 中动态计算，每秒刷新
- `VisualEnhancementEffect`：添加抗火效果 + 发光范围随 amplifier 增长
- `MetabolicBoostEffect`：移速改为 transient modifier 动态计算，添加跳跃提升
- `NeuralOverloadEffect`：`applyEffectTick` 中按 source 分支施加不同副作用

---

## v1.1.0 — 血清品质链路 + 叠加升级 + HUD 扩展

### 新功能
- **血清品质链路：** 原料品质 NBT（植物纤维→Potency，工业乙醇→Purity，生化原液→Concentration）从种子基因继承，灌装机合成莓时计算突触活性（加权平均），血清继承莓的 Activity
- **血清叠加升级：** 多次饮用同种血清 amplifier +1（上限 V 级），持续时间累加（上限 5 分钟），Activity ≥ 8 起步 II 级
- **灌装机 4 种配方：** 莓合成（纤维+乙醇+原液）、S-01（莓+原液+瓶）、S-02（莓+稀土+瓶）、S-03（莓+乙醇+瓶）
- **单片镜 HUD 扩展：** 支持灌装机（配方/进度/活性）、冷凝器（进度/库存/状态）、拼接机（种子基因/输出结果）
- **创造栏品质变体：** 7 种物品 × 10 个品质等级
- **基因拼接机 HUD：** 显示父本种子基因和输出结果

### Bug 修复
- `SynapticOverclockEffect` / `VisualEnhancementEffect` / `MetabolicBoostEffect`：修复 `removeAttributeModifiers` 中直接 `addEffect` 导致 `ConcurrentModificationException` 崩溃（喝牛奶时触发），改用 TickTask 延迟施加
- `SynapticSerumItem`：修复血清叠加时提前触发神经过载副作用，添加 `entity.getEffect(this) == null` 检查
- `SynapticSerumItem`：修复血清持续时间无法叠加，添加 `existing.getDuration()` 累加逻辑
- `SerumBottlerBlockEntity`：修复 HUD 进度条不动，tick 中每 20 tick 同步一次进度
- `SerumBottlerBlockEntity`：修复血清 Activity 继承失败，调换 `getRecipeOutput` 和 `consumeInputs` 调用顺序
- `SerumBottlerBlockEntity`：修复 Activity 公式因输入槽位顺序不同导致结果不一致，改为按物品种类查找输入
- `SerumBottlerBlockEntity`：修复灌装机取回输入物品时未取消加工状态，添加 `cancelProcessing()` 方法
- `GeneSplicerBlockEntity`：修复插入种子 A 直接输出（Forge 1.20.1 单次右键触发两次 `Block.use()`），添加同 tick 防抖
- `GeneSplicerBlockEntity`：修复拼接机 HUD 跳过种子 B 显示，`craftOutput` 后保留种子直到取出输出
- `AtmosphericCondenserBlockEntity`：修复 HUD 进度条不动，tick 中每 20 tick 同步一次进度
- `AtmosphericCondenserBlockEntity`：修复 `extractOutput()` 缺少客户端同步
- `en_us.json`：补充 6 个血清品质链路翻译键

### 变更
- `build.gradle`：排除 `cybercultivator-github.png` 打包进 JAR（7.1MB → 295KB）
- `ModRecipeProvider`：移除莓和 S-01 的合成台配方（迁移到灌装机）
- `ModCreativeTabs`：添加品质变体（1-10 全等级）

---

## v1.0.1 — Bug 修复与体验优化

### Bug 修复
- `BioIncubatorBlockEntity`：修复培养槽放入一个种子可无限产出的 bug，成熟后清除种子
- `BioIncubatorBlockEntity`：修复 `getCropOutput()` 中蛋白质豆种子错误产出突触神经莓，改为正确产出生化原液
- `BioIncubatorBlockEntity`：`tryInsertSeed()` 调用 `ensureGeneData()` 确保种子携带基因 NBT
- `SerumBottlerBlock`：修复血清灌装机放入配方材料后无法取出的 bug，空手右键可取回输入槽材料

### 体验优化
- `IncubatorHudOverlay`：单片镜 HUD 新增作物生长进度条(G) + 预计成熟时间(ETA)
- `IncubatorHudOverlay`：修复 HUD 面板文本重叠问题，ETA 文本移至独立行
- `AtmosphericCondenserBlockEntity`：生产周期从 200 tick 调整为 600 tick（30 秒），库存上限从 16 调整为 32
- `BioIncubatorBlockEntity`：新增 `getGrowthPercent()`、`getCurrentGrowthRate()`、`getEstimatedSecondsRemaining()` 方法
- `BioIncubatorBlockEntity`：`getCropOutput()` 受 Yield 基因影响（yield 1-10 → 产出 2-5），未知种子返回种子副本作为保底

---

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

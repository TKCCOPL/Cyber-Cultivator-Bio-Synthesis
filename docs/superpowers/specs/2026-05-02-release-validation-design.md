# 1.0 发布前验收 — 全面修复设计规范

## 背景

Cyber-Cultivator: Bio-Synthesis 模组 Phase 1-6 全部完成，目标直接发布 1.0.0 正式版。通过全面代码审查发现了 5 个严重 bug、2 个性能问题、多个资源完整性和兼容性问题，需在发布前修复。

## 修复模块

### 模块一：BlockEntity 客户端同步修复

**问题：** `AtmosphericCondenserBlockEntity.tick()` 和 `SerumBottlerBlockEntity.tick()` 状态变更后只调用 `setChanged()`，缺少 `level.sendBlockUpdated()`，客户端无法实时同步。

**修复：**
- `AtmosphericCondenserBlockEntity.java:62-64`：在 `if (changed)` 块中补充 `level.sendBlockUpdated(pos, state, state, 3)`
- `SerumBottlerBlockEntity.java:76-78`：同上

**涉及文件：**
- `src/main/java/com/TKCCOPL/block/entity/AtmosphericCondenserBlockEntity.java`
- `src/main/java/com/TKCCOPL/block/entity/SerumBottlerBlockEntity.java`

---

### 模块二：GeneSplicerBlock 空 Ticker 清理

**问题：** `GeneSplicerBlock.getTicker()` 注册了空 lambda `(lvl, p, s, be) -> {}`，拼接机是瞬时操作不需要 tick，浪费服务端调度资源。

**修复：** `getTicker()` 返回 `null`。

**涉及文件：**
- `src/main/java/com/TKCCOPL/block/GeneSplicerBlock.java`

---

### 模块三：VisualEnhancementEffect 发光逻辑修正

**问题：** 当前给玩家自身施加 `GLOWING`，让其他玩家看到你的轮廓，而非你能看到其他生物轮廓。与 S-02 "透视墙后生物轮廓" 设计意图相反。

**修复：** 移除自身 `GLOWING`，改为在 `applyEffectTick()` 中对 32 格范围内的非玩家 `LivingEntity` 施加短时 `GLOWING`（80 tick），使佩戴者能看到墙后生物轮廓。

**涉及文件：**
- `src/main/java/com/TKCCOPL/effect/VisualEnhancementEffect.java`

---

### 模块四：BioPulseBeltItem 阈值语义修正

**问题：** Purity 和 DataSignal 都使用 `Config.beltNutritionThreshold`（营养阈值），DataSignal 硬编码 `/2`。

**修复：**
- `Config.java` 新增 `beltPurityThreshold`（默认 50）和 `beltDataSignalThreshold`（默认 25）
- `BioPulseBeltItem.java` 改用各自阈值

**涉及文件：**
- `src/main/java/com/TKCCOPL/Config.java`
- `src/main/java/com/TKCCOPL/curios/BioPulseBeltItem.java`

---

### 模块五：GeneticSeedItem 性能优化

**问题：** `inventoryTick()` 每 tick 对每个种子调用 `ensureGeneData()`，产生不必要的 NBT 操作。

**修复：** `ensureGeneData()` 开头加 early return：`if (stack.hasTag() && stack.getTag().contains(GENE_SPEED)) return;`

**涉及文件：**
- `src/main/java/com/TKCCOPL/item/GeneticSeedItem.java`

---

### 模块六：LifeSupportPackItem 频率控制

**问题：** NeuralOverload 消退每 tick 创建新 `MobEffectInstance`。

**修复：** 每 10 tick 执行一次消退，每次减少 `Config.packEffectReductionRate * 10`。使用 `level.getGameTime() % 10L == 0L` 控制频率。

**涉及文件：**
- `src/main/java/com/TKCCOPL/curios/LifeSupportPackItem.java`

---

### 模块七：Curios 兼容性加固

**问题：** `data/cybercultivator/curios/slots/` 缺少 `head.json`。Curios API 5.x 中 `head` 不是内置槽位，需要模组自行定义。

**修复：** 新增 `src/main/resources/data/cybercultivator/curios/slots/head.json`：
```json
{
  "order": 10,
  "size": 1,
  "icon": ""
}
```

同时在 `data/cybercultivator/curios/entities/player.json` 中确认 head 槽位已包含。

**涉及文件：**
- `src/main/resources/data/cybercultivator/curios/slots/head.json`（新增）
- `src/main/resources/data/cybercultivator/curios/entities/player.json`（验证）

---

### 模块八：资源完整性与元数据

1. `gradle.properties`：`mod_version=1.0` → `1.0.0`，`mod_authors` 和 `mod_description` 填写
2. `CHANGELOG.md`："已知问题"中效果贴图条目移除（已修复），追加本次修复内容
3. `en_us.json`：检查是否覆盖新增的 Config 描述（Config 描述在 BUILDER 中为英文，无需 lang 条目）

**涉及文件：**
- `gradle.properties`
- `CHANGELOG.md`
- `src/main/resources/assets/cybercultivator/lang/en_us.json`

---

### 模块九：构建验证链路

修复完成后执行 Phase Gate：
1. `./gradlew compileJava` — 编译检查
2. `./gradlew runData` — 数据生成器验证
3. `./gradlew build` — 完整构建
4. 检查 `build/libs/` 输出的 jar 文件名和版本号

## 不涉及的内容

- 不改动任何游戏玩法设计或数值
- 不新增功能
- 不修改贴图（保持当前占位贴图，待后续美术资源替换）
- 不改动 datagen provider（除非新 Config 需要语言条目）

## 验证标准

- `compileJava` 通过，零 error
- `runData` 通过（如涉及数据变更）
- `build` 通过，产出可用 jar
- 所有修复点有对应的代码变更
- CHANGELOG 完整记录所有变更

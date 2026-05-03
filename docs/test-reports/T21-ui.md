# T21 UI/本地化测试报告

**测试任务:** T21 突变概率计算 + 突变结果
**测试范围:** GeneSplicerBlockEntity.java, GeneticSeedItem.java 及相关客户端渲染/翻译文件
**测试日期:** 2026-05-04

---

## 测试结果：PASS

### 检查结果

| 检查项 | 结果 | 备注 |
|--------|------|------|
| 语言覆盖 | ✅ | zh_cn.json 覆盖全部 16 物品 + 9 方块 + 4 效果 + 所有 tooltip 键 |
| 无暴露 key | ✅ | grep 确认无 `xxx.name` 或未翻译 key 暴露 |
| Tooltip 文本 | ✅ | GeneticSeedItem 的 3 基因 + 隐藏提示键完整；SynapticSerumItem 的活性/等级/倍率键完整 |
| Tooltip 渲染 | ✅ | ClientTooltipEvents 逻辑正确：无单片镜时隐藏基因、有时显示 Speed/Yield/Potency |
| HUD 渲染 | ✅ | Splicer HUD 坐标计算无越界，宽度 200px / 高度 50-62px，进度条 bar 在范围内 |
| 缩放适配 | ✅ | HUD 使用固定锚点 (10,10)，符合 Minecraft HUD overlay 标准做法 |
| Lang Provider | ✅ | ModLangProvider 覆盖全部注册的方块/物品/效果 + tooltip + advancement |
| 模型生成 | ✅ | ModBlockStateProvider 覆盖 9 方块（含 crop 8 阶段），ModItemModelProvider 覆盖 16 物品 |

### 针对 T21 审查要点的逐项分析

#### 1. 突变视觉反馈（HUD "★ MUTATION!"）— 当前状态

**结论: 未实现，属于 T23 范围，当前无问题。**

- `GeneSplicerBlockEntity.craftOutput()` 第 180-182 行写入 `result.getOrCreateTag().putBoolean("Mutation", true)`，但仅作为 NBT 标记
- `IncubatorHudOverlay.drawSplicerHud()` 输出行仅显示 `Out: [S:%d Y:%d P:%d]`，未检查或渲染 Mutation 状态
- `ClientTooltipEvents.onItemTooltip()` 未读取 `Mutation` 布尔值
- dev-plan.md 明确将视觉反馈分配给 T23（"视觉反馈 + Tooltip + HUD — 种子Tooltip+拼接机突变标记+单片镜HUD"）

**T23 需要实现的 UI 增强:**
- ClientTooltipEvents 中添加 Mutation 标记显示（如 "★ 突变!" 黄色文字）
- drawSplicerHud 中 output 行增加突变标识
- 可考虑在 output 基因行后追加一行 "★ MUTATION" 高亮提示

#### 2. Gene_Purity 翻译键 — 当前状态

**结论: 无 `tooltip.cybercultivator.gene_purity` 键，需要在 T24 新增。**

当前 lang 文件中存在的相关键:
- `tooltip.cybercultivator.quality_purity` — 用于工业乙醇原料品质显示（"纯度: %s/10"）
- `tooltip.cybercultivator.gene_potency` — 用于种子基因 Potency 显示

T21 新增了 Purity 突变逻辑（第 172-177 行），突变时写入 `Gene_Purity` 到种子 NBT，但:
- `ClientTooltipEvents` 仅显示 Speed/Yield/Potency，**不显示 Gene_Purity**
- HUD `drawSeedInfo()` 仅显示 Speed/Yield/Potency，**不显示 Gene_Purity 或 Generation**
- 缺少专用翻译键 `tooltip.cybercultivator.gene_purity`

**T24 需要新增的翻译键:**
| 键 | 建议值 | 用途 |
|----|--------|------|
| `tooltip.cybercultivator.gene_purity` | `Gene_Purity: %s` | 种子 Tooltip 显示 Purity 基因 |
| `tooltip.cybercultivator.gene_generation` | `Gen: %s` | 种子 Tooltip 显示代数 |

#### 3. Mutation 标记 Tooltip 显示 — 当前状态

**结论: 未实现，属于 T23 范围，当前无问题。**

- `GeneticSeedItem` 不包含任何 mutation 相关的 tooltip 逻辑
- `ClientTooltipEvents` 不检查 `Mutation` NBT 标签
- 这与 dev-plan.md 的 T23 分配一致

### T21 代码对 UI/本地化的副作用评估

| 变更 | UI 影响 | 说明 |
|------|---------|------|
| `Gene_Generation` NBT 写入（第 186 行） | 无 | 仅写入 NBT，无客户端显示代码 |
| `Gene_Purity` NBT 写入（第 176 行） | 无 | 仅写入 NBT，无客户端显示代码 |
| `Mutation` 布尔标记（第 181 行） | 无 | 仅写入 NBT，无客户端显示代码 |
| 突变概率计算（第 131-140 行） | 无 | 纯服务端逻辑 |

T21 的所有变更均为服务端 NBT 写入和逻辑计算，不引入任何客户端渲染问题。新增的 3 个 NBT 标签（Generation / Purity / Mutation）均为"静默写入"状态，等待 T23/T24 添加对应的可视化支持。

### HUD 坐标安全性审查

`drawSplicerHud()` 坐标分析:
- 背景: `(10, 10)` 到 `(210, 62/72)` — 最大 200x62 像素
- Seed A 行: `y+14` (y=24)
- Seed B 行: `y+26` (y=36)
- 分隔线: `y+37` 到 `y+38` (y=47-48)
- Output 基因行: `y+40` (y=50)
- Ready 行: `y+52` (y=62)
- 空输出行: `y+38` (y=48)

所有行在 HUD 背景范围内，无越界风险。当前 T21 未修改 HUD 代码，无新增坐标风险。

### 问题列表

无严重或中等问题。

轻微问题:
1. [轻微] HUD 文本全部使用 `Component.literal()` 硬编码英文，如 "[Gene-Splicer]"、"Speed:"、"Ready"。对于技术型 HUD 这是可接受的做法，但如需完整本地化应在 T23/T24 中改为 `Component.translatable()`。
2. [轻微] T21 新增的 `Gene_Purity` 和 `Gene_Generation` 在种子上无法被玩家感知（无 Tooltip、无 HUD 显示），需依赖 T23/T24 补全。这不是 T21 的缺陷，但应在 T23/T24 的验收标准中明确追踪。

### 总体评价

T21 的突变系统在 NBT 层面正确写入了 Mutation/Purity/Generation 标记，所有变更均为服务端逻辑，不引入客户端渲染或本地化回归。视觉反馈和翻译键缺口已在 dev-plan.md 中正确分配给 T23/T24，当前状态无阻断问题，**通过审查**。

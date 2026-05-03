# 血清品质链路 + 升级机制 设计文档

> 日期：2026-05-03
> 状态：已确认

## 需求背景

当前血清系统存在两个问题：
1. **血清与种植脱节** — 种子的 Potency 基因没有实际用途，血清效果固定不变
2. **血清缺乏深度** — 三种血清效果固定，无法升级，玩家没有重复使用动力

**目标：** 打通"种植 → 原料品质 → 莓品质 → 血清品质"的完整数值链路，并引入血清叠加升级机制。

---

## 设计概览

```
种子 Potency (1-10)
  ↓ 培养槽成熟
原料 {品质 NBT}  ←── 植物纤维(Potency) / 工业乙醇(Purity) / 生化原液(Concentration)
  ↓ 灌装机合成
突触神经莓 {SynapticActivity}  ←── 加权平均 (0.25/0.375/0.375)
  ↓ 灌装机合成
血清 {SynapticActivity}  ←── 继承莓的值
  ↓ 饮用
效果强度/时长 按 Activity 缩放 + 叠加升级(最高V级)
```

---

## 第 1 节：数据模型 — NBT 品质标签

每种中间产物携带一个品质值（1-10），通过 NBT 存储：

| 物品 | NBT Key | 来源 | 范围 |
|------|---------|------|------|
| 植物纤维 | `Potency` | 纤维芦苇种子的 Potency 基因 | 1-10 |
| 工业乙醇 | `Purity` | 酒精花种子的 Potency 基因 | 1-10 |
| 生化原液 | `Concentration` | 蛋白质大豆种子的 Potency 基因 | 1-10 |
| 突触神经莓 | `SynapticActivity` | 加权平均 (0.25/0.375/0.375) | 1-10 |
| 血清 (S-01/02/03) | `SynapticActivity` | 继承自莓 | 1-10 |

**实现方式：** 不创建新物品子类。NBT 由产出方（培养槽/灌装机）在合成时写入。

---

## 第 2 节：培养槽产出 — 品质写入

修改 `BioIncubatorBlockEntity.getCropOutput()`，在创建产出 ItemStack 后写入种子的 Potency：

```
Fiber Reed Seeds   → ItemStack(PLANT_FIBER, count)        + tag.putInt("Potency", seedPotency)
Protein Soy Seeds  → ItemStack(BIOCHEMICAL_SOLUTION, count) + tag.putInt("Concentration", seedPotency)
Alcohol Bloom Seeds → ItemStack(INDUSTRIAL_ETHANOL, count)  + tag.putInt("Purity", seedPotency)
```

- `seedPotency` 从种子 NBT 的 `Gene_Potency` 读取
- 玩家育种提高 Potency → 产出更高品质原料

---

## 第 3 节：突触活性公式 + 血清效果缩放

### 突触活性计算（加权平均）

```
Activity = clamp(round(Potency × 0.25 + Purity × 0.375 + Concentration × 0.375), 1, 10)
```

- 工业乙醇和生化原液权重更高（各 0.375），植物纤维权重较低（0.25）
- 输出 clamp 到 1-10

### 血清效果缩放

```
durationMultiplier = 0.5 + Activity × 0.1
baseAmplifier = Activity >= 8 ? 1 : 0
```

| Activity | 持续时间倍率 | 基础 amplifier |
|----------|-------------|---------------|
| 1 | ×0.6 | 0 (I级) |
| 5 | ×1.0 | 0 (I级) |
| 8 | ×1.3 | 1 (II级) |
| 10 | ×1.5 | 1 (II级) |

- Activity=5 时效果与当前完全一致（平衡点）
- Activity>=8 时起步 amplifier=1（II级），到顶更快
- 低品质血清时长更短，高品质血清更持久且起步高一级

---

## 第 4 节：血清饮用逻辑 — 叠加升级

### 叠加机制

每种血清（S-01/S-02/S-03）独立叠加，因为它们施加不同的效果类型。

修改 `SynapticSerumItem.finishUsingItem()`：

```
1. 读取血清 NBT 的 SynapticActivity 值
2. 计算实际持续时间 = baseDuration × (0.5 + Activity × 0.1)
3. 计算实际 amplifier：
   a. 检查玩家是否已有相同效果
   b. 若有 → 新 amplifier = min(当前 amplifier + 1, 4)
   c. 若无 → 新 amplifier = Activity >= 8 ? 1 : 0
4. 施加效果
5. 施加 NeuralOverload（同步缩放）
```

### 叠加等级示意

```
Activity < 8 时：
  第1次饮用 → amplifier 0 (I级)
  第2次饮用 → amplifier 1 (II级)
  第3次饮用 → amplifier 2 (III级)
  第4次饮用 → amplifier 3 (IV级)
  第5次饮用 → amplifier 4 (V级)  ← 上限

Activity >= 8 时（高品质血清）：
  第1次饮用 → amplifier 1 (II级)  ← 起步高一级
  第2次饮用 → amplifier 2 (III级)
  第3次饮用 → amplifier 3 (IV级)
  第4次饮用 → amplifier 4 (V级)  ← 少喝一次到顶
```

### 神经过载缩放

血清效果结束时（`removeAttributeModifiers`），施加 NeuralOverload：

```
NeuralOverload 持续时间 = 20 × (12 + 当前血清amplifier × 4) tick
NeuralOverload amplifier = 当前血清amplifier
```

- 当前实现：`20 * (12 + amplifier * 4)` — amplifier 0 时 12s，amplifier 4 时 28s
- amplifier 由血清叠加等级决定，天然随等级递增
- NeuralOverload 的 `applyEffectTick` 施加减速和饥饿，强度随 amplifier 增加
- 不需要额外改动 `NeuralOverloadEffect`，现有逻辑已按 amplifier 缩放减速/饥饿强度

- 等级越高，副作用越猛（持续时间更长 + 减速饥饿更强）
- 高品质血清起步强但副作用也更大，形成风险-收益权衡

---

## 第 5 节：配方变更 + 灌装机改造

### 突触神经莓配方（新）

```
植物纤维 + 工业乙醇 + 生化原液 → 突触神经莓
  ↳ 莓的 SynapticActivity = clamp(round(纤维.Potency×0.25 + 乙醇.Purity×0.375 + 原液.Concentration×0.375), 1, 10)
```

### 血清配方

```
S-01（新）：莓 + 生化原液 + 瓶 → S-01 血清（继承莓的 Activity）
S-02（不变）：莓 + 稀土尘 + 瓶 → S-02 血清（继承莓的 Activity）
S-03（不变）：莓 + 工业乙醇 + 瓶 → S-03 血清（继承莓的 Activity）
```

### 灌装机改造要点

- `matchRecipe()` 扩展支持 4 种配方（莓 + 3 种血清）
- 莓配方：读取 3 输入槽的品质 NBT → 计算 Activity → 写入产出
- 血清配方：读取莓的 Activity → 继承到血清 NBT
- 现有 `consumeInputs()` 和 `getRecipeOutput()` 需要对应扩展

### Tooltip 更新

- 原料物品：显示品质值（如 "品质: 7/10"）
- 莓：显示 "突触活性: 7/10"
- 血清：显示 "突触活性: 7/10" + "等级: III" + 效果缩放信息

---

## 第 6 节：拼接机联动

拼接机不需要改动——现有基因遗传逻辑已经正确处理 Potency。

育种系统因此获得实际意义：
- 之前：Potency 基因无实际用途
- 现在：Potency 直接影响原料品质 → 血清强度
- 玩家有动力三种作物都育种提高 Potency

---

## 影响文件清单

| 文件 | 改动类型 | 说明 |
|------|---------|------|
| `BioIncubatorBlockEntity.java` | 修改 | `getCropOutput()` 写入品质 NBT |
| `SerumBottlerBlockEntity.java` | 修改 | 扩展配方（莓配方 + S-01）、品质读写 |
| `SynapticSerumItem.java` | 修改 | 饮用叠加逻辑、Activity 缩放、Tooltip |
| `SynapticOverclockEffect.java` | 修改 | 移除效果时按当前血清 amplifier 施加 NeuralOverload |
| `VisualEnhancementEffect.java` | 修改 | 同上 |
| `MetabolicBoostEffect.java` | 修改 | 同上 |
| `NeuralOverloadEffect.java` | 不改动 | 现有逻辑已按 amplifier 缩放减速/饥饿强度 |
| `ModLangProvider.java` | 修改 | 新增翻译键（品质、等级、活性等） |
| `ModRecipeProvider.java` | 修改 | 莓配方 + S-01 配方 datagen |

---

## 验收标准

1. **品质传递闭环**：高 Potency 种子 → 品质原料 → 高 Activity 莓 → 强血清
2. **叠加升级**：同种血清多次饮用，amplifier 逐次 +1 直到 V 级
3. **负面效果同步**：高等级血清结束后，神经过载持续时间更长、强度更高
4. **Tooltip 正确**：所有品质相关物品显示正确的数值信息
5. **现有功能不退化**：S-02/S-03 配方和效果在 Activity=5 时与改动前一致
6. **编译通过**：`./gradlew compileJava` + `./gradlew runData` + `./gradlew build`

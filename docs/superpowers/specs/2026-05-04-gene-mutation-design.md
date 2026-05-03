# 基因突变事件系统设计规范

## 需求背景

当前基因拼接公式为 `floor((A+B)/2) + random(-2, +2)`，变异范围固定，缺乏深度。需要引入突变事件系统，增加育种策略性和惊喜感。

## 技术方案

### 1. 代数追踪

种子 NBT 新增 `Gene_Generation` 标签（int）：
- 初始种子（战利品箱/草丛）：Generation = 0
- 拼接产出：Generation = max(父本.Gen, 母本.Gen) + 1
- 培养槽产出的原料：继承种子的 Generation

### 2. 突变概率公式

```
mutationChance = 5% + (max(Gen_A, Gen_B) × 2%) + (maxDiff × 1%)

maxDiff = max(|Speed_A - Speed_B|, |Yield_A - Yield_B|, |Potency_A - Potency_B|)
```

| 代数 | 基础+代数 | 差异5 | 差异9 |
|------|----------|------|------|
| 0 | 5% | 10% | 14% |
| 3 | 11% | 16% | 20% |
| 5 | 15% | 20% | 24% |

### 3. 突变结果

突变发生时，按概率选择类型：

| 类型 | 概率 | 效果 |
|------|------|------|
| 数值突破 | 80% | 随机一个基因变异 ±4（其他 ±2） |
| Gene_Purity 获得 | 20% | Gene_Purity += random(1, 3)，上限 10 |

**数值突破：**
- 随机选中 Speed/Yield/Potency 之一
- 该基因：floor((A+B)/2) + random(-4, +4)
- 其他基因：floor((A+B)/2) + random(-2, +2)
- 结果 clamp 到 1-10

**Gene_Purity 获得：**
- 无 Gene_Purity → 新增 random(1, 3)
- 已有 Gene_Purity → += random(1, 3)，上限 10
- Gene_Purity 不参与标准拼接遗传（突变专属）

### 4. Gene_Purity 效果

```
Activity 上限 = 10 + floor(Gene_Purity / 2)
Activity = clamp(原公式结果, 1, Activity上限)
```

| Gene_Purity | Activity 上限 | 最高持续时间倍率 |
|-------------|-------------|----------------|
| 0 | 10 | 1.5x |
| 2 | 11 | 1.6x |
| 4 | 12 | 1.7x |
| 6 | 13 | 1.8x |
| 8 | 14 | 1.9x |
| 10 | 15 | 2.0x |

### 5. 视觉反馈

- 拼接机 HUD：突变时显示 "★ MUTATION!"
- 种子 Tooltip：Gene_Purity 用紫色显示
- 单片镜 HUD：显示 Generation 和 Gene_Purity

## 任务拆分

### 任务列表

- [ ] **T20: Gene_Generation 代数追踪**
  - 描述：种子 NBT 新增 Gene_Generation，拼接时继承+1，培养槽产出继承
  - 涉及文件：`GeneticSeedItem.java`, `GeneSplicerBlockEntity.java`, `BioIncubatorBlockEntity.java`
  - 预估：60 行
  - 依赖：无

- [ ] **T21: 突变概率计算 + 突变结果**
  - 描述：拼接时计算突变概率，触发后选择类型（数值突破/Gene_Purity），应用结果
  - 涉及文件：`GeneSplicerBlockEntity.java`
  - 预估：100 行
  - 依赖：T20

- [ ] **T22: Gene_Purity 对 Activity 的影响**
  - 描述：灌装机和血清读取 Gene_Purity，Activity 上限突破 10
  - 涉及文件：`SerumBottlerBlockEntity.java`, `SynapticSerumItem.java`
  - 预估：40 行
  - 依赖：T21

- [ ] **T23: 视觉反馈 + Tooltip + HUD**
  - 描述：种子 Tooltip 显示 Generation/Purity，拼接机 HUD 突变标记，单片镜 HUD 更新
  - 涉及文件：`ClientTooltipEvents.java`, `IncubatorHudOverlay.java`, `GeneSplicerBlockEntity.java`
  - 预估：60 行
  - 依赖：T21

- [ ] **T24: 语言文件 + 编译验证**
  - 描述：新增翻译键，编译验证
  - 涉及文件：语言文件
  - 预估：20 行
  - 依赖：T23

## 验收标准

1. `./gradlew compileJava` 通过
2. 初始种子 Generation = 0，拼接后 Generation = max(父本, 母本) + 1
3. 突变概率公式正确（5% + 代数2% + 差异1%）
4. 80% 概率数值突破（±4），20% 概率获得 Gene_Purity
5. Gene_Purity 为突变专属，不通过正常拼接遗传
6. Activity 上限 = 10 + floor(Purity/2)，血清效果正确缩放
7. Tooltip/HUD 正确显示 Generation 和 Gene_Purity

## 参考资料

- `GeneticSeedItem.java`：种子 NBT 管理
- `GeneSplicerBlockEntity.java`：拼接公式
- `SynapticSerumItem.java`：血清 Activity 逻辑
- `docs/superpowers/specs/2026-05-03-serum-quality-chain-design.md`：品质链路设计

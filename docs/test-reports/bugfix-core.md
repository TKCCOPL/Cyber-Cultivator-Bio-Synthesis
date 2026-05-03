## 测试结果：PASS

**测试任务：** 两个 Bug 修复的功能验收
**日期：** 2026-05-03
**测试范围：** SerumBottlerBlock / SerumBottlerBlockEntity / BioIncubatorBlock / BioIncubatorBlockEntity

---

### 检查结果

| 检查项 | 结果 | 备注 |
|--------|------|------|
| 核心交互路径 | ✅ | 血清灌装机空手右键完整闭环（取输出→取输入→状态提示）；培养槽种子插入→生长→成熟产出闭环完整 |
| 注册表声明 | ✅ | 无新增注册表对象，复用已有注册，无遗漏 |
| BlockEntity 模式 | ✅ | tick 静态签名正确，NBT 读写对称（saveAdditional/loadAdditional），客户端同步通过 setChanged + sendBlockUpdated |
| 合成配方 | ✅ | 无配方变更，灌装机硬编码配方逻辑未改动 |
| NBT 边界 | ✅ | load 中 clampStat 保护（0-100）；getGene 返回默认值 1 兜底；progress 使用 Math.max(0, ...) |
| 区块卸载 | ✅ | saveAdditional/loadAdditional 完全对称；种子和输入槽在方块移除时通过 popResource 保底弹出 |
| 跨维度 | ✅ | 无维度相关逻辑，blockEntity 自身持有 level 引用，syncToClient 安全 |

---

### 修复 1 详细审查：血清灌装机取出逻辑

**新增代码：** `SerumBottlerBlockEntity.extractLastInput()` (行 163-173)
**调用位置：** `SerumBottlerBlock.use()` 空手右键分支 (行 73-89)

**逻辑链路：**
1. 空手右键 → `extractOutput()` 优先取出血清
2. 若输出为空 → `extractLastInput()` 从输入槽倒序取出最后一个非空槽位
3. 均为空 → 显示状态提示

**审查结论：** 正确。倒序扫描避免了正向遍历的索引管理复杂度；赋值后立即置 EMPTY，无悬空引用风险。

**加工中取出安全性：** 玩家在加工进行中取出材料时，progress/maxProgress 不会立即重置。下一 tick 的 `matchRecipe()` 返回 -1 后，tick 方法走到行 70 正确重置为 0。短暂的状态不一致（进度条残留）在 1 个 tick 内自愈，可接受。

---

### 修复 2 详细审查：培养槽生长系统

**新增代码：**
- `BioIncubatorBlockEntity.tryInsertSeed()` 调用 `ensureGeneData()` (行 132-134)
- 生长速率计算逻辑 (行 69-73)
- `getCropOutput()` 受 Yield 基因影响 (行 106-121)
- `getGrowthPercent()` / `getCurrentGrowthRate()` / `getEstimatedSecondsRemaining()` (行 187-210)

**生长速率公式验证：**
```
geneMultiplier = 0.5 + (geneSpeed / 10.0) * 1.5    // geneSpeed 1-10 → 0.65-2.0
envMultiplier  = (N + P + D) / 300.0                 // 最大 300/300 = 1.0
growthRate     = max(1, round(geneMultiplier * envMultiplier))
```
- 最低速率：geneSpeed=1, N+P+D=31 (刚过阈值) → 0.65 * 0.103 = 0.067 → round = 0 → max(1, 0) = 1 ✓
- 最高速率：geneSpeed=10, N+P+D=300 → 2.0 * 1.0 = 2.0 ✓
- 整数溢出：sum 最大 300，除以 300.0，无溢出风险 ✓

**ensureGeneData 时序正确性：** 调用发生在 `seed = stack` 之前，确保后续 tick 中 `GeneticSeedItem.getGene()` 必定能读到有效值。`getGene()` 本身也有 null/缺失兜底返回 1，双重保障。

**成熟产出循环：** 成熟后 growthProgress 重置为 0，消耗 N-5/P-5，种子不被移除 → 可持续自动收割。资源耗尽后低于阈值自动暂停，补充后恢复。逻辑自洽。

**ETA 计算验证：**
```
remaining = 200 - growthProgress
seconds   = ceil(remaining / rate / 20.0)
```
rate=1, remaining=200 → ceil(200/20) = 10s ✓
rate=2, remaining=1 → ceil(1/40) = 1s ✓
rate<=0 → 返回 -1（"资源不足"）✓

---

### 问题列表

1. **[轻微]** `BioIncubatorBlockEntity` 行 67：生长条件要求 `dataSignal > 0`，但行 65-66 要求 `nutrition > 10 && purity > 10`。三个阈值不对称，dataSignal 只需 > 0 即可。这不是 bug，但若设计意图是三者均需超过 RESOURCE_THRESHOLD(10)，则此处存在不一致。**当前行为：** 只要有任意 dataSignal 值（哪怕为 1）就可参与生长。**影响：** 极小，不影响功能正确性。

2. **[轻微]** `SerumBottlerBlockEntity.tick()` 行 46-53：当 maxProgress == 0 时启动配方，但 `matchRecipe()` 仅检查物品类型不检查数量（`hasIngredients` 用 `is()` 匹配）。若输入槽放了同类但不同 NBT 的物品，可能误匹配。**实际影响：** 当前所有配方输入物品均无 NBT 区分，不会触发此问题。

---

### 总体评价

两个 Bug 修复逻辑正确，NBT 读写对称，边界值处理到位（clamp、null check、Math.max 兜底），无 NPE 风险，无整数溢出风险，无状态不一致隐患。核心交互路径（灌装机取出、培养槽生长）完整闭环，可进入下一阶段。

# 蛋白质豆产出修复 v2 — 基础设施测试报告

## 测试结果：PASS

### 修复摘要

- BioIncubatorBlockEntity 中蛋白质豆种子(`PROTEIN_SOY_SEEDS`)的产出从错误的 `SYNAPTIC_NEURAL_BERRY` 修正为 `BIOCHEMICAL_SOLUTION`
- ModItems 中移除了错误新增的 `PROTEIN_SOY` 独立物品注册
- 同时附带了培养槽生长系统重写（基因倍率、环境倍率、产量计算等）

### 编译验证

`./gradlew compileJava` — **BUILD SUCCESSFUL**

### 检查结果

| 检查项 | 结果 | 备注 |
|--------|------|------|
| 客户端隔离 | PASS | tick 方法有 `level.isClientSide` 守卫（第 42 行）；`syncToClient()` 正确使用 `setChanged()` + `level.sendBlockUpdated()` |
| 数据同步 | PASS | nutrition/purity/dataSignal/growthProgress/seed 所有状态变更均通过 `syncToClient()` 同步；`getUpdatePacket()` 和 `getUpdateTag()` 正确实现 |
| Dupe 漏洞 | PASS | 培养槽不实现 Container 接口，产出通过 `Containers.dropItemStack` 直接弹出到世界；玩家交互通过 `tryInsertSeed`/`extractSeed` 单次操作，无并发风险 |
| Curios 兼容 | N/A | BioIncubatorBlockEntity 不涉及 Curios API |
| 自动化管道 | PASS | 培养槽不实现 `WorldlyContainer`，通过弹出物品输出，设计合理（漏斗/管道无需直接访问） |
| BlockEntity tick | PASS | tick 中无冗余对象分配；`getCropOutput()` 中 `new ItemStack` 仅在成熟时执行（200 tick 周期），可接受；无多余 NBT 序列化 |
| 内存泄漏 | PASS | 无静态集合无限增长；无未清理的监听器；seed 字段为实例变量，随 BlockEntity 生命周期管理 |

### 修复正确性验证

| 检查项 | 结果 | 详情 |
|--------|------|------|
| 产出映射 | PASS | `PROTEIN_SOY_SEEDS` → `BIOCHEMICAL_SOLUTION`（第 111-112 行），与战利品表 `protein_soy_crop.json` 一致 |
| 独立物品清理 | PASS | grep 确认无 `PROTEIN_SOY`（非 `_SEEDS`/`_CROP`）残留引用 |
| SYNAPTIC_NEURAL_BERRY | PASS | 该物品仍正确用于血清配方 S-02/S-03（SerumBottlerBlockEntity），不作为任何作物产出 |
| 物品注册一致性 | PASS | ModItems 中仅注册 `PROTEIN_SOY_SEEDS`，无多余的 `PROTEIN_SOY` 注册 |
| 三路种子产出对照 | PASS | FIBER_REED→PLANT_FIBER, PROTEIN_SOY→BIOCHEMICAL_SOLUTION, ALCOHOL_BLOOM→INDUSTRIAL_ETHANOL，均有对应战利品表和 datagen |

### 问题列表

1. **轻微** — `BioIncubatorBlockEntity.java` 的 diff 范围超出任务描述（蛋白质豆产出修复），包含完整的生长系统重写（基因倍率、环境倍率、`getGrowthPercent()`/`getCurrentGrowthRate()`/`getEstimatedSecondsRemaining()` 等新方法）。功能上无问题，但建议后续将功能新增与 bugfix 分开提交，便于 review 和回滚。

### 总体评价

修复正确且完整，compileJava 通过，无双端兼容性问题、无 dupe 漏洞、无性能隐患、无残留引用。建议合并。

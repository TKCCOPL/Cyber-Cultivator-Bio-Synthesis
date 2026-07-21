# 测试报告：综合修复 PR GameTest 补充与回归

## 测试结果：PASS

## 范围

为 `fix/multiplayer-stability` 分支的 10 个修复（Commits 1–10）补充 GameTest 回归测试，
并在测试过程中发现并修复 Commit #10 perf 优化引入的装瓶机首 tick 配方启动 bug。

## 检查结果

| 检查项 | 结果 | 备注 |
|--------|------|------|
| `./gradlew compileJava` | ✅ | 无错误，仅原有 null-safety 与 deprecation 警告 |
| `./gradlew runData` | ✅ | 无资源漂移，127→128 文件，written=0 |
| `./gradlew build` | ✅ | 通过 |
| `./gradlew runGameTestServer` | ✅ | 38/38 全部通过 |
| 资源漂移检查 | ✅ | `git status` 仅显示本次新增的 4 个 Java 文件 |

## 新增 GameTest 清单（共 16 个）

### `ModGameTests` 新增（13 个，覆盖修复 3/4/5/6/7/8/10）

1. `neuralOverloadVariantsAreRegistered` — 修复 4：四个效果（含旧版）全部注册，三个子类互为独立实例
2. `neuralOverloadS01AppliesWitherAndHunger` — 修复 4：S-01 副作用为凋零+饥饿，无 S-02/S-03 串扰
3. `neuralOverloadS02AppliesBlindnessAndHunger` — 修复 4：S-02 副作用为失明+饥饿
4. `neuralOverloadS03AppliesSlownessAndPoison` — 修复 4：S-03 副作用为缓慢+中毒
5. `s02SerumDoesNotApplyGlowingToNearbyEntities` — 修复 3：S-02 不再向附近实体施加 GLOWING（核心修复点）
6. `s02DetectionSyncPacketRoundTrip` — 修复 3：S-02 侦测同步包编解码往返，包括空列表与 null 防御
7. `gameplayConfigSnapshotDefaultsAndServerConfigMirror` — 修复 5：快照默认值与 Config 一致；fromServerConfig 镜像运行时值
8. `synapticSerumBaseAmplifierThresholdOverload` — 修复 5：新加 threshold 重载供客户端 Tooltip 使用
9. `serumRecipesExposeInheritFlags` — 修复 6：API 暴露 isInheritActivity/isInheritMutation 标志
10. `incubatorOutputRecipeAssemblePreservesTemplateNbt` — 修复 7：outputItem.copy() 保留模板 NBT，Potency 从种子正确传入
11. `cropMatureEventEmptyOutputSoftCancelsPreservingSeed` — 修复 7：setOutput(EMPTY) 软取消保留种子与资源（区别于 setCanceled 硬取消）
12. `geneSpliceCompleteAdvancementExistsWithCustomTrigger` — 修复 8：新进度加载且触发器已注册
13. `rootAdvancementRequiresRawSiliconCrystal` — 修复 8：根进度触发条件改为 raw_silicon_crystal
14. `bottlerCanPlaceItemRejectsNonRecipeStacks` — 修复 10：缓存版 canPlaceItem 行为与原版一致，越界/空栈正确拒绝
15. `bottlerFindRecipeCacheSkipsIdleTraversal` — 修复 10：lastRecipeQueryFailed 缓存不会造成 false-negative

### `CuriosGameTests` 新增（3 个，覆盖修复 10 饰品路径）

16. `bioPulseBeltInjectsMaterialsIntoNearbyIncubator` — 修复 10：单次扫描后 N/P/D 三项注入正常
17. `bioPulseBeltSkipsMissingMaterialChannelWithoutScanning` — 修复 10：缺料通道跳过，其他通道仍注入
18. `bioPulseBeltStopsScanningChannelAfterExhaustion` — 修复 10：库存耗尽后 hasXxx 翻 false，后续 incubator 跳过该通道

## 发现并修复的回归 bug

### Commit #10 perf 优化引入的装瓶机首 tick 配方启动延迟

**症状**：`machineMenuQuickMoveCommitsMachineTransactions` 测试失败，断言 `bottler.getMaxProgress() > 0` 失败。

**根因**：`SerumBottlerBlockEntity.tick()` 中的配方缓存失效检测块无条件将 `processingCancelled` 置为 `true`，
即便 `cachedRecipeCount` 初始值为 -1（首次 tick 触发无效化）且 `maxProgress == 0`（无进行中的加工可取消）。
后续 `if (!processingCancelled && maxProgress == 0)` 跳过 `findRecipe()`，导致装瓶机需要 2 tick 才能启动配方。

**修复**：仅在确有进行中的加工被打断（`wasProcessing = maxProgress > 0`）时才将 `processingCancelled` 置为 `true`，
首次 tick 的初始化路径允许 `findRecipe()` 立即执行。

**验证**：修复后 `./gradlew runGameTestServer` 全部 38 个测试通过。

## 测试基础结构改动

### `BioPulseBeltItem` 重构：提取 `performScan`

为使 GameTest 能直接验证 BioPulseBelt 的注入逻辑（不被 5 秒节流阻塞），
将 `tick(Player)` 的实际工作提取为 package-private 静态方法 `performScan(Player)`：

- `tick(player)`：保留 5 秒节流，调用 `performScan`
- `performScan(player)`：执行单次库存扫描 + 区域扫描 + 三通道注入

生产路径行为不变（仍然每 5 秒触发一次）；测试可直接调用 `performScan` 验证逻辑。

## 未覆盖项（需手工冒烟）

以下行为无法通过 GameTest 验证，需要双客户端专用服务器手工冒烟：

- **S-02 私有轮廓渲染**：饮用者客户端看到附近实体轮廓，其他客户端看不到（修复 3 核心目标）
- **客户端 Tooltip 显示**：材料描述、营养池提示、血清活性信息（修复 9）
- **JEI 页面展示**：使用 RecipeManager 真实配方数据（修复 6 视觉部分）
- **BioIncubatorScreen 缺料提示**：从 `ClientGameplayConfig.getSnapshot()` 读取配置（修复 5 客户端部分）
- **登录/换维度配置同步**：`GameplayConfigSyncPacket` 推送时机（修复 5 同步部分）

## 日志路径

- GameTest 输出：`run/logs/latest.log`（运行时）
- 关键行：`All 38 required tests passed :)`

## 修复提交点

本测试报告对应的提交：`test: 补充 GameTest 与生成资源`（Commit #11）

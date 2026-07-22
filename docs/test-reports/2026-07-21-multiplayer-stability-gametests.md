# 测试报告：综合修复 PR GameTest 补充与回归

## 测试结果：PASS

## 范围

为 `fix/multiplayer-stability` 分支的 10 个修复（Commits 1–10）补充 GameTest 回归测试，
并在测试过程中发现并修复 Commit #10 perf 优化引入的装瓶机首 tick 配方启动 bug，
以及 KubeJS 烟雾脚本与测试用例的输入物冲突。

## 检查结果

| 检查项 | 结果 | 备注 |
|--------|------|------|
| `./gradlew compileJava` | ✅ | 无错误，仅原有 null-safety 与 deprecation 警告 |
| `./gradlew runData` | ✅ | 无资源漂移，total files 127→128，written=0 |
| `./gradlew build` | ✅ | 通过 |
| `./gradlew runGameTestServer`（默认含 Curios+JEI） | ✅ | 38/38 全部通过 |
| `./gradlew -I .github/gradle/exclude-jei-runtime.init.gradle runGameTestServer`（仅 Curios） | ✅ | 38/38 全部通过 |
| `./gradlew -I .github/gradle/exclude-optional-runtime.init.gradle runGameTestServer`（无可选依赖） | ✅ | 38/38 全部通过 |
| `./gradlew -I .github/gradle/exclude-non-kubejs-runtime.init.gradle -PenableKubeJSRuntime=true runGameTestServer`（KubeJS build.16） | ✅ | 38/38 全部通过 |
| `./gradlew -I .github/gradle/exclude-non-kubejs-runtime.init.gradle -PenableKubeJSRuntime=true -Pkubejs_version=2001.6.5-build.26 runGameTestServer`（KubeJS build.26） | ✅ | 38/38 全部通过 |
| 资源漂移检查 | ✅ | `runData` 后生成资源无差异 |

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

### KubeJS 烟雾脚本注册 dirt 配方与测试用例冲突

**症状**：KubeJS 最低版 GameTest 烟雾测试中两个新增测试失败：
- `bottlercanplaceitemrejectsnonrecipestacks` — `Bottler must reject dirt`
- `bottlerfindrecipecacheskipsidletraversal` — `Berry synthesis inputs must start processing despite the previous failed-query cache`

**根因**：`src/kubejsTest/resources/kubejs/server_scripts/cybercultivator_smoke.js` 注册了两条以 `minecraft:dirt`
为原料的 `cybercultivator:serum_bottling` 配方（priority 100 与 0）。
测试用例将 `dirt` 当作"必然无效的输入"，但 KubeJS 烟雾脚本将其变成"有效配方原料"，
导致 `canPlaceItem(0, dirt)` 返回 true、`findRecipe(dirt)` 启动加工。

**修复**：将两个测试中的"无效输入"从 `Items.DIRT` 改为 `Items.BEDROCK`
（原版方块物品，无内置合成/熔炼配方，KubeJS 烟雾脚本也不会将其注册为血清原料）。
修复同时保持无 KubeJS profile 下测试不变（38/38 通过）。

**验证**：
- `./gradlew runGameTestServer`（默认含 Curios+JEI） — 38/38 通过
- `./gradlew -I .github/gradle/exclude-jei-runtime.init.gradle runGameTestServer`（仅 Curios） — 38/38 通过
- `./gradlew -I .github/gradle/exclude-optional-runtime.init.gradle runGameTestServer`（无可选依赖） — 38/38 通过
- `./gradlew -I .github/gradle/exclude-non-kubejs-runtime.init.gradle -PenableKubeJSRuntime=true runGameTestServer`（KubeJS build.16） — 38/38 通过
- `./gradlew -I .github/gradle/exclude-non-kubejs-runtime.init.gradle -PenableKubeJSRuntime=true -Pkubejs_version=2001.6.5-build.26 runGameTestServer`（KubeJS build.26） — 38/38 通过

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

### S-02 双客户端手工验证步骤（修复 3 核心目标，无法自动化）

1. 启动专用服务器（`./gradlew runServer`），两个开发客户端连接。
2. 玩家 A 饮用 S-02 视觉强化血清（合成或创造取出 `synaptic_serum_s02`，Activity >= 5）。
3. 在 A 附近（半径 16 格内）放置一头被动实体（牛/羊/猪）或让玩家 B 站到 A 附近。
4. **验收点 1**：玩家 A 屏幕上看到附近实体/玩家 B 的发光轮廓。
5. **验收点 2**：玩家 B 屏幕上 **看不到** 任何发光轮廓（包括 B 自己、A 附近的其他实体）。
6. **验收点 3**：通过 `/effect give @e[...] minecraft:glowing` 验证实体本身 **没有** GLOWING MobEffect（可用 `/effect clear` 后再检查 `/data get entity @e[limit=1] ActiveEffects`）。
7. 等 S-02 持续时间结束（或用 `/effect clear`），验证副作用 `NeuralOverloadEffectS02` 触发失明+饥饿（修复 4 配套验证）。
8. 验证不同 Activity 值下发光范围 16–48 格线性变化（修复 3 平衡点）。

通过标准：上述 8 项验收点全部正确，才视为 S-02 私有轮廓修复完成。

## 日志路径

- GameTest 输出：`run/logs/latest.log`（无 KubeJS profile）
- KubeJS 烟雾 profile 输出：`build/kubejs-smoke/logs/latest.log`
- 关键行：`All 38 required tests passed :)`

## 修复提交点

- 本测试报告对应的提交：`test: 补充 GameTest 与生成资源`（Commit #11）
- KubeJS 烟雾 profile 适配修复：`fix(test): 修复 GameTest 与 KubeJS 烟雾脚本的 dirt 冲突`（Commit #12，紧跟 Commit #11 之后）

## 审查后追加修复与验证（2026-07-22）

本轮分支审查补充修复了以下稳定性问题：

- 网络协议版本改为严格匹配，避免不同数据包布局的客户端与服务端误连。
- S-02 同步包复制实体 ID 数组，并在客户端登录/退出时清理目标集合和服务端配置快照，避免跨服务器状态泄漏。
- 装瓶机配方缓存改为按配方 ID 和对象变化失效，覆盖同数量 datapack/KubeJS 重载；首次世界加载观察时保留持久化中的活动配方与进度。

追加验证结果：`compileJava`、`build`、默认/仅 Curios/无可选依赖及 KubeJS build.16/build.26 的 GameTest 均通过（38/38）；客户端启动至资源加载完成，未发现模组异常。`runData` 已验证无生成资源漂移。S-02 双客户端轮廓、Tooltip、JEI 和界面视觉验收仍待手工执行。

版本准备：`gradle.properties`、`README.md` 与 `README_EN.md` 已统一更新为 `1.1.7`。当前按要求暂不创建版本 PR，保留本分支等待后续发布确认。

## 最终验证汇总（PR 合并门槛）

### 自动化门禁（全部 ✅）

| 门禁 | 命令 | 结果 |
|------|------|------|
| 编译 | `./gradlew compileJava` | ✅ 通过 |
| 数据生成 | `./gradlew runData` | ✅ 通过，written=0 无漂移 |
| 构建 | `./gradlew build` | ✅ 通过 |
| GameTest（默认 Curios+JEI） | `./gradlew runGameTestServer` | ✅ 38/38 |
| GameTest（仅 Curios） | `./gradlew -I .github/gradle/exclude-jei-runtime.init.gradle runGameTestServer` | ✅ 38/38 |
| GameTest（无可选依赖） | `./gradlew -I .github/gradle/exclude-optional-runtime.init.gradle runGameTestServer` | ✅ 38/38 |
| GameTest（KubeJS build.16） | `./gradlew -I .github/gradle/exclude-non-kubejs-runtime.init.gradle -PenableKubeJSRuntime=true runGameTestServer` | ✅ 38/38 |
| GameTest（KubeJS build.26） | `./gradlew -I .github/gradle/exclude-non-kubejs-runtime.init.gradle -PenableKubeJSRuntime=true -Pkubejs_version=2001.6.5-build.26 runGameTestServer` | ✅ 38/38 |

### 手工冒烟（未自动化）

| 项 | 状态 | 备注 |
|----|------|------|
| S-02 私有轮廓双客户端验证（8 项验收点） | ⬜ 待执行 | 见上方"S-02 双客户端手工验证步骤" |
| 客户端 Tooltip 显示（材料/营养池/血清活性） | ⬜ 待执行 | 视觉/渲染 |
| JEI 页面（RecipeManager 真实数据） | ⬜ 待执行 | 视觉/渲染 |
| BioIncubatorScreen 缺料提示读取客户端快照 | ⬜ 待执行 | 客户端配置链路 |
| 登录/换维度配置同步时机 | ⬜ 待执行 | 网络链路 |

### PR 合并前的剩余步骤

1. 在专用服务器上执行 S-02 双客户端手工验证（8 项验收点全部通过）。
2. 视情况补做客户端 Tooltip / JEI / Screen / 网络同步的视觉冒烟（不阻塞 CI）。
3. 通过后推送 `fix/multiplayer-stability` 分支并创建 PR（按 AGENTS.md 的版本发布流程要求）。
4. PR 正文中只保留"## 更新"和"## 修复"两节单行列表，不写版本号提升、文档同步或测试过程。

## 提交历史

```
8ee8fb9 fix(test): 修复 GameTest 与 KubeJS 烟雾脚本的 dirt 冲突
23baef5 test: 补充 GameTest 与生成资源
4700ecd perf: 优化装瓶机漏斗查询与饰品扫描路径
9e57a86 docs: 明确蛋白质豆启动链与培养槽共享池
1f635f1 fix(advancement): 修复进度条件与触发器
38c816f fix(incubator): 修复输出 NBT 丢失与成熟事件空输出吞资源
18c4055 fix(jei): 使用 RecipeManager 真实配方数据
d937e97 fix(config): 迁移服务端配置并完善同步时机
3dbba0f fix(effects): 修复神经过载来源丢失和串线
46702a7 fix(serum): 实现 S-02 私有轮廓
88a67fe feat(network): 添加网络通道和配置同步基础
28769f5 fix(ci): 修复 CI 和 Datagen 门禁
```

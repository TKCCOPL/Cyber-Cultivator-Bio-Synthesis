# PR #13 补充修复测试报告（2026-07-25）

## 范围

在 `fix/multiplayer-stability` 分支上追加 8 个提交，覆盖血清效果、网络同步、配置广播、
进度触发、矿石标签、灌装机库存与 CI 工作流：

- 取消 `MobEffectEvent.Remove` 后不再误触发神经过载
- 延迟 TickTask 内反向确认原效果已消失，避免升级/重新施加时误触
- 玩家死亡重生时统一清理 S-02 私有轮廓目标
- 配置热重载广播调度到服务器主线程执行
- 潜行右键领取拼接子代与 GUI 领取统一触发进度
- 深板岩硅/稀土矿石补全 `forge:ores/<type>` 标签
- 灌装机 `canPlaceItem` 拒绝同类原料跨槽位重复占位
- CI `build` job 支持 `full-ci` 标签触发完整验证

## 验证结果

| 命令 | 结果 | 说明 |
|------|------|------|
| `./gradlew compileJava` | PASS | 4 条既有弃用警告，无新增错误 |
| `./gradlew runData` | PASS | 4 个 Forge 标签 JSON 各 +1 行深板岩条目，无意外 diff |
| `./gradlew -I .github/gradle/exclude-optional-runtime.init.gradle -PenablePatchouliRuntime=false runGameTestServer` | PASS | 68/68 required tests passed（原 63 + 新增 5） |

## 新增回归测试

| 测试名 | 验证内容 |
|--------|----------|
| `cancelledSerumRemovalDoesNotScheduleOverload` | HIGH 优先级取消 `Remove` 事件后原效果保留且无过载 |
| `readdedSerumEffectSkipsDelayedOverload` | 效果移除后重新施加时延迟 TickTask 跳过过载 |
| `modTriggersTriggerForOutputHandlesEdgeCases` | null player / 空堆 / 无 NBT / Generation=0 / Generation>0 全部安全 |
| `deepslateOreForgeTagsIncludeBothVariants` | 4 种矿石（含深板岩）均在 `forge:ores/<type>` 和 `forge:ores` 父标签中 |
| `bottlerCanPlaceItemRejectsDuplicateAcrossSlots` | 同种物品跨槽被拒、同槽堆叠允许、IItemHandler 漏斗路径同步拒绝 |

## 提交清单

```
2a59c83 fix(effects): 取消药效移除后不再误触发神经过载
80e9eb5 fix(network): 重生与换维度统一清理 S-02 轮廓目标
d490988 fix(config): 配置重载广播调度到服务器主线程执行
c1ea75f fix(advancement): 统一基因拼接子代领取的进度触发
643ad02 fix(tags): 深板岩矿石补全 Forge 矿石标签
fe824fc fix(bottler): 拒绝同类原料跨槽位重复占位
920a05c ci: 大型发布 PR 可通过 full-ci 标签触发完整验证
79bc4c2 test: 补充血清过载、矿石标签与灌装机去重回归测试
```

## 未验证

- S-02 死亡重生后轮廓清空需双客户端连接专用服务器手动验收
- S-02 换维度后轮廓清空需双客户端手动验收
- `full-ci` 标签触发完整 CI 需在 GitHub PR 上实际验证

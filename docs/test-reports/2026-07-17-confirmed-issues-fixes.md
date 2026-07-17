# 2026-07-17 已确认问题修复验证报告

## 结论

本轮已完成已确认的配方、培养槽成熟取消语义、血清 Activity/事件、旧 API 兼容、null 安全、快照防御、effect registry ID 和机器消息本地化修复。Forge GameTest Server 实际执行 5 项测试，全部通过，不再是 0 项测试。

KubeJS 依赖集成和 15 张占位贴图的正式美术替换未在本轮实施，仍为延期事项。

## 自动化覆盖

| GameTest | 覆盖内容 | 结果 |
|----------|----------|------|
| `activityBoundsAndEffectIds` | Activity 缺失/负数/15/超大 NBT、null 输入、三种血清 effect registry ID | PASS |
| `legacyIdsAndSnapshots` | 四个旧配方编号、自定义回退 -1、DTO/事件深拷贝、API null 安全、不可变配方快照 | PASS |
| `serumEventOverrides` | Activity-only 重算时长/首次等级、显式 duration/amplifier 覆盖、setter 边界、血清快照 | PASS |
| `bottlerCraftingRecipeUsesRareEarthCenter` | 顶部红石、中心稀土，旧双红石布局不匹配 | PASS |
| `cancelledCropMaturityStartsFreshCycle` | 成熟事件取消后保留种子、进度归零、不扣成熟资源、不产出 | PASS |

GameTest 使用 `@GameTestHolder` 注册，结构源为 `src/main/snbt/data/cybercultivator/structures/empty.snbt`，由 datagen 生成 `src/generated/resources/data/cybercultivator/structures/empty.nbt`。

## 命令验收

| 命令/检查 | 结果 |
|-----------|------|
| `./gradlew runData --no-daemon` | PASS；生成配方只将中心红石改为稀土，并生成 87-byte 最小结构 NBT |
| `./gradlew compileJava --no-daemon` | PASS |
| `./gradlew build --no-daemon` | PASS；完成 reobf JAR |
| `./gradlew runGameTestServer --no-daemon` | PASS；`All 5 required tests passed` |
| 中英文 JSON 键集合 | PASS；两侧均为 185 个键，集合完全一致 |
| 四类机器交互消息扫描 | PASS；不再使用 `Component.literal()` 或中文硬编码 |
| `git diff --check` | PASS |

## 文档与历史状态

本轮扫描了 `docs/` 下全部 44 个现有 Markdown 文件，并对照 Git 历史更新了受影响的使用指南、开发计划、第三轮审查计数、贴图规范、Config/KubeJS 计划与历史测试报告。T13 报告标注提交 `8031502` 已修复；T22 报告标注已被 Synergy 重设计取代，并记录 `19cf604` 的后续修复。

## 未自动化项

本轮未启动图形客户端，因此中/英文 action-bar 的实机视觉切换、生存模式手工合成和手工触发成熟取消仍列为可选的交互烟雾测试；对应核心逻辑已由上述 GameTest 覆盖。

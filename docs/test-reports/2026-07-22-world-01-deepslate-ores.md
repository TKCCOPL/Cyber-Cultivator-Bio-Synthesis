# WORLD-01 深板岩矿石变种验证记录

## 结果：PASS（自动化门禁）

## 实现范围

- 新增 `deepslate_silicon_ore` 与 `deepslate_rare_earth_ore`，普通硅矿和稀土矿的注册 ID 保持不变。
- 两个 Configured Feature 均同时匹配 `minecraft:stone_ore_replaceables` 和 `minecraft:deepslate_ore_replaceables`。
- 补齐 BlockItem、创造栏、BlockState/Block Model、纹理、矿石标签、战利品、语言和 JEI 信息。
- 深板岩贴图使用 Minecraft 1.20.1 客户端 `deepslate.png` 底材，并叠加现有矿脉色板。

## 自动化检查

| 检查项 | 结果 | 备注 |
|--------|------|------|
| `./gradlew compileJava` | ✅ | 通过，仅保留既有弃用警告 |
| `./gradlew runData` | ✅ | 生成 2 套 blockstate/model/item model/loot，语言与标签同步 |
| `./gradlew build` | ✅ | 通过 |
| 默认 GameTest | ✅ | 39/39 |
| 仅 Curios GameTest | ✅ | 39/39 |
| 无可选依赖 GameTest | ✅ | 39/39 |
| KubeJS build.16 GameTest | ✅ | 39/39 |
| KubeJS build.26 GameTest | ✅ | 39/39 |
| 客户端启动烟测 | ✅ | 注册、方块图集和资源加载完成；进程由 60 秒超时结束，无模组错误 |
| `git diff --check` | ✅ | 通过 |

## 未覆盖的手工验收

- 新生成区块中石头层普通矿石与负高度深板岩矿石的实际分布尚未在客户端世界中逐层采样。
- 时运数量、精准采集保留普通/深板岩方块的交互验收尚未执行；生成的矿石 loot table 已使用标准 `createOreDrop`，包含 Silk Touch 与 Fortune 分支。

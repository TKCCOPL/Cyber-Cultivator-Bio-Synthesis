# 仓库审计与修复报告（2026-07-16）

## 结论

审计覆盖当前 `main`、近期修复提交、`README.md`、`CLAUDE.md`、设计文档、历史测试报告、Java 源码、配方/资源 JSON 与运行日志。已修复所有本轮确认的严重问题；未发现仍会阻止正常启动或破坏核心玩法闭环的已确认重大缺陷。

## 已修复问题

| 等级 | 问题 | 修复 |
|---|---|---|
| 严重 | 遗传种子在注册阶段读取尚未加载的配置，三个默认种子的基因可能全部固化为 `0`，随后退化为 `1` | 构造默认值改用稳定的领域边界，并在读取时迁移缺失、零值和越界旧 NBT |
| 严重 | 血清灌装机可把不同物品/NBT 的既有输出直接 `grow()`，造成 S01/S02 等输出互相转换、吞料或超堆叠 | 开始和完成加工时均校验配方、物品、NBT、容量与输入；事件修改后的输出再次校验 |
| 中等 | 培养槽成熟清空种子后可能永不再同步客户端 | 成熟状态强制同步 |
| 中等 | Synergy 每代重置，设计声明的累计上限 10 实际不可达 | 子代继承双亲较高值后再累计突变 |
| 中等 | 灌装机客户端只有待恢复的配方 ID，HUD 却只读服务端配方对象 | HUD/API 可读取已同步的待恢复 ID，并安全处理损坏 ID |
| 中等 | JEI 突变概率用三项差值之和，核心逻辑用最大差值 | JEI 公式改为与核心一致 |
| 中等 | 自定义配方可提供非法输入数量、加工时间、输出数量或可变数组引用 | 序列化边界校验、最小加工时间、最大堆叠限制及防御性拷贝 |
| 中等 | 事件监听器可写入空输出、负持续时间/等级、非法协同值或代数并触发崩溃/坏 NBT | 在事件边界归一化空值并限制数值范围 |
| 轻微 | 冷凝器忽略 `purityInjectAmount`；腰带配置允许无效的 6–8 范围；1.20.1 战利品表含无效 `short_grass` ID | 统一使用配置、把扫描上限声明为 5、移除无效表项 |

修复依据为 Forge 官方对[注册生命周期](https://docs.minecraftforge.net/en/1.20.x/concepts/registries/)和[配置加载](https://docs.minecraftforge.net/en/1.20.1/misc/config/)的说明：注册发生在配置可安全读取之前，因此注册对象构造不能依赖运行时配置值。

## 验证结果

| 命令/检查 | 结果 |
|---|---|
| `./gradlew compileJava` | PASS；仅 2 个现有 Forge 弃用警告 |
| `./gradlew runData` | PASS；81 个缓存文件，生成器正常 |
| `./gradlew build` | PASS；已生成重混淆 JAR，测试任务为 `NO-SOURCE` |
| `./gradlew runGameTestServer` | PASS；9 个配方加载，服务器正常启动和关闭 |
| `./gradlew dependencies --configuration runtimeClasspath` | PASS；确认包含 Curios Forge `5.3.5+1.20.1` 与 JEI Forge `15.2.0.27` |
| 带 Curios、JEI 的 GameTest 服务器 | PASS；`Curios loaded: true`，加载 10 个饰品槽、1 类饰品实体，JEI/Curios 服务端配置正常加载 |
| 无 Curios、无 JEI 的 GameTest 服务器 | PASS；`Curios loaded: false`，可选依赖缺失不崩溃 |
| `./gradlew runClient` | PASS；OpenGL、资源、Curios、JEI 和本模组加载到主菜单，无 ERROR/FATAL；随后人工中止 |
| `./gradlew runClient --args=--quickPlaySingleplayer\ 新的世界` | PASS；玩家成功进入集成世界；JEI 注册并载入 `serum_bottling`、`gene_splicing`、`incubator_output`，运行时在 341.2 ms 内构建；Curios 槽位和玩家实体同步无报错；验证后人工中止 |
| JSON 与中英翻译键检查 | PASS；无语法错误，157 个键一致 |

## 剩余风险

项目没有单元测试或已注册的 Forge GameTest；当前 `runGameTestServer` 实际运行 0 个玩法测试。官方 [GameTest 文档](https://docs.minecraftforge.net/en/1.20.x/misc/gametest/)建议为确定性机制增加测试。后续应优先覆盖灌装机混合输出、旧种子 NBT 迁移、Synergy 跨代继承和培养槽成熟同步。客户端已进入集成世界并验证 Curios/JEI 初始化，但尚未自动执行饰品装备效果、JEI 页面视觉内容、HUD 与完整生产链操作，这些交互仍需人工验收或新增 GameTest。

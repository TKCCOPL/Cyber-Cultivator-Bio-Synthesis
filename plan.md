## Plan: Cyber-Cultivator Bio-Synthesis (MVP → 扩展)

本计划基于 README 企划，并结合当前仓库已存在的 Forge 1.20.1 (Forge 47.4.18) + Curios 5.14.1 代码骨架。
目标是先落地“可玩闭环”的 MVP：基础资源 → 核心设施（最小可交互）→ 种子 NBT 基因 → 基因拼接 → 1 种血清 + 1 种副作用 → Curios 信息显示。

**Steps（按 README 的阶段性开发路线图细化）**

### 🏁 Phase 1：基础框架搭建
1. 注册硅晶矿、稀土矿及基础材料 Item（仓库已基本具备：`ModBlocks`/`ModItems`/`ModLootTableProvider`）。
2. 导入 3 种基础作物的 16x16 贴图与模型资源（Fiber Reed / Protein Soy / Alcohol Bloom）。
   - *依赖：先确定作物是“原版作物(CropBlock)”还是“培养槽内生长专用体系”。推荐 MVP 先用 CropBlock 跑通素材与掉落。*
3. 配置 Curios API 环境：
   - Gradle：Curios 运行时依赖（不仅是 compileOnly api）。
   - mods.toml：声明 curios 依赖（缺失时明确报错）。
   - 光谱单片镜注册（已存在）+ 佩戴判定/渲染入口（MVP 先做“装备后解锁信息显示”，渲染细节后置）。

**Phase 1 验收**
- `./gradlew build` 可通过；`./gradlew runClient` 可进入游戏。
- 关键物品/方块能在创造栏看到；矿物掉落与基础物品模型不缺失。
- Curios 安装后，单片镜可被识别为可装备物品（至少能通过 equipped check 判定）。

### ⚙️ Phase 2：核心系统实现
1. 生物培养槽（Bio-Incubator）：Block + BlockEntity（README 里叫 TileEntity，1.20.1 对应 BlockEntity）。
   - 维护并持久化三数值：Nutrition / Purity / Data Signal。
   - 交互：注入纯净水与营养液（MVP 可先用物品占位），并提供最小化“种子放入/取出”。
   - Tick：按规则消耗/衰减并驱动生长进度。
2. 核心种子 Item 类：引入 NBT 基因标签（Speed/Yield/Potency，范围 1–10）。
   - 生成/复制规则要明确：初始种子默认基因值、以及输出种子如何继承。
3. 基因拼接机（Gene Splicer）：遗传算法与输出。
   - 输入两颗种子 → 输出新种子。
   - 公式：新值 = (父 + 母)/2 + 随机(-1..+2)，并 clamp 到 1..10。

**Phase 2 验收**
- 培养槽可放置与交互，三数值能在服务端正确变化并保存到 NBT。
- 种子携带 NBT 基因，重新登录/丢弃拾取后仍存在。
- 拼接机能稳定产出带基因的新种子，公式与 clamp 正确。

### 🧪 Phase 3：强化与数值平衡
1. 注册突触神经莓与血清 Item（至少先落地 1 个等级链路，后续扩等级）。
2. 自定义效果：
   - “突触超频”强化（挖掘/攻速等）。
   - “神经过载”副作用（药效结束后触发：减速/饥饿等）。
3. 完善配方与战利品：机器配方、材料配方、掉落表（当前已有基础 loot provider，后续补齐）。

**Phase 3 验收**
- 血清效果可施加与结束；结束后副作用可靠触发。
- 配方与掉落齐备，能在生存模式跑通“资源→机器→血清”的闭环。


**Relevant files**
- `README.md` — 企划与 Roadmap 源
- `build.gradle` — Curios 依赖（compileOnly + runtimeOnly）
- `src/main/resources/META-INF/mods.toml` — curios 依赖声明
- `src/main/java/com/TKCCOPL/cybercultivator.java` — 注册入口与事件总线
- `src/main/java/com/TKCCOPL/init/ModItems.java` — 种子/血清/Curios 物品注册
- `src/main/java/com/TKCCOPL/init/ModBlocks.java` — 培养槽/拼接机等方块注册
- `src/main/java/com/TKCCOPL/datagen/*` — 语言/掉落/标签等（如走 datagen）
- `src/main/resources/assets/cybercultivator/**` — 贴图/模型/语言文件（如走手写资源）
- `src/main/resources/data/cybercultivator/**` — 战利品/配方/世界生成（如走手写数据包）

**Verification**
1. `./gradlew build`
2. `./gradlew runClient`：创造模式验证注册与交互
3. 如启用 datagen：`./gradlew runData`，并确保 generated 资源可被客户端加载

**Phase Gate（强制）**
每完成一个阶段，必须执行一次完整测试验证；未通过则禁止进入下一阶段。

1. 代码与资源静态验证：
   - `./gradlew compileJava`
   - 如本阶段涉及数据生成：`./gradlew runData`
2. 构建验证：
   - `./gradlew build`
3. 运行时验证（手工冒烟）：
   - `./gradlew runClient`
   - 至少完成：进入主界面 -> 创建/进入世界 -> 打开创造栏检查本阶段新增内容 -> 基础交互（放置/使用/提示）
4. 结果记录（必须）：
   - 记录通过/失败、失败日志路径、修复提交点。
   - 若失败，先修复并重跑本 Gate，直到通过再继续。
5. 文档同步（必须）：
   - 在 `README.md` 的 Roadmap 中勾选本阶段已完成项。
   - 同步更新本阶段相关说明（如算法、交互规则、前置依赖、验证步骤）。
   - 若代码行为与文档描述不一致，优先修正文档并在结果记录中注明变更点。


**Gate Through Criteria（通过标准）**
- 无阻断级错误：不允许崩溃、注册表加载失败、关键资源缺失导致功能不可用。
- 本阶段新增功能可复现：至少 1 条可重复操作路径从输入到输出成功闭环。
- 构建稳定：`compileJava`、`build` 必须通过；涉及 datagen 的阶段 `runData` 必须通过。

**Decisions**
- 推荐按 README 的三阶段走，但每阶段都要有“可运行 + 可验收”的最小闭环。
- MVP 允许用占位物品/交互先把系统串起来；后续再补流体/能量/多方块与美术。
- 每完成一个阶段都要做一次完整的测试验证，确保基础功能稳定后再迭代复杂度。
- 平衡调整（如突变概率、数值范围）先走离线模拟（如 Python 脚本），再落地游戏实测，避免频繁改代码调参。
- 每完成一个阶段后，必须在 `README.md` 的 Roadmap 中打勾，并更新相关文档说明（如新增基因算法细节时同步更新算法描述）。


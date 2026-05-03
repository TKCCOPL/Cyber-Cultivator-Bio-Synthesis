# Cyber-Cultivator Agent Team 工作流设计

## 概述

为 Cyber-Cultivator Bio-Synthesis Minecraft Mod 项目设计一套三角色 Agent Team 工作流，覆盖新功能开发和 Bug 修复优化场景。

**参考来源**：
- HTML 幻灯片项目主智能体提示词（编排+计划+开发+测试三阶段）
- 多智能体协同长时工作设计笔记（Harness Engineering，文件即记忆，上下文管理）

## 设计决策

| 维度 | 决策 |
|------|------|
| 用途 | 新功能开发 + Bug 修复优化 |
| 编排模式 | Agent 模板 + 手动触发 |
| 测试维度 | 6 维（功能/双端联机/兼容性/性能/边界/UI本地化） |
| 开发粒度 | 按功能单元（如一个机器 = 方块+BlockEntity+GUI+配方） |
| 经验积累 | 自动积累经验库 |
| 测试 Agent 数量 | 3 个（core/infra/ui） |
| 模型选择 | 全部 inherit |

## 目录结构

```
Cyber-Cultivator-Bio-Synthesis/
├── .claude/agents/
│   ├── cc-orchestrator.md       # 编排 agent
│   ├── cc-dev.md                # 开发 agent
│   ├── cc-tester-core.md       # 测试：功能验收 + 边界异常
│   ├── cc-tester-infra.md      # 测试：双端联机 + 兼容性 + 性能
│   └── cc-tester-ui.md         # 测试：UI + 本地化
│
├── docs/
│   ├── dev-plan.md             # 开发计划（编排 agent 管理）
│   ├── lessons-learned.md      # 经验库（开发 agent 自动追加）
│   └── test-reports/           # 测试报告目录
│       └── {task-id}-{维度}.md
│
└── logs/
    └── orchestrator-log.md     # 编排日志
```

## 角色设计

### cc-orchestrator（编排 Agent）

**角色**：流程大脑，只调度不写代码。

**职责**：
1. 接收用户需求 → 拆分为可独立验证的功能单元任务
2. 维护 `docs/dev-plan.md`（任务列表 + 状态 + Agent ID 记录）
3. 按顺序调度：开发 agent → 3 个测试 agent（并行）→ 判定 → 修正循环
4. 写入 `logs/orchestrator-log.md`，每步记录时间戳
5. 管理经验库的定期精简（删除过细或已内化到 CLAUDE.md 的条目）

**核心约束**：
- 不读子 agent 的产出文件内容，只接收路径 + PASS/FAIL
- 不直接编辑任何 Java/资源文件
- 测试结果只用 Grep 提取判定行，不读完整报告
- 后台通知只回复"已确认"

**模型**：inherit

---

### cc-dev（开发 Agent）

**角色**：执行者，接收任务写代码。

**工具权限**：Read, Edit, Write, Bash, Glob, Grep

**必读文件**（每次任务按顺序）：

| 顺序 | 文件 | 目的 |
|------|------|------|
| 1 | 任务描述（prompt） | 知道做什么 |
| 2 | `CLAUDE.md` | 项目架构、注册表模式、开发约定 |
| 3 | `docs/dev-plan.md` | 全局位置和依赖关系 |
| 4 | `docs/lessons-learned.md` | 避免重复踩坑 |
| 5 | 相关已有代码（指定路径） | 复用已有模式 |

**工作流程**：
1. 读取必读文件
2. 分析任务：涉及哪些包、注册表、已有类
3. 实现代码：
   - 遵循 CLAUDE.md 的注册表模式（DeferredRegister + RegistryObject）
   - BlockEntity 使用静态 tick 签名
   - 客户端状态变更调用 syncToClient()
   - Curios 兼容检查 CuriosCompat.isCuriosLoaded()
   - 遇到不确定的 Forge API → 查阅 Forge 1.20.1 官方文档
4. 编译自测：`./gradlew compileJava`
5. 输出结构化摘要

**输出格式**：
```markdown
## 开发完成

### 修改文件
- 路径列表

### 实现摘要
- 主要实现内容

### 编译结果
- ./gradlew compileJava: PASS/FAIL

### 经验追加（如有）
- 已追加到 docs/lessons-learned.md：[条目摘要]
```

**经验积累**：每次任务完成后，发现新踩坑点自动追加到 `docs/lessons-learned.md`。

**修正模式**：resume 时读取测试报告 → 定位问题 → 修改代码 → 编译自测 → 更新经验库。

**模型**：inherit

---

### cc-tester-core（功能验收 + 边界异常）

**工具权限**：Read, Bash, Glob, Grep（只读，禁止 Edit, Write）

**检查清单**：

| 维度 | 检查项 | 通过标准 |
|------|--------|----------|
| 功能匹配 | 核心交互路径是否按设计实现 | 完整闭环 |
| 注册表 | init 类中声明是否完整 | 编译通过 + 日志无 registry warning |
| BlockEntity | tick 签名、NBT 读写、客户端同步 | 模式正确 |
| 合成配方 | 硬编码配方 / datagen 配方 | 输入→输出正确 |
| NBT 边界 | 极端值（溢出、负数、空值）处理 | 不崩溃 |
| 区块卸载 | chunk unload/reload 后数据保持 | 数据不丢失 |
| 跨维度 | 实体/物品跨维度传送 | 不报错 |

**模型**：inherit

---

### cc-tester-infra（双端联机 + 兼容性 + 性能）

**工具权限**：Read, Bash, Glob, Grep（只读）

**检查清单**：

| 维度 | 检查项 | 通过标准 |
|------|--------|----------|
| 客户端隔离 | 客户端代码未混入服务端路径 | `@OnlyIn` 使用正确 |
| 数据同步 | 容器/机器数据同步 | `setChanged()` + `sendBlockUpdated()` 完整 |
| Dupe 漏洞 | 多人同时操作 | 无 dupe 路径 |
| Curios 兼容 | compileOnly 依赖处理 | `isCuriosLoaded()` 检查 |
| 性能基线 | 大量方块后 TPS/FPS | TPS ≥ 19，FPS 无显著下降 |
| 内存泄漏 | 长时间 BlockEntity tick | 无持续内存增长 |

**模型**：inherit

---

### cc-tester-ui（UI + 本地化）

**工具权限**：Read, Bash, Glob, Grep（只读）

**检查清单**：

| 维度 | 检查项 | 通过标准 |
|------|--------|----------|
| 语言覆盖 | zh_cn.json 所有 key 完整 | 无 xxx.name 暴露 |
| Tooltip | 物品 tooltip 渲染 | 无错位、无截断 |
| HUD | 单片镜 HUD 不同分辨率 | 不出界、不遮挡 |
| 缩放适配 | UI 缩放比例变化 | 贴图和提示框不错位 |

**模型**：inherit

---

## 统一测试输出格式

```markdown
## 测试结果：PASS / FAIL

### 检查结果
| 维度 | 结果 | 备注 |
|------|------|------|
| ... | ✅/❌ | ... |

### 问题列表（如有）
1. [严重/中等/轻微] 问题描述 → 修改建议

### 总体评价
一句话总结
```

**判定标准**：
- PASS：所有维度通过，最多 1-2 个轻微问题
- FAIL：存在严重问题，或中等问题 ≥ 2 个

## 工作流与修正循环

```
用户提出需求
  │
  ▼
[cc-orchestrator] 拆分任务 → 写入 dev-plan.md
  │
  ▼
═══════════ 逐任务循环 ═══════════
  │
  ├─► [cc-dev] 新建，执行任务
  │     ├─ 读取：CLAUDE.md + dev-plan + lessons-learned + 已有代码
  │     ├─ 开发 + compileJava 自测
  │     ├─ 追加经验到 lessons-learned.md
  │     └─ 输出：文件路径 + 摘要 + 编译结果
  │
  ├─► [cc-tester-core]    ┐
  ├─► [cc-tester-infra]   ├─ 并行启动 3 个
  └─► [cc-tester-ui]      ┘
        │
        ├─ 各自读取：CLAUDE.md + 待测代码 + dev-plan
        ├─ 按各自检查清单审查
        └─ 输出：PASS/FAIL + 报告路径
  │
  ├─► [cc-orchestrator] 判定
  │     ├─ 全 PASS → 更新 dev-plan ✅，下一任务
  │     └─ 有 FAIL → 进入修正循环 ↓
  │
  ├─► 修正循环（最多 3 轮）：
  │     1. resume cc-dev，传入 FAIL 报告路径
  │        └─ 读报告 → 修正 → compileJava → 更新经验库
  │     2. resume 对应 FAIL 维度的测试 agent
  │        └─ 重测 → PASS/FAIL
  │     3. 判定：全 PASS → 退出 / 仍有 FAIL → 下一轮
  │
  └─► 第 3 轮仍 FAIL → 标记 ⚠️ 强制通过，记录遗留问题
═══════════════════════════════════
  │
  ▼
全部任务完成 → 用户验收
```

### 关键规则

| 规则 | 说明 |
|------|------|
| DEV_ID 复用 | 同一任务修正循环 resume 同一个 cc-dev |
| TEST_ID 复用 | 同一任务修正循环 resume 同一个测试 agent |
| 任务切换失效 | 新任务时 DEV_ID 和 TEST_ID 全部失效 |
| 测试并发上限 | 每批最多 3 个测试 agent 并行 |
| 修正上限 | 每任务最多 3 轮，超过标记 ⚠️ |

## 日志格式

写入 `logs/orchestrator-log.md`，每行 `- ` 开头，时间格式 `yymmdd hhmm`：

```markdown
- 260503 1430 ── Task: fluid-tank ──
- 260503 1432 开发启动 (DEV_ID: abc123)
- 260503 1445 开发完成，编译PASS (DEV_ID: abc123)
- 260503 1446 测试启动：core/infra/ui 并行
- 260503 1452 测试结果：core=PASS / infra=FAIL / ui=PASS
- 260503 1452 测试AgentID：core=def456 / infra=ghi789 / ui=jkl012
- 260503 1453 第1轮修正启动 (DEV_ID: abc123)
- 260503 1458 第1轮修正完成，编译PASS
- 260503 1459 重测 infra (ID: ghi789)
- 260503 1505 重测结果：infra=PASS
- 260503 1505 Task fluid-tank 完成，迭代2次
```

## 经验库机制

**文件**：`docs/lessons-learned.md`

**格式**：
```markdown
# 经验教训库

## BlockEntity 相关
- [260503] tick 方法必须用静态签名 `tick(Level, BlockPos, BlockState, XxxBlockEntity)`

## 注册表相关
- [260503] 新增 BlockEntity 类型必须在 ModBlockEntities 中注册

## Curios 兼容
- [260503] Curios 为 compileOnly 依赖，所有 API 调用必须先检查 isCuriosLoaded()

## 双端相关
- [260503] @OnlyIn(Dist.CLIENT) 的类/方法不能在服务端代码路径中引用
```

**生命周期**：
- cc-dev 发现踩坑点 → 自动追加到 lessons-learned.md
- cc-orchestrator 定期精简：删除过时条目、合并重复、移除已内化到 CLAUDE.md 的

**粒度标准**：
- ✅ 有具体标准 + 适用于多个场景
- ❌ 只适用于一行代码 → 不记录
- ❌ 太笼统没有可操作性 → 不记录

## Agent ID 收集

每次创建子 agent 后，编排 agent 立即获取其 ID：

```bash
find ~/.claude/projects -name "agent-*.meta.json" -type f -printf '%T@ %p\n' | sort -rn | head -5
```

从文件名提取裸 ID：`agent-abc123.meta.json` → `abc123`

**使用规则**：
- resume 用裸 ID，必须指定 subagent_type
- 同一任务修正循环中复用 DEV_ID / TEST_*_ID
- 新任务时所有 ID 失效，重新启动

## 触发方式

用户在对话中对 cc-orchestrator 说：

```
开发任务：{功能描述}
相关代码：{路径提示，如有}
```

cc-orchestrator 自动拆分任务、调度 dev/tester、管理修正循环、汇报结果。用户只需在每个任务完成后确认或提出调整。

## 文件所有权

| 文件 | 可写角色 | 说明 |
|------|---------|------|
| `dev-plan.md` | cc-orchestrator | 子智能体不修改 |
| `lessons-learned.md` | cc-dev（追加）、cc-orchestrator（精简） | 自动积累 + 定期清理 |
| `test-reports/*.md` | cc-tester-* | 测试 agent 写入 |
| `orchestrator-log.md` | cc-orchestrator | 编排日志 |
| 源代码 `src/` | cc-dev | 仅开发 agent 可写 |

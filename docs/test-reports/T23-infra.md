# T23 视觉反馈 + Tooltip + HUD — 基础设施审查报告

**审查时间**: 2026-05-04  
**审查文件**:
- `src/main/java/com/TKCCOPL/client/ClientTooltipEvents.java`
- `src/main/java/com/TKCCOPL/client/IncubatorHudOverlay.java`

## 测试结果：PASS

### 检查结果

| 检查项 | 结果 | 备注 |
|--------|------|------|
| 客户端隔离 | ✅ | 两个类均标注 `@Mod.EventBusSubscriber(value = Dist.CLIENT)`，grep 确认无服务端引用 |
| 数据同步 | ✅ | 4 个 BlockEntity 均实现 `getUpdatePacket()` + `getUpdateTag()`，状态变更走 `setChanged()` + `sendBlockUpdated()` |
| Dupe 漏洞 | ✅ | 客户端代码仅读取数据渲染，不操作 Inventory，无刷物品路径 |
| Curios 兼容 | ✅ | `isCuriosLoaded()` 前置检查后才调用 `hasSpectrumMonocle()`，compileOnly 安全 |
| 自动化管道 | ✅ | SerumBottler / AtmosphericCondenser 均实现 `WorldlyContainer`，漏斗交互正确 |
| BlockEntity tick | ✅ | HUD 为事件驱动（RenderGuiOverlayEvent.Post），无冗余 tick 计算 |
| 内存泄漏 | ✅ | 无静态集合、无手动注册的事件监听器，无持续增长风险 |

### 审查要点逐项分析

#### 1. Tooltip 客户端隔离 (ClientTooltipEvents.java)

- **Dist.CLIENT 标注**: 第 14 行 `@Mod.EventBusSubscriber(modid = cybercultivator.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)` — 服务端不加载该类。PASS。
- **无服务端引用**: grep 确认 `ClientTooltipEvents` 仅在 `client/` 包内定义，无其他包引用。PASS。
- **Curios 兼容检查**: 第 28 行 `if (!CuriosCompat.isCuriosLoaded() || ...)` 短路求值，Curios 未安装时直接显示 "基因隐藏" 提示，不会触发任何 Curios API 调用。PASS。

#### 2. HUD 注册与渲染 (IncubatorHudOverlay.java)

- **Dist.CLIENT 标注**: 第 24 行正确标注。PASS。
- **事件注册**: 第 30 行 `@SubscribeEvent` 监听 `RenderGuiOverlayEvent.Post`，这是 Forge 1.20.1 HUD 覆盖层的标准做法。PASS。
- **前置守卫**: 第 33 行 `player == null` 检查、第 39 行 `BlockHitResult` pattern match、第 35 行 Curios 单片镜检查 — 三层守卫完备。PASS。

#### 3. Generation/Purity 读取安全

- `GeneticSeedItem.getGeneration()` (源码 95-98 行): `tag == null || !tag.contains(...)` 时返回 0。安全。
- `GeneticSeedItem.getPurity()` (源码 101-104 行): 同样 null 防护，返回 0 并 clamp 至 0-10。安全。
- `GeneticSeedItem.getGene()` (源码 73-78 行): null 防护 + clamp 1-10。安全。
- `ClientTooltipEvents` 第 26 行: 调用 `ensureGeneData(stack)` 确保基因标签存在后再读取。双重保障。PASS。
- `IncubatorHudOverlay` 中读取 splicer output 的 tag (第 177、207、235 行): 均有 `tag != null &&` 前置检查。PASS。

#### 4. HUD 高度动态调整 (拼接机)

第 180 行: `int hudHeight = hasOutput ? (hasMutation ? 74 : 62) : 50;`

| 场景 | 高度 | 内容布局 | 验证 |
|------|------|----------|------|
| 无 output | 50px | Title(2) + A(14) + B(26) + "Empty"(38) | 38+12=50, 匹配 |
| 有 output, 无突变 | 62px | ... + separator(37) + Out(40) + "Ready"(52) | 52+10=62, 匹配 |
| 有 output, 有突变 | 74px | ... + Out(40) + "MUTATION!"(52) + "Ready"(62) | 62+12=74, 匹配 |

高度计算正确。PASS。

#### 5. 文本溢出风险

**拼接机 (背景宽 200px):**
- 种子信息最长情况: `"A: [S:10 Y:10 P:10] Gen:10"` 约 27 字符 x 6px = 162px
- 突变标记追加在同一行 (第 237-239 行): `"  MUTATION!"` 约 11 字符 x 6px = 66px
- 总宽约 228px，超出 200px 背景 **约 28px**
- **结论**: 轻微问题。仅在 Generation > 0 且有突变时触发，文本渲染在背景外但仍可见，不影响功能。

**培养槽/灌装机/冷凝器 (背景宽 130px):**
- 最长文本: `"[Atmo-Condenser]"` 约 16 字符 x 6px = 96px, 安全。
- `"ETA: 资源不足"` 中文约 72px, 安全。
- 灌装机 `"Output: " + item name` — 英文长名称可能溢出，但中文翻译名（如"突触超频血清"）约 96px，安全。轻微问题（仅英文长名称场景）。

### 问题列表

1. **[轻微] 拼接机 HUD 种子突变标记文本溢出**  
   `drawSeedInfo()` 第 237-239 行将突变标记定位在种子信息文本的右侧（`x + mc.font.width(text)`），当种子含 Generation > 0 且有突变标记时，总宽度约 228px 超出 200px 背景。  
   -> **建议**: 将突变标记换行显示（移至下一行 `y + 8`），或缩小 HUD 字体。优先级低，不影响功能。

2. **[轻微] 灌装机 HUD 输出物品名称潜在英文溢出**  
   `drawBottlerHud()` 第 130 行 `"Output: " + output.getHoverName().getString()` 在英文 locale 下长物品名可能超出 130px 背景宽度。  
   -> **建议**: 可选截断或增加背景宽度。由于主要面向中文用户，优先级极低。

### 总体评价

T23 视觉反馈 + Tooltip + HUD 代码基础设施审查通过。客户端隔离正确、Curios 兼容检查完备、null tag 防护全面、HUD 高度动态调整逻辑准确、数据同步链路完整。仅发现 2 个轻微文本溢出问题，均为纯视觉层面、不影响功能和稳定性。

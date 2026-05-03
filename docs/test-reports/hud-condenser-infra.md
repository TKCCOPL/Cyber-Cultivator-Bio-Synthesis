# 基础设施测试报告：HUD 面板修复 + 冷凝器周期调整

## 测试结果：PASS

| 检查项 | 结果 | 备注 |
|--------|------|------|
| 客户端隔离 | PASS | HUD 类位于 `client/` 包，`@Mod.EventBusSubscriber(value = Dist.CLIENT)` 正确隔离；BlockEntity 无客户端注解 |
| 数据同步 | PASS | HUD 通过 `getUpdatePacket()`/`getUpdateTag()` 读取客户端数据；冷凝器 tick 中 `setChanged()` + `sendBlockUpdated()` 完整 |
| Dupe 漏洞 | PASS | HUD 为纯渲染无交互；冷凝器 `extractOutput()` 清空整个堆叠后重置 progress，无分裂竞态 |
| Curios 兼容 | PASS | HUD 第 30 行显式 `isCuriosLoaded()` 前置守卫；`CuriosCompat.hasCurioItem()` 内部再做二次检查 + try-catch 兜底 |
| 自动化管道 | PASS | `WorldlyContainer` 实现正确：`canPlaceItemThroughFace` 返回 false（禁止输入），`canTakeItemThroughFace` 仅允许 slot 0 输出 |
| BlockEntity tick | PASS | 无 new 对象分配，无 NBT 序列化操作，游戏时间模运算开销可忽略 |
| 内存泄漏 | PASS | 无静态集合增长，无监听器注册，事件总线订阅由 Forge 自动管理 |

## 详细审查

### IncubatorHudOverlay.java — 客户端隔离

- `@Mod.EventBusSubscriber(modid = "cybercultivator", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)` 正确限制为客户端事件总线
- 所有导入均为客户端 API（`Minecraft.getInstance()`, `GuiGraphics`, `RenderGuiOverlayEvent`）
- BlockEntity 引用仅用于读取已同步到客户端的数据（`getNutrition()` 等 getter），不触发服务端逻辑
- 无服务端代码被客户端引用的风险

### IncubatorHudOverlay.java — 布局与文本重叠

修改后布局参数（y 坐标起始偏移 +12/行）：

| 元素 | Y 位置 | 说明 |
|------|--------|------|
| 标题 [Bio-Incubator] | y+2 | 高度 ~9px |
| N 进度条 | y+14 | 含标签 + 8px bar + 数值 |
| P 进度条 | y+26 | 同上 |
| D 进度条 | y+38 | 同上 |
| Seed 状态文字 | y+50 | 高度 ~9px |
| G 进度条（有种子时） | y+62 | 含标签 + 8px bar + 数值 |
| ETA 文字（有种子时） | y+74 | 高度 ~9px，底部 y+83 |

面板总高度 86px（y 到 y+86）。末尾 ETA 文字底部 y+83，在面板范围内（y+86），间距 3px 偏紧但无重叠。无种子时最低元素为 y+50，余量充足。

### AtmosphericCondenserBlockEntity.java — 生产周期调整

- `PRODUCTION_TIME` 从 200 改为 600（10s -> 30s）
- `MAX_STACK` 从 16 改为 32，与生产周期放缓匹配，保持总存储时长一致（约 16 分钟满仓）
- 相邻传输逻辑未变（每 20 tick 检查，Purity < 80 时 +20），节奏合理
- NBT 持久化正确处理 `progress` 和 `output` 字段

### AtmosphericCondenserBlockEntity.java — 漏斗兼容性

- `getSlotsForFace()` 返回 `{0}`，所有面共享单一输出槽
- `canPlaceItemThroughFace()` 返回 false — 禁止外部物品注入（生产机只出不进）
- `canTakeItemThroughFace()` 检查 slot 且 output 非空 — 允许漏斗抽取产出
- `setItem()` 为空实现 — 防御性设计，即使绕过 canPlace 也不会写入

## 问题列表

1. **轻微** — CLAUDE.md 文档与代码不一致：文档记载"每 200 tick 生产 1 纯净水瓶"，代码实际为 600 tick。建议同步更新 CLAUDE.md 中冷凝器描述。

## 总体评价

两项修改均通过基础设施审查。HUD 面板客户端隔离正确、Curios 双重守卫到位；冷凝器 `WorldlyContainer` 实现规范、数据同步完整。编译验证通过（`compileJava` BUILD SUCCESSFUL）。唯一遗留项为文档同步。

# T13 基础设施测试报告 — 大气冷凝器 + 基因拼接机 HUD

**测试日期**: 2026-05-03
**待测文件**:
- `src/main/java/com/TKCCOPL/client/IncubatorHudOverlay.java`
- `src/main/java/com/TKCCOPL/block/entity/AtmosphericCondenserBlockEntity.java`
- `src/main/java/com/TKCCOPL/block/entity/GeneSplicerBlockEntity.java`

## 测试结果：FAIL

### 检查结果

| 检查项 | 结果 | 备注 |
|--------|------|------|
| 客户端隔离 | ✅ | HUD 正确标记 `Dist.CLIENT`，无服务端引用 client 包 |
| 数据同步 | ❌ | 冷凝器 tick 仅在产出完成时同步，进度条无法动画 |
| Dupe 漏洞 | ✅ | 所有提取方法遵循标准模式，单线程安全 |
| Curios 兼容 | ✅ | `isCuriosLoaded()` 前置检查 + try-catch 兜底 |
| 自动化管道 | ⚠️ | 冷凝器 WorldlyContainer 正确；拼接机未实现（设计如此） |
| BlockEntity tick | ⚠️ | 冷凝器 tick 无分配，但进度同步缺失导致无效计算 |
| 内存泄漏 | ✅ | 无静态集合无限增长、无未清理监听器 |

### 问题列表

1. **[严重] 冷凝器进度条无法在客户端动画**

   `AtmosphericCondenserBlockEntity.tick()` 中，`progress` 每 tick 递增，但 `changed` 标志仅在生产完成（`progress >= PRODUCTION_TIME`）或自动传输时置为 `true`。`sendBlockUpdated()` 仅在 `changed = true` 时调用。

   **影响**: 客户端 HUD 进度条会卡在 0%，直到 600 tick 生产周期结束瞬间跳到完成。用户体验为"没有动画"。

   **修改建议**: 在 tick 方法中添加周期性同步，类似 `BioIncubatorBlockEntity` 的每 20 tick 同步模式：
   ```java
   // 每 20 tick 同步进度到客户端（与自动传输检查合并）
   if (level.getGameTime() % 20L == 0L) {
       changed = true; // 强制同步进度
       // ... 原有自动传输逻辑 ...
   }
   ```

2. **[中等] 冷凝器 `extractOutput()` 缺少客户端同步**

   `extractOutput()` 调用 `setChanged()` 但未调用 `sendBlockUpdated()`。当漏斗抽取消息物品时，客户端 HUD 仍显示旧库存数量，直到下一次 `changed = true` 的 tick 才会刷新。

   **修改建议**: 在 `extractOutput()` 末尾添加同步调用：
   ```java
   public ItemStack extractOutput() {
       if (output.isEmpty()) return ItemStack.EMPTY;
       ItemStack out = output;
       output = ItemStack.EMPTY;
       progress = 0;
       setChanged();
       if (level != null && !level.isClientSide) {
           level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
       }
       return out;
   }
   ```

3. **[轻微] HUD 每帧创建 Component.literal() 对象**

   `IncubatorHudOverlay` 的 `drawBar()` 和各 `drawXxxHud()` 方法中，每次渲染都调用 `Component.literal()` 创建新对象。在 60 FPS 下，每个可见方块每帧分配约 5-8 个 Component + String 对象。

   **影响**: 轻微 GC 压力。由于 HUD 仅在准星对准特定方块时渲染，实际影响很小。

   **修改建议**: 可考虑缓存常用 Component（如标题文本），但优先级低，当前实现可接受。

### 总体评价

冷凝器和拼接机的 HUD 渲染代码质量良好，客户端隔离、Curios 兼容、内存安全均无问题。**核心缺陷是冷凝器 tick 的数据同步不完整**——进度值仅在生产完成时推送到客户端，导致 HUD 进度条无法动画。这是一个功能性 bug，影响 T13 的核心验收标准（"单片镜显示冷凝器进度"）。拼接机因无 tick 逻辑，同步机制正确，无问题。

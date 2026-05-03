# 测试报告：HUD 文本重叠修复 + 冷凝器周期调整

- 任务ID：hud-condenser
- 测试类型：core（功能验收）
- 日期：2026-05-03
- 测试文件：
  - `src/main/java/com/TKCCOPL/client/IncubatorHudOverlay.java`
  - `src/main/java/com/TKCCOPL/block/entity/AtmosphericCondenserBlockEntity.java`

## 测试结果：PASS

### 检查结果

| 检查项 | 结果 | 备注 |
|--------|------|------|
| 核心交互路径 | ✅ | HUD 渲染链路完整：准星瞄准培养槽 → 检测 Curios 单片镜 → 读取 BlockEntity 状态 → 绘制面板 |
| 注册表声明 | ✅ | 不涉及新注册表对象 |
| BlockEntity 模式 | ✅ | 静态 tick 签名正确，NBT 读写对称（saveAdditional/loadAdditional），客户端同步通过 getUpdatePacket/getUpdateTag |
| 合成配方 | ✅ | 不涉及配方变更 |
| NBT 边界 | ✅ | progress 在 load() 中 Math.max(0, ...) 防负值；output 通过 ItemStack.of() 安全反序列化，null 时回退 EMPTY |
| 区块卸载 | ✅ | saveAdditional 写入 progress + output，load 读回，对称完整；getUpdateTag 调用 saveWithoutMetadata 确保客户端同步 |
| 跨维度 | ✅ | 不涉及维度逻辑 |

### HUD 坐标验证

面板背景：`fill(x, y, x+130, y+86)` — 高度 86px

| 元素 | Y 偏移 | 占用范围 | 与上一元素间距 |
|------|--------|----------|----------------|
| 标题 [Bio-Incubator] | y+2 | y+2 ~ y+11 | — |
| N 进度条 | y+14 | y+14 ~ y+22 | 3px |
| P 进度条 | y+26 | y+26 ~ y+34 | 4px |
| D 进度条 | y+38 | y+38 ~ y+46 | 4px |
| Seed 状态文本 | y+50 | y+50 ~ y+59 | 4px |
| G 生长进度条（有种子时） | y+62 | y+62 ~ y+70 | 3px |
| ETA 文本（有种子时） | y+74 | y+74 ~ y+83 | 4px |

结论：所有元素间距 >= 3px，无重叠。ETA 文本底部 y+83 < 面板底部 y+86，未溢出。

### 冷凝器周期验证

- PRODUCTION_TIME: 200 → 600 tick（实际 diff），等价 30 秒 ✅
- 任务描述称原值为 100，但实际 git diff 显示原值为 200（与 CLAUDE.md 描述一致："每 200 tick 生产 1 纯净水瓶"）。任务描述与代码 diff 存在不一致，以代码为准。
- 生产逻辑：progress 递增至 PRODUCTION_TIME 后重置为 0 并产出物品，逻辑正确 ✅
- 相邻传输：每 20 tick 检测下方培养槽，Purity < 80 时注入 +20（消耗 1 瓶），逻辑不变 ✅

### 问题列表

1. [轻微] 任务描述中 PRODUCTION_TIME 原值标注为 100，实际 git diff 显示原值为 200。不影响功能正确性，但任务文档准确性有误。建议：后续任务描述以 git diff 为准。
2. [轻微] MAX_STACK 从 16 改为 32 未在任务描述中提及，属于未声明的平衡调整。不影响功能正确性，但属于任务范围外的改动。建议：平衡参数变更应单独记录或纳入任务描述。

### 总体评价

两项修复功能正确，HUD 坐标计算无重叠无溢出，冷凝器逻辑完整且 NBT 持久化对称。编译通过，无阻断级问题。

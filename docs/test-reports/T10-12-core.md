# T10-12 Core 测试报告

## 测试结果：PASS

| 检查项 | 结果 | 备注 |
|--------|------|------|
| 核心交互路径 | ✅ | T10 HUD同步链路完整; T11 getRecipeOutput在consumeInputs之前执行; T12 创造栏70变体完整 |
| 注册表声明 | ✅ | SERUM_BOTTLER BlockEntity已注册; 所有引用物品已在ModItems注册 |
| BlockEntity 模式 | ✅ | tick静态签名匹配; saveAdditional/loadAdditional对称完整 |
| 合成配方 | ✅ | 4种配方与设计规范一致; Activity加权公式正确 |
| NBT 边界 | ✅ | Activity clamp [1,10]; 缺失NBT默认5 |
| 区块卸载 | ✅ | save/load对称; 活跃配方状态完整持久化 |
| 跨维度 | ✅ | tick方法服务器端隔离; HUD仅客户端渲染 |

### 轻微问题
1. `hasIngredients()` 贪心匹配算法 — 当前无影响
2. `calculateActivity()` 对 EMPTY 栈调用 `getOrCreateTag()` — 微小内存浪费

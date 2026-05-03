## 测试结果：FAIL

### 检查结果
| 检查项 | 结果 | 备注 |
|--------|------|------|
| 核心交互路径 | ❌ | Gene_Purity 数据链在培养槽处断裂，berry 合成时 inputs 无 Gene_Purity 标签 |
| 注册表声明 | ✅ | T22 无新增注册内容，无需额外声明 |
| BlockEntity 模式 | ✅ | tick 静态签名匹配、saveAdditional/loadAdditional 对称、客户端同步完整 |
| 合成配方 | ✅ | 4 种配方匹配逻辑正确，activeRecipe 缓存避免 TOCTOU |
| NBT 边界 | ✅ | calculateActivity 中 clamp 至 Math.max(1, Math.min(cap, activity))，cap 非负；getActivity 返回 Math.max(1, ...) 无上限溢出 |
| 区块卸载 | ✅ | saveAdditional/loadAdditional 对称，progress/maxProgress/activeRecipe/inputs/output 均正确持久化 |
| 跨维度 | ✅ | tick 方法仅检查 level.isClientSide，无维度限制逻辑，跨维度不报错 |

### 问题列表

#### 1. [严重] Gene_Purity 数据链断裂 -- 培养槽产出物未携带 Gene_Purity

BioIncubatorBlockEntity.getCropOutput() (行 104-129) 为产出物写入 Potency/Purity(材质标签)/Concentration/Generation，但未写入 Gene_Purity。导致灌装机 calculateActivity() (行 51-57) 遍历 berry 合成原料时永远读到 genePurity=0，cap 始终为 10。

实际数据流：
```
拼接机 → 种子(Gene_Purity=5) → 培养槽 → 产出物(Potency=7, 无Gene_Purity)
→ 灌装机berry合成 → calculateActivity: genePurity=0, cap=10
→ berry(Activity=10, Gene_Purity=0)
```

修改建议：在 BioIncubatorBlockEntity.getCropOutput() 中，从 seed 读取 Gene_Purity 并写入产出物：
```java
int genePurity = GeneticSeedItem.getPurity(seed);
if (genePurity > 0) {
    output.getOrCreateTag().putInt(GeneticSeedItem.GENE_PURITY, genePurity);
}
```

#### 2. [严重] 血清合成路径未使用 Gene_Purity cap

血清合成路径 (case 1/2/3) 使用 getActivity(berry) 直接读取 berry 的 SynapticActivity 标签，而非重新调用 calculateActivity()。Gene_Purity 在血清合成阶段不会被用于 cap 计算，也不会传递给血清输出物。

修改建议：需明确设计意图并选择方案：
- 方案(a)：直接继承 berry Activity（当前行为），Gene_Purity 仅影响 berry 合成阶段，注释需明确说明
- 方案(b)：血清合成也调用 calculateActivity(inputs) 重算，使 Gene_Purity 对血清也生效

#### 3. [中等] 创造栏预置变体未覆盖 Activity > 10 范围

ModCreativeTabs 行 53-60 中 berry 和血清预置变体仅覆盖 SynapticActivity 1-10。若 Gene_Purity cap 生效，berry Activity 理论上可达 15（Gene_Purity=10 时 cap=10+5=15），当前创造栏无法直接测试 >10 效果缩放。

修改建议：修复问题 1 后，增加 Gene_Purity 标签预置 berry 变体用于测试。

#### 4. [轻微] calculateActivity 的 Javadoc 与实际行为不匹配

行 32-33 注释说"Gene_Purity 可突破 Activity 10 的上限"，但当前数据流下 Gene_Purity 在 inputs 中永远为 0。注释描述设计意图而非当前行为，可能导致维护者误解。

### 总体评价

calculateActivity() 的 cap 公式本身实现正确（cap = 10 + floor(Gene_Purity/2)），NBT 边界处理和持久化对称性均通过。但 Gene_Purity 对 Activity 上限影响的**核心功能闭环未实现** -- 数据链在培养槽产出物处断裂，整条 Gene_Purity -> Activity cap 路径在正常游戏流程中不可触发。需修复培养槽 Gene_Purity 传递逻辑并确认血清合成路径设计意图后重新测试。

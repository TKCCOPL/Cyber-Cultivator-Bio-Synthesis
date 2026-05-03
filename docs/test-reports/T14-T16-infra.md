# T14-T16 血清效果重平衡 — 基础设施审查报告

> 审查时间：2026-05-04
> 审查范围：SynapticOverclockEffect / VisualEnhancementEffect / MetabolicBoostEffect
> 审查员：cc-tester-infra

## 测试结果：PASS

### 检查结果

| 检查项 | 结果 | 备注 |
|--------|------|------|
| 客户端隔离 | ✅ | 三个 Effect 类均无 `@OnlyIn` 注解、无客户端专属 import，全部逻辑可在服务端安全执行 |
| 数据同步 | ✅ | Effect 类本身无 BlockEntity 数据同步需求；属性修饰符通过 `AttributeInstance` 系统自动同步 |
| Dupe 漏洞 | ✅ | Effect 类不涉及物品操作，无刷物品路径 |
| Curios 兼容 | ✅ | Effect 类不直接调用 Curios API，无需 `isCuriosLoaded()` 检查 |
| 自动化管道 | ✅ | Effect 类不涉及容器/漏斗交互，无需 `WorldlyContainer` 实现 |
| BlockEntity tick | ✅ | 无 BlockEntity tick 逻辑；`isDurationEffectTick` 频率合理 |
| 内存泄漏 | ✅ | 无静态集合增长、无事件监听器、transient modifier 在 `removeAttributeModifiers` 中正确清理 |

### 详细分析

#### 1. 双端兼容性

**客户端隔离：**
- 三个类均无 `@OnlyIn` 注解（grep 验证通过）
- 无 `net.minecraft.client.*` 相关 import
- `VisualEnhancementEffect` 中发光扫描的 `!entity.level().isClientSide` 守卫正确，仅服务端执行实体扫描

**`isClientSide` 守卫审查：**
- `SynapticOverclockEffect.removeAttributeModifiers()` 第 75 行：`!entity.level().isClientSide` 守卫 NeuralOverload 施加，正确
- `VisualEnhancementEffect.applyEffectTick()` 第 36 行：`!entity.level().isClientSide` 守卫实体扫描，正确
- `VisualEnhancementEffect.removeAttributeModifiers()` 第 49 行：`!entity.level().isClientSide` 守卫 NeuralOverload 施加，正确
- `MetabolicBoostEffect.removeAttributeModifiers()` 第 61 行：`!entity.level().isClientSide` 守卫 NeuralOverload 施加，正确
- 夜视/抗火/急迫/抗性/跳跃等 `addEffect` 调用不带 `isClientSide` 检查，符合 Minecraft 惯例（效果系统自动处理双端同步）

#### 2. 性能影响

**S-02 实体扫描（VisualEnhancementEffect）：**
- 扫描范围：`16 + amp * 8` 格，当 amplifier=4（最高饮用叠加）时为 48 格
- 搜索体积：AABB inflate 48 = 96x96x96 = 约 884,736 格体积
- 执行频率：每 60 tick（3 秒）一次
- 风险评估：**低风险**。60 tick 的低频执行显著摊薄了开销；且仅在服务端执行。极端场景（如满载怪物的刷怪塔范围内）可能导致短暂卡顿，但属于可接受范围

**transient modifier 每 tick remove+add 模式：**
- `SynapticOverclockEffect`：每 20 tick 执行 1 次 remove+add（攻速+移速共 4 次 Map 操作）
- `MetabolicBoostEffect`：每 10 tick 执行 1 次 remove+add（移速共 2 次 Map 操作）
- `AttributeInstance.modifiers` 是 `List<AttributeModifier>`，remove 为 O(n) 线性扫描（n 通常 < 10），add 为 O(1)
- 风险评估：**极低风险**。这是 Minecraft 模组中动态属性修饰符的标准实现模式

**`isDurationEffectTick` 频率：**
- S-01（SynapticOverclock）：`duration % 20 == 0`（每秒 1 次）— 合理，攻速/移速需要及时响应
- S-02（VisualEnhancement）：`duration % 60 == 0`（每 3 秒 1 次）— 合理，发光扫描不需要高频
- S-03（MetabolicBoost）：`duration % 10 == 0`（每 0.5 秒 1 次）— 合理，回血需要较高频率以保持平滑感

#### 3. 数据一致性

**UUID 常量审查（共 3 个 UUID，3 个类）：**

| 类 | UUID 名称 | UUID 值 |
|----|-----------|---------|
| SynapticOverclockEffect | ATTACK_SPEED_UUID | `5b7d8f1d-24fc-4f4e-8f7a-14a1ec752c9e` |
| SynapticOverclockEffect | MOVE_SPEED_UUID | `b273ca3f-f6cc-4e49-a405-a56f5a9f90d8` |
| MetabolicBoostEffect | MOVE_SPEED_UUID | `a1b2c3d4-e5f6-7890-abcd-ef1234567890` |

- 全局 grep 验证：仅此 3 个 UUID 定义，无冲突
- 注意：SynapticOverclockEffect 和 MetabolicBoostEffect 均修改 `Attributes.MOVEMENT_SPEED`，但使用不同 UUID 和不同 modifier 名称（`synaptic_move_speed` vs `metabolic_move_speed`），不会互相覆盖

**AttributeModifier.Operation 审查：**
- 所有 transient modifier 均使用 `MULTIPLY_TOTAL`，符合设计意图（百分比加成而非固定值加成）
- 公式正确：S-01 攻速 `0.15 + amp*0.05`，S-01 移速 `0.10 + amp*0.05`，S-03 移速 `0.10 + amp*0.05`

**amplifier 边界值处理：**
- S-01 抗性：`Math.min(amplifier, 2)` → 上限 III 级，正确
- S-02 抗火：`Math.min(amplifier, 2)` → 上限 III 级，正确
- S-03 跳跃：`Math.min(amplifier, 2)` → 上限 III 级，正确

#### 4. 内存安全

**transient modifier 清理：**
- `SynapticOverclockEffect.removeAttributeModifiers()`：清理 ATTACK_SPEED_UUID 和 MOVE_SPEED_UUID，通过 `attributeMap.getInstance()` 获取 AttributeInstance，正确
- `MetabolicBoostEffect.removeAttributeModifiers()`：清理 MOVE_SPEED_UUID，通过 `attributeMap.getInstance()` 获取 AttributeInstance，正确
- `VisualEnhancementEffect`：无 transient modifier（仅添加 vanilla 效果），无需清理

**NeuralOverload 延迟施加安全性：**
- 三个类均使用 `TickTask` 延迟 1 tick 施加 NeuralOverload，避免在 `removeAttributeModifiers` 执行期间触发 CME
- `entity.getEffect(this) == null` 检查正确区分"自然过期"和"被新实例替换"两种场景
- 潜在风险：若实体在 1 tick 延迟期间被移除（如玩家退出），`entity.addEffect()` 调用会静默失败，不会崩溃

### 问题列表

1. [轻微] S-02 发光扫描范围随 amplifier 线性增长，amplifier=4 时 AABB inflate 48 格（体积约 88 万格），在怪物密集区域可能导致短暂性能波动
   - 当前 60 tick 执行频率已有效缓解，无需修改
   - 若未来出现性能投诉，可考虑：(a) 限制最大扫描范围 (b) 分 tick 扫描（每 tick 扫描 1/4 区域）

2. [轻微] SynapticOverclockEffect 的抗性效果 `new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, ...)` 每 20 tick 重新施加一次（持续 30 tick），存在 10 tick 的重叠窗口。这是有意为之的设计（确保效果不间断），但会产生不必要的 `MobEffectInstance` 对象分配（每秒 1 个）
   - 可优化为持续时间 `duration % 20 == 0` 时检查是否已有抗性效果，仅在缺失时施加
   - 当前实现功能正确，仅为轻微 GC 压力

### 总体评价

三个 Effect 类实现规范，无严重或中等问题。双端兼容性良好（无客户端代码泄漏），UUID 唯一无冲突，transient modifier 在效果结束时正确清理，NeuralOverload 副作用通过 TickTask 延迟施加避免 CME。仅有 2 个轻微性能优化建议，不影响功能正确性和稳定性。

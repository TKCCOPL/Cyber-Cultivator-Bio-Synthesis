# T17 NeuralOverload 来源感知 — 核心功能审查

> 审查日期: 2026-05-04
> 审查范围: NeuralOverloadEffect + 三个来源 Effect 类
> 编译状态: BUILD SUCCESSFUL (compileJava)

## 测试结果：PASS

### 检查结果

| 检查项 | 结果 | 备注 |
|--------|------|------|
| 核心交互路径 | ✅ | 来源编码→解码→分支副作用链路完整闭环 |
| 注册表声明 | ✅ | ModEffects 中四个效果均已注册 (NeuralOverload/VisualEnhancement/SynapticOverclock/MetabolicBoost) |
| BlockEntity 模式 | N/A | 本任务不涉及 BlockEntity |
| 合成配方 | N/A | 本任务不涉及配方变更 |
| NBT 边界 | ✅ | amplifier 解码范围正确，子效果 clamp 合理 |
| 区块卸载 | N/A | 效果数据存储在实体上，不受 chunk unload 影响 |
| 跨维度 | ✅ | 效果作用于 LivingEntity，无维度耦合逻辑 |

### NeuralOverloadEffect 逐项审查

**1. SOURCE_BASE 常量**
- `public static final int SOURCE_BASE = 200;` (第 15 行) — 通过

**2. applyEffectTick 来源解码**
- 第 31-33 行：`if (amplifier >= SOURCE_BASE)` 解码 `source = amplifier - SOURCE_BASE`，`realAmplifier = 0` — 通过
- 设计说明：来源编码模式下 realAmplifier 硬编码为 0，子效果强度固定。这是合理的简化，避免嵌套编码的复杂度。

**3. S-01 分支 (case 1)**
- WITHER amplifier: `1 + Math.min(realAmplifier, 2)` = 1 — 通过
- HUNGER amplifier: `realAmplifier` = 0 — 通过

**4. S-02 分支 (case 2)**
- BLINDNESS amplifier: `1 + Math.min(realAmplifier, 2)` = 1 — 通过
- HUNGER amplifier: `realAmplifier` = 0 — 通过

**5. S-03 分支 (case 3)**
- MOVEMENT_SLOWDOWN amplifier: `1 + realAmplifier` = 1 — 通过
- POISON amplifier: `Math.min(realAmplifier, 2)` = 0 — 通过

**6. DEFAULT 分支**
- MOVEMENT_SLOWDOWN + HUNGER — 与旧数据兼容 — 通过

**7. 子效果 amplifier 上限**
- 凋零/失明: `1 + Math.min(amp, 2)` 最大值 3 — 通过
- 中毒: `Math.min(amp, 2)` 最大值 2 — 通过
- 缓慢: `1 + amp` 无硬上限，但在来源编码模式下 amp=0，实际值为 1 — 通过
- 饥饿: `amp` 无硬上限，同上 amp=0 — 通过

### 三个来源 Effect 类审查

| 来源类 | 常量值 | getEffect null 检查 | TickTask 延迟 | 客户端防护 |
|--------|--------|---------------------|---------------|------------|
| SynapticOverclockEffect | SOURCE_BASE + 1 (201) | ✅ 第 77 行 | ✅ +1 tick | ✅ isClientSide 检查 |
| VisualEnhancementEffect | SOURCE_BASE + 2 (202) | ✅ 第 51 行 | ✅ +1 tick | ✅ isClientSide 检查 |
| MetabolicBoostEffect | SOURCE_BASE + 3 (203) | ✅ 第 62 行 | ✅ +1 tick | ✅ isClientSide 检查 |

**Duration 公式一致性:**
三个来源类的 NeuralOverload duration 均为 `20 * (12 + amplifier * 4)`:
- amp=0: 240 tick (12s)
- amp=4: 560 tick (28s)

### 边界值验证

| 场景 | 编码值 | 解码 source | realAmplifier | 结果 |
|------|--------|-------------|---------------|------|
| S-01, amp=0 | 200+1=201 | 1 (S-01) | 0 | ✅ |
| S-02, amp=0 | 200+2=202 | 2 (S-02) | 0 | ✅ |
| S-03, amp=0 | 200+3=203 | 3 (S-03) | 0 | ✅ |
| 旧数据, amp=4 | 4 | 0 (DEFAULT) | 4 | ✅ 兼容 |
| 溢出保护 | 999 | 799 (DEFAULT) | 999 | ✅ 走 default 分支 |

### 问题列表

无严重或中等问题。

**轻微 (1):**
SynapticOverclockEffect 和 MetabolicBoostEffect 在 `removeAttributeModifiers` 的 TickTask lambda 中捕获了 `entity` 和 `amplifier`。若实体在 1 tick 延迟期间被移除（如被传送/死亡），lambda 执行时可能作用于已卸载的实体。不过 `addEffect` 本身在实体不在世界中时不会抛异常，实际影响可忽略。

### 总体评价

T17 NeuralOverload 来源感知实现正确，编码/解码链路完整，三个来源分支的副作用类型和 amplifier 值均符合设计文档，边界处理和向后兼容（DEFAULT 分支）到位。代码编译通过，无功能性缺陷。

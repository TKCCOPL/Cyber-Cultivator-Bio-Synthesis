# 血清效果重平衡设计规范

## 需求背景

当前三个血清的效果设计存在以下问题：
- S-01 攻速/移速固定值，不随 amplifier 增长，高等级缺乏正反馈
- S-02 发光范围固定 32 格，不随 amplifier 变化
- S-03 急迫效果与"代谢加速"主题不匹配
- 所有血清共享相同的神经过载副作用（缓慢+饥饿），缺乏差异化

## 技术方案

### 1. S-01 突触超频 — 正面战斗型

**效果变更：**

| amplifier | 等级 | 攻速 | 移速 | 抗性 | 副作用（神经过载） |
|-----------|------|------|------|------|-------------------|
| 0 | I | +15% | +10% | 抗性 I | 12秒：凋零I + 饥饿I |
| 1 | II | +20% | +15% | 抗性 II | 16秒：凋零II + 饥饿II |
| 2 | III | +25% | +20% | 抗性 III | 20秒：凋零III + 饥饿III |
| 3 | IV | +30% | +25% | 抗性 III | 24秒：凋零III + 饥饿IV |
| 4 | V | +35% | +30% | 抗性 III | 28秒：凋零III + 饥饿V |

**公式：**
- 攻速：`0.15 + amplifier * 0.05`（MULTIPLY_TOTAL）
- 移速：`0.10 + amplifier * 0.05`（MULTIPLY_TOTAL）
- 抗性：`MobEffects.DAMAGE_RESISTANCE, min(amplifier, 2)`（上限 III）
- 凋零：`1 + min(amplifier, 2)`（上限 III）
- 饥饿：`amplifier`

**实现变更：**
- 移除构造函数中的 `addAttributeModifier`（攻速/移速）
- `applyEffectTick` 中用 `addTransientModifier` 按 amplifier 动态计算
- 移除急迫施加，改为抗性 `addEffect(DAMAGE_RESISTANCE, 30, min(amp, 2))`
- `removeAttributeModifiers` 中清理 transient modifier

### 2. S-02 视觉强化 — 侦察型

**效果变更：**

| amplifier | 等级 | 夜视 | 发光范围 | 抗火 | 副作用（神经过载） |
|-----------|------|------|---------|------|-------------------|
| 0 | I | ✓ | 16格 | 抗火 I | 12秒：失明I + 饥饿I |
| 1 | II | ✓ | 24格 | 抗火 II | 16秒：失明II + 饥饿II |
| 2 | III | ✓ | 32格 | 抗火 III | 20秒：失明III + 饥饿III |
| 3 | IV | ✓ | 40格 | 抗火 III | 24秒：失明III + 饥饿IV |
| 4 | V | ✓ | 48格 | 抗火 III | 28秒：失明III + 饥饿V |

**公式：**
- 发光范围：`16 + amplifier * 8`
- 抗火：`MobEffects.FIRE_RESISTANCE, min(amplifier, 2)`（上限 III）
- 失明：`1 + min(amplifier, 2)`（上限 III）
- 饥饿：`amplifier`

**实现变更：**
- `SCAN_RANGE` 常量改为动态计算 `16 + amplifier * 8`
- 新增抗火施加：`addEffect(FIRE_RESISTANCE, 100, min(amp, 2))`

### 3. S-03 代谢加速 — 机动型

**效果变更：**

| amplifier | 等级 | 回血/0.5秒 | 移速 | 跳跃 | 副作用（神经过载） |
|-----------|------|-----------|------|------|-------------------|
| 0 | I | 1.0 HP | +10% | 跳跃 I | 12秒：缓慢III + 中毒I |
| 1 | II | 1.5 HP | +15% | 跳跃 II | 16秒：缓慢IV + 中毒II |
| 2 | III | 2.0 HP | +20% | 跳跃 III | 20秒：缓慢V + 中毒III |
| 3 | IV | 2.5 HP | +25% | 跳跃 III | 24秒：缓慢VI + 中毒III |
| 4 | V | 3.0 HP | +30% | 跳跃 III | 28秒：缓慢VII + 中毒III |

**公式：**
- 回血：`1.0 + amplifier * 0.5`（每 0.5 秒）
- 移速：`0.10 + amplifier * 0.05`（addTransientModifier, MULTIPLY_TOTAL）
- 跳跃：`MobEffects.JUMP, min(amplifier, 2)`（上限 III）
- 缓慢：`1 + amplifier`
- 中毒：`min(amplifier, 2)`（上限 III）

**实现变更：**
- 回血恢复为 `heal(1.0F + amplifier * 0.5F)`
- 移除急迫施加
- 新增移速 transient modifier
- 新增跳跃 `addEffect(JUMP, 30, min(amp, 2))`

### 4. 神经过载来源感知

**问题：** 三个血清的神经过载需要施加不同的子效果，但 `NeuralOverloadEffect` 是单一类。

**方案：** 通过 MobEffectInstance NBT 标记来源血清类型。

```
施加时：
  CompoundTag tag = new CompoundTag();
  tag.putString("Source", "S01");  // 或 "S02", "S03"
  instance.setTag(tag);

读取时：
  CompoundTag tag = instance.getTag();
  String source = tag != null ? tag.getString("Source") : "S01";
```

**NeuralOverloadEffect.applyEffectTick 分支：**

| Source | 缓慢 | 饥饿 | 凋零 | 失明 | 中毒 |
|--------|------|------|------|------|------|
| S-01 | - | ✓ amp | ✓ 1+min(amp,2) | - | - |
| S-02 | - | ✓ amp | - | ✓ 1+min(amp,2) | - |
| S-03 | ✓ 1+amp | - | - | - | ✓ min(amp,2) |
| 默认 | ✓ 1+amp | ✓ amp | - | - | - |

**实现变更：**
- 修改 `SynapticOverclockEffect.removeAttributeModifiers`：写入 Source="S01"
- 修改 `VisualEnhancementEffect.removeAttributeModifiers`：写入 Source="S02"
- 修改 `MetabolicBoostEffect.removeAttributeModifiers`：写入 Source="S03"
- 修改 `NeuralOverloadEffect.applyEffectTick`：按 Source 分支施加子效果

### 5. 语言文件

新增翻译键：
```
"effect.cybercultivator.neural_overload": "神经过载"  // 已有
```

副作用子效果使用原版翻译键，无需新增。

## 任务拆分

### 任务列表

- [ ] **T1: S-01 突触超频效果重写**
  - 描述：移除构造函数 addAttributeModifier，applyEffectTick 中动态施加攻速/移速/抗性
  - 涉及文件：`src/.../effect/SynapticOverclockEffect.java`
  - 预估：60 行
  - 依赖：无

- [ ] **T2: S-02 视觉强化效果重写**
  - 描述：发光范围随 amplifier 增长，新增抗火效果
  - 涉及文件：`src/.../effect/VisualEnhancementEffect.java`
  - 预估：40 行
  - 依赖：无

- [ ] **T3: S-03 代谢加速效果重写**
  - 描述：移除急迫，新增移速 transient modifier + 跳跃效果，回血恢复 amplifier 缩放
  - 涉及文件：`src/.../effect/MetabolicBoostEffect.java`
  - 预估：50 行
  - 依赖：无

- [ ] **T4: NeuralOverload 来源感知**
  - 描述：三个 Effect 类写入 Source NBT，NeuralOverloadEffect 按 Source 分支施加子效果
  - 涉及文件：`NeuralOverloadEffect.java`, `SynapticOverclockEffect.java`, `VisualEnhancementEffect.java`, `MetabolicBoostEffect.java`
  - 预估：80 行
  - 依赖：T1, T2, T3

- [ ] **T5: 编译验证 + 语言文件更新**
  - 描述：确保编译通过，更新 zh_cn/en_us 翻译
  - 涉及文件：语言文件
  - 预估：20 行
  - 依赖：T4

## 验收标准

1. `./gradlew compileJava` 通过
2. S-01：攻速/移速随 amplifier 可见增长，抗性上限 III
3. S-02：发光范围随 amplifier 变化（16-48），抗火上限 III
4. S-03：回血随 amplifier 增长，跳跃上限 III，有移速加成
5. 每个血清的神经过载施加正确的差异化子效果
6. 叠加升级时副作用正确增强

## 参考资料

- `SynapticSerumItem.java`：血清饮用逻辑，amplifier 叠加
- `SynapticOverclockEffect.java`：当前 S-01 实现
- `VisualEnhancementEffect.java`：当前 S-02 实现
- `MetabolicBoostEffect.java`：当前 S-03 实现
- `NeuralOverloadEffect.java`：当前神经过载实现

# 基因种子模型推导与参数说明

## 【模型目标】

描述玩家通过基因拼接机将基础种子逐步培育为高基因值种子的过程，评估：
- 达到目标基因值（如 Gene_Potency ≥ 8）的期望尝试次数
- 不同初始基因分布下的收敛速度
- 多台拼接机并行时的长期产率

---

## 【实际游戏机制】

基因种子携带三个隐藏属性（NBT 标签）：
- `Gene_Speed`: 1-10
- `Gene_Yield`: 1-10
- `Gene_Potency`: 1-10

基因拼接公式（GeneSplicerBlockEntity）：
```
新值 = floor((父本 + 母本) / 2) + random(-2, -1, 0, +1, +2)
新值 = clamp(新值, 1, 10)
```

初始种子基因值（GeneticSeedItem 构造函数）：
- 纤维草：Speed=4, Yield=7, Potency=3
- 蛋白质豆：Speed=5, Yield=4, Potency=7
- 酒精花：Speed=6, Yield=3, Potency=5

---

## 【数学模型】

### 单属性基因传播

将单个基因属性建模为离散时间马尔可夫链，状态空间 {1, 2, ..., 10}。

转移概率：给定父本值 a 和母本值 b，子代值 c 的分布为：
```
base = floor((a + b) / 2)
c = base + delta,  delta ∈ {-2, -1, 0, +1, +2} 均匀分布
c = clamp(c, 1, 10)
```

### 期望收敛分析

当两个相同基因值 x 的亲本杂交时：
- base = x
- 子代 = clamp(x + delta, 1, 10)，delta ∈ {-2, -1, 0, +1, +2}

期望子代值 E[c|x,x] = x（当 x 在 3-8 范围内时，期望值=0）

这意味着同值杂交无偏移，基因提升完全依赖随机波动。

### 达到目标值的期望尝试次数

设目标为基因值 ≥ G（如 G=8），初始值为 v₀。

近似分析（忽略边界效应）：
- 每次同值杂交期望提升 Δ = 0（无偏移）
- 实际收敛依赖随机游走，方差 σ² = 2（delta 的方差）
- 达到目标所需次数近似为 d² / σ² 的量级

仿真结果（对称突变 {-2,-1,0,+1,+2}）：
- 从 Potency=3 提升到 ≥8：中位数 ≈ 18 次拼接
- 从 Potency=3 提升到 ≥10：中位数 ≈ 26 次拼接

---

## 【建议默认参数（用于仿真）】

- 基因值范围：1-10
- 初始基因值：Speed=4, Yield=7, Potency=3（纤维草）
- 目标基因值：8
- 拼接机数量：1-100

---

## 【如何使用仿真脚本】

```bash
# 基因收敛仿真
python simulations/advanced_seed_sim.py --trials 10000 --target 8

# 达到 Level 10 概率仿真（需要 numpy + matplotlib）
python simulations/level10_probability_sim.py --trials 10000 --target 10
```

#!/usr/bin/env python3
"""仿真：高级种子产率与首次到达时间蒙特卡洛验证

支持两种近似模式：
- poisson: 使用组合速率（采集成功率 + 交配完成率）的指数分布近似，快速且适用于大规模参数扫描。
- individual: 基于简单个体模型的离散事件模拟（较慢，较精确）。

将结果打印为统计量（平均、分位数、估算产率）。
"""
import random
import math
import argparse
from tqdm import trange

def simulate_one_poisson(p0=0.0005, mu=0.02, L=5, Tb=30.0, hps=0.2, Nh=500, Nb=500, max_time=86400):
    # 速率模型：采集成功率与交配完成率近似为泊松过程
    harvest_rate = Nh * hps
    breeding_attempts_per_sec = Nb / Tb
    harvest_success_rate = harvest_rate * p0
    breeding_success_rate = breeding_attempts_per_sec * (mu / L)
    total_rate = harvest_success_rate + breeding_success_rate
    if total_rate <= 0:
        return float('inf')
    t = random.expovariate(total_rate)
    return t if t <= max_time else float('inf')

def simulate_one_individual(p0=0.0005, mu=0.02, L=5, Tb=30.0, hps=0.2, Nh=500, Nb=500, max_time=86400):
    # 个体级简化模型：每个 breeder 单独进行晋级尝试，采集按概率事件
    # 为控制复杂度，按秒步进，统计是否在 max_time 内出现第一次高级种子
    # 注意：此方法随 Nb, Nh 和 max_time 线性增长，可能较慢
    levels = [0] * Nb  # breeders current level
    time = 0.0
    dt = 1.0
    harvest_rate = Nh * hps
    # 将每秒的采集次数视作 Poisson(harvest_rate) 次采集
    while time < max_time:
        # 采集事件
        if harvest_rate > 0:
            num_harvests = random.poissonvariate(harvest_rate) if hasattr(random, 'poissonvariate') else sum(1 for _ in range(int(math.ceil(harvest_rate))) if random.random() < (harvest_rate / max(1, int(math.ceil(harvest_rate)))))
            for _ in range(num_harvests):
                if random.random() < p0:
                    return time
        # breeding attempts: each breeder attempts with probability dt/Tb per second
        p_attempt = dt / Tb
        for i in range(Nb):
            if random.random() < p_attempt:
                if random.random() < mu:
                    levels[i] += 1
                    if levels[i] >= L:
                        return time
        time += dt
    return float('inf')

def monte_carlo(trials=20000, mode='poisson', **kwargs):
    times = []
    for _ in trange(trials):
        if mode == 'poisson':
            t = simulate_one_poisson(**kwargs)
        else:
            t = simulate_one_individual(**kwargs)
        times.append(t)
    finite = [t for t in times if t < float('inf')]
    successes = len(finite)
    if successes == 0:
        return {'trials': trials, 'successes': 0}
    avg_time = sum(finite) / successes
    median_time = sorted(finite)[len(finite)//2]
    # 简单估算长期产率： successes / (total simulated time across successes)
    per_sec_rate = successes / (trials * avg_time)
    return {
        'trials': trials,
        'successes': successes,
        'avg_time_s': avg_time,
        'median_time_s': median_time,
        'per_sec_rate_est': per_sec_rate,
    }

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--trials', type=int, default=20000)
    parser.add_argument('--mode', choices=['poisson', 'individual'], default='poisson')
    parser.add_argument('--p0', type=float, default=0.0005)
    parser.add_argument('--mu', type=float, default=0.02)
    parser.add_argument('--L', type=int, default=5)
    parser.add_argument('--Tb', type=float, default=30.0)
    parser.add_argument('--hps', type=float, default=0.2)
    parser.add_argument('--Nh', type=int, default=500)
    parser.add_argument('--Nb', type=int, default=500)
    parser.add_argument('--max_time', type=float, default=86400.0)
    args = parser.parse_args()
    res = monte_carlo(trials=args.trials, mode=args.mode, p0=args.p0, mu=args.mu, L=args.L, Tb=args.Tb, hps=args.hps, Nh=args.Nh, Nb=args.Nb, max_time=args.max_time)
    print(res)

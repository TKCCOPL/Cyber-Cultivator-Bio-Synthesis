#!/usr/bin/env python3
"""计算并模拟在给定参数下达到 Level 10 的概率分布，并绘制直方图与正态拟合曲线。

方法说明：
- 每个 breeder 在时间 T 内会有 n = floor(T / Tb) 次独立晋级尝试，单次晋级概率为 mu。
- 单个 breeder 在 T 内至少获得 L 次成功的概率 p_b = 1 - BinomialCDF(L-1; n, mu).
- 在 Nb 个独立 breeder 下，达到 Level 10 的数量 ~ Binomial(Nb, p_b)。
"""
import math
import argparse
import numpy as np
import matplotlib.pyplot as plt
from math import comb

def per_breeder_success_prob(mu, L, Tb, T):
    n = int(math.floor(T / Tb))
    if n < L:
        return 0.0
    # cumulative probability of <= L-1 successes
    cdf = 0.0
    q = 1.0 - mu
    for k in range(0, L):
        cdf += comb(n, k) * (mu ** k) * (q ** (n - k))
    return 1.0 - cdf

def simulate_trials(trials, Nb, p_b):
    # draw Binomial samples: number of breeders reaching Level L per trial
    samples = np.random.binomial(Nb, p_b, size=trials)
    return samples

def plot_histogram(samples, Nb, p_b, outpath):
    mean = Nb * p_b
    var = Nb * p_b * (1 - p_b)
    std = math.sqrt(var)

    plt.figure(figsize=(8,5))
    # histogram
    counts, bins, _ = plt.hist(samples, bins=range(int(samples.max())+2), density=True, alpha=0.6, color='C0', label='Empirical')
    # normal pdf overlay
    xs = np.linspace(0, max(samples.max(), mean + 4*std), 200)
    from math import exp, pi
    norm_pdf = [ (1.0/(std * math.sqrt(2*pi))) * math.exp(-0.5*((x-mean)/std)**2) if std>0 else 0.0 for x in xs]
    plt.plot(xs, norm_pdf, 'r--', label='Normal fit')
    plt.xlabel('Number of breeders reaching Level')
    plt.ylabel('Density')
    plt.title(f'Level 10 successes per trial (Nb={Nb}, p_b={p_b:.3e})')
    plt.legend()
    plt.grid(True, alpha=0.3)
    plt.tight_layout()
    plt.savefig(outpath)
    plt.close()

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--trials', type=int, default=10000)
    parser.add_argument('--mu', type=float, default=0.02)
    parser.add_argument('--L', type=int, default=10)
    parser.add_argument('--Tb', type=float, default=30.0)
    parser.add_argument('--T', type=float, default=86400.0)
    parser.add_argument('--Nb', type=int, default=1000)
    parser.add_argument('--out', type=str, default='simulations/level10_hist.png')
    args = parser.parse_args()

    p_b = per_breeder_success_prob(args.mu, args.L, args.Tb, args.T)
    print(f'Per-breeder success probability within T={args.T}s: p_b = {p_b:.6e}')

    samples = simulate_trials(args.trials, args.Nb, p_b)
    prob_at_least_one = np.mean(samples > 0)
    print(f'Estimated probability at least one Level {args.L} in Nb={args.Nb}: {prob_at_least_one:.6f}')
    print(f'Sample mean successes per trial: {samples.mean():.6f}, std: {samples.std(ddof=1):.6f}')

    plot_histogram(samples, args.Nb, p_b, args.out)
    print(f'Histogram saved to: {args.out}')

if __name__ == '__main__':
    main()

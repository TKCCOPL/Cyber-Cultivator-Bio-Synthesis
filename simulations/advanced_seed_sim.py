#!/usr/bin/env python3
"""基因种子拼接仿真：模拟实际基因拼接公式，评估达到目标基因值的期望次数。

实际游戏公式：
    新值 = floor((父本 + 母本) / 2) + random(-2, -1, 0, +1, +2)
    新值 = clamp(新值, 1, 10)

使用方法：
    python simulations/advanced_seed_sim.py --trials 10000 --target 8
"""
import random
import argparse


def splice_gene(parent_a: int, parent_b: int) -> int:
    """模拟基因拼接公式"""
    base = (parent_a + parent_b) // 2
    delta = random.choice([-2, -1, 0, 1, 2])
    return max(1, min(10, base + delta))


def simulate_to_target(initial: int, target: int, max_attempts: int = 10000) -> int:
    """模拟从初始基因值达到目标值所需的拼接次数。

    策略：始终用当前最高值的两个种子拼接。
    返回尝试次数，超过 max_attempts 则返回 -1。
    """
    # 种子库：开始有两个相同初始值的种子
    seeds = [initial, initial]
    attempts = 0

    while attempts < max_attempts:
        # 找到当前最高基因值
        current_max = max(seeds)
        if current_max >= target:
            return attempts

        # 取两个最高值的种子拼接
        seeds.sort(reverse=True)
        a, b = seeds[0], seeds[1]
        child = splice_gene(a, b)

        # 将子代加入种子库，保留合理数量
        seeds.append(child)
        if len(seeds) > 20:
            seeds = sorted(seeds, reverse=True)[:10]

        attempts += 1

    return -1  # 未达到目标


def monte_carlo(trials: int, initial: int, target: int) -> dict:
    """蒙特卡洛仿真"""
    results = []
    failures = 0

    for _ in range(trials):
        n = simulate_to_target(initial, target)
        if n < 0:
            failures += 1
        else:
            results.append(n)

    if not results:
        return {
            'trials': trials,
            'successes': 0,
            'failures': failures,
            'success_rate': 0.0,
        }

    results.sort()
    avg = sum(results) / len(results)
    median = results[len(results) // 2]
    p90 = results[int(len(results) * 0.9)]
    p99 = results[int(len(results) * 0.99)]

    return {
        'trials': trials,
        'successes': len(results),
        'failures': failures,
        'success_rate': len(results) / trials,
        'avg_attempts': avg,
        'median_attempts': median,
        'p90_attempts': p90,
        'p99_attempts': p99,
        'min_attempts': results[0],
        'max_attempts': results[-1],
    }


def main():
    parser = argparse.ArgumentParser(description='基因种子拼接仿真')
    parser.add_argument('--trials', type=int, default=10000, help='仿真次数')
    parser.add_argument('--initial', type=int, default=3, help='初始基因值 (1-10)')
    parser.add_argument('--target', type=int, default=8, help='目标基因值 (1-10)')
    parser.add_argument('--max-attempts', type=int, default=10000, help='单次最大尝试次数')
    args = parser.parse_args()

    if not (1 <= args.initial <= 10 and 1 <= args.target <= 10):
        print("错误：基因值必须在 1-10 范围内")
        return

    print(f"参数：初始值={args.initial}, 目标={args.target}, 仿真次数={args.trials}")
    print(f"拼接公式：new = floor((A+B)/2) + random(-2,-1,0,+1,+2), clamp(1,10)")
    print()

    results = monte_carlo(args.trials, args.initial, args.target)

    print(f"成功率：{results['success_rate']:.2%} ({results['successes']}/{results['trials']})")
    if results['successes'] > 0:
        print(f"平均尝试次数：{results['avg_attempts']:.1f}")
        print(f"中位数：{results['median_attempts']}")
        print(f"P90：{results['p90_attempts']}")
        print(f"P99：{results['p99_attempts']}")
        print(f"范围：{results['min_attempts']} - {results['max_attempts']}")

    # 理论估算
    theoretical = 4 * (args.target - args.initial)
    print(f"\n理论估算（忽略边界）：≈ {theoretical} 次")


if __name__ == '__main__':
    main()

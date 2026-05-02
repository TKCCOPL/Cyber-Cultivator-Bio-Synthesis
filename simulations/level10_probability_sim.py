#!/usr/bin/env python3
"""计算在给定时间内，多台拼接机并行操作时达到目标基因值的概率分布。

使用实际游戏基因拼接公式：
    新值 = floor((父本 + 母本) / 2) + random(-2, -1, 0, +1, +2)
    新值 = clamp(新值, 1, 10)

使用方法：
    python simulations/level10_probability_sim.py --trials 1000 --target 10

可选依赖（用于绘图）：
    pip install numpy matplotlib
"""
import random
import math
import argparse


def splice_gene(parent_a: int, parent_b: int) -> int:
    """模拟基因拼接公式"""
    base = (parent_a + parent_b) // 2
    delta = random.choice([-2, -1, 0, 1, 2])
    return max(1, min(10, base + delta))


def simulate_single_machine(initial: int, target: int, attempts: int) -> bool:
    """模拟单台拼接机在指定尝试次数内是否达到目标。"""
    a, b = initial, initial
    for _ in range(attempts):
        child = splice_gene(a, b)
        if child >= target:
            return True
        # 策略：用最高值的两个种子继续
        a, b = max(a, b), child
    return False


def simulate_parallel(initial: int, target: int, attempts_per_machine: int, n_machines: int) -> int:
    """模拟 N 台并行拼接机，返回达到目标的机器数量。"""
    successes = 0
    for _ in range(n_machines):
        if simulate_single_machine(initial, target, attempts_per_machine):
            successes += 1
    return successes


def run_trials(trials: int, initial: int, target: int, attempts: int, n_machines: int):
    """运行多次仿真，统计结果。"""
    results = []
    for _ in range(trials):
        s = simulate_parallel(initial, target, attempts, n_machines)
        results.append(s)

    results.sort()
    total = len(results)
    avg = sum(results) / total
    median = results[total // 2]
    any_success = sum(1 for r in results if r > 0)
    all_success = sum(1 for r in results if r == n_machines)

    return {
        'trials': trials,
        'n_machines': n_machines,
        'attempts_per_machine': attempts,
        'avg_successes': avg,
        'median_successes': median,
        'prob_at_least_one': any_success / total,
        'prob_all_success': all_success / total,
        'results': results,
    }


def main():
    parser = argparse.ArgumentParser(description='并行拼接机达到目标基因值概率仿真')
    parser.add_argument('--trials', type=int, default=1000, help='仿真次数')
    parser.add_argument('--initial', type=int, default=3, help='初始基因值 (1-10)')
    parser.add_argument('--target', type=int, default=10, help='目标基因值 (1-10)')
    parser.add_argument('--attempts', type=int, default=100, help='每台机器的尝试次数')
    parser.add_argument('--machines', type=int, default=10, help='并行拼接机数量')
    parser.add_argument('--out', type=str, default=None, help='直方图输出路径（需要 numpy+matplotlib）')
    args = parser.parse_args()

    if not (1 <= args.initial <= 10 and 1 <= args.target <= 10):
        print("错误：基因值必须在 1-10 范围内")
        return

    print(f"参数：初始值={args.initial}, 目标={args.target}")
    print(f"      每台尝试次数={args.attempts}, 并行机器数={args.machines}, 仿真次数={args.trials}")
    print(f"拼接公式：new = floor((A+B)/2) + random(-2,-1,0,+1,+2), clamp(1,10)")
    print()

    stats = run_trials(args.trials, args.initial, args.target, args.attempts, args.machines)

    print(f"平均成功机器数：{stats['avg_successes']:.2f} / {args.machines}")
    print(f"中位数：{stats['median_successes']}")
    print(f"至少一台成功的概率：{stats['prob_at_least_one']:.4f}")
    print(f"全部成功的概率：{stats['prob_all_success']:.4f}")

    # 单台理论估算
    theoretical_attempts = 4 * (args.target - args.initial)
    print(f"\n单台理论估算（达到目标 ≈ {theoretical_attempts} 次）：")
    print(f"  每台 {args.attempts} 次尝试时，单台成功率 ≈ {min(1.0, args.attempts / max(1, theoretical_attempts)):.2%}")

    # 可选绘图
    if args.out:
        try:
            import numpy as np
            import matplotlib.pyplot as plt

            samples = np.array(stats['results'])
            plt.figure(figsize=(8, 5))
            plt.hist(samples, bins=range(args.machines + 2), density=True, alpha=0.6, color='C0')
            plt.xlabel('成功机器数')
            plt.ylabel('密度')
            plt.title(f'并行拼接机成功率分布 (target={args.target}, attempts={args.attempts})')
            plt.grid(True, alpha=0.3)
            plt.tight_layout()
            plt.savefig(args.out)
            plt.close()
            print(f"\n直方图已保存：{args.out}")
        except ImportError:
            print("\n提示：安装 numpy+matplotlib 可生成直方图：pip install numpy matplotlib")


if __name__ == '__main__':
    main()

<p align="center">
  <img src="docs/assets/cybercultivator-logo.png" alt="Cyber-Cultivator: Bio-Synthesis" width="100%">
</p>

<p align="center">
  <b>v1.1.7 · Minecraft Forge 1.20.1</b><br>
  遗传育种、自动化培养与生物强化血清
</p>

<p align="center">
  <a href="https://github.com/TKCCOPL/Cyber-Cultivator-Bio-Synthesis/actions/workflows/ci.yml">
    <img src="https://github.com/TKCCOPL/Cyber-Cultivator-Bio-Synthesis/actions/workflows/ci.yml/badge.svg" alt="CI">
  </a>
</p>

<p align="center">
  <a href="https://github.com/TKCCOPL/Cyber-Cultivator-Bio-Synthesis/releases/latest">下载最新版</a> ·
  <a href="docs/USER_GUIDE.md">完整使用指南</a> ·
  <a href="README_EN.md">English</a>
</p>

## 简介

Cyber-Cultivator: Bio-Synthesis 是一个围绕生物科技生产链构建的 Forge 模组。玩家可以培养携带基因数据的作物、拼接优良种子、自动化生产原料，并将其加工为具有不同效果与副作用的强化血清。

## 核心特色

- 三项种子基因与可遗传、可变异的育种系统
- 培养槽、基因拼接机、血清灌装机和大气冷凝器组成的可交互 GUI 自动化生产链
- 三类强化血清、Activity 品质缩放与叠加机制
- 红石控制（忽略/高/低电平）与统一比较器信号，接入红石自动化系统
- Forge `IItemHandler` 分面能力与跨模组材料标签，兼容 Create、Mekanism、AE2 等物流系统
- 可选 Curios 种子基因解析、自动注入和生命支持能力
- 可选 KubeJS 配方 DSL 与可热重载玩法事件
- 漏斗自动化及公开查询/事件 API

兼容 JEI，并以机器实际 GUI 展示培养、拼接、灌装与冷凝流程；KubeJS、JEI 与 Curios 均为可选依赖。

具体配方、机器操作、基因算法和血清数值请查看[完整使用指南](docs/USER_GUIDE.md)。

## 安装

| 组件 | 版本 | 要求 |
|------|------|------|
| Minecraft | 1.20.1 | 必需 |
| Forge | 47.4.18+ | 必需 |
| Curios API | 5.3.5+ | 可选；启用饰品功能 |
| KubeJS | 2001.6.5-build.16 至 build.26 | 可选；启用脚本配方与事件 |

1. 安装 Minecraft Forge 1.20.1。
2. 从 [Releases](https://github.com/TKCCOPL/Cyber-Cultivator-Bio-Synthesis/releases/latest) 下载模组 JAR，并放入 `mods` 目录。
3. 按需安装 Curios API 或 KubeJS 及其前置依赖。

## 文档

- [完整使用指南](docs/USER_GUIDE.md)
- [GitHub Releases](https://github.com/TKCCOPL/Cyber-Cultivator-Bio-Synthesis/releases)
- [问题反馈](https://github.com/TKCCOPL/Cyber-Cultivator-Bio-Synthesis/issues)

## 许可证

MIT License

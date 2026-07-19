<p align="center">
  <img src="docs/assets/cybercultivator-logo.png" alt="Cyber-Cultivator: Bio-Synthesis" width="100%">
</p>

<p align="center">
  <b>v1.1.7 · Minecraft Forge 1.20.1</b><br>
  Genetic breeding, automated cultivation, and bio-enhancement serums
</p>

<p align="center">
  <a href="https://github.com/TKCCOPL/Cyber-Cultivator-Bio-Synthesis/actions/workflows/ci.yml">
    <img src="https://github.com/TKCCOPL/Cyber-Cultivator-Bio-Synthesis/actions/workflows/ci.yml/badge.svg" alt="CI">
  </a>
</p>

<p align="center">
  <a href="https://github.com/TKCCOPL/Cyber-Cultivator-Bio-Synthesis/releases/latest">Download</a> ·
  <a href="docs/USER_GUIDE.md">Full Guide (Chinese)</a> ·
  <a href="README.md">中文</a>
</p>

## Overview

Cyber-Cultivator: Bio-Synthesis is a Forge mod built around a biotechnology production chain. Cultivate crops carrying genetic data, splice stronger seeds, automate material production, and process the results into enhancement serums with distinct benefits and side effects.

## Highlights

- Three inheritable seed genes with mutation-driven breeding
- An interactive GUI production chain using Incubators, Gene Splicers, Serum Bottlers, and Atmospheric Condensers
- Three enhancement serums with Activity-based quality scaling and stacking
- Redstone control (ignore/high/low) and unified comparator output for redstone automation
- Forge `IItemHandler` sided capabilities and cross-mod material tags, compatible with Create, Mekanism, AE2 logistics
- Optional Curios accessories for seed analysis, automatic injection, and life support
- Optional KubeJS recipe DSL and reloadable gameplay events
- Hopper automation and public query/event APIs

Compatible with JEI, with cultivation, splicing, bottling, and condensation shown through the machines' actual GUI layouts; KubeJS, JEI, and Curios are all optional.

For recipes, machine operation, breeding rules, and serum values, see the [full user guide](docs/USER_GUIDE.md) (Chinese).

## Installation

| Component | Version | Requirement |
|-----------|---------|-------------|
| Minecraft | 1.20.1 | Required |
| Forge | 47.4.18+ | Required |
| Curios API | 5.3.5+ | Optional; enables accessories |
| KubeJS | 2001.6.5-build.16 through build.26 | Optional; enables scripted recipes and events |

1. Install Minecraft Forge 1.20.1.
2. Download the mod JAR from [Releases](https://github.com/TKCCOPL/Cyber-Cultivator-Bio-Synthesis/releases/latest) and place it in the `mods` directory.
3. Install Curios API or KubeJS and its dependencies if desired.

## Documentation

- [Full user guide (Chinese)](docs/USER_GUIDE.md)
- [GitHub Releases](https://github.com/TKCCOPL/Cyber-Cultivator-Bio-Synthesis/releases)
- [Issue tracker](https://github.com/TKCCOPL/Cyber-Cultivator-Bio-Synthesis/issues)

## License

MIT License

<p align="center">
  <img src="src/main/resources/cybercultivator-logo.png" alt="Cyber-Cultivator: Bio-Synthesis" width="100%">
</p>

<p align="center">
  <b>v1.1.3</b> · Forge 1.20.1 · Curios API 5.3.5<br>
  Genetic Breeding Algorithm + Bio-Enhancement Serum System
</p>

<p align="center">
  <a href="#gameplay">Gameplay</a> ·
  <a href="#machines">Machines</a> ·
  <a href="#serums">Serums</a> ·
  <a href="#curios-accessories">Accessories</a> ·
  <a href="#changelog">Changelog</a> ·
  <a href="README.md">中文</a>
</p>

---

## Gameplay

> Mining → Growing → Incubation → Gene Splicing → Serum Synthesis → Self-Enhancement

Replace traditional farming with precision laboratory equipment. Every seed carries a unique genetic code, and every breeding attempt is a gamble.

---

## Resource System

<table>
<tr>
<td width="50%">

### Minerals

| Ore | Drop | Use |
|-----|------|-----|
| Silicon Crystal | Silicon Shard | Data Signal, Machine Crafting |
| Rare Earth | Rare Earth Powder | Precision Cores, Advanced Recipes |

</td>
<td width="50%">

### Crops

| Crop | Harvest | Use |
|------|---------|-----|
| Fiber Reed | Plant Fiber | Life Support Pack |
| Protein Soy | Biochemical Solution | Serum Base Solvent |
| Alcohol Bloom | Industrial Ethanol | S-03 Recipe |

</td>
</tr>
</table>

> Seeds: Fiber Reed drops from grass, Protein Soy / Alcohol Bloom from loot chests.

---

## Machines

### Atmospheric Condenser
Condenses purified water from air — 1 bottle every 30s, max stock 32. Place above an Incubator to auto-inject Purity. Hopper-compatible.

### Bio Incubator
Core farming block. Insert a gene seed and maintain three stats:

| Stat | Source | Effect |
|------|--------|--------|
| Nutrition | Biochemical Solution | Growth stops below threshold |
| Purity | Purified Water / Condenser | Affects crop quality |
| Data Signal | Silicon Shard | Required for advanced crops |

### Gene Splicer
Fuse two seeds to produce offspring. Each seed has three genes (1-10):
- **Speed** — Growth rate
- **Yield** — Harvest amount
- **Potency** — Serum quality

**Formula:** `new = floor((parentA + parentB) / 2) + random(-2 .. +2)`

### Serum Bottler
Processes Synaptic Neural Berries into advanced serums. Hopper-compatible, monocle HUD shows recipe / progress / activity.

---

## Serums

Breeding increases Potency → raw material quality NBT → berry activity → serum effect scaling.

```
Seed Potency → Material Quality → Bottler Berry (Activity 1-10) → Serum inherits Activity → Effect scales
```

| Serum | Effect | Duration | Side Effect |
|-------|--------|----------|-------------|
| **S-01 Synaptic Overclock** | Attack Speed + Strength + Resistance III | 25s | Wither + Hunger |
| **S-02 Visual Enhancement** | Night Vision + Glow 16-48 blocks + Fire Resistance III | 30s | Blindness + Hunger |
| **S-03 Metabolic Boost** | Regen + Speed + Jump Boost III | 15s | Slowness + Poison |

**Stacking:** Drinking again adds amplifier+1 (cap VIII), duration accumulates (cap 5 min). Activity ≥ 8 starts at level II.

---

## Curios Accessories

| Accessory | Slot | Function |
|-----------|------|----------|
| Spectral Monocle | Head | HUD: Incubator / Bottler / Condenser / Splicer status |
| Bio-Pulse Belt | Waist | Auto-scan nearby Incubators, inject stats from inventory |
| Life Support Pack | Back | Accelerate side-effect decay, auto-heal at low HP |

---

## Automation Pipeline

```
[Atmospheric Condenser] ── auto-inject Purity ──→ [Bio Incubator] ←── Belt auto-inject
                                                        │ crop matures
                                                        ↓
                                                 [Serum Bottler] ──→ Advanced Serum
```

Hopper connections: Condenser side extraction, Bottler top/side input, bottom output.

---

## Dependencies

| Dependency | Version | Required |
|------------|---------|----------|
| Minecraft Forge | 1.20.1 (47.4.18) | ✅ |
| Curios API | 5.3.5 | Optional (accessories disabled without) |
| JEI | 15.0.0+ | Optional (recipe viewer disabled without) |

---

## Changelog

<details>
<summary><b>v1.1.3</b> — Code Review Fixes + Hopper Interaction Optimization + API Improvements</summary>

- Fixed 12 issues found in third code review
- Bottler hopper extraction cancels processing + Condenser hopper behavior unified + output direction restriction
- tryInsertSeed copies ItemStack + cleanup dead code and unused imports
- Added cybercultivator module icon

</details>

<details>
<summary><b>v1.1.2</b> — Gene_Synergy Rename + Mutation Tag Upgrade + HUD Transparency</summary>

- Gene_Purity → Gene_Synergy to avoid confusion with ethanol Purity
- Mutation tag upgraded from boolean to integer type code, Tooltip/HUD shows mutation details
- HUD background changed to fully transparent, only progress bar backgrounds remain
- 3 BlockEntity sync mechanisms unified (empty tag sentinel + flags=2)

</details>

<details>
<summary><b>v1.1.1</b> — Serum Effect Rebalance</summary>

- S-01: Attack speed + strength scale with amplifier, added Resistance III
- S-02: Glow range 16-48 blocks scales with amplifier, added Fire Resistance III
- S-03: Added Speed + Jump Boost III, regen keeps amplifier scaling
- Side effects differentiated: S-01 Wither+Hunger, S-02 Blindness+Hunger, S-03 Slowness+Poison

</details>

<details>
<summary><b>v1.1.0</b> — Quality Chain + Serum Stacking + HUD Expansion</summary>

- Serum quality chain: material quality → berry activity → serum effect scaling
- Serum stacking: repeated drinking upgrades level (cap V)
- Bottler 4 recipes + Monocle HUD expansion
- Creative tab quality variants: 7 items × 10 quality levels

**Bug Fixes:** Milk CME crash, serum stacking early side-effect trigger, Bottler HUD progress bar stuck, Activity inheritance failure, Activity formula slot order inconsistency, Splicer Forge double-invoke, Condenser HUD progress bar stuck, en_us.json missing translations

</details>

---

## License

MIT License

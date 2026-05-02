# Cyber-Cultivator 贴图生成清单与标准

## 1. 统一贴图标准（给 AI 生成时固定使用）

### 1.1 基础规格
- 风格：Minecraft 原生风格兼容的 16x16 像素风，赛博实验室气质（高对比、轻霓虹点缀）。
- 分辨率：16x16（默认）。
- 放大稿：可让 AI 先出 512x512 或 1024x1024，再手工缩放到 16x16 并做像素修正。
- 文件格式：PNG（RGBA，非索引色）。
- 透明通道：
  - 物品与作物：必须支持透明背景。
  - 方块实心面：可不透明，但建议保留 RGBA 以便后续调整。
- 色彩建议：控制在 12-24 主色，避免照片质感和过度平滑渐变。
- 光照建议：左上高光、右下阴影，保持像素级硬边，不做真实光追质感。

### 1.2 命名与落盘规范
- 方块贴图目录：src/main/resources/assets/cybercultivator/textures/block
- 物品贴图目录：src/main/resources/assets/cybercultivator/textures/item
- 文件名：与模型中引用名保持一致（小写蛇形命名）。
- 示例：silicon_ore.png、synaptic_serum_s01.png。

### 1.3 AI 提示词通用模板
可按下面模板替换"主题描述"：
- Positive prompt：
  Minecraft mod item/block texture, 16x16 pixel art style, clean silhouette, high readability at small size, cyberpunk laboratory theme, limited palette, crisp pixel edges, no text, no watermark, transparent background
- Negative prompt：
  blurry, photorealistic, 3d render, text, watermark, logo, smooth shading, noise, over-detailed background

---

## 2. 贴图状态总览

### 2.1 方块贴图

#### 矿石方块（cube_all，单面贴图）

| 资源名 | 中文名 | 目标文件 | 状态 | 尺寸 |
|--------|--------|----------|------|------|
| silicon_ore | 硅晶矿 | textures/block/silicon_ore.png | ✓ 已有 | 16x16 |
| rare_earth_ore | 稀土矿 | textures/block/rare_earth_ore.png | ✓ 已有 | 16x16 |

#### 机器方块（cube，多面贴图）

每个机器方块有 2 张独立贴图 + 2 张统一贴图：`_top`（顶面）、`_front`（前面/north）、`machine_side.png`（侧面+后面，所有机器共用）、`machine_bottom.png`（底面，所有机器共用）。

| 资源名 | 中文名 | 贴图文件 | 状态 |
|--------|--------|----------|------|
| bio_incubator | 生物培养槽 | `_top` / `_front` | ⚠ 占位 |
| gene_splicer | 基因拼接机 | `_top` / `_front` | ⚠ 占位 |
| atmospheric_condenser | 大气冷凝器 | `_top` / `_front` | ⚠ 占位 |
| serum_bottler | 血清灌装机 | `_top` / `_front` | ⚠ 占位 |
| (统一侧面) | — | `machine_side.png` | ⚠ 占位 |
| (统一底面) | — | `machine_bottom.png` | ⚠ 占位 |

> 命名示例：`bio_incubator_top.png`、`bio_incubator_front.png`，侧面/底面统一为 `machine_side.png` / `machine_bottom.png`

#### 作物方块（crop，透明背景）

| 资源名 | 中文名 | 目标文件 | 状态 | 尺寸 |
|--------|--------|----------|------|------|
| fiber_reed_stage0 | 纤维草 | textures/block/fiber_reed_stage0.png | ⚠ 占位 | 16x16 |
| protein_soy_stage0 | 蛋白质豆 | textures/block/protein_soy_stage0.png | ⚠ 占位 | 16x16 |
| alcohol_bloom_stage0 | 酒精花 | textures/block/alcohol_bloom_stage0.png | ⚠ 占位 | 16x16 |

> 注：作物方块所有生长阶段（age 0-7）共用 stage0 贴图，由 datagen 生成 blockstate 映射。

### 2.2 物品贴图

| 资源名 | 中文名 | 目标文件 | 状态 | 尺寸 |
|--------|--------|----------|------|------|
| silicon_shard | 硅碎片 | textures/item/silicon_shard.png | ✓ 已有 | 32x32 |
| rare_earth_dust | 稀土粉末 | textures/item/rare_earth_dust.png | ✓ 已有 | 32x32 |
| plant_fiber | 植物纤维 | textures/item/plant_fiber.png | ✓ 已有 | 32x32 |
| biochemical_solution | 生化原液 | textures/item/biochemical_solution.png | ✓ 已有 | 32x32 |
| industrial_ethanol | 工业乙醇 | textures/item/industrial_ethanol.png | ✓ 已有 | 32x32 |
| purified_water_bottle | 纯净水瓶 | textures/item/purified_water_bottle.png | ✓ 已有 | 32x32 |
| fiber_reed_seeds | 纤维草种子 | textures/item/fiber_reed_seeds.png | ✓ 已有 | 16x16 |
| protein_soy_seeds | 蛋白质豆种子 | textures/item/protein_soy_seeds.png | ✓ 已有 | 16x16 |
| alcohol_bloom_seeds | 酒精花种子 | textures/item/alcohol_bloom_seeds.png | ✓ 已有 | 32x32 |
| spectrum_monocle | 光谱单片镜 | textures/item/spectrum_monocle.png | ✓ 已有 | 16x16 |
| synaptic_neural_berry | 突触神经莓 | textures/item/synaptic_neural_berry.png | ✓ 已有 | 16x16 |
| synaptic_serum_s01 | S-01 突触超频血清 | textures/item/synaptic_serum_s01.png | ✓ 已有 | 16x16 |
| synaptic_serum_s02 | S-02 视觉强化血清 | textures/item/synaptic_serum_s02.png | ✓ 已有 | 16x16 |
| synaptic_serum_s03 | S-03 代谢加速血清 | textures/item/synaptic_serum_s03.png | ✓ 已有 | 16x16 |
| bio_pulse_belt | 生化脉冲腰带 | textures/item/bio_pulse_belt.png | ⚠ 占位 | 16x16 |
| life_support_pack | 生命支持箱 | textures/item/life_support_pack.png | ⚠ 占位 | 16x16 |

---

## 3. 待修复清单

### 3.1 占位贴图需替换（15 张）

以下贴图为程序生成的占位色块，需替换为正式美术资源：

#### 机器方块多面贴图（10 张）

| 优先级 | 文件 | 中文名 | AI 关键词 |
|--------|------|--------|-----------|
| P1 | `block/bio_incubator_top.png` | 培养槽-顶 | 金属边框、中心玻璃观察口、淡绿色液体反光、实验室器皿顶部 |
| P1 | `block/bio_incubator_front.png` | 培养槽-前 | 玻璃观察窗、绿色发光液体、金属边框、培养槽正面 |
| P1 | `block/gene_splicer_top.png` | 拼接机-顶 | 双输入槽口、中心激光发射点、紫色光芒 |
| P1 | `block/gene_splicer_front.png` | 拼接机-前 | 显示面板、垂直激光束、紫色霓虹、科幻机器正面 |
| P1 | `block/atmospheric_condenser_top.png` | 冷凝器-顶 | 旋转风扇格栅、冷蓝色、工业散热器顶部 |
| P1 | `block/atmospheric_condenser_front.png` | 冷凝器-前 | 进气格栅、冷凝管排列、蓝色水汽灯带 |
| P1 | `block/serum_bottler_top.png` | 灌装机-顶 | 机械针头安装座、红色指示灯、灌装头 |
| P1 | `block/serum_bottler_front.png` | 灌装机-前 | 玻璃观察窗、红色指示灯、液路刻度线 |
| P1 | `block/machine_side.png` | 机器-统一侧面 | 深灰金属面板、边缘暗角、工业实验室设备通用侧面、赛博朋克金属质感 |
| P1 | `block/machine_bottom.png` | 机器-统一底面 | 深色金属底座、散热纹理、工业设备底部 |

#### 作物方块贴图（3 张）

| 优先级 | 文件 | 中文名 | AI 关键词 |
|--------|------|--------|-----------|
| P1 | `block/fiber_reed_stage0.png` | 纤维草 | 细长直立草束、翠绿到青绿渐层、边缘金属反光、玻璃纤维质感、3-4根细草、fiber reed sprout |
| P1 | `block/protein_soy_stage0.png` | 蛋白质豆 | 灰白色豆荚簇、方块化颗粒、结构紧凑、实验培育作物感、2-3个方形豆荚、protein soy pod |
| P1 | `block/alcohol_bloom_stage0.png` | 酒精花 | 半透明淡紫花瓣、亮红色花蕊、荧光边缘、花形清晰不糊、单朵小花、alcohol bloom flower |

#### 物品贴图（2 张）

| 优先级 | 文件 | 中文名 | AI 关键词 |
|--------|------|--------|-----------|
| P1 | `item/bio_pulse_belt.png` | 生化脉冲腰带 | 模块化金属腰带、蓝色能量匣、脉冲灯点、赛博朋克风格、cyberpunk belt with energy cell |
| P1 | `item/life_support_pack.png` | 生命支持箱 | 背部医疗包、管路接口、应急药剂仓、绿色十字标识、military medical backpack with tubes |

### 3.2 尺寸不一致（7 张）

以下物品贴图为 32x32，规范要求 16x16。Minecraft 会自动缩放但可能导致模糊：

| 文件 | 中文名 | 当前 → 目标 |
|------|--------|-------------|
| `item/silicon_shard.png` | 硅碎片 | 32x32 → 16x16 |
| `item/rare_earth_dust.png` | 稀土粉末 | 32x32 → 16x16 |
| `item/plant_fiber.png` | 植物纤维 | 32x32 → 16x16 |
| `item/biochemical_solution.png` | 生化原液 | 32x32 → 16x16 |
| `item/industrial_ethanol.png` | 工业乙醇 | 32x32 → 16x16 |
| `item/purified_water_bottle.png` | 纯净水瓶 | 32x32 → 16x16 |
| `item/alcohol_bloom_seeds.png` | 酒精花种子 | 32x32 → 16x16 |

---

## 4. 交付检查清单（生成后逐项验收）
- [ ] 尺寸是否为 16x16
- [ ] 文件名是否与模型引用一致
- [ ] 透明背景是否正确（RGBA 格式，非索引色）
- [ ] 在创造栏中缩略图是否可辨识（不糊、不脏、不撞色）
- [ ] 与同类原版物品并列时，亮度和对比是否不过分突兀
- [ ] 作物贴图 stage0 是否为透明背景（crop 模型需要）

---

## 5. 建议工作流
1. 优先替换 15 张占位贴图（P1，影响视觉品质）
2. 将 7 张 32x32 物品贴图缩放到 16x16
3. 放入对应目录后 `./gradlew runData && ./gradlew runClient` 实机查看
4. 最后做第二轮微调（轮廓、对比、色相统一）

---

## 6. Datagen 说明

所有方块状态（blockstates）、方块模型（block models）和物品模型（item models）均由 datagen 自动生成：

- `ModBlockStateProvider`：生成 9 个方块的 blockstate + block model + item model
- `ModItemModelProvider`：生成 16 个物品的 item model

运行 `./gradlew runData` 即可重新生成。生成文件位于 `src/generated/resources/assets/cybercultivator/`。

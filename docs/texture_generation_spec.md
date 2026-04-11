# Cyber-Cultivator 贴图生成清单与标准

## 1. 统一贴图标准（给 AI 生成时固定使用）

### 1.1 基础规格
- 风格：Minecraft 原生风格兼容的 16x16 像素风，赛博实验室气质（高对比、轻霓虹点缀）。
- 分辨率：16x16（默认）。
- 放大稿：可让 AI 先出 512x512 或 1024x1024，再手工缩放到 16x16 并做像素修正。
- 文件格式：PNG。
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
可按下面模板替换“主题描述”：
- Positive prompt：
  Minecraft mod item/block texture, 16x16 pixel art style, clean silhouette, high readability at small size, cyberpunk laboratory theme, limited palette, crisp pixel edges, no text, no watermark, transparent background
- Negative prompt：
  blurry, photorealistic, 3d render, text, watermark, logo, smooth shading, noise, over-detailed background

## 2. 当前代码已引用的贴图（必须制作）

以下清单基于当前模型与注册项整理，属于立即需要补齐的贴图。

### 2.1 方块贴图

| 资源名 | 目标文件 | 用途 | 特征描述（给 AI） |
|---|---|---|---|
| silicon_ore | textures/block/silicon_ore.png | 硅晶矿方块面 | 石质基底 + 青蓝色晶体脉络，晶体呈锐角碎片，冷色发光感但不过曝。 |
| rare_earth_ore | textures/block/rare_earth_ore.png | 稀土矿方块面 | 深灰岩体 + 暖金/橙红稀土斑块，颗粒感明显，工业矿物质感。 |
| fiber_reed_stage0 | textures/block/fiber_reed_stage0.png | 纤维草作物贴图 | 细长直立草束，翠绿到青绿渐层，边缘有轻微金属反光。 |
| protein_soy_stage0 | textures/block/protein_soy_stage0.png | 蛋白质豆作物贴图 | 灰白色豆荚簇，方块化颗粒，结构紧凑，偏实验培育作物感。 |
| alcohol_bloom_stage0 | textures/block/alcohol_bloom_stage0.png | 酒精花作物贴图 | 半透明淡紫花瓣 + 红色花蕊，荧光边缘，花形清晰不糊。 |
| bio_incubator | textures/block/bio_incubator.png | 生物培养槽方块面（当前 cube_all） | 金属框体 + 玻璃观察窗 + 蓝绿色液体舱视觉，实验设备风格。 |
| gene_splicer | textures/block/gene_splicer.png | 基因拼接机方块面（当前 cube_all） | 深色金属壳体 + 中央激光槽/能量线，红青对比的科幻仪器感。 |

说明：
- 当前 crop blockstates 都使用 stage0，可先做 stage0 单帧；后续若补生长阶段，再扩 stage1-stage7。
- bio_incubator 与 gene_splicer 现在是 cube_all，可先做单张通用面，后续再升级为多面贴图。

### 2.2 物品贴图

| 资源名 | 目标文件 | 用途 | 特征描述（给 AI） |
|---|---|---|---|
| silicon_shard | textures/item/silicon_shard.png | 硅碎片 | 不规则晶片，青蓝反光，半导体碎晶质感。 |
| rare_earth_dust | textures/item/rare_earth_dust.png | 稀土粉末 | 橙金色细粉堆，边缘散粒，工业化学原料感。 |
| plant_fiber | textures/item/plant_fiber.png | 植物纤维 | 浅绿色纤维束，细丝交织，轻微干燥纹理。 |
| biochemical_solution | textures/item/biochemical_solution.png | 生化原液 | 玻璃小瓶内荧光绿色液体，有液面和高光。 |
| industrial_ethanol | textures/item/industrial_ethanol.png | 工业乙醇 | 透明试剂瓶，浅蓝或无色液体，带警示条纹标签。 |
| fiber_reed_seeds | textures/item/fiber_reed_seeds.png | 纤维草种子 | 细长种粒，青绿色调，简洁轮廓。 |
| protein_soy_seeds | textures/item/protein_soy_seeds.png | 蛋白质豆种子 | 灰白豆粒 2-3 颗，方形感明显。 |
| alcohol_bloom_seeds | textures/item/alcohol_bloom_seeds.png | 酒精花种子 | 淡紫与红色点缀的小型种粒，偏花粉团。 |
| spectrum_monocle | textures/item/spectrum_monocle.png | 光谱单片镜 | 单目镜片 + 金属镜框 + 微型电路，镜片有蓝紫偏振反光。 |
| synaptic_neural_berry | textures/item/synaptic_neural_berry.png | 突触神经莓 | 荧光蓝果实，内部纹理像神经网络，边缘有微发光轮廓。 |
| synaptic_serum_s01 | textures/item/synaptic_serum_s01.png | S-01 血清 | 注射瓶/安瓿形态，蓝青色活性液体，带 S-01 标识条块但不写文字。 |

说明：
- silicon_ore、rare_earth_ore、bio_incubator、gene_splicer 的物品模型走 block parent，不单独需要 item 贴图。

## 3. 路线图预留贴图（建议提前生成）

以下资源尚未全部进入注册，但已在 Roadmap 中明确，建议提前做概念稿以加快 Phase 4-6。

### 3.1 设施类（Phase 4）
- atmospheric_condenser（大气冷凝器）
  - 建议文件：textures/block/atmospheric_condenser.png
  - 特征：顶部风扇、冷凝管、冷蓝色水汽灯带。
- serum_bottler（血清灌装机）
  - 建议文件：textures/block/serum_bottler.png
  - 特征：下压针头、卡扣底座、液路刻度线。

### 3.2 Curios 饰品（Phase 5）
- bio_pulse_belt（生化脉冲腰带）
  - 建议文件：textures/item/bio_pulse_belt.png
  - 特征：模块化腰带、能量匣、脉冲灯点。
- life_support_pack（生命支持箱）
  - 建议文件：textures/item/life_support_pack.png
  - 特征：背部医疗包、管路接口、应急药剂仓。

### 3.3 血清扩展（Phase 6 之前可先做）
- synaptic_serum_s02（视觉强化）
  - 建议文件：textures/item/synaptic_serum_s02.png
  - 特征：偏紫蓝光谱液体，镜片符号感。
- synaptic_serum_s03（代谢加速）
  - 建议文件：textures/item/synaptic_serum_s03.png
  - 特征：暖红橙活性液体，能量上涌感。

## 4. 交付检查清单（生成后逐项验收）
- 尺寸是否为 16x16。
- 文件名是否与模型引用一致。
- 透明背景是否正确。
- 在创造栏中缩略图是否可辨识（不糊、不脏、不撞色）。
- 与同类原版物品并列时，亮度和对比是否不过分突兀。

## 5. 建议工作流（你和 AI 协作）
1. 先批量出 11 个物品贴图概念图。
2. 再出 7 个方块贴图（含 3 个作物）。
3. 统一缩放到 16x16，手工做像素级清理。
4. 放入对应目录后 runClient 实机查看。
5. 最后做第二轮微调（轮廓、对比、色相统一）。

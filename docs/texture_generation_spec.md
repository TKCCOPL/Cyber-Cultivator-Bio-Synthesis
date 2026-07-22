# Cyber-Cultivator 贴图生成清单与标准

## 1. 统一贴图标准（给 AI 生成时固定使用）

### 1.1 基础规格
- 风格：Minecraft 原生风格兼容的 16x16 像素风，赛博实验室气质（高对比、轻霓虹点缀）。
- 分辨率：16x16（默认）。
- 放大稿：可让 AI 先出 512x512 或 1024x1024，再手工缩放到 16x16 并做像素修正。
- 文件格式：PNG。物品和作物使用 RGBA；完全不透明的机器面可使用 RGB 或索引色 PNG。
- 透明通道：
  - 物品与作物：必须支持透明背景。
  - 玻璃容器例外：允许在硬边轮廓内使用少量固定半透明 Alpha 表现可透视空腔与反光；不得使用平滑抗锯齿边缘。
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
| silicon_ore | 硅晶矿石 | textures/block/silicon_ore.png | ✓ 已优化 | 16x16 |
| rare_earth_ore | 稀土矿石 | textures/block/rare_earth_ore.png | ✓ 已优化 | 16x16 |
| deepslate_silicon_ore | 深板岩硅晶矿石 | textures/block/deepslate_silicon_ore.png | ✓ 已完成 | 16x16 |
| deepslate_rare_earth_ore | 深板岩稀土矿石 | textures/block/deepslate_rare_earth_ore.png | ✓ 已完成 | 16x16 |

#### 材料储存方块（cube_all，单面贴图）

| 资源名 | 中文名 | 目标文件 | 状态 | 尺寸 |
|--------|--------|----------|------|------|
| raw_silicon_block | 粗硅晶块 | textures/block/raw_silicon_block.png | ✓ 已完成 | 16x16 |
| raw_rare_earth_block | 粗稀土块 | textures/block/raw_rare_earth_block.png | ✓ 已完成 | 16x16 |
| silicon_block | 硅晶块 | textures/block/silicon_block.png | ✓ 已完成 | 16x16 |
| rare_earth_block | 稀土块 | textures/block/rare_earth_block.png | ✓ 已完成 | 16x16 |

#### 机器方块（cube，多面贴图）

每个机器方块有 2 张独立贴图 + 2 张统一贴图：`_top`（顶面）、`_front`（前面/north）、`machine_side.png`（侧面+后面，所有机器共用）、`machine_bottom.png`（底面，所有机器共用）。

| 资源名 | 中文名 | 贴图文件 | 状态 |
|--------|--------|----------|------|
| bio_incubator | 生物培养槽 | `_top` / `_front` | ✓ 已优化 |
| gene_splicer | 基因拼接机 | `_top` / `_front` | ✓ 已优化（蓝红横向 DNA 扭转梯） |
| atmospheric_condenser | 大气冷凝器 | `_top` / `_front` | ✓ 已优化 |
| serum_bottler | 血清灌装机 | `_top` / `_front` | ✓ 已优化 |
| (统一侧面) | — | `machine_side.png` | ✓ 已优化 |
| (统一底面) | — | `machine_bottom.png` | ✓ 已优化 |

> 命名示例：`bio_incubator_top.png`、`bio_incubator_front.png`，侧面/底面统一为 `machine_side.png` / `machine_bottom.png`

#### 作物方块（crop，透明背景）

| 资源名 | 中文名 | 目标文件 | 状态 | 尺寸 |
|--------|--------|----------|------|------|
| fiber_reed_stage0-stage3 | 纤维草 | textures/block/fiber_reed_stage0.png … stage3.png | ✓ 四阶段已优化 | 16x16 |
| protein_soy_stage0-stage3 | 蛋白质豆 | textures/block/protein_soy_stage0.png … stage3.png | ✓ 四阶段已优化 | 16x16 |
| alcohol_bloom_stage0-stage3 | 酒精花 | textures/block/alcohol_bloom_stage0.png … stage3.png | ✓ 四阶段已优化 | 16x16 |

> 注：作物 age 0-7 映射到四张贴图：0-1 → stage0、2-3 → stage1、4-5 → stage2、6-7 → stage3。

### 2.2 物品贴图

| 资源名 | 中文名 | 目标文件 | 状态 | 尺寸 |
|--------|--------|----------|------|------|
| raw_silicon_crystal | 粗硅晶矿 | textures/item/raw_silicon_crystal.png | ✓ 已完成 | 16x16 |
| raw_rare_earth | 粗稀土矿 | textures/item/raw_rare_earth.png | ✓ 已完成 | 16x16 |
| silicon_shard | 硅碎片 | textures/item/silicon_shard.png | ✓ 已优化 | 16x16 |
| rare_earth_dust | 稀土粉末 | textures/item/rare_earth_dust.png | ✓ 已优化 | 16x16 |
| plant_fiber | 植物纤维 | textures/item/plant_fiber.png | ✓ 已优化 | 16x16 |
| biochemical_solution | 生化原液 | textures/item/biochemical_solution.png | ✓ 已优化 | 16x16 |
| industrial_ethanol | 工业乙醇 | textures/item/industrial_ethanol.png | ✓ 已优化 | 16x16 |
| purified_water_bottle | 纯净水瓶 | textures/item/purified_water_bottle.png | ✓ 已优化 | 16x16 |
| fiber_reed_seeds | 纤维草种子 | textures/item/fiber_reed_seeds.png | ✓ 已优化 | 16x16 |
| protein_soy_seeds | 蛋白质豆种子 | textures/item/protein_soy_seeds.png | ✓ 已优化 | 16x16 |
| alcohol_bloom_seeds | 酒精花种子 | textures/item/alcohol_bloom_seeds.png | ✓ 已优化 | 16x16 |
| spectrum_monocle | 光谱单片镜 | textures/item/spectrum_monocle.png | ✓ 已优化 | 16x16 |
| synaptic_neural_berry | 突触神经莓 | textures/item/synaptic_neural_berry.png | ✓ 已优化 | 16x16 |
| synaptic_serum_s01 | S-01 突触超频血清 | textures/item/synaptic_serum_s01.png | ✓ 已优化 | 16x16 |
| synaptic_serum_s02 | S-02 视觉强化血清 | textures/item/synaptic_serum_s02.png | ✓ 已优化 | 16x16 |
| synaptic_serum_s03 | S-03 代谢加速血清 | textures/item/synaptic_serum_s03.png | ✓ 已优化 | 16x16 |
| bio_pulse_belt | 生化脉冲腰带 | textures/item/bio_pulse_belt.png | ✓ 已优化 | 16x16 |
| life_support_pack | 生命支持箱 | textures/item/life_support_pack.png | ✓ 已优化 | 16x16 |

---

## 3. 当前优化状态

截至 v1.1.6，已无已知占位贴图。本轮重新绘制 2 张矿石和 13 张旧物品贴图，统一为 16×16、硬边透明和有限色板；其中矿石、矿物与 Curios 饰品又根据原版贴图基线完成第二轮校正。机器方块贴图已在此前完成，不再列为占位。

- 矿石：基底使用原版石头的 4 个精确灰阶（`#686868`、`#747474`、`#7F7F7F`、`#8F8F8F`）及其 16×16 明暗分布；每面只保留 4 组约 3–6px 宽的非对称连片矿簇。矿物使用 5–6 色，整体 9–10 色，避免细碎散点、规则点阵和缩放噪点。
- 深板岩矿石：底材复用 Minecraft 1.20.1 客户端的 `minecraft:textures/block/deepslate.png` 深板岩材质，叠加现有硅晶/稀土矿脉色板，保持与普通矿石的矿簇形状一致；两张最终贴图均为 16×16、不透明 PNG。
- 材料储存块：粗块与精炼块使用不同表面语言。粗硅晶块采用 7 色青灰碎晶、短折线高光和非对称晶簇；粗稀土块采用 6 色橙褐矿团、短暗缝与少量淡金亮点。精炼硅晶块按用户指定的 Thermal 风格内嵌矿物块模板重配为 10 色青蓝—冰青色板，保留包边、内嵌晶面和斜向细高光；精炼稀土块按用户指定的 Modern Industrialization 风格烧结块模板重配为 8 色橙褐—琥珀色板，保留细颗粒面、斜向暗带和四角压痕。四者均为全不透明的原生 16×16 `cube_all` 贴图。
- 基础材料：粗硅晶矿和粗稀土矿按原版粗铁、粗铜、粗金的 16×16 多瓣紧凑轮廓设计，使用 2–4px 连片色块、左上高光和右下阴影，并分别继承硅晶与稀土色板；精炼硅碎片保留既有轮廓，仅将内部色块整理为连续晶面。植物纤维使用纤维草种子的同源青蓝—深青色板，以 14×14 多股交错束替代简化单枝。
- 瓶装材料：三种材料共用用户确认的圆形实验瓶模板；在原生 16×16 网格中使用 12×14 有效轮廓，固定保留灰白瓶塞、蓝色封带、透明上腔、左上玻璃高光、水平液面和灰色圆底，只以三档液体色区分生化原液、工业乙醇与纯净水。液面以上空腔使用 48 Alpha，次级折光 104，高光 192，玻璃边缘 208；液体和主轮廓保持 255。
- 血清：三支血清以 v1.0.0 最初稿的 9×14 非对称剪影为基准，不套用材料圆瓶；固定保留双段偏置瓶口、左侧缺口、弯曲瓶身和右侧环形管。液体使用七级阶梯渐变，分别保留黄—橙—红、青—蓝—紫、粉—洋红—深紫三组初始色系；每张统一为 12 色、0/255 硬边 Alpha。
- 突触神经莓：以用户确认稿为造型基准，转译为 13×14 的原生 16×16 像素图标；保留饱满的蓝色双瓣果形、青色高光与分叉枝叶，并使用 13 个实体色和全透明背景。
- Curios 装备：采用 11–13 色；单片镜使用 13×14 轮廓，保留单枚圆形紫蓝分面镜片、双层银灰机械框、青色光谱高光和带坠饰的右侧弧形垂链；腰带保留水平带体、扣件和生化模块；生命支持箱使用 14×14 刚性箱体，明确表现顶部提手、装甲外壳、金属包角、双侧锁扣及正面青色生命十字，不再使用软质背包和肩带造型。
- 机器方块：四台机器的金属外框、尺寸与朝向保持统一。培养槽正面保持既有培养液分层、反光与气泡构图不变；拼接机正面采用占满观察窗主体的蓝红横向 DNA“扭转梯”，由两条连续外链、四组长短递变的碱基横档和中央前后交叉组成，移除干扰轮廓的样本点；观察窗补充低亮冷暖反光和交叉遮挡阴影，底部状态灯以完整暗色行和 DNA 分离；冷凝器正面重排为上部封闭冷凝鳍片、中部滴水通道和下部储水槽；灌装机正面整理为顶部灌装头、瓶颈、瓶身液体与右侧状态灯。顶部风扇/功能板以及共用侧面、底面沿用上一轮逐像素细化结果。
- 可复现生成：运行 `node scripts/generate_core_item_textures.mjs` 重建本轮核心物品、矿石与材料储存块贴图。

---

## 4. 交付检查清单（生成后逐项验收）
- [ ] 尺寸是否为 16x16
- [ ] 文件名是否与模型引用一致
- [ ] 透明背景是否正确（物品/作物需 RGBA；不透明机器面可使用索引色）
- [ ] 在创造栏中缩略图是否可辨识（不糊、不脏、不撞色）
- [ ] 与同类原版物品并列时，亮度和对比是否不过分突兀
- [ ] 作物四阶段 stage0-stage3 是否均为透明背景（crop 模型需要）

---

## 5. 参考研究与工作流

本轮检查了 11 个同类模组的 1.20.1 发布资源，共扫描约 6800 张物品与方块贴图：

- 农业与自然：[Farmer's Delight](https://modrinth.com/mod/farmers-delight)、[Mystical Agriculture](https://modrinth.com/mod/mystical-agriculture)、[Botania](https://modrinth.com/mod/botania)
- 工业材料与矿石：[Mekanism](https://modrinth.com/mod/mekanism)、[Immersive Engineering](https://modrinth.com/mod/immersiveengineering)、[Applied Energistics 2](https://modrinth.com/mod/ae2)、[Modern Industrialization](https://modrinth.com/mod/modern-industrialization)、[Ad Astra](https://modrinth.com/mod/ad-astra)
- 装备与机械：[Create](https://modrinth.com/mod/create)、[PneumaticCraft: Repressurized](https://modrinth.com/mod/pneumaticcraft-repressurized)、[Industrial Foregoing](https://modrinth.com/mod/industrial-foregoing)

仅提炼共同构图规律，不复制任何参考像素。后续修改按以下顺序执行：运行确定性生成脚本、检查尺寸/色板/透明度、运行 `./gradlew build`，最后用 `./gradlew runClient` 在创造物品栏和快捷栏中实机查看。

---

## 6. Datagen 说明

所有方块状态（blockstates）、方块模型（block models）和物品模型（item models）均由 datagen 自动生成：

- `ModBlockStateProvider`：生成 11 个方块的 blockstate + block model + item model
- `ModItemModelProvider`：生成 16 个物品的 item model

运行 `./gradlew runData` 即可重新生成。生成文件位于 `src/generated/resources/assets/cybercultivator/`。

---

## 7. 机器 GUI 纹理

四台机器使用独立的 256×256 RGBA 图集，界面有效区域为左上角 194×210，其余区域保持透明：

| 文件 | 结构重点 |
|------|----------|
| `gui/bio_incubator.png` | 底部生长进度、位于对应物品槽左侧的 N/P/D 状态条、自动注入通道、右侧成熟资源输出、水槽下方玻璃瓶输出 |
| `gui/gene_splicer.png` | 对齐父本槽中心的上方汇流连接条、动态拼接脉冲、细进度条、独立箭头和子代输出 |
| `gui/serum_bottler.png` | 三路无序输入、液路合流、血清输出 |
| `gui/atmospheric_condenser.png` | 进气栅格、冷凝柱、纯净水库存 |

纹理采用 Industrial Foregoing 风格参考：透明未用区、灰色平面、1px 明暗倒角、原版物品栏槽位和少量功能色。动态进度、开关状态及按钮由 Screen 代码绘制，不烘焙进静态纹理。运行 `node scripts/generate_machine_gui_textures.mjs` 可确定性重建四张图集。

JEI 不维护另一套机器背景纹理。四类机器配方页面直接裁切对应图集的 `(8, 19)`、`178×95` 工作区，复用实际槽位坐标并由分类代码绘制进度动画与简要数据；修改机器布局时必须同步检查 Screen 与 JEI 分类。

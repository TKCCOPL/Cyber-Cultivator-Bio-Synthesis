---
name: "Cyber-Cultivator Machine Interface"
description: "A compact pixel interface system for readable, deterministic bio-industrial machine workflows in Minecraft Forge."
colors:
  transparent: "#00000000"
  chassis-outline: "#373737"
  chassis-shadow: "#555555"
  slot-bed: "#8B8B8B"
  chassis-panel: "#C6C6C6"
  chassis-highlight: "#EEEEEE"
  process-track: "#2B3031"
  growth-green: "#75BD4B"
  nutrition-lime: "#B7CF45"
  purity-cyan: "#49B8D1"
  signal-amber: "#DBB441"
  splice-magenta: "#B868B2"
  process-cyan: "#5DB9C7"
typography:
  title:
    fontFamily: "Minecraft Default, Unihex, sans-serif"
    fontSize: "9px"
    fontWeight: 400
    lineHeight: "9px"
    letterSpacing: "normal"
  body:
    fontFamily: "Minecraft Default, Unihex, sans-serif"
    fontSize: "9px"
    fontWeight: 400
    lineHeight: "9px"
    letterSpacing: "normal"
  label:
    fontFamily: "Minecraft Default, Unihex, sans-serif"
    fontSize: "9px"
    fontWeight: 400
    lineHeight: "9px"
    letterSpacing: "normal"
rounded:
  square: "0px"
spacing:
  hairline: "1px"
  micro: "2px"
  compact: "4px"
  control-gap: "6px"
  standard: "8px"
  content-inset: "12px"
  inventory-inset: "16px"
  slot-step: "18px"
components:
  machine-panel:
    backgroundColor: "{colors.chassis-panel}"
    textColor: "{colors.chassis-outline}"
    borderColor: "{colors.chassis-outline}"
    typography: "{typography.body}"
    rounded: "{rounded.square}"
    width: "194px"
    height: "210px"
  inventory-slot:
    backgroundColor: "{colors.slot-bed}"
    borderColor: "{colors.chassis-shadow}"
    rounded: "{rounded.square}"
    width: "18px"
    height: "18px"
  standard-action-button:
    textColor: "{colors.chassis-outline}"
    typography: "{typography.label}"
    rounded: "{rounded.square}"
    height: "16px"
  standard-progress:
    backgroundColor: "{colors.process-track}"
    textColor: "{colors.growth-green}"
    rounded: "{rounded.square}"
    width: "155px"
    height: "5px"
  thin-progress:
    backgroundColor: "{colors.process-track}"
    textColor: "{colors.splice-magenta}"
    rounded: "{rounded.square}"
    width: "27px"
    height: "3px"
  vertical-meter:
    backgroundColor: "{colors.process-track}"
    textColor: "{colors.process-cyan}"
    rounded: "{rounded.square}"
    width: "6px"
    height: "16px"
---

# Cyber-Cultivator Machine Interface

## 1. Overview

**Creative North Star: "The Bio-Industrial Lab Bench / 生物工业实验台"**

界面应像一套耐用、可读、可维护的生物工业实验设备，而不是悬浮全息屏或网页仪表盘。每台机器都必须一眼呈现“输入 → 处理 → 输出”的完整路径，同时保留 Minecraft 原生物品栏的操作习惯。

四台机器共享一张 256×256 RGBA 纹理图集，其中有效窗口为 194×210 px；未使用区域保持透明。机器工作区固定在 `(8, 19)`、尺寸 `178×95 px`，玩家物品栏标签从 `y=116` 开始，三行背包槽位位于 `y=128/146/164`，快捷栏位于 `y=186`。这些坐标是共同骨架，不应因机器主题不同而漂移。

界面的主要特征是紧凑、像素化、工业、临床、精确。静态纹理负责金属面板、槽位、管线和空轨道；Java Screen 负责文字、按钮、进度填充、状态变化和加工动画。所有动态状态都必须来自菜单同步数据，不能烘焙进背景纹理。

**Key Characteristics:**

- 194×210 px 紧凑机壳与 178×95 px 固定工作区。
- 硬边像素、1 px 斜角和原版 Minecraft 库存交互。
- 完整可见的输入、处理、输出与阻塞状态。
- 四台机器共享工业语言，各自保留独特流程图。
- 动画只说明真实处理，不承担唯一的信息表达。

## 2. Colors

灰阶金属机壳承担结构，少量高对比功能色承担资源与处理语义。

### Primary

- **Chassis Panel** (#C6C6C6): 机器窗口和工作区的主表面。
- **Chassis Outline** (#373737): 外轮廓、进度轨道边界和主要深色文字。
- **Chassis Highlight** (#EEEEEE): 面板左上侧 1 px 高光。
- **Chassis Shadow** (#555555): 面板右下侧 1 px 阴影和次要状态文字。

### Secondary

- **Growth Green** (#75BD4B): 培养槽生长进度。
- **Nutrition Lime** (#B7CF45): 培养槽 N 营养通道。
- **Purity Cyan** (#49B8D1): 培养槽 P/纯度通道。
- **Signal Amber** (#DBB441): 培养槽 D/信号通道和警示型参数。
- **Splice Magenta** (#B868B2): 基因拼接进度、汇流节点和活动脉冲。
- **Process Cyan** (#5DB9C7): 血清灌装与大气冷凝处理进度。

### Neutral

- **Slot Bed** (#8B8B8B): 原版尺度槽位的凹陷底色。
- **Process Track** (#2B3031): 所有未填充进度轨道。
- **Transparent** (#00000000): 256×256 图集中的未使用区域。

### Named Rules

**The Functional Accent Rule.** 普通面板、槽位和说明文字保持中性；强调色只进入资源标识、动态填充、连接脉冲或必要的状态文字。培养槽的 N/P/D 三通道是允许并列三种强调色的明确例外。

**The State Redundancy Rule.** 颜色不能单独表达状态。运行、等待、阻塞、已完成和不可操作状态还必须通过文字、填充长度、按钮启用状态、图形形状或动画是否播放来区分。

## 3. Typography

**Display Font:** Minecraft Default (with Unihex and sans-serif fallback)

**Body Font:** Minecraft Default (with Unihex and sans-serif fallback)

**Label/Mono Font:** Minecraft Default (with Unihex and sans-serif fallback)

**Character:** 像素字形与 Minecraft 原生界面保持一致；层级来自位置、内容、颜色与留白，而不是字号跳变、粗体堆叠或抗锯齿缩放。

### Hierarchy

- **Title** (400, 9 px, 9 px): 机器名称，只占一行并受窗口宽度限制。
- **Body** (400, 9 px, 9 px): 当前状态、配方、库存、结果和辅助说明，相邻信息行至少错开 10 px。
- **Label** (400, 9 px, normal): 槽位、资源通道和按钮标签，在控件高度内垂直居中。

信息层级按“机器名称 → 当前状态/关键结果 → 辅助指标 → 玩家物品栏”排列。基因拼接机以三行分别显示状态、代数与突变概率、子代均值或实际结果，不重复展示父本 A/B 的 S/Y/P 数值。

### Named Rules

**The Nine-Pixel Rule.** 单行界面文字按 9 px 高度规划；中文、英文和数字都必须在相同宽度约束下验证。

**The Fit-or-Truncate Rule.** 机器名称、状态、配方、库存、子代预估、代数和突变概率均使用可用宽度计算；超长内容以 `...` 截断并通过悬停提示提供完整文本。禁止让文字穿过按钮、箭头、进度条、槽位或工作区边框。

## 4. Elevation

本系统明确不使用投影。深度完全由硬边像素完成：外框和工作区采用 1 px 左上亮、右下暗斜角，槽位采用相反方向的凹陷斜角，进度轨道使用纯深色矩形。机器信息集中在 GUI 与复用同一工作区的 JEI 页面中，不在游戏画面上叠加机器 HUD。

### Named Rules

**The One-Pixel Bevel Rule.** 所有面板和槽位的高光、阴影与轮廓只占 1 px，不得放大为厚边框。按钮直接使用 Minecraft 原生 `Button` 渲染和禁用反馈，不在 GUI 纹理中复制静态按钮外观。

## 5. Components

### Machine Panel

- **Shape:** 194×210 px 方角窗口，工作区固定为 `(8, 19, 178, 95)`。
- **Color:** `chassis-panel` 表面，1 px `chassis-highlight`/`chassis-shadow` 斜角和 `chassis-outline` 外框。
- **Behavior:** 标题位于顶部，工作流程占上半区，玩家物品栏保持原生 9×3 加快捷栏布局。

### Inventory Slot

- **Shape:** 18×18 px 方角凹槽，相邻槽位步进 18 px。
- **Color:** `slot-bed` 底色和反向 1 px 压印边缘。
- **Behavior:** 保留原生点击、Shift 点击和物品提示；输入、容器和输出角色通过连线、箭头或邻近标签表达。

### Action Button

- **Shape:** 标准按钮使用翻译适配宽度和 16 px 高度。
- **Color:** 使用 Minecraft 原生按钮皮肤、悬停、焦点和禁用外观。
- **Behavior:** 运行条件由服务端状态决定，禁用时提供原因提示；自动加工流程不保留开始或注入按钮。

### Progress Indicator

- **Shape:** 标准水平轨道高 5 px、内部填充高 3 px；细轨道高 3 px、内部填充高 1 px；竖向资源条宽 6 px。
- **Color:** `process-track` 空轨道与机器语义强调色填充。
- **Behavior:** 填充向上取整并限制在轨道尺寸内。培养槽的大生长条保持原位，N/P/D 竖条分别位于对应物品槽左侧并对齐。

### Flow Connector

- **Shape:** 1–3 px 硬边管线，端点落在槽位中心线或明确输入/输出锚点上。
- **Color:** 静态连线使用中性灰；拼接脉冲使用 `splice-magenta`，灌装脉冲使用 `process-cyan`。
- **Behavior:** 基因拼接机的双父本连线汇入中央节点后通向子代输出；血清灌装机的三路输入先汇入上方总线，再下行进入与输出槽同中心线的灌装条。灌装时任一时刻只显示一个材料包，按输入支路上升、总线右移、灌装入口下降的顺序连续移动，三路输入轮流播放。脉冲仅在处理期间出现且不遮盖箭头、文字或进度条。

### Status Text Block

- **Shape:** 164 px 以内的 9 px 单行信息，每行独占至少 10 px 垂直区间。
- **Color:** 主要状态使用 `chassis-outline`，次要/等待状态使用 `chassis-shadow`，机器语义色只用于必要强调。
- **Behavior:** 依次显示当前状态、代数与突变概率等辅助信息、输出预估或结果；长文本截断并提供完整悬停提示。

### Machine Signatures

- **Bio Incubator:** N/P/D 数值与对应竖条、`growth-green` 大生长条，以及等待种子、缺失通道、培养/ETA、成熟阻塞和产物就绪状态。
- **Gene Splicer:** 双输入自动汇流、`splice-magenta` 脉冲，以及状态、代数/突变率、子代信息三行文字。
- **Serum Bottler:** 三输入汇流、单材料包 `process-cyan` 路径动画、灌装条与输出箭头，以及状态、产出/耗时、活性三行信息；全流程自动运行，无取消按钮。
- **Atmospheric Condenser:** 散热鳍片、`process-cyan` 生产扫描与竖向冷凝条、输出箭头，以及进度/剩余时间、库存/下游状态两行信息；底部保留下游注入与收取两个可逆按钮。

### Named Rules

**The Vanilla Inventory Rule.** 不改变玩家物品栏的槽位尺寸、排列、点击语义和提示行为；机器特有交互只能增加可理解的流程控制，不能隐藏或劫持原生库存操作。

**The Motion Means Work Rule.** 动画只在机器确实处理时播放；等待、阻塞和完成状态保持静止，并由文字说明原因。动画按游戏刻度推进，不使用持续装饰性闪烁。

## 6. Do's and Don'ts

### Do:

- **Do** 让每台机器在一个视野内展示输入、处理、输出和阻塞原因。
- **Do** 使用硬边像素、1 px 斜角和有限功能色保持 Industrial Foregoing 式的机械可读性。
- **Do** 让连接条、箭头、进度条和槽位共享中心线，并为文字预留独立区域。
- **Do** 对所有动态文字使用宽度测量、截断和完整悬停提示。
- **Do** 使用文字、形状、填充与按钮状态共同表达运行状态。
- **Do** 在中文和英文环境下检查标题、按钮、状态、代数、突变概率和配方信息。

### Don't:

- **Don't** 使用 glossy gradients、oversized decoration 或 excessive neon。
- **Don't** 使用 ambiguous icon-only controls，也不要让颜色成为唯一状态信号。
- **Don't** 隐藏 vanilla inventory behavior 或把自动处理拆成无意义的额外按钮。
- **Don't** imitate modern SaaS cards、网页仪表盘或悬浮玻璃面板。
- **Don't** 把动态填充和状态动画烘焙进静态 GUI 纹理。
- **Don't** 允许文字、按钮、箭头、连接条、进度图像或槽位彼此重叠。

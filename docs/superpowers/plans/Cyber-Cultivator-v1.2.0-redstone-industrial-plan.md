# Cyber-Cultivator v1.2.0 红石控制与工业兼容实施计划

> 审计基准：`TKCCOPL/Cyber-Cultivator-Bio-Synthesis`  
> 基准分支：`main`  
> 基准版本：`1.1.6`  
> 目标版本：`1.2.0`  
> 建议工作分支：`feature/v1.2.0-redstone-industrial`  
> 状态：审计后修订，可进入实施

---

## 1. 结论

本次改动不应作为 `1.1.7` 补丁版本发布，应升级为 `1.2.0`。

原因：

- 新增三种红石控制模式；
- 新增公开机器控制 API；
- 修改冷凝器、培养槽、灌装机的比较器语义；
- 为拼接机新增比较器；
- 新增 Forge `IItemHandler` 自动化能力；
- 开放培养槽种子自动输入；
- 新增跨模组材料标签和机器语义标签。

本计划拆成两个独立阶段实施：

1. **PR 1：红石控制与统一比较器**
2. **PR 2：工业自动化与标签兼容**

两个阶段分别通过测试门禁后再发布 `v1.2.0`。

---

# 第一阶段：红石控制与统一比较器

## 2. 目标范围

实现：

- `IGNORE / HIGH / LOW` 三种红石控制模式；
- 四台机器统一红石门控；
- 四台机器统一比较器语义；
- GUI 红石模式按钮；
- 公开机器控制 API；
- 旧存档兼容；
- GameTest 覆盖。

本版本不实现：

- `PULSE` 模式；
- 普通强/弱红石输出；
- FE 能源；
- 流体能力；
- 多方块结构。

---

## 3. 核心设计

### 3.1 红石模式

新增：

```java
public enum RedstoneControlMode {
    IGNORE("ignore"),
    HIGH("high"),
    LOW("low");

    private final String serializedName;
}
```

持久化必须使用稳定字符串，不使用 ordinal。

模式语义：

| 模式 | 运行条件 |
|---|---|
| `IGNORE` | 忽略红石输入 |
| `HIGH` | 有红石信号时允许加工 |
| `LOW` | 无红石信号时允许加工 |

### 3.2 持久化边界

只持久化：

```text
RedstoneMode
```

不持久化：

```text
RedstonePowered
lastComparatorSignal
processingAllowed
```

原因：这些字段都可以从当前世界和机器状态重新计算，写入 NBT 会产生陈旧状态。

旧存档中缺少 `RedstoneMode` 时默认：

```text
IGNORE
```

NBT 中出现未知字符串时：

```text
IGNORE + warn 日志
```

同一个区块实体加载周期内只记录一次警告，避免刷屏。

---

## 4. 共享架构

不要创建大型 `AbstractMachineBlockEntity`，四台机器加工逻辑差异过大。

新增轻量共享接口和控制组件：

```java
public interface RedstoneControlledMachine {
    RedstoneControlMode getRedstoneMode();

    void setRedstoneMode(RedstoneControlMode mode);

    boolean isRedstonePowered();

    boolean isProcessingAllowed();

    boolean hasActiveWork();

    boolean hasMainOutput();

    int getMachineProgress();

    int getMachineMaxProgress();

    int getComparatorSignal();

    void refreshRedstonePower();
}
```

新增组合组件：

```text
MachineRedstoneController
```

职责：

- 保存当前模式；
- 缓存当前供电状态；
- 计算 `processingAllowed`；
- 计算比较器输出；
- 保存和读取模式 NBT；
- 仅在状态变化时触发同步和比较器更新。

`MachineBlock` 负责统一转发：

```text
neighborChanged
    -> 获取方块实体
    -> 若实现 RedstoneControlledMachine
    -> 重新读取 level.hasNeighborSignal(pos)
```

方块实体 `onLoad()` 时必须重新采样当前红石状态。

---

## 5. 四台机器门控规则

### 5.1 大气冷凝器

红石只控制：

- 产水进度；
- 产水完成。

红石不控制：

- 自动向下输送；
- 手动暂停；
- 输出抽取。

最终加工许可：

```java
boolean canProduce =
        !paused
        && redstoneController.isProcessingAllowed()
        && output.getCount() < MAX_STACK;
```

手动暂停和红石许可是两道独立门。

### 5.2 生物培养槽

红石只控制：

- 生长进度推进；
- 成熟判定和成熟事件。

红石不控制：

- N/P/D 资源自动注入；
- 玻璃瓶返还；
- N/P/D 自然衰减；
- 输出抽取。

活动批次定义：

```text
已放入种子 = hasActiveWork
```

资源不足、红石禁止或输出阻塞时，批次仍然存在，只是暂时不推进。

### 5.3 基因拼接机

红石只控制：

- `spliceProgress` 推进；
- 最终拼接完成。

红石不控制：

- 亲本插入；
- 亲本取回；
- 输出抽取。

两个亲本就位后仍设置：

```text
splicing = true
spliceProgress = 0
```

若红石禁止，则保持该状态，恢复许可后继续。

### 5.4 血清灌装机

红石控制：

- 配方选取；
- 新批次开始；
- 当前进度推进；
- 批次完成。

红石不控制：

- 输入变更触发的旧加工取消；
- 配方重载导致的旧加工取消；
- 输出抽取；
- 库存同步。

禁止加工时不得新建 `cachedRecipe`。

已有批次失去红石许可时冻结：

```text
cachedRecipe 保留
progress 保留
maxProgress 保留
输入不消耗
```

---

## 6. 统一比较器语义

### 6.1 信号定义

优先级：

```text
主产物存在 -> 15
活动批次存在 -> 1..14
无活动批次且无主产物 -> 0
```

计算：

```java
public static int calculateComparatorSignal(
        boolean hasMainOutput,
        boolean hasActiveWork,
        int progress,
        int maxProgress
) {
    if (hasMainOutput) {
        return 15;
    }
    if (!hasActiveWork) {
        return 0;
    }
    if (maxProgress <= 0) {
        return 1;
    }

    long scaled = ((long) progress * 14L + maxProgress - 1L) / maxProgress;
    return Mth.clamp((int) scaled, 1, 14);
}
```

### 6.2 活动批次定义

| 机器 | `hasActiveWork()` |
|---|---|
| 冷凝器 | 未完成的生产周期存在，或机器处于可生产状态 |
| 培养槽 | 种子槽非空 |
| 拼接机 | `splicing == true` |
| 灌装机 | `maxProgress > 0` |

### 6.3 主产物

| 机器 | 主产物 |
|---|---|
| 冷凝器 | 纯净水瓶 |
| 培养槽 | 成熟培养产物 |
| 拼接机 | 子代种子 |
| 灌装机 | 灌装成品 |

培养槽玻璃瓶副产物不触发 15。

### 6.4 更新缓存

每个机器缓存：

```text
lastComparatorSignal
```

仅在新旧数值不同后调用：

```java
level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
```

该缓存不写入 NBT。

---

## 7. 比较器兼容性变化

升级说明必须同时包含：

### 冷凝器

旧行为：

```text
无产物 = 0
有产物 = 15
```

新行为：

```text
生产中 = 1..14
有产物 = 15
```

### 培养槽

旧行为：

```text
nutrition / 7
```

新行为：

```text
无批次 = 0
培养批次 = 1..14
成熟产物 = 15
```

### 拼接机

旧行为：

```text
无比较器输出
```

新行为：

```text
拼接中 = 1..14
子代待取 = 15
```

### 灌装机

旧行为：

- 加工开始时可能输出 0；
- 加工状态优先于已有产物。

新行为：

- 活动批次最低输出 1；
- 主产物 15 优先。

---

## 8. GUI 与菜单

### 8.1 不使用固定右上角 24 px 按钮

当前机器 GUI 宽度不足以在 `leftPos + 170` 放置完整文字按钮。

不得使用该坐标方案。

### 8.2 按钮设计

按钮文本：

```text
RS:忽略
RS:高电平
RS:低电平
```

英文：

```text
RS: Ignore
RS: High
RS: Low
```

按钮宽度建议：

```text
68–76 px
```

高度沿用当前 GUI：

```text
16 px
```

每个屏幕自行决定坐标，公共 `MachineScreen` 只提供：

```java
protected Button addRedstoneModeButton(...);
protected void updateRedstoneModeButton(...);
```

冷凝器因已有两个按钮，必须单独重新布局。

### 8.3 菜单按钮 ID

建议：

| 机器 | 红石按钮 ID |
|---|---:|
| 冷凝器 | `2` |
| 培养槽 | `0` |
| 拼接机 | `0` |
| 灌装机 | `0` |

四个菜单都必须实现 `clickMenuButton()`。

模式修改必须由服务端执行。

### 8.4 ContainerData

四个菜单追加同步：

```text
redstoneMode
redstonePowered
processingAllowed
```

不要只同步 `processingAllowed`，GUI tooltip 需要知道当前模式和当前输入电平。

### 8.5 状态优先级

状态行优先级建议：

1. 主产物待取；
2. 手动暂停；
3. 红石禁止；
4. 输出阻塞；
5. 资源不足；
6. 正常加工；
7. 等待输入。

红石状态示例：

```text
红石禁止：等待高电平
红石禁止：等待低电平
```

---

## 9. i18n

中文继续通过 `ModLangProvider` 生成。

英文继续手工维护：

```text
src/main/resources/assets/cybercultivator/lang/en_us.json
```

至少新增以下 key：

```text
gui.cybercultivator.redstone.ignore
gui.cybercultivator.redstone.high
gui.cybercultivator.redstone.low
gui.cybercultivator.redstone.powered
gui.cybercultivator.redstone.unpowered
gui.cybercultivator.redstone.tooltip
gui.cybercultivator.redstone.waiting_high
gui.cybercultivator.redstone.waiting_low
```

必要时增加机器专用状态键。

新增测试：

```text
zh_cn 与 en_us key 集合一致
```

---

## 10. 公开 API

新增：

```java
public record MachineControlInfo(
        RedstoneControlMode mode,
        boolean powered,
        boolean processingAllowed,
        int comparatorSignal
) {}
```

该 record 发布后不得直接增加组件字段。

未来需要脉冲状态时：

```text
新建 MachineControlInfoV2
或新增独立查询 API
```

不要修改现有 record 构造签名。

新增 API：

```java
@Nullable
public static MachineControlInfo getMachineControlInfo(
        Level level,
        BlockPos pos
);

public static boolean setMachineRedstoneMode(
        Level level,
        BlockPos pos,
        RedstoneControlMode mode
);
```

约束：

- `level == null`、`pos == null`、`mode == null` 返回失败；
- 目标不是受控机器时返回失败；
- 客户端调用返回失败；
- 仅允许服务端主线程写入；
- 使用和 GUI 相同的服务端修改入口；
- 修改后触发持久化、客户端同步和比较器刷新。

线程检查建议：

```java
if (!(level instanceof ServerLevel serverLevel)) {
    return false;
}
if (!serverLevel.getServer().isSameThread()) {
    return false;
}
```

现有状态 DTO 不修改构造签名。

---

## 11. 第一阶段测试

### 11.1 NBT

- 四台机器旧 NBT 缺少模式时默认 `IGNORE`；
- 未知模式字符串回退 `IGNORE`；
- `RedstonePowered` 不写入 NBT；
- 加载后重新从世界采样供电状态；
- 模式往返保存正确。

### 11.2 HIGH/LOW

四台机器分别覆盖：

- HIGH 无信号不运行；
- HIGH 有信号运行；
- LOW 无信号运行；
- LOW 有信号不运行；
- IGNORE 始终不受信号影响。

### 11.3 冻结恢复

- 冷凝器进度冻结并恢复；
- 培养槽生长冻结但资源注入和衰减继续；
- 拼接机冻结但不清空亲本；
- 灌装机冻结但不清空配方缓存和输入；
- 恢复后不重复消耗输入；
- 冻结期间不重复触发完成事件。

### 11.4 比较器

每台机器覆盖：

```text
0
1
14
15
```

额外覆盖：

- 拼接机 `splicing=true, progress=0` 输出 1；
- 灌装机有输出且仍有活动状态时输出 15；
- 培养槽玻璃瓶副产物不触发 15；
- 相邻比较器实际收到更新；
- 信号未变化时不重复通知邻居。

### 11.5 GUI/API

- 四个菜单按钮都能循环模式；
- 无效按钮 ID 返回 false；
- 客户端 API 写入返回 false；
- 非主线程 API 写入返回 false；
- 无效位置返回 null/false；
- API 和 GUI 修改结果一致。

---

# 第二阶段：工业自动化与标签兼容

## 12. 目标范围

实现：

- Forge `IItemHandler` capability；
- 拼接机 `WorldlyContainer`；
- 培养槽顶部种子自动输入；
- 统一槽位权限；
- 物品标签 provider；
- 材料标签和机器语义标签；
- 原版漏斗、GUI、能力行为一致；
- 跨模组 smoke profile 基础设施。

---

## 13. 当前代码边界

现状：

| 机器 | 当前容器 |
|---|---|
| 冷凝器 | `WorldlyContainer` |
| 培养槽 | `WorldlyContainer` |
| 拼接机 | `Container` |
| 灌装机 | `WorldlyContainer` |

因此本阶段不是从零增加漏斗兼容。

重点是：

- 给现有库存增加 capability 适配器；
- 让拼接机补齐分面容器；
- 抽取统一槽位策略；
- 避免 GUI、漏斗、管线行为分叉。

---

## 14. 不建立第二套库存

禁止为机器新建独立 `ItemStackHandler` 作为真实库存。

现有机器字段仍是唯一真实库存。

新增适配器：

```text
MachineInventoryPolicy
SidedMachineItemHandler
```

### 14.1 MachineInventoryPolicy

负责：

```java
int[] visibleSlots(@Nullable Direction side);

boolean canInsert(int slot, ItemStack stack, @Nullable Direction side);

boolean canExtract(int slot, ItemStack stack, @Nullable Direction side);

ItemStack normalizeInsertedStack(int slot, ItemStack stack);
```

### 14.2 SidedMachineItemHandler

要求：

- 包装现有机器库存；
- `simulate=true` 不修改任何状态；
- `getStackInSlot()` 返回 copy；
- 输出槽拒绝插入；
- 输入槽拒绝抽取；
- 保留全部 NBT；
- 实际修改调用机器统一事务入口；
- 修改后正确 `setChanged()` 和同步。

---

## 15. 分面矩阵

| 机器 | 顶部 | 水平面 | 底部 | 无方向 |
|---|---|---|---|---|
| 冷凝器 | 无槽位 | 输出 | 输出 | 输出 |
| 培养槽 | 种子输入 | N/P/D 输入 | 成熟产物、玻璃瓶输出 | 全部槽位，仍执行槽位权限 |
| 拼接机 | 两个亲本输入 | 两个亲本输入 | 子代输出 | 全部槽位，仍执行槽位权限 |
| 灌装机 | 三个材料输入 | 三个材料输入 | 成品输出 | 全部槽位，仍执行槽位权限 |

注意：

- 当前冷凝器允许所有非顶部方向抽取，新实现保持该行为；
- 不无故收紧已有自动化接口。

---

## 16. LazyOptional 生命周期

按角色缓存：

```text
INPUT
OUTPUT
UNSIDED
EMPTY
```

不必为六个方向分别创建完全相同的实例。

实现：

```java
@Override
public <T> LazyOptional<T> getCapability(
        Capability<T> cap,
        @Nullable Direction side
) {
    if (cap == ForgeCapabilities.ITEM_HANDLER) {
        return handlerFor(side).cast();
    }
    return super.getCapability(cap, side);
}
```

生命周期：

```java
invalidateCaps()
    -> invalidate 所有 LazyOptional
    -> super.invalidateCaps()

reviveCaps()
    -> super.reviveCaps()
    -> 重建 capability
```

---

## 17. 不增加生产环境线程拒绝

不要在每个 `IItemHandler` 方法中检查 Minecraft 主线程并静默返回 `EMPTY`。

原因：

- 这不是完整线程安全；
- 会造成自动化模组偶发传输失败；
- 难以诊断。

处理方式：

- capability 方法保持同步、短小、无异步任务；
- 所有修改仍通过方块实体统一入口；
- 开发环境可添加 debug 断言或日志；
- 公共 API 写入继续保持服务端主线程限制。

---

## 18. 机器事务规范

### 18.1 培养槽种子

自动插入必须：

- 只允许合法基因种子；
- 数量限制为 1；
- 补齐缺失基因 NBT；
- 更新基因缓存；
- 重置生长进度；
- 不在模拟插入时补 NBT 或修改缓存。

### 18.2 拼接机亲本

自动插入必须：

- 保持每槽最多 1；
- 补齐基因 NBT；
- 第二颗亲本进入后自动开始拼接；
- 输出存在时拒绝新亲本；
- 模拟插入不得启动拼接。

### 18.3 灌装机输入

输入变更必须：

- 标记 `inputsDirty`；
- 正在加工时取消旧配方；
- 不消耗输入；
- 模拟插入不得取消加工；
- 保持 `RecipeOrdering` 的确定性。

不要在 capability 层重新实现配方选择。

---

## 19. GUI、漏斗、能力统一验证

机器输入改为标签后，必须同步修改：

- 方块实体 `canPlaceItem()`；
- `canPlaceItemThroughFace()`；
- `MachineInventoryPolicy`；
- 菜单 `Slot.mayPlace()`；
- Shift-click 行为；
- 手动右键插入路径。

禁止出现：

```text
管线接受，但 GUI 拒绝
GUI 接受，但漏斗拒绝
模拟接受，但实际插入失败
```

---

## 20. 标签方案

### 20.1 新增类

```text
ModTags
ModItemTagProvider
```

在 `ModDataGenerators` 注册物品标签 provider。

生成目标：

```text
src/generated/resources
```

### 20.2 本模组语义标签

确定新增：

```text
cybercultivator:genetic_seeds
cybercultivator:machine_inputs/incubator/nutrition
cybercultivator:machine_inputs/incubator/purity
cybercultivator:machine_inputs/incubator/data_signal
```

默认只包含当前原物品。

整合包可通过数据包扩展。

### 20.3 材料标签

可发布：

```text
forge:silicon
forge:ores/silicon
forge:raw_materials/silicon
forge:storage_blocks/raw_silicon
forge:storage_blocks/silicon

forge:ores/rare_earth
forge:raw_materials/rare_earth
forge:dusts/rare_earth
forge:storage_blocks/raw_rare_earth
forge:storage_blocks/rare_earth
```

同时加入父标签：

```text
forge:ores
forge:raw_materials
forge:dusts
forge:storage_blocks
```

文档措辞必须是：

> 本模组发布这些材料标签，供其他模组和整合包复用。

不要宣称所有工业模组都会自动识别这些材料。

### 20.4 配方边界

本阶段不修改核心品质链配方为外部通用标签。

原因：

- 外部材料可能缺少品质 NBT；
- 可能绕过基因和品质链；
- 可能改变平衡。

机器输入语义标签只用于明确允许的数据包扩展。

---

## 21. 跨模组兼容声明

使用以下分级：

### 已验证

只有完成实际 smoke test 的模组才能写入：

```text
已验证兼容
```

### 标准能力预期兼容

未实测但使用标准 `IItemHandler` 的模组写为：

```text
标准能力预期兼容，未承诺专用功能
```

不得直接写“免费兼容”或“完全兼容”。

优先 smoke test：

1. Create；
2. Mekanism；
3. AE2；
4. Immersive Engineering。

---

## 22. Gradle smoke profile

当前依赖配置无法真正执行“无 Curios/JEI/KubeJS”启动测试。

应增加独立开关：

```text
enableCuriosRuntime
enableJeiRuntime
enableKubeJSRuntime
```

建议：

```groovy
def enableCuriosRuntime = providers.gradleProperty("enableCuriosRuntime")
        .map(String::toBoolean)
        .getOrElse(true)

def enableJeiRuntime = providers.gradleProperty("enableJeiRuntime")
        .map(String::toBoolean)
        .getOrElse(true)
```

根据开关决定是否加入 runtime dependency。

增加 smoke 任务或运行说明：

```text
./gradlew runGameTestServer \
  -PenableCuriosRuntime=false \
  -PenableJeiRuntime=false \
  -PenableKubeJSRuntime=false
```

KubeJS 最低/最高版本验证必须真正切换依赖版本，不能只在 `gradle.properties` 声明两个变量。

---

## 23. 第二阶段测试

### 23.1 分面 capability

每台机器验证：

- 顶部；
- 北/南/东/西；
- 底部；
- `side == null`。

验证：

- capability 存在性；
- 可见槽位；
- 合法插入；
- 非法插入；
- 合法抽取；
- 非法抽取。

### 23.2 模拟操作

`simulate=true` 后验证以下内容完全不变：

- 库存；
- 物品 NBT；
- 进度；
- 配方缓存；
- `inputsDirty`；
- 红石状态；
- 比较器；
- 客户端同步计数。

### 23.3 NBT

验证：

- 基因；
- Generation；
- Gene_Synergy；
- Mutation；
- MutationDetail；
- 品质；
- SynapticActivity。

插入和抽取往返后不得丢失。

### 23.4 生命周期

- `invalidateCaps()` 后旧 optional 不可再使用；
- `reviveCaps()` 后可重新查询；
- 方块移除后 capability 不再有效。

### 23.5 回归

- 原版漏斗行为；
- GUI Shift-click；
- 手动右键交互；
- 方块拆除掉落；
- 配方重载；
- 冷凝器自动向下输送；
- 培养槽资源定时注入；
- 拼接事务；
- 灌装输入变化取消加工。

### 23.6 既有测试修改

现有“培养槽种子不可由漏斗自动化”测试需要改为：

```text
顶部允许种子输入
水平面仍只允许 N/P/D 输入
底部只允许输出
```

该变化必须写入升级说明。

---

# 第三部分：工作流与发布

## 24. PR 拆分

### PR 1

标题建议：

```text
feat: add machine redstone control and comparator states
```

范围：

- 红石枚举；
- 控制组件和接口；
- 四机器门控；
- 统一比较器；
- GUI/menu；
- API；
- i18n；
- 红石 GameTests；
- 迁移文档。

### PR 2

标题建议：

```text
feat: add industrial item automation compatibility
```

范围：

- `IItemHandler`；
- 库存策略；
- 拼接机 `WorldlyContainer`；
- 培养槽顶部种子输入；
- 物品标签；
- smoke profile；
- 能力 GameTests；
- 工业兼容文档。

不要把两部分压成一个不可回退的大提交。

---

## 25. 构建门禁

每个 PR 必须执行：

```bash
./gradlew compileJava
./gradlew runData
./gradlew build
./gradlew runGameTestServer
```

PR 1 手工检查：

- 四个 GUI；
- 三种红石模式；
- 当前供电 tooltip；
- 红石冻结和恢复；
- 比较器信号；
- 冷凝器按钮布局。

PR 2 手工检查：

- 漏斗；
- Create/Mekanism/AE2/IE smoke；
- 各方向输入输出；
- 种子自动输入；
- NBT 保留；
- 无可选模组启动。

测试报告写入：

```text
docs/test-reports/v1.2.0-redstone.md
docs/test-reports/v1.2.0-industrial.md
```

---

## 26. 文档更新

更新：

```text
README.md
README_EN.md
docs/USER_GUIDE.md
CHANGELOG.md
```

新增：

```text
docs/design/redstone-control.md
docs/design/industrial-item-automation.md
```

升级说明必须明确：

- 冷凝器比较器语义变化；
- 培养槽比较器语义变化；
- 灌装机比较器优先级变化；
- 拼接机新增比较器；
- 培养槽顶部开放种子自动输入；
- 默认红石模式为 `IGNORE`；
- 旧存档无需 DataFixer。

---

## 27. PULSE 后续候选

`PULSE` 不进入 `v1.2.0`。

未来只有满足以下条件之一时再考虑：

- 玩家明确需要“一次红石脉冲加工一批”；
- 整合包作者需要多机器同步批次；
- 工厂测试证明 PULSE 有不可替代价值。

未来实现时：

- 在枚举末尾增加 `PULSE`；
- 不修改现有 `MachineControlInfo` record；
- 使用新 DTO 或独立 API 暴露脉冲状态；
- 单独设计授权消耗规则；
- 单独发布次版本。

---

# 28. Codex 执行约束

Codex 实施时必须遵守：

1. 先阅读四台机器现有事务逻辑，不复制实现。
2. 不建立第二套机器库存。
3. 不创建大型通用方块实体基类。
4. 红石门控只包围加工推进，不包围独立维护逻辑。
5. 所有输入权限使用统一策略。
6. 模拟能力调用不得有任何副作用。
7. 不把瞬时红石状态写入 NBT。
8. 不修改现有公开 DTO 构造签名。
9. 不在生产 capability 中静默拒绝非主线程调用。
10. 每完成一台机器先补 GameTest，再继续下一台。
11. `runGameTestServer` 必须确认实际执行测试数大于零。
12. 不在未实测时声明专用模组完全兼容。

---

# 29. 完成标准

只有同时满足以下条件才可发布 `v1.2.0`：

- 四台机器三种红石模式工作正确；
- 红石冻结恢复不复制或丢失物品；
- 四台机器比较器遵循统一语义；
- 旧存档默认 `IGNORE`；
- GUI 中英文无溢出；
- API 无破坏现有 DTO；
- 四台机器正确暴露分面 `IItemHandler`；
- 模拟插入/抽取完全无副作用；
- GUI、漏斗和 capability 权限一致；
- 所有 NBT 数据往返无损；
- 数据生成只产生预期标签和语言差异；
- `compileJava`、`build`、`runGameTestServer` 全绿；
- 手工测试和兼容矩阵记录完整。

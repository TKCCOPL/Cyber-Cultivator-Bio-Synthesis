# 经验教训库

> 由 cc-dev 自动追加，cc-orchestrator 定期精简。
> 粒度标准：有具体标准 + 适用于多个场景 ✅ | 只适用于一行代码 ❌ | 太笼统没有可操作性 ❌

## BlockEntity 相关

- [260503] 状态机周期性产出后必须清除输入状态，否则会导致无限循环 → BioIncubator 成熟后需同时重置 growthProgress 和 seed；通用规则：任何 tick 驱动的加工流程完成后，必须移除输入物品/标记，不能只重置进度计数器

- [260503] BlockEntity tick 中的进度值必须定期同步到客户端，否则 HUD 进度条无法动画 → `progress++` 后需添加 `if (progress % 20 == 0) { changed = true; }` 触发同步。通用规则：任何需要在客户端实时显示的 BlockEntity 状态（进度条、库存数量等），tick 中必须周期性调用 `setChanged()` + `syncToClient()`，不能只在状态变更完成时同步。已应用于：BioIncubatorBlockEntity、SerumBottlerBlockEntity、AtmosphericCondenserBlockEntity

- [260504] **BlockEntity 客户端同步完整链路** — 三个条件缺一不可：① `getUpdatePacket()` 返回 `ClientboundBlockEntityDataPacket.create(this)`；② `saveAdditional()` 写入的 tag **必须非空**（否则 packet 构造函数将 tag 设为 null，客户端 `onDataPacket` 收到 null 后跳过 `load()`）；③ 调用 `level.sendBlockUpdated(pos, state, state, 2)` 触发发送（flags=2 是 `Block.UPDATE_CLIENTS`）。**关键陷阱**：`ClientboundBlockEntityDataPacket` 构造函数中有 `this.tag = pTag.isEmpty() ? null : pTag`，当 `saveAdditional()` 只有条件写入 ItemStack（空时跳过）且无其他 `putInt/putString` 时，tag 为空 → packet tag = null → 客户端永远收不到更新。**修复**：空字段写入哨兵空 CompoundTag `new CompoundTag()` 确保 tag 非空。BioIncubator/SerumBottler/AtmosphericCondenser 不受影响（有 putInt 值），仅 GeneSplicer 纯靠 ItemStack 字段会触发此问题。已应用于：GeneSplicerBlockEntity

- [260504] `syncToClient()` 必须包含 `!level.isClientSide` 守卫 → 在客户端调用 `sendBlockUpdated()` 会导致冗余网络包甚至异常。统一模板：`if (level != null && !level.isClientSide) { level.sendBlockUpdated(...) }`。同时，WorldlyContainer 的 `removeItem()` 漏斗抽取路径也需要调用 `syncToClient()` 而非仅 `setChanged()`，否则 HUD 不会实时更新库存变化

## 注册表相关

## Curios 兼容

## 双端相关

- [260503] Forge 1.20.1 中 `Block.use()` 在单次右键时可能被服务器端调用两次（同一 tick 内） → 对于有多阶段交互的方块（如基因拼接机：先放种子A再放种子B），必须在 BlockEntity 中添加同 tick 防抖机制，使用 `level.getGameTime()` 记录上次交互 tick，同一 tick 内拒绝重复操作。单阶段交互方块（如培养槽）不受影响，因为第二次调用时状态已变更会自然跳过

- [260504] Forge 1.20.1 中 `Component.translatable()` 返回 `MutableComponent`，但赋值给 `Component` 变量后会丢失 `withStyle()` 方法 → 需要 `withStyle()` 时必须声明为 `net.minecraft.network.chat.MutableComponent` 类型，或将 `.withStyle()` 链式调用内联到 `translatable()` 返回值上

## 数据生成相关

## MobEffect 相关

- [260503] `removeAttributeModifiers` 中不能直接调用 `entity.addEffect()` → 当效果被 `curePotionEffects` 批量移除时，遍历 `activeEffects` HashMap 期间插入新效果会抛 `ConcurrentModificationException`。正确做法：用 `entity.level().getServer().tell(new TickTask(tickCount + 1, ...))` 延迟到下一 tick 再施加

- [260504] 需要随 amplifier 动态缩放的属性修饰符，不能用构造函数中的 `addAttributeModifier`（固定值）→ 正确做法：构造函数留空，在 `applyEffectTick` 中用 `removeModifier(uuid)` + `addTransientModifier(new)` 每 tick 刷新；`removeAttributeModifiers` 中通过 `attributeMap.getInstance(Attributes.XXX)` 清理 transient modifier。已应用于：SynapticOverclockEffect（攻速/移速）

- [260504] Forge 1.20.1 中 `MobEffectInstance` 没有 `getTag()` / `getOrCreateTag()` 方法（这些是 `ItemStack` 的 API），无法在效果实例上附加自定义 NBT 数据 → 如需在效果间传递元数据（如来源标识），不能用 amplifier 编码（会导致效果面板显示异常等级如 201/202/203）。正确做法：使用静态 `ConcurrentHashMap<UUID, Integer>` 存储元数据，施加前调用 `setSource()`，效果 tick 中读取 `getSource()`，效果结束时在 `removeAttributeModifiers` 中调用 `clearSource()` 清理。已应用于：NeuralOverloadEffect 来源感知（S-01/S-02/S-03）

## NBT 数据链传递

- [260504] 向多阶段数据管道（种子→培养槽→莓→血清）添加新 NBT 字段时，必须逐阶段检查所有中间产出点是否已传递该字段 → T22 中 Gene_Purity 在 getCropOutput() 中遗漏，导致下游 calculateActivity() 始终读到 0。通用规则：新增 NBT 字段后，用 grep 追踪该字段名在所有 ItemStack 创建点的出现情况，确保从源头到终点的完整传递链

## 重命名/重构相关

- [260504] 重命名 NBT 标签/常量时，必须用 `grep -rn` 全量扫描整个 `src/` 目录，不能只改任务明确列出的文件 → T26 重命名 Gene_Purity→Gene_Synergy 时，任务列了 5 个文件，但 grep 发现 ClientTooltipEvents、ModCreativeTabs、IncubatorHudOverlay 也引用了该符号，漏改会导致编译失败。通用规则：任何跨文件符号重命名，先 grep 全量收集引用点，再逐一修改

- [260504] 升级 NBT 标签类型（如 boolean→int）时，必须 grep 全量扫描 `putBoolean("TagName"` 和 `getBoolean("TagName"` → T28 任务只列了 5 个文件，但 ModCreativeTabs 中的创造栏预置物品也有 `putBoolean("Mutation", true)` 需要同步改为 `putInt`。通用规则：任何 NBT 标签类型变更，用 grep 收集所有写入点和读取点，确保全量替换

## 其他


# 经验教训库

> 由 cc-dev 自动追加，cc-orchestrator 定期精简。
> 粒度标准：有具体标准 + 适用于多个场景 ✅ | 只适用于一行代码 ❌ | 太笼统没有可操作性 ❌

## BlockEntity 相关

- [260503] 状态机周期性产出后必须清除输入状态，否则会导致无限循环 → BioIncubator 成熟后需同时重置 growthProgress 和 seed；通用规则：任何 tick 驱动的加工流程完成后，必须移除输入物品/标记，不能只重置进度计数器

- [260503] BlockEntity tick 中的进度值必须定期同步到客户端，否则 HUD 进度条无法动画 → `progress++` 后需添加 `if (progress % 20 == 0) { changed = true; }` 触发 `sendBlockUpdated()`。通用规则：任何需要在客户端实时显示的 BlockEntity 状态（进度条、库存数量等），tick 中必须周期性调用 `setChanged()` + `sendBlockUpdated()`，不能只在状态变更完成时同步。已应用于：BioIncubatorBlockEntity、SerumBottlerBlockEntity、AtmosphericCondenserBlockEntity

- [260504] `syncToClient()` 必须包含 `!level.isClientSide` 守卫 → 在客户端调用 `sendBlockUpdated()` 会导致冗余网络包甚至异常。统一模板：`if (level != null && !level.isClientSide) { level.sendBlockUpdated(...) }`。同时，WorldlyContainer 的 `removeItem()` 漏斗抽取路径也需要调用 `syncToClient()` 而非仅 `setChanged()`，否则 HUD 不会实时更新库存变化

## 注册表相关

## Curios 兼容

## 双端相关

- [260503] Forge 1.20.1 中 `Block.use()` 在单次右键时可能被服务器端调用两次（同一 tick 内） → 对于有多阶段交互的方块（如基因拼接机：先放种子A再放种子B），必须在 BlockEntity 中添加同 tick 防抖机制，使用 `level.getGameTime()` 记录上次交互 tick，同一 tick 内拒绝重复操作。单阶段交互方块（如培养槽）不受影响，因为第二次调用时状态已变更会自然跳过

## 数据生成相关

## MobEffect 相关

- [260503] `removeAttributeModifiers` 中不能直接调用 `entity.addEffect()` → 当效果被 `curePotionEffects` 批量移除时，遍历 `activeEffects` HashMap 期间插入新效果会抛 `ConcurrentModificationException`。正确做法：用 `entity.level().getServer().tell(new TickTask(tickCount + 1, ...))` 延迟到下一 tick 再施加

- [260504] 需要随 amplifier 动态缩放的属性修饰符，不能用构造函数中的 `addAttributeModifier`（固定值）→ 正确做法：构造函数留空，在 `applyEffectTick` 中用 `removeModifier(uuid)` + `addTransientModifier(new)` 每 tick 刷新；`removeAttributeModifiers` 中通过 `attributeMap.getInstance(Attributes.XXX)` 清理 transient modifier。已应用于：SynapticOverclockEffect（攻速/移速）

- [260504] Forge 1.20.1 中 `MobEffectInstance` 没有 `getTag()` / `getOrCreateTag()` 方法（这些是 `ItemStack` 的 API），无法在效果实例上附加自定义 NBT 数据 → 如需在效果间传递元数据（如来源标识），不能用 amplifier 编码（会导致效果面板显示异常等级如 201/202/203）。正确做法：使用静态 `ConcurrentHashMap<UUID, Integer>` 存储元数据，施加前调用 `setSource()`，效果 tick 中读取 `getSource()`，效果结束时在 `removeAttributeModifiers` 中调用 `clearSource()` 清理。已应用于：NeuralOverloadEffect 来源感知（S-01/S-02/S-03）

## NBT 数据链传递

- [260504] 向多阶段数据管道（种子→培养槽→莓→血清）添加新 NBT 字段时，必须逐阶段检查所有中间产出点是否已传递该字段 → T22 中 Gene_Purity 在 getCropOutput() 中遗漏，导致下游 calculateActivity() 始终读到 0。通用规则：新增 NBT 字段后，用 grep 追踪该字段名在所有 ItemStack 创建点的出现情况，确保从源头到终点的完整传递链

## 其他


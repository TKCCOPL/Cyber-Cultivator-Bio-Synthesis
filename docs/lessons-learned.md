# 经验教训库

> 由 cc-dev 自动追加，cc-orchestrator 定期精简。
> 粒度标准：有具体标准 + 适用于多个场景 ✅ | 只适用于一行代码 ❌ | 太笼统没有可操作性 ❌

## BlockEntity 相关

- [260503] 状态机周期性产出后必须清除输入状态，否则会导致无限循环 → BioIncubator 成熟后需同时重置 growthProgress 和 seed；通用规则：任何 tick 驱动的加工流程完成后，必须移除输入物品/标记，不能只重置进度计数器

- [260503] BlockEntity tick 中的进度值必须定期同步到客户端，否则 HUD 进度条无法动画 → `progress++` 后需添加 `if (progress % 20 == 0) { changed = true; }` 触发 `sendBlockUpdated()`。通用规则：任何需要在客户端实时显示的 BlockEntity 状态（进度条、库存数量等），tick 中必须周期性调用 `setChanged()` + `sendBlockUpdated()`，不能只在状态变更完成时同步。已应用于：BioIncubatorBlockEntity、SerumBottlerBlockEntity、AtmosphericCondenserBlockEntity

## 注册表相关

## Curios 兼容

## 双端相关

## 数据生成相关

## MobEffect 相关

- [260503] `removeAttributeModifiers` 中不能直接调用 `entity.addEffect()` → 当效果被 `curePotionEffects` 批量移除时，遍历 `activeEffects` HashMap 期间插入新效果会抛 `ConcurrentModificationException`。正确做法：用 `entity.level().getServer().tell(new TickTask(tickCount + 1, ...))` 延迟到下一 tick 再施加

## 其他


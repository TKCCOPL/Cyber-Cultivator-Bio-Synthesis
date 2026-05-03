# 血清品质链路 + 升级机制 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 打通"种植 → 原料品质 → 莓品质 → 血清品质"的完整数值链路，引入血清叠加升级机制（最高 V 级）。

**Architecture:** 原料品质通过 NBT 标签在培养槽产出时写入，灌装机合成时读取并计算突触活性(Activity)，血清饮用时按 Activity 缩放效果并支持叠加升级。每种效果独立叠加，amplifier 上限 4（V 级）。

**Tech Stack:** Minecraft Forge 1.20.1, Java 17, DeferredRegister, NBT CompoundTag

---

## 文件结构

| 文件 | 操作 | 职责 |
|------|------|------|
| `block/entity/BioIncubatorBlockEntity.java:105-120` | 修改 | `getCropOutput()` 写入品质 NBT |
| `block/entity/SerumBottlerBlockEntity.java` | 修改 | 扩展 4 种配方、品质读写、Activity 计算 |
| `item/SynapticSerumItem.java` | 修改 | 饮用叠加逻辑、Activity 缩放、Tooltip |
| `effect/SynapticOverclockEffect.java:36-39` | 修改 | NeuralOverload 按 amplifier 缩放 |
| `effect/VisualEnhancementEffect.java:40-45` | 修改 | 同上 |
| `effect/MetabolicBoostEffect.java:29-34` | 修改 | 同上 |
| `datagen/ModRecipeProvider.java:46-59` | 修改 | 移除合成台莓/S-01 配方（迁移到灌装机） |
| `datagen/ModLangProvider.java` | 修改 | 新增翻译键 |

---

### Task 1: 培养槽产出写入品质 NBT

**Files:**
- Modify: `src/main/java/com/TKCCOPL/block/entity/BioIncubatorBlockEntity.java:105-120`

**目标：** 成熟作物产出时，将种子的 Potency 基因值写入对应原料的 NBT。

- [ ] **Step 1: 修改 getCropOutput 方法**

在 `BioIncubatorBlockEntity.java` 的 `getCropOutput` 方法中，在创建 ItemStack 之后写入品质 NBT。

当前代码（第 105-120 行）：
```java
private static ItemStack getCropOutput(ItemStack seed) {
    Item seedItem = seed.getItem();
    int geneYield = GeneticSeedItem.getGene(seed, GeneticSeedItem.GENE_YIELD);
    int count = 2 + geneYield / 3;

    if (seedItem == ModItems.FIBER_REED_SEEDS.get()) {
        return new ItemStack(ModItems.PLANT_FIBER.get(), count);
    } else if (seedItem == ModItems.PROTEIN_SOY_SEEDS.get()) {
        return new ItemStack(ModItems.BIOCHEMICAL_SOLUTION.get(), count);
    } else if (seedItem == ModItems.ALCOHOL_BLOOM_SEEDS.get()) {
        return new ItemStack(ModItems.INDUSTRIAL_ETHANOL.get(), count);
    }

    return seed.copy();
}
```

替换为：
```java
private static ItemStack getCropOutput(ItemStack seed) {
    Item seedItem = seed.getItem();
    int geneYield = GeneticSeedItem.getGene(seed, GeneticSeedItem.GENE_YIELD);
    int genePotency = GeneticSeedItem.getGene(seed, GeneticSeedItem.GENE_POTENCY);
    int count = 2 + geneYield / 3;

    ItemStack output;
    if (seedItem == ModItems.FIBER_REED_SEEDS.get()) {
        output = new ItemStack(ModItems.PLANT_FIBER.get(), count);
        output.getOrCreateTag().putInt("Potency", genePotency);
    } else if (seedItem == ModItems.PROTEIN_SOY_SEEDS.get()) {
        output = new ItemStack(ModItems.BIOCHEMICAL_SOLUTION.get(), count);
        output.getOrCreateTag().putInt("Concentration", genePotency);
    } else if (seedItem == ModItems.ALCOHOL_BLOOM_SEEDS.get()) {
        output = new ItemStack(ModItems.INDUSTRIAL_ETHANOL.get(), count);
        output.getOrCreateTag().putInt("Purity", genePotency);
    } else {
        output = seed.copy();
    }

    return output;
}
```

- [ ] **Step 2: 编译验证**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/TKCCOPL/block/entity/BioIncubatorBlockEntity.java
git commit -m "feat: 培养槽产出写入品质 NBT (Potency/Purity/Concentration)"
```

---

### Task 2: 灌装机配方扩展 — 莓合成 + Activity 公式

**Files:**
- Modify: `src/main/java/com/TKCCOPL/block/entity/SerumBottlerBlockEntity.java`

**目标：** 灌装机支持莓合成配方（植物纤维+工业乙醇+生化原液→莓），读取输入品质 NBT 计算 Activity 并写入产出。

- [ ] **Step 1: 添加 Activity 常量和工具方法**

在 `SerumBottlerBlockEntity.java` 类的顶部常量区域（`OUTPUT_SLOT` 之后）添加：

```java
private static final String TAG_ACTIVITY = "SynapticActivity";

/**
 * 计算突触活性：加权平均 (Potency×0.25 + Purity×0.375 + Concentration×0.375)
 */
public static int calculateActivity(ItemStack fiber, ItemStack ethanol, ItemStack solution) {
    int potency = fiber.getOrCreateTag().getInt("Potency");
    int purity = ethanol.getOrCreateTag().getInt("Purity");
    int concentration = solution.getOrCreateTag().getInt("Concentration");
    // 默认值 5（无 NBT 时的保底）
    if (potency == 0) potency = 5;
    if (purity == 0) purity = 5;
    if (concentration == 0) concentration = 5;
    double raw = potency * 0.25 + purity * 0.375 + concentration * 0.375;
    return Math.max(1, Math.min(10, (int) Math.round(raw)));
}

/**
 * 从 ItemStack 读取 SynapticActivity，无 NBT 时返回 5（平衡点）
 */
public static int getActivity(ItemStack stack) {
    CompoundTag tag = stack.getTag();
    if (tag == null || !tag.contains(TAG_ACTIVITY)) return 5;
    return Math.max(1, Math.min(10, tag.getInt(TAG_ACTIVITY)));
}
```

- [ ] **Step 2: 扩展 matchRecipe 支持莓配方**

在 `matchRecipe()` 方法中，在现有 S-02 检查之前添加莓配方匹配。将方法签名中的返回值含义扩展为：0=莓, 1=S-01, 2=S-02, 3=S-03。

替换整个 `matchRecipe()` 方法：
```java
/**
 * @return recipe index (0=berry, 1=S-01, 2=S-02, 3=S-03), or -1 if no match
 */
private int matchRecipe() {
    // Berry: plant_fiber + industrial_ethanol + biochemical_solution → synaptic_neural_berry
    if (hasIngredients(ModItems.PLANT_FIBER.get(), ModItems.INDUSTRIAL_ETHANOL.get(), ModItems.BIOCHEMICAL_SOLUTION.get())
            && canOutput(ModItems.SYNAPTIC_NEURAL_BERRY.get())) {
        return 0;
    }
    // S-01: synaptic_neural_berry + biochemical_solution + glass_bottle
    if (hasIngredients(ModItems.SYNAPTIC_NEURAL_BERRY.get(), ModItems.BIOCHEMICAL_SOLUTION.get(), Items.GLASS_BOTTLE)
            && canOutput(ModItems.SYNAPTIC_SERUM_S01.get())) {
        return 1;
    }
    // S-02: synaptic_neural_berry + rare_earth_dust + glass_bottle
    if (hasIngredients(ModItems.SYNAPTIC_NEURAL_BERRY.get(), ModItems.RARE_EARTH_DUST.get(), Items.GLASS_BOTTLE)
            && canOutput(ModItems.SYNAPTIC_SERUM_S02.get())) {
        return 2;
    }
    // S-03: synaptic_neural_berry + industrial_ethanol + glass_bottle
    if (hasIngredients(ModItems.SYNAPTIC_NEURAL_BERRY.get(), ModItems.INDUSTRIAL_ETHANOL.get(), Items.GLASS_BOTTLE)
            && canOutput(ModItems.SYNAPTIC_SERUM_S03.get())) {
        return 3;
    }
    return -1;
}
```

- [ ] **Step 3: 扩展 consumeInputs 支持 4 种配方**

替换整个 `consumeInputs` 方法：
```java
private void consumeInputs(int recipe) {
    net.minecraft.world.item.Item[] required = switch (recipe) {
        case 0 -> new net.minecraft.world.item.Item[]{ModItems.PLANT_FIBER.get(), ModItems.INDUSTRIAL_ETHANOL.get(), ModItems.BIOCHEMICAL_SOLUTION.get()};
        case 1 -> new net.minecraft.world.item.Item[]{ModItems.SYNAPTIC_NEURAL_BERRY.get(), ModItems.BIOCHEMICAL_SOLUTION.get(), Items.GLASS_BOTTLE};
        case 2 -> new net.minecraft.world.item.Item[]{ModItems.SYNAPTIC_NEURAL_BERRY.get(), ModItems.RARE_EARTH_DUST.get(), Items.GLASS_BOTTLE};
        case 3 -> new net.minecraft.world.item.Item[]{ModItems.SYNAPTIC_NEURAL_BERRY.get(), ModItems.INDUSTRIAL_ETHANOL.get(), Items.GLASS_BOTTLE};
        default -> new net.minecraft.world.item.Item[0];
    };

    for (net.minecraft.world.item.Item item : required) {
        for (int i = 0; i < INPUT_SLOTS; i++) {
            if (inputs[i].is(item)) {
                inputs[i].shrink(1);
                break;
            }
        }
    }
}
```

- [ ] **Step 4: 扩展 getRecipeOutput 写入 Activity**

替换整个 `getRecipeOutput` 方法，使莓和血清都携带 Activity NBT：
```java
private ItemStack getRecipeOutput(int recipe) {
    return switch (recipe) {
        case 0 -> {
            // Berry: calculate Activity from input qualities
            int activity = calculateActivity(inputs[0], inputs[1], inputs[2]);
            ItemStack berry = new ItemStack(ModItems.SYNAPTIC_NEURAL_BERRY.get());
            berry.getOrCreateTag().putInt(TAG_ACTIVITY, activity);
            yield berry;
        }
        case 1 -> {
            // S-01: inherit Activity from berry input
            ItemStack berry = findInput(ModItems.SYNAPTIC_NEURAL_BERRY.get());
            int activity = getActivity(berry);
            ItemStack serum = new ItemStack(ModItems.SYNAPTIC_SERUM_S01.get());
            serum.getOrCreateTag().putInt(TAG_ACTIVITY, activity);
            yield serum;
        }
        case 2 -> {
            // S-02: inherit Activity from berry input
            ItemStack berry = findInput(ModItems.SYNAPTIC_NEURAL_BERRY.get());
            int activity = getActivity(berry);
            ItemStack serum = new ItemStack(ModItems.SYNAPTIC_SERUM_S02.get());
            serum.getOrCreateTag().putInt(TAG_ACTIVITY, activity);
            yield serum;
        }
        case 3 -> {
            // S-03: inherit Activity from berry input
            ItemStack berry = findInput(ModItems.SYNAPTIC_NEURAL_BERRY.get());
            int activity = getActivity(berry);
            ItemStack serum = new ItemStack(ModItems.SYNAPTIC_SERUM_S03.get());
            serum.getOrCreateTag().putInt(TAG_ACTIVITY, activity);
            yield serum;
        }
        default -> ItemStack.EMPTY;
    };
}

private ItemStack findInput(net.minecraft.world.item.Item item) {
    for (int i = 0; i < INPUT_SLOTS; i++) {
        if (inputs[i].is(item)) return inputs[i];
    }
    return ItemStack.EMPTY;
}
```

- [ ] **Step 5: 编译验证**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/TKCCOPL/block/entity/SerumBottlerBlockEntity.java
git commit -m "feat: 灌装机扩展莓合成配方 + Activity 公式计算 + S-01 配方"
```

---

### Task 3: 血清饮用叠加逻辑 — Activity 缩放 + 等级升级

**Files:**
- Modify: `src/main/java/com/TKCCOPL/item/SynapticSerumItem.java`

**目标：** 血清饮用时按 Activity 缩放持续时间，支持叠加升级（amplifier+1，上限 4），NeuralOverload 同步缩放。

- [ ] **Step 1: 添加 Activity 读取方法和常量**

在 `SynapticSerumItem.java` 类中添加：
```java
private static final String TAG_ACTIVITY = "SynapticActivity";
private static final int MAX_AMPLIFIER = 4; // V 级上限

public static int getActivity(ItemStack stack) {
    net.minecraft.nbt.CompoundTag tag = stack.getTag();
    if (tag == null || !tag.contains(TAG_ACTIVITY)) return 5;
    return Math.max(1, Math.min(10, tag.getInt(TAG_ACTIVITY)));
}

/**
 * 计算实际持续时间 = baseDuration × (0.5 + Activity × 0.1)
 */
public static int getScaledDuration(int baseDuration, int activity) {
    double multiplier = 0.5 + activity * 0.1;
    return (int) Math.round(baseDuration * multiplier);
}

/**
 * 计算基础 amplifier：Activity >= 8 时为 1，否则为 0
 */
public static int getBaseAmplifier(int activity) {
    return activity >= 8 ? 1 : 0;
}
```

- [ ] **Step 2: 重写 finishUsingItem 实现叠加逻辑**

替换整个 `finishUsingItem` 方法：
```java
@Override
public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
    if (!level.isClientSide) {
        int activity = getActivity(stack);
        int scaledDuration = getScaledDuration(durationTicks, activity);

        // 叠加升级：检查是否已有相同效果
        int amplifier;
        MobEffectInstance existing = entity.getEffect(effect.get());
        if (existing != null) {
            // 已有相同效果 → amplifier +1，上限 MAX_AMPLIFIER
            amplifier = Math.min(existing.getAmplifier() + 1, MAX_AMPLIFIER);
        } else {
            // 无已有效果 → 使用基础 amplifier
            amplifier = getBaseAmplifier(activity);
        }

        entity.addEffect(new MobEffectInstance(effect.get(), scaledDuration, amplifier));
    }

    if (entity instanceof Player player && !player.getAbilities().instabuild) {
        stack.shrink(1);
    }
    return stack;
}
```

- [ ] **Step 3: 添加 Tooltip 显示 Activity 和等级**

在 `SynapticSerumItem` 类中添加 `appendHoverText` 方法：
```java
@Override
public void appendHoverText(ItemStack stack, @Nullable Level level, List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
    super.appendHoverText(stack, level, tooltip, flag);
    int activity = getActivity(stack);
    tooltip.add(net.minecraft.network.chat.Component.translatable(
            "tooltip.cybercultivator.serum_activity", activity).withStyle(net.minecraft.ChatFormatting.GOLD));

    double multiplier = 0.5 + activity * 0.1;
    int baseAmp = getBaseAmplifier(activity);
    String baseLevel = baseAmp >= 1 ? "II" : "I";
    tooltip.add(net.minecraft.network.chat.Component.translatable(
            "tooltip.cybercultivator.serum_base_level", baseLevel).withStyle(net.minecraft.ChatFormatting.GRAY));

    tooltip.add(net.minecraft.network.chat.Component.translatable(
            "tooltip.cybercultivator.serum_duration_mult", String.format("%.1f", multiplier)).withStyle(net.minecraft.ChatFormatting.GRAY));
}
```

需要在文件顶部添加 import：
```java
import java.util.List;
```

- [ ] **Step 4: 编译验证**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/TKCCOPL/item/SynapticSerumItem.java
git commit -m "feat: 血清饮用叠加升级 + Activity 缩放 + Tooltip 显示"
```

---

### Task 4: 神经过载缩放 — 按血清 amplifier 施加副作用

**Files:**
- Modify: `src/main/java/com/TKCCOPL/effect/SynapticOverclockEffect.java:36-39`
- Modify: `src/main/java/com/TKCCOPL/effect/VisualEnhancementEffect.java:40-45`
- Modify: `src/main/java/com/TKCCOPL/effect/MetabolicBoostEffect.java:29-34`

**目标：** 血清效果结束时，NeuralOverload 的持续时间按当前血清 amplifier 缩放（天然随叠加等级递增）。

**说明：** 当前三个效果的 `removeAttributeModifiers` 已经按 amplifier 缩放 NeuralOverload 持续时间。验证现有公式 `20 * (12 + amplifier * 4)` 是否正确：
- amplifier 0: 240 tick = 12s
- amplifier 4: 560 tick = 28s

现有实现已经正确，不需要修改 NeuralOverload 的施加逻辑。amplifier 由血清饮用时的叠加机制决定，自然传递到 `removeAttributeModifiers` 的参数中。

- [ ] **Step 1: 验证现有代码无需修改**

确认 `SynapticOverclockEffect.removeAttributeModifiers` 已使用参数 `amplifier`：
```java
entity.addEffect(new MobEffectInstance(ModEffects.NEURAL_OVERLOAD.get(), 20 * (12 + amplifier * 4), amplifier));
```

确认 `VisualEnhancementEffect.removeAttributeModifiers` 已使用参数 `amplifier`：
```java
entity.addEffect(new MobEffectInstance(ModEffects.NEURAL_OVERLOAD.get(), 20 * (15 + amplifier * 5), amplifier));
```

确认 `MetabolicBoostEffect.removeAttributeModifiers` 已使用参数 `amplifier`：
```java
entity.addEffect(new MobEffectInstance(ModEffects.NEURAL_OVERLOAD.get(), 20 * (15 + amplifier * 5), amplifier));
```

这三处均已正确使用 amplifier 参数。血清叠加升级后，amplifier 自然递增，NeuralOverload 持续时间和强度自动缩放。无需代码改动。

- [ ] **Step: Skip commit (no changes needed)**

---

### Task 5: 移除合成台莓/S-01 配方（迁移到灌装机）

**Files:**
- Modify: `src/main/java/com/TKCCOPL/datagen/ModRecipeProvider.java:46-59`

**目标：** 莓和 S-01 的合成配方已迁移到灌装机（Task 2），需要从合成台配方中移除。

- [ ] **Step 1: 删除莓和 S-01 的合成台配方**

在 `ModRecipeProvider.java` 的 `buildRecipes` 方法中，删除第 46-59 行的两个 ShapelessRecipeBuilder 调用：

```java
// 删除以下代码块：
ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, ModItems.SYNAPTIC_NEURAL_BERRY.get())
        .requires(ModItems.BIOCHEMICAL_SOLUTION.get())
        .requires(ModItems.PLANT_FIBER.get())
        .requires(ModItems.SILICON_SHARD.get())
        .unlockedBy(getHasName(ModItems.BIOCHEMICAL_SOLUTION.get()), has(ModItems.BIOCHEMICAL_SOLUTION.get()))
        .save(writer);

ShapelessRecipeBuilder.shapeless(RecipeCategory.BREWING, ModItems.SYNAPTIC_SERUM_S01.get())
        .requires(ModItems.SYNAPTIC_NEURAL_BERRY.get())
        .requires(ModItems.BIOCHEMICAL_SOLUTION.get())
        .requires(ModItems.INDUSTRIAL_ETHANOL.get())
        .requires(Items.GLASS_BOTTLE)
        .unlockedBy(getHasName(ModItems.SYNAPTIC_NEURAL_BERRY.get()), has(ModItems.SYNAPTIC_NEURAL_BERRY.get()))
        .save(writer);
```

同时清理不再需要的 import（`ShapelessRecipeBuilder` 如果不再使用）。

- [ ] **Step 2: 运行 datagen 验证**

Run: `./gradlew runData`
Expected: 生成的配方 JSON 中不再包含 berry 和 S-01 的合成台配方

- [ ] **Step 3: 编译验证**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/TKCCOPL/datagen/ModRecipeProvider.java
git commit -m "refactor: 移除合成台莓/S-01 配方（已迁移到灌装机）"
```

---

### Task 6: 语言文件更新 — 新增翻译键

**Files:**
- Modify: `src/main/java/com/TKCCOPL/datagen/ModLangProvider.java`

**目标：** 添加品质相关 Tooltip 翻译键。

- [ ] **Step 1: 添加新翻译键**

在 `ModLangProvider.java` 的 `addTranslations()` 方法末尾（`advancement.cybercultivator.cyber_equip.description` 之后）添加：

```java
// Serum quality chain tooltips
add("tooltip.cybercultivator.serum_activity", "突触活性: %s/10");
add("tooltip.cybercultivator.serum_base_level", "基础等级: %s");
add("tooltip.cybercultivator.serum_duration_mult", "时长倍率: ×%s");
add("tooltip.cybercultivator.quality_potency", "品质: %s/10");
add("tooltip.cybercultivator.quality_purity", "纯度: %s/10");
add("tooltip.cybercultivator.quality_concentration", "浓度: %s/10");
```

- [ ] **Step 2: 运行 datagen 验证**

Run: `./gradlew runData`
Expected: `src/generated/resources/assets/cybercultivator/lang/zh_cn.json` 包含新翻译键

- [ ] **Step 3: 编译验证**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/TKCCOPL/datagen/ModLangProvider.java
git commit -m "feat: 添加品质链路相关 Tooltip 翻译键"
```

---

### Task 7: 原料物品 Tooltip 显示品质值

**Files:**
- Modify: `src/main/java/com/TKCCOPL/init/ModItems.java:30-34`

**目标：** 植物纤维、工业乙醇、生化原液在 Tooltip 中显示各自的品质值。

- [ ] **Step 1: 将三个原料从匿名 Item 改为带 Tooltip 的实例**

当前代码（第 30-34 行）：
```java
public static final RegistryObject<Item> SILICON_SHARD = ITEMS.register("silicon_shard", () -> new Item(new Item.Properties()));
public static final RegistryObject<Item> RARE_EARTH_DUST = ITEMS.register("rare_earth_dust", () -> new Item(new Item.Properties()));
public static final RegistryObject<Item> PLANT_FIBER = ITEMS.register("plant_fiber", () -> new Item(new Item.Properties()));
public static final RegistryObject<Item> BIOCHEMICAL_SOLUTION = ITEMS.register("biochemical_solution", () -> new Item(new Item.Properties()));
public static final RegistryObject<Item> INDUSTRIAL_ETHANOL = ITEMS.register("industrial_ethanol", () -> new Item(new Item.Properties()));
```

替换为：
```java
public static final RegistryObject<Item> SILICON_SHARD = ITEMS.register("silicon_shard", () -> new Item(new Item.Properties()));
public static final RegistryObject<Item> RARE_EARTH_DUST = ITEMS.register("rare_earth_dust", () -> new Item(new Item.Properties()));
public static final RegistryObject<Item> PLANT_FIBER = ITEMS.register("plant_fiber", () -> new Item(new Item.Properties()) {
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (stack.hasTag() && stack.getTag().contains("Potency")) {
            tooltip.add(Component.translatable("tooltip.cybercultivator.quality_potency",
                    stack.getTag().getInt("Potency")).withStyle(net.minecraft.ChatFormatting.GREEN));
        }
    }
});
public static final RegistryObject<Item> BIOCHEMICAL_SOLUTION = ITEMS.register("biochemical_solution", () -> new Item(new Item.Properties()) {
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (stack.hasTag() && stack.getTag().contains("Concentration")) {
            tooltip.add(Component.translatable("tooltip.cybercultivator.quality_concentration",
                    stack.getTag().getInt("Concentration")).withStyle(net.minecraft.ChatFormatting.GREEN));
        }
    }
});
public static final RegistryObject<Item> INDUSTRIAL_ETHANOL = ITEMS.register("industrial_ethanol", () -> new Item(new Item.Properties()) {
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (stack.hasTag() && stack.getTag().contains("Purity")) {
            tooltip.add(Component.translatable("tooltip.cybercultivator.quality_purity",
                    stack.getTag().getInt("Purity")).withStyle(net.minecraft.ChatFormatting.GREEN));
        }
    }
});
```

需要确认文件顶部已有 `import java.util.List;` 和 `import net.minecraft.network.chat.Component;`（已有）。

- [ ] **Step 2: 添加突触神经莓 Tooltip**

在 `SYNAPTIC_NEURAL_BERRY` 注册处（第 35 行）添加 Activity Tooltip。当前代码：
```java
public static final RegistryObject<Item> SYNAPTIC_NEURAL_BERRY = ITEMS.register("synaptic_neural_berry", () -> new Item(new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(2).saturationMod(0.3F).build())));
```

替换为：
```java
public static final RegistryObject<Item> SYNAPTIC_NEURAL_BERRY = ITEMS.register("synaptic_neural_berry", () -> new Item(new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(2).saturationMod(0.3F).build())) {
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (stack.hasTag() && stack.getTag().contains("SynapticActivity")) {
            int activity = stack.getTag().getInt("SynapticActivity");
            tooltip.add(Component.translatable("tooltip.cybercultivator.serum_activity", activity).withStyle(net.minecraft.ChatFormatting.GOLD));
        }
    }
});
```

- [ ] **Step 3: 编译验证**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/TKCCOPL/init/ModItems.java
git commit -m "feat: 原料和莓物品 Tooltip 显示品质/活性值"
```

---

### Task 8: 全量构建验证 + Phase Gate

**目标：** 确保所有改动通过编译、数据生成和构建。

- [ ] **Step 1: 编译检查**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 数据生成**

Run: `./gradlew runData`
Expected: 生成的资源文件正确（配方已移除莓/S-01 合成台配方，语言文件包含新翻译键）

- [ ] **Step 3: 完整构建**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL，jar 生成在 `build/libs/`

- [ ] **Step 4: 更新 dev-plan.md**

在 `docs/dev-plan.md` 的任务列表中添加新任务行，记录本次功能开发。

- [ ] **Step 5: Final commit**

```bash
git add -A
git commit -m "feat: 血清品质链路 + 升级机制 — 完整功能实现"
```

---

## 验收标准对照

| 验收项 | 对应 Task | 验证方式 |
|--------|----------|---------|
| 品质传递闭环 | Task 1, 2 | 高 Potency 种子 → 品质原料 → 高 Activity 莓 → 强血清 |
| 叠加升级 | Task 3 | 同种血清多次饮用，amplifier 逐次 +1 直到 V 级 |
| 负面效果同步 | Task 4 | 高等级血清结束后，NeuralOverload 更强 |
| Tooltip 正确 | Task 6, 7 | 所有品质相关物品显示数值 |
| 现有功能不退化 | Task 5, 8 | Activity=5 时效果与改动前一致 |
| 编译通过 | Task 8 | compileJava + runData + build |

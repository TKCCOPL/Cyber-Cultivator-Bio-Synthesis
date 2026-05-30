# 模组兼容 & API 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 暴露 Cyber-Cultivator 的配方/基因/机器系统为公开 API，集成 JEI 配方查看器。

**Architecture:** 血清灌装机配方改用 Forge RecipeType（JSON 数据驱动），基因拼接机/培养槽通过自定义接口暴露。4 个自定义 Forge 事件供 KubeJS/其他 mod 监听。JEI 通过 compileOnly 依赖集成，与现有 Curios 模式一致。

**Tech Stack:** Minecraft Forge 1.20.1, Java 17, JEI 15.2.0.27 (compileOnly), Parchment mappings

**Spec:** `docs/superpowers/specs/2026-05-30-mod-compatibility-api-design.md`

---

## 文件结构

### 新增文件（16 个）

| 文件 | 职责 |
|------|------|
| `recipe/ModRecipeTypes.java` | 注册 RecipeType |
| `recipe/ModRecipes.java` | 静态注册表（拼接机/培养槽配方） |
| `recipe/SerumRecipe.java` | 血清配方类（JSON 数据驱动） |
| `recipe/SerumRecipeSerializer.java` | JSON 序列化器 |
| `event/GeneSpliceEvent.java` | 基因拼接事件 |
| `event/CropMatureEvent.java` | 作物成熟事件 |
| `event/SerumCraftEvent.java` | 血清灌装事件 |
| `event/SerumConsumeEvent.java` | 血清饮用事件 |
| `api/CyberCultivatorAPI.java` | API 门面类 |
| `api/IncubatorInfo.java` | 培养槽 DTO |
| `api/BottlerInfo.java` | 灌装机 DTO |
| `api/CondenserInfo.java` | 冷凝器 DTO |
| `api/SplicerInfo.java` | 拼接机 DTO |
| `api/SerumEffectInfo.java` | 血清效果 DTO |
| `compat/jei/CyberCultivatorJEIPlugin.java` | JEI 插件入口 |
| `compat/jei/SerumBottlingCategory.java` | 血清灌装 JEI 类别 |
| `compat/jei/GeneSplicingCategory.java` | 基因拼接 JEI 类别 |
| `compat/jei/IncubatorOutputCategory.java` | 培养槽产出 JEI 类别 |

### 修改文件（4 个）

| 文件 | 修改内容 |
|------|---------|
| `block/entity/SerumBottlerBlockEntity.java` | matchRecipe() 改用 RecipeManager |
| `block/entity/BioIncubatorBlockEntity.java` | 成熟时触发 CropMatureEvent |
| `block/entity/GeneSplicerBlockEntity.java` | 拼接时触发 GeneSpliceEvent |
| `item/SynapticSerumItem.java` | 饮用时触发 SerumConsumeEvent |

### 配置文件修改

| 文件 | 修改内容 |
|------|---------|
| `build.gradle` | 新增 JEI compileOnly 依赖 |
| `gradle.properties` | 新增 jei_version |
| `mods.toml` | 新增 JEI 可选依赖声明 |

---

## Phase 1: RecipeType 基础

### Task 1: 注册 RecipeType

**Files:**
- Create: `src/main/java/com/TKCCOPL/recipe/ModRecipeTypes.java`

- [ ] **Step 1: 创建 ModRecipeTypes.java**

```java
package com.TKCCOPL.recipe;

import com.TKCCOPL.cybercultivator;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRecipeTypes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, cybercultivator.MODID);

    public static final RegistryObject<RecipeType<SerumRecipe>> SERUM_BOTTLING =
            RECIPE_TYPES.register("serum_bottling",
                    () -> new RecipeType<>() {
                        @Override
                        public String toString() {
                            return "cybercultivator:serum_bottling";
                        }
                    });

    private ModRecipeTypes() {
    }

    public static void register(IEventBus eventBus) {
        RECIPE_TYPES.register(eventBus);
    }
}
```

- [ ] **Step 2: 在 cybercultivator.java 主类中注册**

在 `cybercultivator.java` 构造函数中，`ModBlocks.register(modEventBus)` 之后添加：

```java
ModRecipeTypes.register(modEventBus);
```

- [ ] **Step 3: 编译验证**

```bash
./gradlew compileJava
```

预期：BUILD SUCCESSFUL（此时 SerumRecipe 不存在，会编译失败，需要先完成 Task 2）

- [ ] **Step 4: 暂存（不提交，等 Task 2 完成后一起提交）**

---

### Task 2: SerumRecipe 配方类

**Files:**
- Create: `src/main/java/com/TKCCOPL/recipe/SerumRecipe.java`

- [ ] **Step 1: 创建 SerumRecipe.java**

```java
package com.TKCCOPL.recipe;

import com.TKCCOPL.init.ModItems;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

public class SerumRecipe implements Recipe<Container> {
    private final ResourceLocation id;
    private final Ingredient[] inputs;
    private final ItemStack baseOutput;
    private final int processingTime;
    private final boolean inheritActivity;
    private final boolean inheritMutation;

    public SerumRecipe(ResourceLocation id, Ingredient[] inputs, ItemStack baseOutput,
                       int processingTime, boolean inheritActivity, boolean inheritMutation) {
        this.id = id;
        this.inputs = inputs;
        this.baseOutput = baseOutput;
        this.processingTime = processingTime;
        this.inheritActivity = inheritActivity;
        this.inheritMutation = inheritMutation;
    }

    @Override
    public boolean matches(Container container, Level level) {
        boolean[] matched = new boolean[inputs.length];
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack slotStack = container.getItem(i);
            if (slotStack.isEmpty()) continue;
            for (int j = 0; j < inputs.length; j++) {
                if (!matched[j] && inputs[j].test(slotStack)) {
                    matched[j] = true;
                    break;
                }
            }
        }
        for (boolean m : matched) {
            if (!m) return false;
        }
        return true;
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess registryAccess) {
        return baseOutput.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return false; // Not a grid recipe
    }

    @Override
    public ItemStack getResultItem(@Nullable RegistryAccess registryAccess) {
        return baseOutput.copy();
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.SERUM_BOTTLING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.SERUM_BOTTLING.get();
    }

    // Accessors
    public Ingredient[] getInputs() { return inputs; }
    public ItemStack getBaseOutput() { return baseOutput.copy(); }
    public int getProcessingTime() { return processingTime; }
    public boolean isInheritActivity() { return inheritActivity; }
    public boolean isInheritMutation() { return inheritMutation; }
}
```

- [ ] **Step 2: 编译验证**

```bash
./gradlew compileJava
```

预期：可能因 ModRecipeTypes 引用 SERUM_BOTTLING_SERIALIZER 失败，需 Task 3 完成

- [ ] **Step 3: 暂存**

---

### Task 3: SerumRecipeSerializer 序列化器

**Files:**
- Create: `src/main/java/com/TKCCOPL/recipe/SerumRecipeSerializer.java`
- Modify: `src/main/java/com/TKCCOPL/recipe/ModRecipeTypes.java`

- [ ] **Step 1: 创建 SerumRecipeSerializer.java**

```java
package com.TKCCOPL.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;

public class SerumRecipeSerializer implements RecipeSerializer<SerumRecipe> {
    @Override
    public SerumRecipe fromJson(ResourceLocation id, JsonObject json) {
        JsonArray ingredientsJson = GsonHelper.getAsJsonArray(json, "ingredients");
        Ingredient[] inputs = new Ingredient[ingredientsJson.size()];
        for (int i = 0; i < ingredientsJson.size(); i++) {
            inputs[i] = Ingredient.fromJson(ingredientsJson.get(i));
        }

        JsonObject resultJson = GsonHelper.getAsJsonObject(json, "result");
        ItemStack output = CraftingHelper.getItemStack(resultJson, true);

        int processingTime = GsonHelper.getAsInt(json, "processing_time", 300);
        boolean inheritActivity = GsonHelper.getAsBoolean(json, "inherit_activity", false);
        boolean inheritMutation = GsonHelper.getAsBoolean(json, "inherit_mutation", false);

        return new SerumRecipe(id, inputs, output, processingTime, inheritActivity, inheritMutation);
    }

    @Nullable
    @Override
    public SerumRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
        int inputCount = buf.readVarInt();
        Ingredient[] inputs = new Ingredient[inputCount];
        for (int i = 0; i < inputCount; i++) {
            inputs[i] = Ingredient.fromNetwork(buf);
        }
        ItemStack output = buf.readItem();
        int processingTime = buf.readVarInt();
        boolean inheritActivity = buf.readBoolean();
        boolean inheritMutation = buf.readBoolean();
        return new SerumRecipe(id, inputs, output, processingTime, inheritActivity, inheritMutation);
    }

    @Override
    public void toNetwork(FriendlyByteBuf buf, SerumRecipe recipe) {
        buf.writeVarInt(recipe.getInputs().length);
        for (Ingredient input : recipe.getInputs()) {
            input.toNetwork(buf);
        }
        buf.writeItem(recipe.getBaseOutput());
        buf.writeVarInt(recipe.getProcessingTime());
        buf.writeBoolean(recipe.isInheritActivity());
        buf.writeBoolean(recipe.isInheritMutation());
    }
}
```

- [ ] **Step 2: 在 ModRecipeTypes 中注册序列化器**

在 `ModRecipeTypes.java` 中添加：

```java
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.item.crafting.RecipeSerializer;

// 在 SERUM_BOTTLING 之后添加：
public static final RegistryObject<RecipeSerializer<SerumRecipe>> SERUM_BOTTLING_SERIALIZER =
        DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, cybercultivator.MODID)
                .register("serum_bottling", SerumRecipeSerializer::new);
```

同时需要将 `RECIPE_SERIALIZERS` 的 DeferredRegister 也注册到事件总线。修改 `register` 方法：

```java
private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
        DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, cybercultivator.MODID);

public static final RegistryObject<RecipeSerializer<SerumRecipe>> SERUM_BOTTLING_SERIALIZER =
        SERIALIZERS.register("serum_bottling", SerumRecipeSerializer::new);

public static void register(IEventBus eventBus) {
    RECIPE_TYPES.register(eventBus);
    SERIALIZERS.register(eventBus);
}
```

- [ ] **Step 3: 编译验证**

```bash
./gradlew compileJava
```

预期：BUILD SUCCESSFUL

- [ ] **Step 4: 提交**

```bash
git add src/main/java/com/TKCCOPL/recipe/
git commit -m "feat(recipe): 新增 SerumRecipeType + SerumRecipe + Serializer"
```

---

### Task 4: ModRecipes 静态注册表

**Files:**
- Create: `src/main/java/com/TKCCOPL/recipe/ModRecipes.java`

- [ ] **Step 1: 创建 ModRecipes.java**

```java
package com.TKCCOPL.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 静态注册表：基因拼接机和培养槽的配方/产出规则。
 * 非 JSON 驱动（算法驱动），供 JEI 和第三方 mod 查询。
 */
public final class ModRecipes {

    /** 基因拼接配方接口 */
    public interface IGeneSpliceRecipe {
        /** 计算子代基因，返回 [speed, yield, potency] */
        int[] calculateOffspring(int speedA, int yieldA, int potencyA,
                                 int speedB, int yieldB, int potencyB,
                                 RandomSource random);
        /** 突变概率计算 */
        double getMutationChance(int generation, int geneDifference);
    }

    /** 培养槽产出接口 */
    public interface IIncubatorOutput {
        /** 种子类型标识（用于 JEI 展示） */
        String getSeedType();
        /** 默认基因值 [speed, yield, potency]（用于 JEI 展示） */
        int[] getDefaultGenes();
        /** 基因对生长速率的倍率 */
        double getGrowthMultiplier(int geneSpeed);
    }

    private static final List<IGeneSpliceRecipe> SPLICE_RECIPES = new ArrayList<>();
    private static final List<IIncubatorOutput> INCUBATOR_OUTPUTS = new ArrayList<>();

    /** 默认拼接配方实现 */
    private static final IGeneSpliceRecipe DEFAULT_SPLICE = new IGeneSpliceRecipe() {
        @Override
        public int[] calculateOffspring(int speedA, int yieldA, int potencyA,
                                        int speedB, int yieldB, int potencyB,
                                        RandomSource random) {
            int newSpeed = clampGene((speedA + speedB) / 2 + random.nextInt(5) - 2);
            int newYield = clampGene((yieldA + yieldB) / 2 + random.nextInt(5) - 2);
            int newPotency = clampGene((potencyA + potencyB) / 2 + random.nextInt(5) - 2);
            return new int[]{newSpeed, newYield, newPotency};
        }

        @Override
        public double getMutationChance(int generation, int geneDifference) {
            return 0.05 + generation * 0.02 + geneDifference * 0.01;
        }

        private int clampGene(int value) {
            return Math.max(1, Math.min(10, value));
        }
    };

    static {
        SPLICE_RECIPES.add(DEFAULT_SPLICE);
    }

    private ModRecipes() {
    }

    public static List<IGeneSpliceRecipe> getSPLICE_RECIPES() {
        return Collections.unmodifiableList(SPLICE_RECIPES);
    }

    public static List<IIncubatorOutput> getINCUBATOR_OUTPUTS() {
        return Collections.unmodifiableList(INCUBATOR_OUTPUTS);
    }

    public static IGeneSpliceRecipe getDefaultSpliceRecipe() {
        return DEFAULT_SPLICE;
    }

    /** 注册自定义拼接配方（供其他 mod 调用） */
    public static void registerSpliceRecipe(IGeneSpliceRecipe recipe) {
        SPLICE_RECIPES.add(recipe);
    }

    /** 注册培养槽产出（供其他 mod 调用） */
    public static void registerIncubatorOutput(IIncubatorOutput output) {
        INCUBATOR_OUTPUTS.add(output);
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
./gradlew compileJava
```

预期：BUILD SUCCESSFUL

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/TKCCOPL/recipe/ModRecipes.java
git commit -m "feat(recipe): 新增 ModRecipes 静态注册表"
```

---

### Task 5: JSON 配方文件

**Files:**
- Create: `src/main/resources/data/cybercultivator/recipes/serum/berry_synthesis.json`
- Create: `src/main/resources/data/cybercultivator/recipes/serum/s01_bottling.json`
- Create: `src/main/resources/data/cybercultivator/recipes/serum/s02_bottling.json`
- Create: `src/main/resources/data/cybercultivator/recipes/serum/s03_bottling.json`

- [ ] **Step 1: 创建莓合成配方**

```json
{
  "type": "cybercultivator:serum_bottling",
  "ingredients": [
    { "item": "cybercultivator:plant_fiber" },
    { "item": "cybercultivator:industrial_ethanol" },
    { "item": "cybercultivator:biochemical_solution" }
  ],
  "result": { "item": "cybercultivator:synaptic_neural_berry" },
  "processing_time": 300,
  "inherit_activity": true,
  "inherit_mutation": true
}
```

- [ ] **Step 2: 创建 S-01 配方**

```json
{
  "type": "cybercultivator:serum_bottling",
  "ingredients": [
    { "item": "cybercultivator:synaptic_neural_berry" },
    { "item": "cybercultivator:biochemical_solution" },
    { "item": "minecraft:glass_bottle" }
  ],
  "result": { "item": "cybercultivator:synaptic_serum_s01" },
  "processing_time": 300,
  "inherit_activity": true,
  "inherit_mutation": false
}
```

- [ ] **Step 3: 创建 S-02 配方**

```json
{
  "type": "cybercultivator:serum_bottling",
  "ingredients": [
    { "item": "cybercultivator:synaptic_neural_berry" },
    { "item": "cybercultivator:rare_earth_dust" },
    { "item": "minecraft:glass_bottle" }
  ],
  "result": { "item": "cybercultivator:synaptic_serum_s02" },
  "processing_time": 300,
  "inherit_activity": true,
  "inherit_mutation": false
}
```

- [ ] **Step 4: 创建 S-03 配方**

```json
{
  "type": "cybercultivator:serum_bottling",
  "ingredients": [
    { "item": "cybercultivator:synaptic_neural_berry" },
    { "item": "cybercultivator:industrial_ethanol" },
    { "item": "minecraft:glass_bottle" }
  ],
  "result": { "item": "cybercultivator:synaptic_serum_s03" },
  "processing_time": 300,
  "inherit_activity": true,
  "inherit_mutation": false
}
```

- [ ] **Step 5: 提交**

```bash
git add src/main/resources/data/cybercultivator/recipes/serum/
git commit -m "feat(recipe): 新增 4 个血清灌装机 JSON 配方"
```

---

## Phase 2: BlockEntity 重构

### Task 6: 重构 SerumBottlerBlockEntity 使用 RecipeManager

**Files:**
- Modify: `src/main/java/com/TKCCOPL/block/entity/SerumBottlerBlockEntity.java`

- [ ] **Step 1: 添加 import**

在文件顶部添加：

```java
import com.TKCCOPL.recipe.ModRecipeTypes;
import com.TKCCOPL.recipe.SerumRecipe;
import net.minecraft.world.SimpleContainer;
```

- [ ] **Step 2: 替换 matchRecipe() 方法**

将现有的 `matchRecipe()` 方法替换为：

```java
/**
 * @return 匹配的 SerumRecipe，无匹配返回 null
 */
private SerumRecipe findRecipe() {
    if (level == null) return null;
    SimpleContainer container = new SimpleContainer(INPUT_SLOTS);
    for (int i = 0; i < INPUT_SLOTS; i++) {
        container.setItem(i, inputs[i]);
    }
    return level.getRecipeManager()
            .getAllRecipesFor(ModRecipeTypes.SERUM_BOTTLING.get())
            .stream()
            .filter(r -> r.matches(container, level))
            .findFirst()
            .orElse(null);
}
```

- [ ] **Step 3: 修改 tick() 方法中的配方查找逻辑**

将 `tick()` 方法中：

```java
// Try to start a recipe
if (blockEntity.maxProgress == 0) {
    int recipe = blockEntity.matchRecipe();
    if (recipe >= 0) {
        blockEntity.activeRecipe = recipe;
        blockEntity.maxProgress = PROCESSING_TIME;
        blockEntity.progress = 0;
        changed = true;
    }
}
```

替换为：

```java
// Try to start a recipe
if (blockEntity.maxProgress == 0) {
    SerumRecipe recipe = blockEntity.findRecipe();
    if (recipe != null) {
        blockEntity.cachedRecipe = recipe;
        blockEntity.maxProgress = recipe.getProcessingTime();
        blockEntity.progress = 0;
        changed = true;
    }
}
```

- [ ] **Step 4: 修改配方完成逻辑**

将 `tick()` 方法中的配方完成逻辑：

```java
if (blockEntity.progress >= blockEntity.maxProgress) {
    // Complete recipe — use cached activeRecipe to avoid TOCTOU
    int recipe = blockEntity.activeRecipe;
    if (recipe >= 0) {
        // 先获取产出（需要读取输入的 NBT），再消耗输入
        ItemStack result = blockEntity.getRecipeOutput(recipe);
        blockEntity.consumeInputs(recipe);
        ...
    }
    blockEntity.activeRecipe = -1;
    ...
}
```

替换为：

```java
if (blockEntity.progress >= blockEntity.maxProgress) {
    SerumRecipe recipe = blockEntity.cachedRecipe;
    if (recipe != null) {
        ItemStack result = blockEntity.assembleRecipe(recipe);
        blockEntity.consumeRecipeInputs(recipe);
        if (blockEntity.output.isEmpty()) {
            blockEntity.output = result;
        } else {
            blockEntity.output.grow(result.getCount());
        }
    }
    blockEntity.cachedRecipe = null;
    blockEntity.progress = 0;
    blockEntity.maxProgress = 0;
    changed = true;
}
```

- [ ] **Step 5: 添加 assembleRecipe() 方法**

```java
/**
 * 根据配方组装输出，处理 Activity 继承和 Mutation 标签转移。
 */
private ItemStack assembleRecipe(SerumRecipe recipe) {
    ItemStack result = recipe.getBaseOutput();

    if (recipe.isInheritActivity()) {
        int activity = calculateActivity(inputs);
        result.getOrCreateTag().putInt(TAG_ACTIVITY, activity);
    }

    if (recipe.isInheritMutation()) {
        int mutationType = 0;
        String mutationDetail = "";
        for (ItemStack input : inputs) {
            if (input.isEmpty()) continue;
            CompoundTag tag = input.getTag();
            if (tag != null && tag.contains("Mutation")) {
                int mt = tag.getInt("Mutation");
                if (mt > mutationType) {
                    mutationType = mt;
                    mutationDetail = tag.contains("MutationDetail") ? tag.getString("MutationDetail") : "";
                }
            }
        }
        if (mutationType > 0) {
            result.getOrCreateTag().putInt("Mutation", mutationType);
            result.getOrCreateTag().putString("MutationDetail", mutationDetail);
        }
    }

    // 对于血清配方（非莓合成），从莓输入继承 Activity
    if (!recipe.isInheritActivity() || recipe.isInheritMutation()) {
        // 如果输出是血清，从莓输入继承 Activity
        ItemStack berry = findInput(ModItems.SYNAPTIC_NEURAL_BERRY.get());
        if (!berry.isEmpty()) {
            int activity = getActivity(berry);
            result.getOrCreateTag().putInt(TAG_ACTIVITY, activity);
        }
    }

    return result;
}
```

- [ ] **Step 6: 添加 consumeRecipeInputs() 方法**

```java
private void consumeRecipeInputs(SerumRecipe recipe) {
    for (Ingredient ingredient : recipe.getInputs()) {
        for (int i = 0; i < INPUT_SLOTS; i++) {
            if (!inputs[i].isEmpty() && ingredient.test(inputs[i])) {
                inputs[i].shrink(1);
                break;
            }
        }
    }
}
```

- [ ] **Step 7: 添加 cachedRecipe 字段并更新 NBT**

在字段声明区域添加：

```java
private SerumRecipe cachedRecipe; // 运行时缓存，不持久化
```

- [ ] **Step 8: 清理废弃方法**

删除不再使用的方法：`matchRecipe()`, `hasIngredients()`, `canOutput()`, `consumeInputs()`, `getRecipeOutput()`。

保留 `findInput()`, `calculateActivity()`, `getActivity()`。

- [ ] **Step 9: 更新 getActiveRecipe() 返回值**

修改 `getActiveRecipe()` 方法返回配方索引（用于 HUD 显示）：

```java
public int getActiveRecipe() {
    if (cachedRecipe == null) return -1;
    // 简化：根据输出物品判断配方类型
    ItemStack out = cachedRecipe.getBaseOutput();
    if (out.is(ModItems.SYNAPTIC_NEURAL_BERRY.get())) return 0;
    if (out.is(ModItems.SYNAPTIC_SERUM_S01.get())) return 1;
    if (out.is(ModItems.SYNAPTIC_SERUM_S02.get())) return 2;
    if (out.is(ModItems.SYNAPTIC_SERUM_S03.get())) return 3;
    return -1;
}
```

- [ ] **Step 10: 编译验证**

```bash
./gradlew compileJava
```

预期：BUILD SUCCESSFUL

- [ ] **Step 11: 提交**

```bash
git add src/main/java/com/TKCCOPL/block/entity/SerumBottlerBlockEntity.java
git commit -m "refactor(bottler): matchRecipe() 改用 RecipeManager + SerumRecipe"
```

---

## Phase 3: 事件系统

### Task 7: GeneSpliceEvent

**Files:**
- Create: `src/main/java/com/TKCCOPL/event/GeneSpliceEvent.java`

- [ ] **Step 1: 创建 GeneSpliceEvent.java**

```java
package com.TKCCOPL.event;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.Event;

/**
 * 基因拼接完成时触发。
 * 监听此事件可修改子代基因、突变结果等。
 */
public class GeneSpliceEvent extends Event {
    private final ItemStack seedA;
    private final ItemStack seedB;
    private int speed;
    private int yield;
    private int potency;
    private int synergy;
    private int generation;
    private boolean isMutation;
    private int mutationType;
    private String mutationDetail;

    public GeneSpliceEvent(ItemStack seedA, ItemStack seedB,
                           int speed, int yield, int potency,
                           int synergy, int generation,
                           boolean isMutation, int mutationType, String mutationDetail) {
        this.seedA = seedA;
        this.seedB = seedB;
        this.speed = speed;
        this.yield = yield;
        this.potency = potency;
        this.synergy = synergy;
        this.generation = generation;
        this.isMutation = isMutation;
        this.mutationType = mutationType;
        this.mutationDetail = mutationDetail;
    }

    public ItemStack getSeedA() { return seedA; }
    public ItemStack getSeedB() { return seedB; }

    public int getSpeed() { return speed; }
    public void setSpeed(int speed) { this.speed = speed; }

    public int getYield() { return yield; }
    public void setYield(int yield) { this.yield = yield; }

    public int getPotency() { return potency; }
    public void setPotency(int potency) { this.potency = potency; }

    public int getSynergy() { return synergy; }
    public void setSynergy(int synergy) { this.synergy = synergy; }

    public int getGeneration() { return generation; }
    public void setGeneration(int generation) { this.generation = generation; }

    public boolean isMutation() { return isMutation; }
    public void setMutation(boolean mutation) { isMutation = mutation; }

    public int getMutationType() { return mutationType; }
    public void setMutationType(int mutationType) { this.mutationType = mutationType; }

    public String getMutationDetail() { return mutationDetail; }
    public void setMutationDetail(String mutationDetail) { this.mutationDetail = mutationDetail; }

    @Override
    public boolean isCancelable() { return true; }
}
```

- [ ] **Step 2: 暂存**

---

### Task 8: CropMatureEvent

**Files:**
- Create: `src/main/java/com/TKCCOPL/event/CropMatureEvent.java`

- [ ] **Step 1: 创建 CropMatureEvent.java**

```java
package com.TKCCOPL.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.Event;

/**
 * 作物成熟时触发。
 * 监听此事件可修改产出物品。
 */
public class CropMatureEvent extends Event {
    private final Level level;
    private final BlockPos pos;
    private final ItemStack seed;
    private ItemStack output;

    public CropMatureEvent(Level level, BlockPos pos, ItemStack seed, ItemStack output) {
        this.level = level;
        this.pos = pos;
        this.seed = seed;
        this.output = output;
    }

    public Level getLevel() { return level; }
    public BlockPos getPos() { return pos; }
    public ItemStack getSeed() { return seed; }

    public ItemStack getOutput() { return output; }
    public void setOutput(ItemStack output) { this.output = output; }

    @Override
    public boolean isCancelable() { return true; }
}
```

- [ ] **Step 2: 暂存**

---

### Task 9: SerumCraftEvent

**Files:**
- Create: `src/main/java/com/TKCCOPL/event/SerumCraftEvent.java`

- [ ] **Step 1: 创建 SerumCraftEvent.java**

```java
package com.TKCCOPL.event;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.Event;

/**
 * 血清灌装机配方完成时触发。
 * 监听此事件可修改输出物品或 Activity 值。
 */
public class SerumCraftEvent extends Event {
    private final ItemStack[] inputs;
    private ItemStack output;
    private int activity;
    private final int recipeIndex;

    public SerumCraftEvent(ItemStack[] inputs, ItemStack output, int activity, int recipeIndex) {
        this.inputs = inputs;
        this.output = output;
        this.activity = activity;
        this.recipeIndex = recipeIndex;
    }

    public ItemStack[] getInputs() { return inputs; }

    public ItemStack getOutput() { return output; }
    public void setOutput(ItemStack output) { this.output = output; }

    public int getActivity() { return activity; }
    public void setActivity(int activity) { this.activity = activity; }

    public int getRecipeIndex() { return recipeIndex; }

    @Override
    public boolean isCancelable() { return true; }
}
```

- [ ] **Step 2: 暂存**

---

### Task 10: SerumConsumeEvent

**Files:**
- Create: `src/main/java/com/TKCCOPL/event/SerumConsumeEvent.java`

- [ ] **Step 1: 创建 SerumConsumeEvent.java**

```java
package com.TKCCOPL.event;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.Event;

/**
 * 血清饮用时触发（在效果施加之前）。
 * 监听此事件可修改效果参数或取消效果施加。
 */
public class SerumConsumeEvent extends Event {
    private final LivingEntity entity;
    private final ItemStack serum;
    private final MobEffect effect;
    private int activity;
    private int duration;
    private int amplifier;

    public SerumConsumeEvent(LivingEntity entity, ItemStack serum, MobEffect effect,
                             int activity, int duration, int amplifier) {
        this.entity = entity;
        this.serum = serum;
        this.effect = effect;
        this.activity = activity;
        this.duration = duration;
        this.amplifier = amplifier;
    }

    public LivingEntity getEntity() { return entity; }
    public ItemStack getSerum() { return serum; }
    public MobEffect getEffect() { return effect; }

    public int getActivity() { return activity; }
    public void setActivity(int activity) { this.activity = activity; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public int getAmplifier() { return amplifier; }
    public void setAmplifier(int amplifier) { this.amplifier = amplifier; }

    @Override
    public boolean isCancelable() { return true; }
}
```

- [ ] **Step 2: 编译验证（4 个事件类）**

```bash
./gradlew compileJava
```

预期：BUILD SUCCESSFUL

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/TKCCOPL/event/
git commit -m "feat(event): 新增 4 个自定义 Forge 事件（拼接/成熟/灌装/饮用）"
```

---

### Task 11: 在 BlockEntity 中触发事件

**Files:**
- Modify: `src/main/java/com/TKCCOPL/block/entity/GeneSplicerBlockEntity.java`
- Modify: `src/main/java/com/TKCCOPL/block/entity/BioIncubatorBlockEntity.java`

- [ ] **Step 1: GeneSplicerBlockEntity 触发 GeneSpliceEvent**

在 `craftOutput()` 方法中，步骤 7（标记突变）之后、`output = result` 之前，插入事件触发：

```java
// 7.5 触发 GeneSpliceEvent，允许其他 mod 修改结果
GeneSpliceEvent event = new GeneSpliceEvent(
        seedA, seedB, newSpeed, newYield, newPotency,
        result.getOrCreateTag().getInt(GeneSeedItem.GENE_SYNERGY),
        childGen, isMutation, mutationType, mutationDetail
);
if (net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event)) {
    return; // 事件被取消
}
// 使用事件修改后的值
newSpeed = event.getSpeed();
newYield = event.getYield();
newPotency = event.getPotency();
GeneticSeedItem.setGenes(result, newSpeed, newYield, newPotency);
if (event.getSynergy() > 0) {
    result.getOrCreateTag().putInt(GeneSeedItem.GENE_SYNERGY, event.getSynergy());
}
```

同时在文件顶部添加 import：

```java
import com.TKCCOPL.event.GeneSpliceEvent;
```

- [ ] **Step 2: BioIncubatorBlockEntity 触发 CropMatureEvent**

在 `tick()` 方法中，成熟判定逻辑中，`getCropOutput()` 之后、`Containers.dropItemStack()` 之前，插入：

```java
// 触发 CropMatureEvent，允许其他 mod 修改产出
CropMatureEvent cropEvent = new CropMatureEvent(level, pos, blockEntity.seed, cropOutput);
if (net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(cropEvent)) {
    return; // 事件被取消，不产出
}
cropOutput = cropEvent.getOutput();
```

同时在文件顶部添加 import：

```java
import com.TKCCOPL.event.CropMatureEvent;
```

- [ ] **Step 3: 编译验证**

```bash
./gradlew compileJava
```

预期：BUILD SUCCESSFUL

- [ ] **Step 4: 提交**

```bash
git add src/main/java/com/TKCCOPL/block/entity/GeneSplicerBlockEntity.java
git add src/main/java/com/TKCCOPL/block/entity/BioIncubatorBlockEntity.java
git commit -m "feat(event): 在 BlockEntity 中触发 GeneSpliceEvent 和 CropMatureEvent"
```

---

### Task 12: 在 Item 和 Bottler 中触发事件

**Files:**
- Modify: `src/main/java/com/TKCCOPL/item/SynapticSerumItem.java`
- Modify: `src/main/java/com/TKCCOPL/block/entity/SerumBottlerBlockEntity.java`

- [ ] **Step 1: SynapticSerumItem 触发 SerumConsumeEvent**

在 `finishUsingItem()` 方法中，`entity.addEffect(...)` 之前，插入：

```java
// 触发 SerumConsumeEvent，允许其他 mod 修改效果参数
SerumConsumeEvent consumeEvent = new SerumConsumeEvent(
        entity, stack, effect.get(), activity, scaledDuration, amp
);
if (net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(consumeEvent)) {
    // 事件被取消，不施加效果
    if (entity instanceof Player player && !player.getAbilities().instabuild) {
        stack.shrink(1);
    }
    return stack;
}
scaledDuration = consumeEvent.getDuration();
amp = consumeEvent.getAmplifier();
```

同时在文件顶部添加 import：

```java
import com.TKCCOPL.event.SerumConsumeEvent;
```

- [ ] **Step 2: SerumBottlerBlockEntity 触发 SerumCraftEvent**

在 `assembleRecipe()` 方法中，构建 result 之后，返回之前，插入：

```java
// 触发 SerumCraftEvent，允许其他 mod 修改输出
int recipeIndex = getActiveRecipe();
SerumCraftEvent craftEvent = new SerumCraftEvent(inputs, result, getActivity(result), recipeIndex);
if (net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(craftEvent)) {
    return ItemStack.EMPTY; // 事件被取消
}
result = craftEvent.getOutput();
if (craftEvent.getActivity() > 0) {
    result.getOrCreateTag().putInt(TAG_ACTIVITY, craftEvent.getActivity());
}
```

同时在文件顶部添加 import：

```java
import com.TKCCOPL.event.SerumCraftEvent;
```

- [ ] **Step 3: 编译验证**

```bash
./gradlew compileJava
```

预期：BUILD SUCCESSFUL

- [ ] **Step 4: 提交**

```bash
git add src/main/java/com/TKCCOPL/item/SynapticSerumItem.java
git add src/main/java/com/TKCCOPL/block/entity/SerumBottlerBlockEntity.java
git commit -m "feat(event): 触发 SerumConsumeEvent 和 SerumCraftEvent"
```

---

## Phase 4: API 门面

### Task 13: DTO Record 类

**Files:**
- Create: `src/main/java/com/TKCCOPL/api/IncubatorInfo.java`
- Create: `src/main/java/com/TKCCOPL/api/BottlerInfo.java`
- Create: `src/main/java/com/TKCCOPL/api/CondenserInfo.java`
- Create: `src/main/java/com/TKCCOPL/api/SplicerInfo.java`
- Create: `src/main/java/com/TKCCOPL/api/SerumEffectInfo.java`

- [ ] **Step 1: 创建 IncubatorInfo.java**

```java
package com.TKCCOPL.api;

import net.minecraft.world.item.ItemStack;

/** 培养槽状态（只读快照） */
public record IncubatorInfo(
    int nutrition, int purity, int dataSignal,
    int growthPercent, int estimatedSeconds,
    boolean hasSeed, ItemStack seed
) {
    public IncubatorInfo {
        if (seed != null) seed = seed.copy(); // defensive copy
    }
}
```

- [ ] **Step 2: 创建 BottlerInfo.java**

```java
package com.TKCCOPL.api;

import net.minecraft.world.item.ItemStack;

/** 灌装机状态（只读快照） */
public record BottlerInfo(
    int progress, int maxProgress,
    int activeRecipe, ItemStack output,
    int activity
) {
    public BottlerInfo {
        if (output != null) output = output.copy();
    }
}
```

- [ ] **Step 3: 创建 CondenserInfo.java**

```java
package com.TKCCOPL.api;

/** 冷凝器状态（只读快照） */
public record CondenserInfo(
    int progress, int maxProgress,
    int stock, int maxStock,
    boolean isFull
) {}
```

- [ ] **Step 4: 创建 SplicerInfo.java**

```java
package com.TKCCOPL.api;

import net.minecraft.world.item.ItemStack;

/** 拼接机状态（只读快照） */
public record SplicerInfo(
    ItemStack seedA, ItemStack seedB,
    ItemStack output, boolean hasOutput,
    int inputCount
) {
    public SplicerInfo {
        if (seedA != null) seedA = seedA.copy();
        if (seedB != null) seedB = seedB.copy();
        if (output != null) output = output.copy();
    }
}
```

- [ ] **Step 5: 创建 SerumEffectInfo.java**

```java
package com.TKCCOPL.api;

/** 血清效果参数（只读） */
public record SerumEffectInfo(
    String effectId,
    int baseDuration,
    int baseAmplifier,
    double durationMultiplier,
    int activity
) {}
```

- [ ] **Step 6: 编译验证**

```bash
./gradlew compileJava
```

预期：BUILD SUCCESSFUL

- [ ] **Step 7: 提交**

```bash
git add src/main/java/com/TKCCOPL/api/
git commit -m "feat(api): 新增 5 个只读 DTO record"
```

---

### Task 14: CyberCultivatorAPI 门面类

**Files:**
- Create: `src/main/java/com/TKCCOPL/api/CyberCultivatorAPI.java`

- [ ] **Step 1: 创建 CyberCultivatorAPI.java**

```java
package com.TKCCOPL.api;

import com.TKCCOPL.block.entity.*;
import com.TKCCOPL.curios.CuriosCompat;
import com.TKCCOPL.cybercultivator;
import com.TKCCOPL.item.GeneticSeedItem;
import com.TKCCOPL.item.SynapticSerumItem;
import com.TKCCOPL.recipe.ModRecipeTypes;
import com.TKCCOPL.recipe.SerumRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fml.ModList;

import java.util.Collections;
import java.util.List;

/**
 * Cyber-Cultivator 公开 API 门面类。
 * 所有方法 null 安全，供第三方模组调用。
 */
public final class CyberCultivatorAPI {
    private CyberCultivatorAPI() {}

    // === 基因数据 API ===

    /** 读取种子基因值 (1-10)，无数据返回 1 */
    public static int getGene(ItemStack seed, String geneKey) {
        return GeneticSeedItem.getGene(seed, geneKey);
    }

    /** 设置种子基因值（clamp 至 1-10） */
    public static void setGene(ItemStack seed, String geneKey, int value) {
        GeneticSeedItem.setGene(seed, geneKey, value);
    }

    /** 读取种子代数，无数据返回 0 */
    public static int getGeneration(ItemStack seed) {
        return GeneticSeedItem.getGeneration(seed);
    }

    /** 读取协同基因值 (0-10)，无数据返回 0 */
    public static int getSynergy(ItemStack seed) {
        return GeneticSeedItem.getSynergy(seed);
    }

    // === 机器状态 API ===

    /** 获取培养槽状态快照，位置无效返回 null */
    public static IncubatorInfo getIncubatorInfo(Level level, BlockPos pos) {
        if (level == null || pos == null) return null;
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof BioIncubatorBlockEntity incubator)) return null;
        return new IncubatorInfo(
                incubator.getNutrition(),
                incubator.getPurity(),
                incubator.getDataSignal(),
                incubator.getGrowthPercent(),
                incubator.getEstimatedSecondsRemaining(),
                incubator.hasSeed(),
                incubator.getSeed()
        );
    }

    /** 获取灌装机状态快照 */
    public static BottlerInfo getBottlerInfo(Level level, BlockPos pos) {
        if (level == null || pos == null) return null;
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof SerumBottlerBlockEntity bottler)) return null;
        return new BottlerInfo(
                bottler.getProgress(),
                bottler.getMaxProgress(),
                bottler.getActiveRecipe(),
                bottler.getOutput(),
                SerumBottlerBlockEntity.getActivity(bottler.getOutput())
        );
    }

    /** 获取冷凝器状态快照 */
    public static CondenserInfo getCondenserInfo(Level level, BlockPos pos) {
        if (level == null || pos == null) return null;
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof AtmosphericCondenserBlockEntity condenser)) return null;
        return new CondenserInfo(
                condenser.getProgress(),
                condenser.getMaxProgress(),
                condenser.getStock(),
                condenser.getMaxStock(),
                condenser.getStock() >= condenser.getMaxStock()
        );
    }

    /** 获取拼接机状态快照 */
    public static SplicerInfo getSplicerInfo(Level level, BlockPos pos) {
        if (level == null || pos == null) return null;
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof GeneSplicerBlockEntity splicer)) return null;
        return new SplicerInfo(
                splicer.getSeedA(),
                splicer.getSeedB(),
                splicer.getOutput(),
                splicer.hasOutput(),
                splicer.getInputCount()
        );
    }

    // === 血清配方 API ===

    /** 查询所有血清配方 */
    public static List<SerumRecipe> getSerumRecipes(Level level) {
        if (level == null) return Collections.emptyList();
        return level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.SERUM_BOTTLING.get());
    }

    /** 计算 Activity 值 */
    public static int calculateActivity(ItemStack[] inputs) {
        return SerumBottlerBlockEntity.calculateActivity(inputs);
    }

    /** 查询血清效果参数 */
    public static SerumEffectInfo getSerumEffectInfo(ItemStack serum) {
        if (serum == null) return null;
        int activity = SynapticSerumItem.getActivity(serum);
        double multiplier = 0.5 + activity * 0.1;
        int baseAmp = Math.min(SynapticSerumItem.getBaseAmplifier(activity)
                + SynapticSerumItem.getActivityBonusAmplifier(activity), 7);
        return new SerumEffectInfo(
                serum.getItem().getDescriptionId(),
                0, // baseDuration 由具体血清类型决定
                baseAmp,
                multiplier,
                activity
        );
    }

    // === 版本/兼容信息 ===

    public static String getModVersion() {
        return ModList.get().getModContainerById(cybercultivator.MODID)
                .map(c -> c.getModInfo().getVersion().toString())
                .orElse("unknown");
    }

    public static boolean isCuriosLoaded() {
        return CuriosCompat.isCuriosLoaded();
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
./gradlew compileJava
```

预期：BUILD SUCCESSFUL

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/TKCCOPL/api/CyberCultivatorAPI.java
git commit -m "feat(api): 新增 CyberCultivatorAPI 门面类"
```

---

## Phase 5: JEI 集成

### Task 15: 配置 JEI 依赖

**Files:**
- Modify: `build.gradle`
- Modify: `gradle.properties`
- Modify: `src/main/resources/META-INF/mods.toml`

- [ ] **Step 1: build.gradle 添加 JEI 依赖**

在 `dependencies` 块中，Curios 依赖之后添加：

```groovy
compileOnly fg.deobf("mezz.jei:jei-${minecraft_version}-forge:${jei_version}:api")
runtimeOnly fg.deobf("mezz.jei:jei-${minecraft_version}-forge:${jei_version}")
```

- [ ] **Step 2: gradle.properties 添加版本号**

在文件末尾添加：

```properties
jei_version=15.2.0.27
```

- [ ] **Step 3: mods.toml 添加 JEI 可选依赖**

在现有 Curios 依赖声明之后添加：

```toml
[[dependencies.cybercultivator]]
modId = "jei"
mandatory = false
ordering = "AFTER"
side = "CLIENT"
```

- [ ] **Step 4: 编译验证**

```bash
./gradlew compileJava
```

预期：BUILD SUCCESSFUL

- [ ] **Step 5: 提交**

```bash
git add build.gradle gradle.properties src/main/resources/META-INF/mods.toml
git commit -m "chore: 添加 JEI compileOnly 依赖"
```

---

### Task 16: CyberCultivatorJEIPlugin 入口

**Files:**
- Create: `src/main/java/com/TKCCOPL/compat/jei/CyberCultivatorJEIPlugin.java`

- [ ] **Step 1: 创建 CyberCultivatorJEIPlugin.java**

```java
package com.TKCCOPL.compat.jei;

import com.TKCCOPL.cybercultivator;
import com.TKCCOPL.init.ModItems;
import com.TKCCOPL.recipe.ModRecipeTypes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

@JeiPlugin
public class CyberCultivatorJEIPlugin implements IModPlugin {
    private static final ResourceLocation UID = new ResourceLocation(cybercultivator.MODID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new SerumBottlingCategory(guiHelper),
                new GeneSplicingCategory(guiHelper),
                new IncubatorOutputCategory(guiHelper)
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;

        // 血清灌装配方
        registration.addRecipes(
                SerumBottlingCategory.RECIPE_TYPE,
                level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.SERUM_BOTTLING.get())
        );

        // 基因拼接配方
        registration.addRecipes(
                GeneSplicingCategory.RECIPE_TYPE,
                GeneSplicingCategory.buildRecipes()
        );

        // 培养槽产出
        registration.addRecipes(
                IncubatorOutputCategory.RECIPE_TYPE,
                IncubatorOutputCategory.buildRecipes()
        );
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(
                new ItemStack(ModItems.SERUM_BOTTLER_ITEM.get()),
                SerumBottlingCategory.RECIPE_TYPE
        );
        registration.addRecipeCatalyst(
                new ItemStack(ModItems.GENE_SPLICER_ITEM.get()),
                GeneSplicingCategory.RECIPE_TYPE
        );
        registration.addRecipeCatalyst(
                new ItemStack(ModItems.BIO_INCUBATOR_ITEM.get()),
                IncubatorOutputCategory.RECIPE_TYPE
        );
    }
}
```

- [ ] **Step 2: 暂存**

---

### Task 17: SerumBottlingCategory

**Files:**
- Create: `src/main/java/com/TKCCOPL/compat/jei/SerumBottlingCategory.java`

- [ ] **Step 1: 创建 SerumBottlingCategory.java**

```java
package com.TKCCOPL.compat.jei;

import com.TKCCOPL.cybercultivator;
import com.TKCCOPL.init.ModItems;
import com.TKCCOPL.recipe.SerumRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class SerumBottlingCategory implements IRecipeCategory<SerumRecipe> {
    public static final RecipeType<SerumRecipe> RECIPE_TYPE =
            new RecipeType<>(new ResourceLocation(cybercultivator.MODID, "serum_bottling"), SerumRecipe.class);

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(cybercultivator.MODID, "textures/gui/jei_serum_bottling.png");

    private final IDrawable background;
    private final IDrawable icon;

    public SerumBottlingCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(TEXTURE, 0, 0, 120, 40);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.SERUM_BOTTLER_ITEM.get()));
    }

    @Override
    public RecipeType<SerumRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.cybercultivator.serum_bottling");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, SerumRecipe recipe, IFocusGroup focuses) {
        Ingredient[] inputs = recipe.getInputs();
        for (int i = 0; i < inputs.length; i++) {
            builder.addSlot(RecipeIngredientRole.INPUT, 1 + i * 18, 1)
                    .addIngredients(inputs[i]);
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, 91, 11)
                .addItemStack(recipe.getBaseOutput());
    }
}
```

- [ ] **Step 2: 创建 JEI 纹理占位文件**

创建一个简单的 120x40 PNG 纹理文件（或使用现有纹理作为占位）：
`src/main/resources/assets/cybercultivator/textures/gui/jei_serum_bottling.png`

（实际开发时需要绘制纹理，此处先用占位）

- [ ] **Step 3: 暂存**

---

### Task 18: GeneSplicingCategory

**Files:**
- Create: `src/main/java/com/TKCCOPL/compat/jei/GeneSplicingCategory.java`

- [ ] **Step 1: 创建 GeneSplicingCategory.java**

```java
package com.TKCCOPL.compat.jei;

import com.TKCCOPL.cybercultivator;
import com.TKCCOPL.init.ModItems;
import com.TKCCOPL.item.GeneticSeedItem;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class GeneSplicingCategory implements IRecipeCategory<GeneSplicingCategory.DisplayRecipe> {
    public static final RecipeType<DisplayRecipe> RECIPE_TYPE =
            new RecipeType<>(new ResourceLocation(cybercultivator.MODID, "gene_splicing"), DisplayRecipe.class);

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(cybercultivator.MODID, "textures/gui/jei_gene_splicing.png");

    private final IDrawable background;
    private final IDrawable icon;

    /** JEI 展示用配方数据 */
    public record DisplayRecipe(ItemStack seedA, ItemStack seedB, String description) {}

    public GeneSplicingCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(TEXTURE, 0, 0, 120, 40);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.GENE_SPLICER_ITEM.get()));
    }

    @Override
    public RecipeType<DisplayRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.cybercultivator.gene_splicing");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, DisplayRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 11)
                .addItemStack(recipe.seedA());
        builder.addSlot(RecipeIngredientRole.INPUT, 37, 11)
                .addItemStack(recipe.seedB());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 91, 11)
                .addItemStack(new ItemStack(ModItems.FIBER_REED_SEEDS.get()));
    }

    /** 构建展示用配方列表 */
    public static List<DisplayRecipe> buildRecipes() {
        List<DisplayRecipe> recipes = new ArrayList<>();
        // 展示不同种子类型的拼接示例
        ItemStack fiber = new ItemStack(ModItems.FIBER_REED_SEEDS.get());
        ItemStack soy = new ItemStack(ModItems.PROTEIN_SOY_SEEDS.get());
        ItemStack bloom = new ItemStack(ModItems.ALCOHOL_BLOOM_SEEDS.get());

        recipes.add(new DisplayRecipe(fiber, soy, "同类种子拼接"));
        recipes.add(new DisplayRecipe(fiber, bloom, "跨类型拼接"));
        return recipes;
    }
}
```

- [ ] **Step 2: 暂存**

---

### Task 19: IncubatorOutputCategory

**Files:**
- Create: `src/main/java/com/TKCCOPL/compat/jei/IncubatorOutputCategory.java`

- [ ] **Step 1: 创建 IncubatorOutputCategory.java**

```java
package com.TKCCOPL.compat.jei;

import com.TKCCOPL.cybercultivator;
import com.TKCCOPL.init.ModItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class IncubatorOutputCategory implements IRecipeCategory<IncubatorOutputCategory.DisplayRecipe> {
    public static final RecipeType<DisplayRecipe> RECIPE_TYPE =
            new RecipeType<>(new ResourceLocation(cybercultivator.MODID, "incubator_output"), DisplayRecipe.class);

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(cybercultivator.MODID, "textures/gui/jei_incubator_output.png");

    private final IDrawable background;
    private final IDrawable icon;

    /** JEI 展示用配方数据 */
    public record DisplayRecipe(ItemStack seed, ItemStack output, String cropName) {}

    public IncubatorOutputCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(TEXTURE, 0, 0, 120, 40);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.BIO_INCUBATOR_ITEM.get()));
    }

    @Override
    public RecipeType<DisplayRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.cybercultivator.incubator_output");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, DisplayRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 11)
                .addItemStack(recipe.seed());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 91, 11)
                .addItemStack(recipe.output());
    }

    /** 构建展示用配方列表 */
    public static List<DisplayRecipe> buildRecipes() {
        List<DisplayRecipe> recipes = new ArrayList<>();
        recipes.add(new DisplayRecipe(
                new ItemStack(ModItems.FIBER_REED_SEEDS.get()),
                new ItemStack(ModItems.PLANT_FIBER.get()),
                "纤维草"
        ));
        recipes.add(new DisplayRecipe(
                new ItemStack(ModItems.PROTEIN_SOY_SEEDS.get()),
                new ItemStack(ModItems.BIOCHEMICAL_SOLUTION.get()),
                "蛋白质豆"
        ));
        recipes.add(new DisplayRecipe(
                new ItemStack(ModItems.ALCOHOL_BLOOM_SEEDS.get()),
                new ItemStack(ModItems.INDUSTRIAL_ETHANOL.get()),
                "酒精花"
        ));
        return recipes;
    }
}
```

- [ ] **Step 2: 创建 JEI 纹理占位文件**

`src/main/resources/assets/cybercultivator/textures/gui/jei_gene_splicing.png`
`src/main/resources/assets/cybercultivator/textures/gui/jei_incubator_output.png`

（实际开发时需要绘制纹理）

- [ ] **Step 3: 编译验证（Phase 5 全部）**

```bash
./gradlew compileJava
```

预期：BUILD SUCCESSFUL

- [ ] **Step 4: 提交**

```bash
git add src/main/java/com/TKCCOPL/compat/
git add src/main/resources/assets/cybercultivator/textures/gui/
git commit -m "feat(jei): 新增 JEI 配方类别（血清灌装/基因拼接/培养槽产出）"
```

---

## Phase 6: 最终验证

### Task 20: 完整构建验证

- [ ] **Step 1: 编译检查**

```bash
./gradlew compileJava
```

预期：BUILD SUCCESSFUL

- [ ] **Step 2: 构建 jar**

```bash
./gradlew build
```

预期：BUILD SUCCESSFUL，生成 `build/libs/cybercultivator-*.jar`

- [ ] **Step 3: 运行客户端冒烟测试**

```bash
./gradlew runClient
```

验证项：
- 进入世界无崩溃
- 打开创造栏，检查 JEI 是否显示血清灌装/基因拼接/培养槽产出类别
- 放置灌装机，放入材料，确认 JEI 配方查看正常

- [ ] **Step 4: 最终提交**

```bash
git add -A
git commit -m "release: v1.2.0 模组兼容 API + JEI 集成"
```

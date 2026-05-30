# Mod Compatibility & API Design Spec

**Date:** 2026-05-30
**Mod:** Cyber-Cultivator: Bio-Synthesis v1.1.2
**Target:** Forge 1.20.1

## Goal

Expose Cyber-Cultivator's recipe, gene, and machine systems as a public API, enabling JEI recipe viewing, KubeJS scripting, and third-party mod integration.

## Scope

**In scope:**
- JEI recipe viewer integration (4 recipe categories)
- Forge RecipeType for serum bottler recipes (JSON data-driven)
- Public API facade (`CyberCultivatorAPI`) with read-only DTOs
- Custom Forge events for gene splicing, crop maturity, serum crafting, serum consumption

**Out of scope (future work):**
- KubeJS plugin (requires separate KubeJS compileOnly dependency)
- Patchouli guidebook
- TOP/WTHIT/Jade block info overlay
- Create mechanical compatibility
- Automation pipe beyond existing WorldlyContainer

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                    Cyber-Cultivator                   │
├─────────────┬─────────────┬─────────────┬───────────┤
│  RecipeType │ Custom API  │  Event Bus  │ compat/   │
│  (JSON驱动)  │ (DTO只读)    │ (Forge事件)  │ (compileOnly)│
├─────────────┴─────────────┴─────────────┴───────────┤
│           核心游戏逻辑（BlockEntity/Item/Effect）       │
└─────────────────────────────────────────────────────┘
         │              │              │
    ┌────▼────┐   ┌────▼────┐   ┌────▼────┐
    │   JEI   │   │ KubeJS  │   │ 其他Mod  │
    │(compileOnly)│(compileOnly)│ (API调用) │
    └─────────┘   └─────────┘   └─────────┘
```

**Layering strategy:**
- **RecipeType layer:** Serum bottler recipes (JSON data-driven, JEI auto-discovery)
- **Custom API layer:** Gene splicer / Incubator (algorithm-driven, exposed via static registry)
- **Event Bus layer:** GeneSpliceEvent / CropMatureEvent / SerumCraftEvent / SerumConsumeEvent
- **compat layer:** `compat/jei/` package, compileOnly dependency

## 1. Recipe System

### 1.1 Serum Bottler → Forge RecipeType

New `RecipeType<SerumRecipe>` registered via `ModRecipeTypes`.

**SerumRecipe structure:**
```java
public class SerumRecipe implements Recipe<SimpleContainer> {
    Ingredient[] inputs;     // 3 inputs (JSON defined)
    ItemStack baseOutput;    // base output item (JSON defined)
    int processingTime;      // processing time (JSON defined, default 300)
    boolean inheritActivity; // inherit Activity from inputs (JSON defined)
    boolean inheritMutation; // inherit Mutation tag from inputs (JSON defined)
}
```

**JSON recipe example** (`data/cybercultivator/recipes/serum/s01.json`):
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

**4 recipes to define:**
1. Berry synthesis: plant_fiber + industrial_ethanol + biochemical_solution → synaptic_neural_berry (inherit_activity=true, inherit_mutation=true)
2. S-01: synaptic_neural_berry + biochemical_solution + glass_bottle → synaptic_serum_s01 (inherit_activity=true)
3. S-02: synaptic_neural_berry + rare_earth_dust + glass_bottle → synaptic_serum_s02 (inherit_activity=true)
4. S-03: synaptic_neural_berry + industrial_ethanol + glass_bottle → synaptic_serum_s03 (inherit_activity=true)

**NBT transfer logic** (kept in code, not JSON):
- Activity calculation: `calculateActivity(inputs)` for berry recipe
- Activity inheritance: `getActivity(berry)` for serum recipes
- Mutation tag inheritance: scan inputs for Mutation/MutationDetail, take highest type

### 1.2 Gene Splicer → Custom API

Not JSON-driven (algorithm-driven). Exposed via `ModRecipes` static registry.

```java
public interface IGeneSpliceRecipe {
    int[] calculateOffspring(int speedA, int yieldA, int potencyA,
                             int speedB, int yieldB, int potencyB,
                             RandomSource random);
    double getMutationChance(int generation, int geneDifference);
}
```

### 1.3 Incubator Output → Custom API

Not JSON-driven (gene-dependent). Exposed via `ModRecipes` static registry.

```java
public interface IIncubatorOutput {
    ItemStack getOutput(ItemStack seed);
    double getGrowthMultiplier(int geneSpeed);
}
```

### 1.4 JEI Recipe Categories

| Category | Recipe Source | Display Content |
|----------|-------------|-----------------|
| Serum Bottling | `RecipeManager.getAllRecipesFor(SERUM_RECIPE)` | 3 inputs → 1 output + Activity |
| Gene Splicing | `ModRecipes.getSPLICE_RECIPES()` | Parent A + B → Offspring + mutation chance |
| Incubator Output | `ModRecipes.getINCUBATOR_OUTPUTS()` | Seed → Crop output + gene multiplier |
| Machine Crafting | Standard `CraftingRecipe` | Already exists, JEI auto-discovers |

## 2. Event System

### 2.1 GeneSpliceEvent

**Fired in:** `GeneSplicerBlockEntity.craftOutput()` after step 7 (mutation marking)
**Cancelable:** Yes
**Fields (all modifiable):**
- `seedA`, `seedB` (input seeds, read-only)
- `speed`, `yield`, `potency` (calculated offspring genes, modifiable)
- `synergy` (modifiable)
- `generation` (modifiable)
- `isMutation` (modifiable)
- `mutationType` (0=none, 1=numerical, 2=synergy, modifiable)
- `mutationDetail` (modifiable)

### 2.2 CropMatureEvent

**Fired in:** `BioIncubatorBlockEntity.tick()` after `getCropOutput()`, before `Containers.dropItemStack()`
**Cancelable:** Yes
**Fields:**
- `seed` (input seed, read-only)
- `output` (calculated crop output, modifiable)

### 2.3 SerumCraftEvent

**Fired in:** `SerumBottlerBlockEntity.getRecipeOutput()` after Activity calculation
**Cancelable:** Yes
**Fields:**
- `inputs` (ItemStack[3], read-only)
- `output` (modifiable)
- `activity` (modifiable)
- `recipeIndex` (0=berry, 1=S01, 2=S02, 3=S03, read-only)

### 2.4 SerumConsumeEvent

**Fired in:** `SynapticSerumItem.finishUsingItem()` before `entity.addEffect()`
**Cancelable:** Yes (cancel prevents effect application)
**Fields:**
- `serum` (ItemStack, read-only)
- `activity` (modifiable)
- `duration` (modifiable)
- `amplifier` (modifiable)
- `effect` (MobEffect, read-only)

### KubeJS usage example:
```javascript
onEvent('cybercultivator.gene_splice', event => {
    event.setSpeed(event.getSpeed() + 2); // All splicing results Speed +2
})

onEvent('cybercultivator.serum_consume', event => {
    if (event.getActivity() >= 8) {
        event.setDuration(event.getDuration() * 2); // High-quality serum duration doubled
    }
})
```

## 3. Public API

### 3.1 CyberCultivatorAPI Facade

```java
public final class CyberCultivatorAPI {
    // Gene data API
    public static int getGene(ItemStack seed, String geneKey);
    public static void setGene(ItemStack seed, String geneKey, int value);
    public static int getGeneration(ItemStack seed);
    public static int getSynergy(ItemStack seed);

    // Machine state API (returns read-only DTOs)
    public static IncubatorInfo getIncubatorInfo(Level level, BlockPos pos);
    public static BottlerInfo getBottlerInfo(Level level, BlockPos pos);
    public static CondenserInfo getCondenserInfo(Level level, BlockPos pos);
    public static SplicerInfo getSplicerInfo(Level level, BlockPos pos);

    // Serum recipe API
    public static List<SerumRecipe> getSerumRecipes(Level level);
    public static int calculateActivity(ItemStack[] inputs);
    public static SerumEffectInfo getSerumEffectInfo(ItemStack serum);

    // Meta
    public static String getModVersion();
    public static boolean isCuriosLoaded();
}
```

### 3.2 Read-Only DTOs (Java Records)

**Note:** `ItemStack` fields use defensive copies (`stack.copy()`) in constructors to prevent external mutation.

```java
public record IncubatorInfo(
    int nutrition, int purity, int dataSignal,
    int growthPercent, int estimatedSeconds,
    boolean hasSeed, ItemStack seed
) {}

public record BottlerInfo(
    int progress, int maxProgress,
    int activeRecipe, ItemStack output,
    int activity
) {}

public record CondenserInfo(
    int progress, int maxProgress,
    int stock, int maxStock,
    boolean isFull
) {}

public record SplicerInfo(
    ItemStack seedA, ItemStack seedB,
    ItemStack output, boolean hasOutput,
    int inputCount
) {}

public record SerumEffectInfo(
    String effectId,
    int baseDuration,
    int baseAmplifier,
    double durationMultiplier,
    int activity
) {}
```

### 3.3 API Design Rules

- All methods null-safe (return defaults or Optional)
- No compileOnly dependencies in API package (pure Forge API)
- DTOs are immutable records (no setters)
- Gene key constants exposed as `GeneticSeedItem.GENE_SPEED` etc.

## 4. File Structure

```
src/main/java/com/TKCCOPL/
├── api/                              # NEW: Public API
│   ├── CyberCultivatorAPI.java       # Facade class
│   ├── IncubatorInfo.java            # Incubator DTO (record)
│   ├── BottlerInfo.java              # Bottler DTO (record)
│   ├── CondenserInfo.java            # Condenser DTO (record)
│   ├── SplicerInfo.java              # Splicer DTO (record)
│   └── SerumEffectInfo.java          # Serum effect DTO (record)
├── recipe/                           # NEW: Recipe system
│   ├── ModRecipeTypes.java           # RecipeType registration
│   ├── ModRecipes.java               # Static registry (gene splicer / incubator recipes)
│   ├── SerumRecipe.java              # Serum recipe (JSON data-driven)
│   └── SerumRecipeSerializer.java    # Serializer
├── event/                            # NEW: Custom events
│   ├── GeneSpliceEvent.java          # Splice event
│   ├── CropMatureEvent.java          # Maturity event
│   ├── SerumCraftEvent.java          # Bottler craft event
│   └── SerumConsumeEvent.java        # Serum consume event
├── compat/                           # NEW: Compatibility layer
│   └── jei/
│       ├── CyberCultivatorJEIPlugin.java   # @JeiPlugin entry
│       ├── SerumBottlingCategory.java       # Bottler JEI category
│       ├── GeneSplicingCategory.java        # Splicer JEI category
│       └── IncubatorOutputCategory.java     # Incubator JEI category
├── block/entity/                     # MODIFY: Inject events + recipe lookup
│   ├── BioIncubatorBlockEntity.java  # Fire CropMatureEvent on maturity
│   ├── GeneSplicerBlockEntity.java   # Fire GeneSpliceEvent on splice
│   └── SerumBottlerBlockEntity.java  # matchRecipe() → RecipeManager
├── item/
│   └── SynapticSerumItem.java        # Fire SerumConsumeEvent on consume
└── (existing files unchanged)
```

**New files:** 16
**Modified files:** 4

## 5. Dependencies

### build.gradle additions:
```groovy
// JEI (same compileOnly pattern as Curios)
compileOnly fg.deobf("mezz.jei:jei-${minecraft_version}-forge:${jei_version}:api")
runtimeOnly fg.deobf("mezz.jei:jei-${minecraft_version}-forge:${jei_version}")
```

### gradle.properties additions:
```properties
jei_version=15.2.0.27
```

### mods.toml additions:
```toml
[[dependencies.cybercultivator]]
modId = "jei"
mandatory = false
ordering = "AFTER"
side = "CLIENT"
```

## 6. Data Generation

New JSON recipe files in `data/cybercultivator/recipes/serum/`:
- `berry_synthesis.json`
- `s01_bottling.json`
- `s02_bottling.json`
- `s03_bottling.json`

These can be hand-written JSON or generated via a new `ModRecipeProvider` extension.

## 7. Implementation Order

1. **RecipeType foundation** — `ModRecipeTypes`, `SerumRecipe`, `SerumRecipeSerializer`, `ModRecipes`, JSON recipes
2. **BlockEntity refactoring** — `SerumBottlerBlockEntity.matchRecipe()` → RecipeManager (RecipeType must work first)
3. **Event system** — 4 event classes + fire points in BlockEntity/Item
4. **API facade** — `CyberCultivatorAPI` + DTO records
5. **JEI integration** — `compat/jei/` package (depends on RecipeType + BlockEntity refactoring)

## 8. Risks & Mitigations

| Risk | Mitigation |
|------|-----------|
| JEI API version changes | compileOnly pattern isolates runtime |
| SerumRecipe NBT transfer breaks | Unit test Activity calculation + Mutation inheritance |
| Event ordering issues | Events fire before state mutation, cancelable |
| RecipeManager performance | Cache recipe lookup results per tick |
| BlockEntity refactoring breaks existing saves | Keep NBT format backward-compatible |

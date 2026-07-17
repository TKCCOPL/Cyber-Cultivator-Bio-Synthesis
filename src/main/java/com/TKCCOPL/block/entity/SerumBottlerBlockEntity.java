package com.TKCCOPL.block.entity;

import com.TKCCOPL.init.ModBlockEntities;
import com.TKCCOPL.init.ModItems;
import com.TKCCOPL.item.SynapticSerumItem;
import com.TKCCOPL.recipe.ModRecipeTypes;
import com.TKCCOPL.recipe.SerumRecipe;
import com.TKCCOPL.recipe.SerumRecipeIds;
import com.TKCCOPL.recipe.RecipeOrdering;
import com.TKCCOPL.event.SerumCraftEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SerumBottlerBlockEntity extends BlockEntity implements WorldlyContainer {
    private static final String TAG_PROGRESS = "Progress";
    private static final String TAG_MAX_PROGRESS = "MaxProgress";
    private static final String TAG_INPUT = "Input";
    private static final String TAG_OUTPUT = "Output";

    private static final int INPUT_SLOTS = 3;
    private static final int OUTPUT_SLOT = 3;
    private static final String TAG_ACTIVITY = "SynapticActivity";
    private static final String TAG_RECIPE_ID = "RecipeId";

    /**
     * 计算突触活性值。加权平均 (Potency×0.25 + Purity×0.375 + Concentration×0.375)，
     * 按物品种类查找输入，不依赖槽位顺序。
     * Gene_Synergy 直接加成 Activity，突破 10 上限。
     * Gene_Synergy 从输入物品的 NBT 中读取（通过种子突变获得，经培养槽传递到原料，再传递到莓）。
     */
    public static int calculateActivity(ItemStack[] inputs) {
        int potency = 5, purity = 5, concentration = 5;
        int geneSynergy = 0;
        if (inputs == null) return SynapticSerumItem.DEFAULT_ACTIVITY;
        for (ItemStack input : inputs) {
            if (input == null || input.isEmpty()) continue;
            CompoundTag tag = input.getTag();
            if (tag == null) continue;
            if (input.is(ModItems.PLANT_FIBER.get()) && tag.contains("Potency")) {
                potency = clampQuality(tag.getInt("Potency"));
            } else if (input.is(ModItems.INDUSTRIAL_ETHANOL.get()) && tag.contains("Purity")) {
                purity = clampQuality(tag.getInt("Purity"));
            } else if (input.is(ModItems.BIOCHEMICAL_SOLUTION.get()) && tag.contains("Concentration")) {
                concentration = clampQuality(tag.getInt("Concentration"));
            }
            // Gene_Synergy：从任何输入物品的 NBT 中读取（莓或原料）
            if (tag.contains("Gene_Synergy")) {
                geneSynergy = Math.max(geneSynergy, Math.max(0, Math.min(10, tag.getInt("Gene_Synergy"))));
            }
        }

        double raw = potency * 0.25 + purity * 0.375 + concentration * 0.375;
        int activity = (int) Math.round(raw);

        // Gene_Synergy 直接加成 Activity（突破 10 上限）
        int bonus = geneSynergy / 2;
        return Math.max(1, activity + bonus);
    }

    /**
     * 从 ItemStack 读取 SynapticActivity，无 NBT 时返回 5（平衡点）
     * Gene_Synergy 可突破上限，此处不额外 clamp（上限由 calculateActivity 计算）
     */
    public static int getActivity(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return SynapticSerumItem.DEFAULT_ACTIVITY;
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_ACTIVITY)) return SynapticSerumItem.DEFAULT_ACTIVITY;
        return SynapticSerumItem.clampActivity(tag.getInt(TAG_ACTIVITY));
    }

    private static int clampQuality(int value) {
        return Math.max(1, Math.min(10, value));
    }

    private final ItemStack[] inputs = new ItemStack[INPUT_SLOTS];
    private ItemStack output = ItemStack.EMPTY;
    private int progress;
    private int maxProgress;
    private SerumRecipe cachedRecipe;
    private String pendingRecipeId; // 用于 load() 后延迟恢复 cachedRecipe
    private final SimpleContainer recipeContainer = new SimpleContainer(INPUT_SLOTS);
    private boolean inputsDirty = true;

    public SerumBottlerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SERUM_BOTTLER.get(), pos, state);
        for (int i = 0; i < INPUT_SLOTS; i++) {
            inputs[i] = ItemStack.EMPTY;
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SerumBottlerBlockEntity blockEntity) {
        if (level.isClientSide) return;

        boolean changed = false;
        boolean processingCancelled = false;

        // RecipeManager replaces recipe objects on datapack/KubeJS reload.
        // Cancel stale work without consuming inputs.
        if (blockEntity.cachedRecipe != null) {
            var currentRecipe = level.getRecipeManager().byKey(blockEntity.cachedRecipe.getId()).orElse(null);
            if (currentRecipe != blockEntity.cachedRecipe) {
                blockEntity.cachedRecipe = null;
                blockEntity.pendingRecipeId = null;
                blockEntity.progress = 0;
                blockEntity.maxProgress = 0;
                changed = true;
                processingCancelled = true;
            }
        }

        // Task 1: 延迟恢复 cachedRecipe（load() 时 level 为 null）
        if (blockEntity.maxProgress > 0 && blockEntity.cachedRecipe == null && blockEntity.pendingRecipeId != null) {
            ResourceLocation recipeId = ResourceLocation.tryParse(blockEntity.pendingRecipeId);
            var loadedRecipe = recipeId == null ? null : level.getRecipeManager().byKey(recipeId).orElse(null);
            blockEntity.cachedRecipe = loadedRecipe instanceof SerumRecipe serumRecipe ? serumRecipe : null;
            blockEntity.pendingRecipeId = null;
            if (blockEntity.cachedRecipe == null) {
                // 配方已被移除（数据包变更），重置加工状态
                blockEntity.progress = 0;
                blockEntity.maxProgress = 0;
                changed = true;
                processingCancelled = true;
            }
        }

        // Try to start a recipe
        if (!processingCancelled && blockEntity.maxProgress == 0) {
            SerumRecipe recipe = blockEntity.findRecipe();
            if (recipe != null) {
                blockEntity.cachedRecipe = recipe;
                blockEntity.maxProgress = recipe.getProcessingTime();
                blockEntity.progress = 0;
                changed = true;
            }
        }

        // Process current recipe
        if (!processingCancelled && blockEntity.maxProgress > 0) {
            blockEntity.progress++;
            // 每秒同步一次进度到客户端，驱动 HUD 进度条动画
            if (blockEntity.progress % 20 == 0) {
                changed = true;
            }
            if (blockEntity.progress >= blockEntity.maxProgress) {
                // Complete recipe — use cached recipe to avoid TOCTOU
                SerumRecipe recipe = blockEntity.cachedRecipe;
                if (recipe != null && blockEntity.matchesCurrentInputs(recipe)) {
                    ItemStack result = blockEntity.assembleRecipe(recipe);
                    if (!result.isEmpty() && blockEntity.canAcceptOutput(result)) {
                        blockEntity.consumeRecipeInputs(recipe);
                        if (blockEntity.output.isEmpty()) {
                            blockEntity.output = result;
                        } else {
                            blockEntity.output.grow(result.getCount());
                        }
                    }
                }
                blockEntity.cachedRecipe = null;
                blockEntity.progress = 0;
                blockEntity.maxProgress = 0;
                changed = true;
            }
        }

        if (changed) {
            blockEntity.syncToClient();
        }
    }

    /**
     * @return 匹配的 SerumRecipe，无匹配返回 null
     */
    private SerumRecipe findRecipe() {
        if (level == null) return null;
        refreshRecipeContainer();
        SerumRecipe recipe = RecipeOrdering.sorted(level.getRecipeManager()
                .getAllRecipesFor(ModRecipeTypes.SERUM_BOTTLING.get()))
                .stream()
                .filter(r -> r.matches(recipeContainer, level))
                .findFirst()
                .orElse(null);
        if (recipe == null) return null;
        return canAcceptOutput(createRecipeResult(recipe)) ? recipe : null;
    }

    private boolean matchesCurrentInputs(SerumRecipe recipe) {
        if (level == null) return false;
        refreshRecipeContainer();
        return recipe.matches(recipeContainer, level);
    }

    private void refreshRecipeContainer() {
        if (inputsDirty) {
            for (int i = 0; i < INPUT_SLOTS; i++) {
                recipeContainer.setItem(i, inputs[i]);
            }
            inputsDirty = false;
        }
    }

    /**
     * 根据配方组装输出，处理 Activity 继承和 Mutation 标签转移。
     */
    private ItemStack assembleRecipe(SerumRecipe recipe) {
        ItemStack result = createRecipeResult(recipe);

        // Task 4: 深拷贝 inputs（防止事件监听器污染内部状态）
        ItemStack[] copiedInputs = new ItemStack[inputs.length];
        for (int i = 0; i < inputs.length; i++) {
            copiedInputs[i] = inputs[i].copy();
        }
        // 触发 SerumCraftEvent，允许其他 mod 修改输出
        ResourceLocation recipeId = getActiveRecipeId();
        SerumCraftEvent craftEvent = new SerumCraftEvent(copiedInputs, result, getActivity(result), recipeId);
        if (net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(craftEvent)) {
            return ItemStack.EMPTY; // 事件被取消
        }
        result = craftEvent.getOutput();
        if (result == null || result.isEmpty()) return ItemStack.EMPTY;
        if (craftEvent.getActivity() > 0) {
            result.getOrCreateTag().putInt(TAG_ACTIVITY, SynapticSerumItem.clampActivity(craftEvent.getActivity()));
        }

        return result;
    }

    /** Build the deterministic recipe result without firing integration events. */
    private ItemStack createRecipeResult(SerumRecipe recipe) {
        ItemStack result = recipe.getBaseOutput();

        // 莓合成：从三种原料计算 Activity
        if (recipe.isInheritActivity() && result.is(ModItems.SYNAPTIC_NEURAL_BERRY.get())) {
            int activity = calculateActivity(inputs);
            result.getOrCreateTag().putInt(TAG_ACTIVITY, activity);
        }

        // Mutation 标签继承（仅莓合成）
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

        // 血清配方（S-01/S-02/S-03）：从莓输入直接继承 Activity（仅当配方声明 inheritActivity）
        boolean isSerumOutput = !result.is(ModItems.SYNAPTIC_NEURAL_BERRY.get());
        if (isSerumOutput && recipe.isInheritActivity()) {
            ItemStack berry = findInput(ModItems.SYNAPTIC_NEURAL_BERRY.get());
            if (!berry.isEmpty()) {
                int activity = getActivity(berry);
                result.getOrCreateTag().putInt(TAG_ACTIVITY, activity);
            }
        }

        return result;
    }

    /** Prevent mixed outputs, NBT loss, and stacks above the item's limit. */
    private boolean canAcceptOutput(ItemStack result) {
        if (result == null || result.isEmpty()) return false;
        int slotLimit = Math.min(getMaxStackSize(), result.getMaxStackSize());
        if (result.getCount() > slotLimit) return false;
        if (output.isEmpty()) return true;
        return ItemStack.isSameItemSameTags(output, result)
                && output.getCount() + result.getCount() <= slotLimit;
    }

    private void consumeRecipeInputs(SerumRecipe recipe) {
        for (Ingredient ingredient : recipe.getInputs()) {
            for (int i = 0; i < INPUT_SLOTS; i++) {
                if (!inputs[i].isEmpty() && ingredient.test(inputs[i])) {
                    inputs[i].shrink(1);
                    break;
                }
            }
        }
        markInputsDirty();
    }

    private ItemStack findInput(net.minecraft.world.item.Item item) {
        for (int i = 0; i < INPUT_SLOTS; i++) {
            if (inputs[i].is(item)) return inputs[i];
        }
        return ItemStack.EMPTY;
    }

    public int getProgress() { return progress; }
    public int getMaxProgress() { return maxProgress; }

    /** @deprecated 使用 {@link #getActiveRecipeId()} 替代，支持自定义配方 */
    @Deprecated
    public int getActiveRecipe() {
        return SerumRecipeIds.legacyIndex(getActiveRecipeId());
    }
    /** Task 7: 返回当前配方的 ResourceLocation ID */
    public ResourceLocation getActiveRecipeId() {
        return cachedRecipe != null
                ? cachedRecipe.getId()
                : pendingRecipeId == null ? null : ResourceLocation.tryParse(pendingRecipeId);
    }
    public ItemStack getOutput() { return output; }

    public ItemStack extractOutput() {
        if (output.isEmpty()) return ItemStack.EMPTY;
        ItemStack out = output;
        output = ItemStack.EMPTY;
        syncToClient();
        return out;
    }

    /**
     * Extract the last non-empty input slot (backwards scan).
     * Used by the block's right-click handler to return materials to the player.
     */
    public ItemStack extractLastInput() {
        for (int i = INPUT_SLOTS - 1; i >= 0; i--) {
            if (!inputs[i].isEmpty()) {
                ItemStack out = inputs[i];
                inputs[i] = ItemStack.EMPTY;
                markInputsDirty();
                syncToClient();
                return out;
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * Cancel any in-progress recipe and reset processing state.
     * Called when the player extracts input materials mid-processing.
     */
    public void cancelProcessing() {
        cachedRecipe = null;
        pendingRecipeId = null;
        progress = 0;
        maxProgress = 0;
        syncToClient();
    }

    private void markInputsDirty() {
        inputsDirty = true;
    }

    private void syncToClient() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    // WorldlyContainer implementation
    @Override
    public int getContainerSize() {
        return 4; // 3 input + 1 output
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack input : inputs) {
            if (!input.isEmpty()) return false;
        }
        return output.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        if (slot < INPUT_SLOTS) return inputs[slot];
        if (slot == OUTPUT_SLOT) return output;
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot == OUTPUT_SLOT && !output.isEmpty()) {
            int taken = Math.min(amount, output.getCount());
            ItemStack result = output.split(taken);
            if (output.isEmpty()) output = ItemStack.EMPTY;
            syncToClient(); // 漏斗抽取输出后立即同步到客户端
            return result;
        }
        if (slot < INPUT_SLOTS && !inputs[slot].isEmpty()) {
            int taken = Math.min(amount, inputs[slot].getCount());
            ItemStack result = inputs[slot].split(taken);
            if (inputs[slot].isEmpty()) inputs[slot] = ItemStack.EMPTY;
            // 漏斗抽取输入槽时取消当前加工，防止无材料产出
            if (maxProgress > 0) {
                cancelProcessing();
            } else {
                setChanged();
            }
            markInputsDirty();
            return result;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot == OUTPUT_SLOT) {
            ItemStack out = output;
            output = ItemStack.EMPTY;
            setChanged();
            return out;
        }
        if (slot < INPUT_SLOTS) {
            ItemStack out = inputs[slot];
            inputs[slot] = ItemStack.EMPTY;
            markInputsDirty();
            setChanged();
            return out;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < INPUT_SLOTS) {
            inputs[slot] = stack;
            markInputsDirty();
            setChanged();
        }
    }

    @Override
    public boolean stillValid(net.minecraft.world.entity.player.Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < INPUT_SLOTS; i++) {
            inputs[i] = ItemStack.EMPTY;
        }
        output = ItemStack.EMPTY;
        markInputsDirty();
        setChanged();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) return new int[]{OUTPUT_SLOT};
        if (side == Direction.UP) return new int[]{0, 1, 2};
        return new int[]{0, 1, 2}; // Sides also accept input
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot < INPUT_SLOTS && side != Direction.DOWN;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == OUTPUT_SLOT && side == Direction.DOWN;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        progress = Math.max(0, tag.getInt(TAG_PROGRESS));
        maxProgress = Math.max(0, tag.getInt(TAG_MAX_PROGRESS));
        // 加载配方 ID（延迟恢复，因为 load() 时 level 为 null）
        cachedRecipe = null;
        pendingRecipeId = tag.contains(TAG_RECIPE_ID) ? tag.getString(TAG_RECIPE_ID) : null;
        for (int i = 0; i < INPUT_SLOTS; i++) {
            String key = TAG_INPUT + i;
            inputs[i] = tag.contains(key) ? ItemStack.of(tag.getCompound(key)) : ItemStack.EMPTY;
        }
        output = tag.contains(TAG_OUTPUT) ? ItemStack.of(tag.getCompound(TAG_OUTPUT)) : ItemStack.EMPTY;
        markInputsDirty();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(TAG_PROGRESS, progress);
        tag.putInt(TAG_MAX_PROGRESS, maxProgress);
        // 持久化配方 ID，用于世界重载后恢复
        if (cachedRecipe != null) {
            tag.putString(TAG_RECIPE_ID, cachedRecipe.getId().toString());
        }
        for (int i = 0; i < INPUT_SLOTS; i++) {
            String key = TAG_INPUT + i;
            if (!inputs[i].isEmpty()) {
                tag.put(key, inputs[i].save(new CompoundTag()));
            } else {
                tag.put(key, new CompoundTag()); // sentinel: ensure tag is non-empty for client sync
            }
        }
        if (!output.isEmpty()) {
            tag.put(TAG_OUTPUT, output.save(new CompoundTag()));
        } else {
            tag.put(TAG_OUTPUT, new CompoundTag()); // sentinel: ensure tag is non-empty for client sync
        }
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }
}

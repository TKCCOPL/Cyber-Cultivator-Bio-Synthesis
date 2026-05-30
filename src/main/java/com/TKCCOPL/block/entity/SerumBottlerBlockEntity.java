package com.TKCCOPL.block.entity;

import com.TKCCOPL.init.ModBlockEntities;
import com.TKCCOPL.init.ModItems;
import com.TKCCOPL.recipe.ModRecipeTypes;
import com.TKCCOPL.recipe.SerumRecipe;
import com.TKCCOPL.event.SerumCraftEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SerumBottlerBlockEntity extends BlockEntity implements WorldlyContainer {
    private static final String TAG_PROGRESS = "Progress";
    private static final String TAG_MAX_PROGRESS = "MaxProgress";
    private static final String TAG_INPUT = "Input";
    private static final String TAG_OUTPUT = "Output";

    private static final int PROCESSING_TIME = 300; // 15 seconds
    private static final int INPUT_SLOTS = 3;
    private static final int OUTPUT_SLOT = 3;
    private static final String TAG_ACTIVITY = "SynapticActivity";
    private static final String TAG_ACTIVE_RECIPE = "ActiveRecipe";

    /**
     * 计算突触活性值。加权平均 (Potency×0.25 + Purity×0.375 + Concentration×0.375)，
     * 按物品种类查找输入，不依赖槽位顺序。
     * Gene_Synergy 直接加成 Activity，突破 10 上限。
     * Gene_Synergy 从输入物品的 NBT 中读取（通过种子突变获得，经培养槽传递到原料，再传递到莓）。
     */
    public static int calculateActivity(ItemStack[] inputs) {
        int potency = 5, purity = 5, concentration = 5;
        int geneSynergy = 0;
        for (ItemStack input : inputs) {
            if (input.isEmpty()) continue;
            CompoundTag tag = input.getTag();
            if (tag == null) continue;
            if (input.is(ModItems.PLANT_FIBER.get()) && tag.contains("Potency")) {
                potency = tag.getInt("Potency");
            } else if (input.is(ModItems.INDUSTRIAL_ETHANOL.get()) && tag.contains("Purity")) {
                purity = tag.getInt("Purity");
            } else if (input.is(ModItems.BIOCHEMICAL_SOLUTION.get()) && tag.contains("Concentration")) {
                concentration = tag.getInt("Concentration");
            }
            // Gene_Synergy：从任何输入物品的 NBT 中读取（莓或原料）
            if (tag.contains("Gene_Synergy")) {
                geneSynergy = Math.max(geneSynergy, tag.getInt("Gene_Synergy"));
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
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_ACTIVITY)) return 5;
        return Math.max(1, tag.getInt(TAG_ACTIVITY));
    }

    private final ItemStack[] inputs = new ItemStack[INPUT_SLOTS];
    private ItemStack output = ItemStack.EMPTY;
    private int progress;
    private int maxProgress;
    private int activeRecipe = -1;
    private SerumRecipe cachedRecipe; // 运行时缓存，不持久化

    public SerumBottlerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SERUM_BOTTLER.get(), pos, state);
        for (int i = 0; i < INPUT_SLOTS; i++) {
            inputs[i] = ItemStack.EMPTY;
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SerumBottlerBlockEntity blockEntity) {
        if (level.isClientSide) return;

        boolean changed = false;

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

        // Process current recipe
        if (blockEntity.maxProgress > 0) {
            blockEntity.progress++;
            // 每秒同步一次进度到客户端，驱动 HUD 进度条动画
            if (blockEntity.progress % 20 == 0) {
                changed = true;
            }
            if (blockEntity.progress >= blockEntity.maxProgress) {
                // Complete recipe — use cached recipe to avoid TOCTOU
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
                blockEntity.activeRecipe = -1;
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

        // 触发 SerumCraftEvent，允许其他 mod 修改输出
        int recipeIndex = getActiveRecipe();
        SerumCraftEvent craftEvent = new SerumCraftEvent(inputs.clone(), result, getActivity(result), recipeIndex);
        if (net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(craftEvent)) {
            return ItemStack.EMPTY; // 事件被取消
        }
        result = craftEvent.getOutput();
        if (craftEvent.getActivity() > 0) {
            result.getOrCreateTag().putInt(TAG_ACTIVITY, craftEvent.getActivity());
        }

        return result;
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
    }

    private ItemStack findInput(net.minecraft.world.item.Item item) {
        for (int i = 0; i < INPUT_SLOTS; i++) {
            if (inputs[i].is(item)) return inputs[i];
        }
        return ItemStack.EMPTY;
    }

    public int getProgress() { return progress; }
    public int getMaxProgress() { return maxProgress; }
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
        activeRecipe = -1;
        progress = 0;
        maxProgress = 0;
        syncToClient();
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
            setChanged();
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
            setChanged();
            return out;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < INPUT_SLOTS) {
            inputs[slot] = stack;
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
        activeRecipe = tag.getInt(TAG_ACTIVE_RECIPE);
        for (int i = 0; i < INPUT_SLOTS; i++) {
            String key = TAG_INPUT + i;
            inputs[i] = tag.contains(key) ? ItemStack.of(tag.getCompound(key)) : ItemStack.EMPTY;
        }
        output = tag.contains(TAG_OUTPUT) ? ItemStack.of(tag.getCompound(TAG_OUTPUT)) : ItemStack.EMPTY;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(TAG_PROGRESS, progress);
        tag.putInt(TAG_MAX_PROGRESS, maxProgress);
        tag.putInt(TAG_ACTIVE_RECIPE, activeRecipe);
        for (int i = 0; i < INPUT_SLOTS; i++) {
            if (!inputs[i].isEmpty()) {
                tag.put(TAG_INPUT + i, inputs[i].save(new CompoundTag()));
            }
        }
        if (!output.isEmpty()) {
            tag.put(TAG_OUTPUT, output.save(new CompoundTag()));
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

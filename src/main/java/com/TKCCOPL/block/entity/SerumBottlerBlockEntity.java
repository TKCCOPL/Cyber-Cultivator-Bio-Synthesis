package com.TKCCOPL.block.entity;

import com.TKCCOPL.init.ModBlockEntities;
import com.TKCCOPL.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
     * Gene_Purity 直接加成 Activity，突破 10 上限。
     * Gene_Purity 从输入物品的 NBT 中读取（通过种子突变获得，经培养槽传递到原料，再传递到莓）。
     */
    public static int calculateActivity(ItemStack[] inputs) {
        int potency = 5, purity = 5, concentration = 5;
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
        }

        // 读取 Gene_Purity（从莓的 NBT 中，莓是合成血清的中间产物）
        int genePurity = 0;
        for (ItemStack input : inputs) {
            if (input.isEmpty()) continue;
            CompoundTag tag = input.getTag();
            if (tag != null && tag.contains("Gene_Purity")) {
                genePurity = Math.max(genePurity, tag.getInt("Gene_Purity"));
            }
        }

        double raw = potency * 0.25 + purity * 0.375 + concentration * 0.375;
        int activity = (int) Math.round(raw);

        // Gene_Purity 直接加成 Activity（突破 10 上限）
        int bonus = genePurity / 2;
        return Math.max(1, activity + bonus);
    }

    /**
     * 从 ItemStack 读取 SynapticActivity，无 NBT 时返回 5（平衡点）
     * Gene_Purity 可突破上限，此处不额外 clamp（上限由 calculateActivity 计算）
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
            int recipe = blockEntity.matchRecipe();
            if (recipe >= 0) {
                blockEntity.activeRecipe = recipe;
                blockEntity.maxProgress = PROCESSING_TIME;
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
                // Complete recipe — use cached activeRecipe to avoid TOCTOU
                int recipe = blockEntity.activeRecipe;
                if (recipe >= 0) {
                    // 先获取产出（需要读取输入的 NBT），再消耗输入
                    ItemStack result = blockEntity.getRecipeOutput(recipe);
                    blockEntity.consumeInputs(recipe);
                    if (blockEntity.output.isEmpty()) {
                        blockEntity.output = result;
                    } else {
                        blockEntity.output.grow(result.getCount());
                    }
                }
                blockEntity.activeRecipe = -1;
                blockEntity.progress = 0;
                blockEntity.maxProgress = 0;
                changed = true;
            }
        }

        if (changed) {
            blockEntity.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }

    /**
     * @return recipe index (0=berry, 1=S-01, 2=S-02, 3=S-03), or -1 if no match
     */
    private int matchRecipe() {
        // Berry: plant_fiber + industrial_ethanol + biochemical_solution
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

    private boolean hasIngredients(net.minecraft.world.item.Item... required) {
        boolean[] matched = new boolean[required.length];
        for (int i = 0; i < INPUT_SLOTS; i++) {
            if (inputs[i].isEmpty()) continue;
            for (int j = 0; j < required.length; j++) {
                if (!matched[j] && inputs[i].is(required[j])) {
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

    private boolean canOutput(ItemStack result) {
        if (output.isEmpty()) return true;
        if (!ItemStack.isSameItemSameTags(output, result)) return false;
        return output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    private boolean canOutput(net.minecraft.world.item.Item result) {
        return canOutput(new ItemStack(result));
    }

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

    private ItemStack getRecipeOutput(int recipe) {
        return switch (recipe) {
            case 0 -> {
                int activity = calculateActivity(inputs);
                ItemStack berry = new ItemStack(ModItems.SYNAPTIC_NEURAL_BERRY.get());
                berry.getOrCreateTag().putInt(TAG_ACTIVITY, activity);
                // 莓合成时继承原料的 Gene_Purity（用于后续突破 Activity 上限）
                int maxPurity = 0;
                for (ItemStack input : inputs) {
                    if (input.isEmpty()) continue;
                    CompoundTag tag = input.getTag();
                    if (tag != null && tag.contains("Gene_Purity")) {
                        maxPurity = Math.max(maxPurity, tag.getInt("Gene_Purity"));
                    }
                }
                if (maxPurity > 0) {
                    berry.getOrCreateTag().putInt("Gene_Purity", maxPurity);
                }
                yield berry;
            }
            case 1 -> {
                ItemStack berry = findInput(ModItems.SYNAPTIC_NEURAL_BERRY.get());
                int activity = getActivity(berry);
                ItemStack serum = new ItemStack(ModItems.SYNAPTIC_SERUM_S01.get());
                serum.getOrCreateTag().putInt(TAG_ACTIVITY, activity);
                yield serum;
            }
            case 2 -> {
                ItemStack berry = findInput(ModItems.SYNAPTIC_NEURAL_BERRY.get());
                int activity = getActivity(berry);
                ItemStack serum = new ItemStack(ModItems.SYNAPTIC_SERUM_S02.get());
                serum.getOrCreateTag().putInt(TAG_ACTIVITY, activity);
                yield serum;
            }
            case 3 -> {
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

    public int getProgress() { return progress; }
    public int getMaxProgress() { return maxProgress; }
    public int getActiveRecipe() { return activeRecipe; }
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
        activeRecipe = -1;
        progress = 0;
        maxProgress = 0;
        syncToClient();
    }

    private void syncToClient() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
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

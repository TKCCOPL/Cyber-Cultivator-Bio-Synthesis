package com.TKCCOPL.block.entity;

import com.TKCCOPL.Config;
import com.TKCCOPL.init.ModBlockEntities;
import com.TKCCOPL.init.ModTags;
import com.TKCCOPL.item.GeneticSeedItem;
import com.TKCCOPL.event.CropMatureEvent;
import com.TKCCOPL.menu.BioIncubatorMenu;
import com.TKCCOPL.recipe.ModRecipeTypes;
import com.TKCCOPL.recipe.RecipeOrdering;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class BioIncubatorBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider, MachineRedstoneBlockEntity, MachineInventoryPolicy {
    private static final String TAG_NUTRITION = "Nutrition";
    private static final String TAG_PURITY = "Purity";
    private static final String TAG_DATA_SIGNAL = "DataSignal";
    private static final String TAG_GROWTH_PROGRESS = "GrowthProgress";
    private static final String TAG_SEED = "Seed";
    private static final String TAG_NUTRITION_INPUT = "NutritionInput";
    private static final String TAG_PURITY_INPUT = "PurityInput";
    private static final String TAG_SIGNAL_INPUT = "SignalInput";
    private static final String TAG_RESOURCE_OUTPUT = "ResourceOutput";
    private static final String TAG_BOTTLE_OUTPUT = "BottleOutput";
    private static final String TAG_NEXT_INPUT_INJECTION_TICK = "NextInputInjectionTick";

    public static final int SEED_SLOT = 0;
    public static final int NUTRITION_SLOT = 1;
    public static final int PURITY_SLOT = 2;
    public static final int SIGNAL_SLOT = 3;
    public static final int RESOURCE_OUTPUT_SLOT = 4;
    public static final int BOTTLE_OUTPUT_SLOT = 5;

    private static final int SYNC_INTERVAL = 10;
    public static final int INPUT_INJECTION_INTERVAL_TICKS = 20;

    // ContainerData 索引：0-4 为原有字段，5-7 为红石字段（mode/powered/processingAllowed）
    private static final int DATA_REDSTONE_BASE = 5;

    private int nutrition;
    private int purity;
    private int dataSignal;
    private int growthProgress;
    private ItemStack seed = ItemStack.EMPTY;
    private ItemStack nutritionInput = ItemStack.EMPTY;
    private ItemStack purityInput = ItemStack.EMPTY;
    private ItemStack signalInput = ItemStack.EMPTY;
    private ItemStack resourceOutput = ItemStack.EMPTY;
    private ItemStack bottleOutput = ItemStack.EMPTY;
    private long nextInputInjectionTick;

    /** v1.1.7 红石控制状态 */
    private final MachineRedstoneState redstone = new MachineRedstoneState();

    /** v1.1.7 比较器信号缓存，仅在变化时通知相邻比较器 */
    private int lastComparatorSignal = 0;

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> nutrition;
                case 1 -> purity;
                case 2 -> dataSignal;
                case 3 -> getGrowthPercent();
                case 4 -> getEstimatedSecondsRemaining() + 1;
                // 红石字段（mode/powered/processingAllowed）
                case 5 -> redstone.getMenuData(MachineRedstoneState.DATA_MODE);
                case 6 -> redstone.getMenuData(MachineRedstoneState.DATA_POWERED);
                case 7 -> redstone.getMenuData(MachineRedstoneState.DATA_PROCESSING_ALLOWED);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 8;
        }
    };

    // 基因缓存（避免每 tick 解析 NBT）
    private int cachedSpeed = 1;
    private int syncCounter = 0;

    public BioIncubatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BIO_INCUBATOR.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BioIncubatorBlockEntity blockEntity) {
        if (level.isClientSide) {
            return;
        }

        // v1.1.7 hotfix：执行 clearRemoved 推迟的红石重新采样
        if (blockEntity.redstone.consumePendingResample(level, pos)) {
            blockEntity.setChanged();
            level.sendBlockUpdated(pos, state, state, 2);
        }

        boolean autoInjected = false;
        if (level.getGameTime() >= blockEntity.nextInputInjectionTick) {
            autoInjected = blockEntity.consumeAvailableInputs();
            if (autoInjected) {
                blockEntity.nextInputInjectionTick = level.getGameTime() + INPUT_INJECTION_INTERVAL_TICKS;
            }
        }
        if (blockEntity.seed.isEmpty()) {
            if (autoInjected) blockEntity.syncToClient();
            return;
        }

        boolean changed = autoInjected;
        boolean forceSync = autoInjected;

        // 资源自然衰减（不受红石控制，按 §2.5 设计）
        if (level.getGameTime() % (long) Config.nutritionDecayInterval == 0L && blockEntity.nutrition > 0) {
            blockEntity.nutrition -= 1;
            changed = true;
        }
        if (level.getGameTime() % (long) Config.purityDecayInterval == 0L && blockEntity.purity > 0) {
            blockEntity.purity -= 1;
            changed = true;
        }
        if (level.getGameTime() % (long) Config.dataSignalDecayInterval == 0L && blockEntity.dataSignal > 0) {
            blockEntity.dataSignal -= 1;
            changed = true;
        }

        // v1.1.7 红石门控：生长推进与成熟判定受红石模式控制
        boolean redstoneAllows = blockEntity.redstone.isProcessingAllowed();

        // 生长推进：需要三项资源均高于阈值，且红石允许加工
        if (redstoneAllows
                && blockEntity.nutrition > Config.resourceThreshold
                && blockEntity.purity > Config.resourceThreshold
                && blockEntity.dataSignal > 0) {

            // 计算生长速率：基础速率 * 基因倍率 * 环境倍率
            int geneSpeed = blockEntity.cachedSpeed;
            double geneMultiplier = 0.5 + (geneSpeed / 10.0) * 1.5; // 范围 0.5 - 2.0
            double envMultiplier = (blockEntity.nutrition + blockEntity.purity + blockEntity.dataSignal) / 300.0;
            int growthRate = Math.max(1, (int) Math.round(geneMultiplier * envMultiplier));

            int previousProgress = blockEntity.growthProgress;
            blockEntity.growthProgress = Math.min(Config.maturationThreshold,
                    blockEntity.growthProgress + growthRate);
            if (blockEntity.growthProgress != previousProgress) changed = true;

            // 成熟判定
            if (blockEntity.growthProgress >= Config.maturationThreshold) {
                // 产出作物物品
                ItemStack cropOutput = getCropOutput(level, blockEntity.seed);

                if (!blockEntity.canAcceptResourceOutput(cropOutput)) {
                    blockEntity.growthProgress = Config.maturationThreshold;
                } else {
                    // 触发 CropMatureEvent，允许其他 mod 修改产出
                    CropMatureEvent cropEvent = new CropMatureEvent(level, pos, blockEntity.seed, cropOutput);
                    boolean cancelled = net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(cropEvent);

                    // A cancelled maturity attempt starts a fresh growth cycle while
                    // preserving the seed and resources. Resetting progress prevents
                    // the event from firing again every tick.
                    if (cancelled) {
                        blockEntity.growthProgress = 0;
                        changed = true;
                        forceSync = true;
                    } else {
                        cropOutput = cropEvent.getOutput();
                        if (!blockEntity.canAcceptResourceOutput(cropOutput)) {
                            blockEntity.growthProgress = Config.maturationThreshold;
                        } else {
                            blockEntity.seed = ItemStack.EMPTY;
                            blockEntity.growthProgress = 0;
                            blockEntity.addResourceOutput(cropOutput);
                            blockEntity.nutrition = Math.max(0,
                                    blockEntity.nutrition - Config.matureNutritionCost);
                            blockEntity.purity = Math.max(0,
                                    blockEntity.purity - Config.maturePurityCost);
                            changed = true;
                            forceSync = true;
                        }
                    }
                }
            }
        }

        if (changed) {
            blockEntity.syncCounter++;
            if (forceSync || blockEntity.syncCounter >= SYNC_INTERVAL) {
                blockEntity.syncToClient();
                blockEntity.syncCounter = 0;
            }
        }

        // v1.1.7 比较器信号变化检测（仅在数值变化时通知相邻比较器）
        blockEntity.updateComparatorIfChanged(level, pos);
    }

    /**
     * 根据种子类型返回对应的作物产出物品。
     * 从 RecipeManager 查询 IncubatorOutputRecipe，使用 JSON 数据驱动。
     * 未知种子类型返回种子本身作为保底产出。
     */
    private static ItemStack getCropOutput(Level level, ItemStack seed) {
        if (level == null || seed.isEmpty()) return seed.copy();

        return RecipeOrdering.sorted(level.getRecipeManager()
                .getAllRecipesFor(ModRecipeTypes.INCUBATOR_OUTPUT.get()))
                .stream()
                .filter(r -> r.matches(seed))
                .findFirst()
                .map(r -> r.assemble(seed))
                .orElse(seed.copy()); // 未知种子保底产出种子本身
    }

    public boolean hasSeed() {
        return !seed.isEmpty();
    }

    public ItemStack getSeed() {
        return seed.copy();
    }

    public boolean tryInsertSeed(ItemStack stack) {
        if (!seed.isEmpty()) {
            return false;
        }
        ItemStack inserted = stack.copy();
        inserted.setCount(1);
        // 确保种子携带基因数据，防止缺少 NBT 导致生长计算异常
        if (inserted.getItem() instanceof GeneticSeedItem geneticSeed) {
            geneticSeed.ensureGeneData(inserted);
        }
        seed = inserted;
        cachedSpeed = GeneticSeedItem.getGene(inserted, GeneticSeedItem.GENE_SPEED);
        growthProgress = 0;
        syncCounter = 0;
        syncToClient();
        return true;
    }

    public ItemStack extractSeed() {
        if (seed.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack out = seed;
        seed = ItemStack.EMPTY;
        cachedSpeed = 1;
        growthProgress = 0;
        syncCounter = 0;
        syncToClient();
        return out;
    }

    public void addNutrition(int value) {
        nutrition = clampStat(nutrition + value);
        syncToClient();
    }

    public void addPurity(int value) {
        purity = clampStat(purity + value);
        syncToClient();
    }

    public void addDataSignal(int value) {
        dataSignal = clampStat(dataSignal + value);
        syncToClient();
    }

    private boolean consumeOneInput(int slot) {
        if (slot == NUTRITION_SLOT && !nutritionInput.isEmpty() && nutrition < 100) {
            nutritionInput.shrink(1);
            if (nutritionInput.isEmpty()) nutritionInput = ItemStack.EMPTY;
            nutrition = clampStat(nutrition + Config.nutritionInjectAmount);
            return true;
        }
        if (slot == PURITY_SLOT && !purityInput.isEmpty() && purity < 100 && canAcceptBottle()) {
            purityInput.shrink(1);
            if (purityInput.isEmpty()) purityInput = ItemStack.EMPTY;
            addBottleOutput();
            purity = clampStat(purity + Config.purityInjectAmount);
            return true;
        }
        if (slot == SIGNAL_SLOT && !signalInput.isEmpty() && dataSignal < 100) {
            signalInput.shrink(1);
            if (signalInput.isEmpty()) signalInput = ItemStack.EMPTY;
            dataSignal = clampStat(dataSignal + Config.dataSignalInjectAmount);
            return true;
        }
        return false;
    }

    private boolean consumeAvailableInputs() {
        boolean changed = false;
        if (consumeOneInput(NUTRITION_SLOT)) changed = true;
        if (consumeOneInput(PURITY_SLOT)) changed = true;
        if (consumeOneInput(SIGNAL_SLOT)) changed = true;
        return changed;
    }

    private boolean canAcceptBottle() {
        return bottleOutput.isEmpty()
                || bottleOutput.is(Items.GLASS_BOTTLE) && bottleOutput.getCount() < bottleOutput.getMaxStackSize();
    }

    private void addBottleOutput() {
        if (bottleOutput.isEmpty()) bottleOutput = new ItemStack(Items.GLASS_BOTTLE);
        else bottleOutput.grow(1);
    }

    private boolean canAcceptResourceOutput(ItemStack stack) {
        if (stack.isEmpty()) return true;
        if (resourceOutput.isEmpty()) return stack.getCount() <= stack.getMaxStackSize();
        return ItemStack.isSameItemSameTags(resourceOutput, stack)
                && resourceOutput.getCount() + stack.getCount() <= resourceOutput.getMaxStackSize();
    }

    private void addResourceOutput(ItemStack stack) {
        if (stack.isEmpty()) return;
        if (resourceOutput.isEmpty()) resourceOutput = stack.copy();
        else resourceOutput.grow(stack.getCount());
    }

    private void syncToClient() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    /**
     * v1.1.7 比较器信号缓存检测：仅在数值变化时通知相邻比较器。
     * 避免每 tick 更新比较器网络造成的性能开销。
     */
    private void updateComparatorIfChanged(Level level, BlockPos pos) {
        int current = getComparatorSignal();
        if (current != lastComparatorSignal) {
            lastComparatorSignal = current;
            level.updateNeighbourForOutputSignal(pos, getBlockState().getBlock());
        }
    }

    // === v1.1.7 MachineRedstoneBlockEntity 接口实现 ===

    @Override
    public MachineRedstoneState getRedstoneState() {
        return redstone;
    }

    /**
     * v1.1.7 统一比较器三段语义（0/1-14/15）：
     * <ul>
     *   <li>{@code 15}：成熟产物槽存在可抽取物品</li>
     *   <li>{@code 1..14}：生长中（{@code growthProgress > 0}），按进度比例</li>
     *   <li>{@code 0}：无种子且无产物</li>
     * </ul>
     * 玻璃瓶副产物不触发 15（按 §3.2 主产物定义）。
     */
    @Override
    public int getComparatorSignal() {
        if (!resourceOutput.isEmpty()) return 15;
        if (seed.isEmpty() && growthProgress == 0) return 0;
        // 生长中：按 growthProgress 比例映射到 1..14
        int max = Config.maturationThreshold;
        if (max <= 0) return 1;
        int raw = (int) Math.ceil((double) growthProgress * 14 / max);
        return Math.max(1, Math.min(14, raw));
    }

    /**
     * 区块加载时仅标记需要重新采样；实际采样在首次 tick 时执行。
     *
     * <p>v1.1.7 hotfix：在 post-load 阶段调用 {@code level.hasNeighborSignal}
     * 会触发相邻 chunk 加载，而 spawn area 生成期间 Server thread 自身被阻塞
     * 导致死锁。改为延迟到 tick，此时区块已完全加载。</p>
     */
    @Override
    public void clearRemoved() {
        super.clearRemoved();
        if (level != null && !level.isClientSide) {
            redstone.markPendingResample();
        }
    }

    public int getNutrition() {
        return nutrition;
    }

    public int getPurity() {
        return purity;
    }

    public int getDataSignal() {
        return dataSignal;
    }

    /** 获取生长进度百分比 (0-100) */
    public int getGrowthPercent() {
        if (seed.isEmpty() || Config.maturationThreshold <= 0) return 0;
        return Math.min(100, (int) ((long) growthProgress * 100 / Config.maturationThreshold));
    }

    /** 获取当前生长速率（每 tick 推进量），用于外部估算 */
    public int getCurrentGrowthRate() {
        if (seed.isEmpty()) return 0;
        if (nutrition <= Config.resourceThreshold || purity <= Config.resourceThreshold || dataSignal <= 0) return 0;
        int geneSpeed = cachedSpeed;
        double geneMultiplier = 0.5 + (geneSpeed / 10.0) * 1.5;
        double envMultiplier = (nutrition + purity + dataSignal) / 300.0;
        return Math.max(1, (int) Math.round(geneMultiplier * envMultiplier));
    }

    /** 估算剩余成熟时间（秒），-1 表示无法生长 */
    public int getEstimatedSecondsRemaining() {
        int rate = getCurrentGrowthRate();
        if (rate <= 0) return -1;
        int remaining = Config.maturationThreshold - growthProgress;
        if (remaining <= 0) return 0;
        // 每秒 20 tick
        return (int) Math.ceil(remaining / (double) rate / 20.0);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        nutrition = clampStat(tag.getInt(TAG_NUTRITION));
        purity = clampStat(tag.getInt(TAG_PURITY));
        dataSignal = clampStat(tag.getInt(TAG_DATA_SIGNAL));
        growthProgress = Math.max(0, tag.getInt(TAG_GROWTH_PROGRESS));
        seed = tag.contains(TAG_SEED) ? ItemStack.of(tag.getCompound(TAG_SEED)) : ItemStack.EMPTY;
        nutritionInput = readStack(tag, TAG_NUTRITION_INPUT);
        purityInput = readStack(tag, TAG_PURITY_INPUT);
        signalInput = readStack(tag, TAG_SIGNAL_INPUT);
        resourceOutput = readStack(tag, TAG_RESOURCE_OUTPUT);
        bottleOutput = readStack(tag, TAG_BOTTLE_OUTPUT);
        nextInputInjectionTick = Math.max(0L, tag.getLong(TAG_NEXT_INPUT_INJECTION_TICK));
        // v1.1.7 红石状态（缺失字段默认 IGNORE，不崩溃）
        redstone.load(tag);
        // 初始化基因缓存
        if (!seed.isEmpty()) {
            cachedSpeed = GeneticSeedItem.getGene(seed, GeneticSeedItem.GENE_SPEED);
        } else {
            cachedSpeed = 1;
        }
        syncCounter = 0;
        // 比较器缓存重置，下次 tick 重新检测
        lastComparatorSignal = -1;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(TAG_NUTRITION, nutrition);
        tag.putInt(TAG_PURITY, purity);
        tag.putInt(TAG_DATA_SIGNAL, dataSignal);
        tag.putInt(TAG_GROWTH_PROGRESS, growthProgress);
        if (!seed.isEmpty()) {
            tag.put(TAG_SEED, seed.save(new CompoundTag()));
        } else {
            tag.put(TAG_SEED, new CompoundTag()); // sentinel: ensure tag is non-empty for client sync
        }
        saveStack(tag, TAG_NUTRITION_INPUT, nutritionInput);
        saveStack(tag, TAG_PURITY_INPUT, purityInput);
        saveStack(tag, TAG_SIGNAL_INPUT, signalInput);
        saveStack(tag, TAG_RESOURCE_OUTPUT, resourceOutput);
        saveStack(tag, TAG_BOTTLE_OUTPUT, bottleOutput);
        tag.putLong(TAG_NEXT_INPUT_INJECTION_TICK, nextInputInjectionTick);
        // v1.1.7 红石状态持久化
        redstone.save(tag);
    }

    private static ItemStack readStack(CompoundTag tag, String key) {
        return tag.contains(key) ? ItemStack.of(tag.getCompound(key)) : ItemStack.EMPTY;
    }

    private static void saveStack(CompoundTag tag, String key, ItemStack stack) {
        if (!stack.isEmpty()) tag.put(key, stack.save(new CompoundTag()));
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.cybercultivator.bio_incubator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new BioIncubatorMenu(containerId, inventory, this, menuData);
    }

    @Override
    public int getContainerSize() {
        return 6;
    }

    @Override
    public boolean isEmpty() {
        return seed.isEmpty() && nutritionInput.isEmpty() && purityInput.isEmpty()
                && signalInput.isEmpty() && resourceOutput.isEmpty() && bottleOutput.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return switch (slot) {
            case SEED_SLOT -> seed;
            case NUTRITION_SLOT -> nutritionInput;
            case PURITY_SLOT -> purityInput;
            case SIGNAL_SLOT -> signalInput;
            case RESOURCE_OUTPUT_SLOT -> resourceOutput;
            case BOTTLE_OUTPUT_SLOT -> bottleOutput;
            default -> ItemStack.EMPTY;
        };
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = getItem(slot);
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack result = stack.split(amount);
        if (stack.isEmpty()) setItem(slot, ItemStack.EMPTY);
        else syncToClient();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack result = getItem(slot);
        setItemInternal(slot, ItemStack.EMPTY);
        setChanged();
        return result;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        ItemStack normalized = (slot == SEED_SLOT && !stack.isEmpty())
                ? normalizeInsertedStack(slot, stack)
                : stack.copy();
        setItemInternal(slot, normalized);
        if (slot == SEED_SLOT) {
            cachedSpeed = normalized.isEmpty() ? 1 : GeneticSeedItem.getGene(normalized, GeneticSeedItem.GENE_SPEED);
            growthProgress = 0;
        }
        if (level != null && !level.isClientSide
                && !normalized.isEmpty()
                && (slot == NUTRITION_SLOT || slot == PURITY_SLOT || slot == SIGNAL_SLOT)
                && nextInputInjectionTick <= level.getGameTime()) {
            nextInputInjectionTick = level.getGameTime() + INPUT_INJECTION_INTERVAL_TICKS;
        }
        syncToClient();
    }

    private void setItemInternal(int slot, ItemStack stack) {
        switch (slot) {
            case SEED_SLOT -> seed = stack;
            case NUTRITION_SLOT -> nutritionInput = stack;
            case PURITY_SLOT -> purityInput = stack;
            case SIGNAL_SLOT -> signalInput = stack;
            case RESOURCE_OUTPUT_SLOT -> resourceOutput = stack;
            case BOTTLE_OUTPUT_SLOT -> bottleOutput = stack;
            default -> { }
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        // 委托给 policy（不带 side）：保持 Container.canPlaceItem 与 capability 路径一致（§9.9）
        return canInsert(slot, stack, null);
    }

    @Override
    public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clearContent() {
        seed = ItemStack.EMPTY;
        nutritionInput = ItemStack.EMPTY;
        purityInput = ItemStack.EMPTY;
        signalInput = ItemStack.EMPTY;
        resourceOutput = ItemStack.EMPTY;
        bottleOutput = ItemStack.EMPTY;
        cachedSpeed = 1;
        growthProgress = 0;
        nextInputInjectionTick = 0L;
        syncToClient();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return visibleSlots(side);
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canInsert(slot, stack, side);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return canExtract(slot, stack, side);
    }

    // === v1.1.7 §9 MachineInventoryPolicy 实现 ===
    // 分面矩阵（§9.1 + §9.4 顶部种子自动输入）：
    //   UP=种子只入；水平面=N/P/D 资源只入；DOWN=成熟产物、玻璃瓶只出。

    @Override
    public int[] visibleSlots(@Nullable Direction side) {
        if (side == null) {
            // §9.2 无方向查询：暴露全部槽位
            return new int[]{SEED_SLOT, NUTRITION_SLOT, PURITY_SLOT, SIGNAL_SLOT, RESOURCE_OUTPUT_SLOT, BOTTLE_OUTPUT_SLOT};
        }
        if (side == Direction.DOWN) {
            return new int[]{RESOURCE_OUTPUT_SLOT, BOTTLE_OUTPUT_SLOT};
        }
        if (side == Direction.UP) {
            // §9.4 开放顶部种子自动输入，实现完整无人值守链路
            return new int[]{SEED_SLOT};
        }
        // 四个水平面：N/P/D 资源
        return new int[]{NUTRITION_SLOT, PURITY_SLOT, SIGNAL_SLOT};
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction side) {
        if (stack.isEmpty()) return false;
        boolean sideAllows = side == null || switch (slot) {
            case SEED_SLOT -> side == Direction.UP;
            case NUTRITION_SLOT, PURITY_SLOT, SIGNAL_SLOT -> side != Direction.UP && side != Direction.DOWN;
            default -> false;
        };
        if (!sideAllows) return false;
        // §10.4 机器输入验证改用语义标签；默认仅含原物品，整合包可通过数据包扩展
        return switch (slot) {
            case SEED_SLOT -> stack.is(ModTags.SemanticItems.GENETIC_SEEDS);
            case NUTRITION_SLOT -> stack.is(ModTags.SemanticItems.INCUBATOR_NUTRITION);
            case PURITY_SLOT -> stack.is(ModTags.SemanticItems.INCUBATOR_PURITY);
            case SIGNAL_SLOT -> stack.is(ModTags.SemanticItems.INCUBATOR_DATA_SIGNAL);
            default -> false;
        };
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, @Nullable Direction side) {
        // null side 或 DOWN 可抽取成熟产物与玻璃瓶；输入槽拒绝外部抽取
        if (side != null && side != Direction.DOWN) return false;
        return slot == RESOURCE_OUTPUT_SLOT || slot == BOTTLE_OUTPUT_SLOT;
    }

    @Override
    public ItemStack normalizeInsertedStack(int slot, ItemStack stack) {
        if (slot == SEED_SLOT && stack.getItem() instanceof GeneticSeedItem geneticSeed) {
            ItemStack normalized = stack.copy();
            normalized.setCount(1);
            geneticSeed.ensureGeneData(normalized);
            return normalized;
        }
        return stack.copy();
    }

    @Override
    public int getSlotLimit(int slot) {
        // 种子槽限 1；N/P/D 输入槽限 1（每周期每通道最多消耗一份）；输出槽不限
        if (slot == SEED_SLOT || slot == NUTRITION_SLOT || slot == PURITY_SLOT || slot == SIGNAL_SLOT) {
            return 1;
        }
        return 64;
    }

    private static int clampStat(int value) {
        return Math.max(0, Math.min(100, value));
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    // === v1.1.7 §9.5 IItemHandler capability（按 face→角色映射缓存，§9.6） ===

    private net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler> capUp =
            net.minecraftforge.common.util.LazyOptional.empty();
    private net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler> capHorizontal =
            net.minecraftforge.common.util.LazyOptional.empty();
    private net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler> capDown =
            net.minecraftforge.common.util.LazyOptional.empty();
    private net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler> capNull =
            net.minecraftforge.common.util.LazyOptional.empty();

    @Override
    public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(
            net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER) {
            net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler> handler;
            if (side == null) {
                if (!capNull.isPresent()) {
                    capNull = net.minecraftforge.common.util.LazyOptional.of(
                            () -> new SidedMachineItemHandler(this, this, null));
                }
                handler = capNull;
            } else if (side == Direction.UP) {
                if (!capUp.isPresent()) {
                    capUp = net.minecraftforge.common.util.LazyOptional.of(
                            () -> new SidedMachineItemHandler(this, this, Direction.UP));
                }
                handler = capUp;
            } else if (side == Direction.DOWN) {
                if (!capDown.isPresent()) {
                    capDown = net.minecraftforge.common.util.LazyOptional.of(
                            () -> new SidedMachineItemHandler(this, this, Direction.DOWN));
                }
                handler = capDown;
            } else {
                if (!capHorizontal.isPresent()) {
                    capHorizontal = net.minecraftforge.common.util.LazyOptional.of(
                            () -> new SidedMachineItemHandler(this, this, side));
                }
                handler = capHorizontal;
            }
            return handler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        capUp.invalidate();
        capHorizontal.invalidate();
        capDown.invalidate();
        capNull.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
    }
}

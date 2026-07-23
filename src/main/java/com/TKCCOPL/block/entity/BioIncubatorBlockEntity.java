package com.TKCCOPL.block.entity;

import com.TKCCOPL.Config;
import com.TKCCOPL.init.ModBlockEntities;
import com.TKCCOPL.init.ModItems;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class BioIncubatorBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {
    private static final String TAG_NUTRITION = "Nutrition";
    private static final String TAG_PURITY = "Purity";
    private static final String TAG_DATA_SIGNAL = "DataSignal";
    private static final String TAG_GROWTH_PROGRESS = "GrowthProgress";
    private static final String TAG_GROWTH_REMAINDER_MILLI = "GrowthRemainderMilli";
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
    /** @deprecated Legacy save compatibility only; no longer exposed by the five-slot container. */
    @Deprecated
    public static final int BOTTLE_OUTPUT_SLOT = 5;

    private static final int SYNC_INTERVAL = 10;
    public static final int INPUT_INJECTION_INTERVAL_TICKS = 20;

    private int nutrition;
    private int purity;
    private int dataSignal;
    private int growthProgress;
    private int growthRemainderMilli;
    private ItemStack seed = ItemStack.EMPTY;
    private ItemStack nutritionInput = ItemStack.EMPTY;
    private ItemStack purityInput = ItemStack.EMPTY;
    private ItemStack signalInput = ItemStack.EMPTY;
    private ItemStack resourceOutput = ItemStack.EMPTY;
    private ItemStack legacyBottleOutput = ItemStack.EMPTY;
    private long nextInputInjectionTick;

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> nutrition;
                case 1 -> purity;
                case 2 -> dataSignal;
                case 3 -> getGrowthPercent();
                case 4 -> getEstimatedSecondsRemaining() + 1;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 5;
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

        // 资源自然衰减
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

        // 生长推进：需要三项资源均高于阈值
        if (blockEntity.nutrition > Config.resourceThreshold
                && blockEntity.purity > Config.resourceThreshold
                && blockEntity.dataSignal > 0) {

            int growthRateMilli = blockEntity.getCurrentGrowthRateMilli();
            int accumulatedGrowth = blockEntity.growthRemainderMilli + growthRateMilli;
            int growthDelta = accumulatedGrowth / 1000;
            blockEntity.growthRemainderMilli = accumulatedGrowth % 1000;
            int previousProgress = blockEntity.growthProgress;
            blockEntity.growthProgress = Math.min(Config.maturationThreshold,
                    blockEntity.growthProgress + growthDelta);
            if (blockEntity.growthProgress != previousProgress || growthRateMilli > 0) changed = true;

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
                        blockEntity.growthRemainderMilli = 0;
                        changed = true;
                        forceSync = true;
                    } else {
                        cropOutput = cropEvent.getOutput();
                        // 监听器通过 setOutput(EMPTY) 实现"软取消"：保留种子和资源，
                        // 仅重置进度避免每 tick 重复触发事件
                        if (cropOutput.isEmpty()) {
                            blockEntity.growthProgress = 0;
                            blockEntity.growthRemainderMilli = 0;
                            changed = true;
                            forceSync = true;
                        } else if (!blockEntity.canAcceptResourceOutput(cropOutput)) {
                            blockEntity.growthProgress = Config.maturationThreshold;
                        } else {
                            blockEntity.seed = ItemStack.EMPTY;
                            blockEntity.growthProgress = 0;
                            blockEntity.growthRemainderMilli = 0;
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
        growthRemainderMilli = 0;
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
        growthRemainderMilli = 0;
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
        if (slot == PURITY_SLOT && !purityInput.isEmpty() && purity < 100) {
            purityInput.shrink(1);
            if (purityInput.isEmpty()) purityInput = ItemStack.EMPTY;
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
        int milli = getCurrentGrowthRateMilli();
        return milli <= 0 ? 0 : Math.max(1, (int) Math.round(milli / 1000.0D));
    }

    /** 精确生长速率，1000 表示每 tick 推进 1 点。 */
    public int getCurrentGrowthRateMilli() {
        if (seed.isEmpty()) return 0;
        if (nutrition <= Config.resourceThreshold || purity <= Config.resourceThreshold || dataSignal <= 0) return 0;
        double geneMultiplier = 0.5D + cachedSpeed / 10.0D * 1.5D;
        double resourceRatio = (nutrition + purity + dataSignal) / 300.0D;
        double environmentMultiplier = 0.65D + 0.35D * resourceRatio;
        return Math.max(1, (int) Math.round(geneMultiplier * environmentMultiplier * 1000.0D));
    }

    /** 估算剩余成熟时间（秒），-1 表示无法生长 */
    public int getEstimatedSecondsRemaining() {
        int rateMilli = getCurrentGrowthRateMilli();
        if (rateMilli <= 0) return -1;
        long remainingMilli = (long) (Config.maturationThreshold - growthProgress) * 1000L
                - growthRemainderMilli;
        if (remainingMilli <= 0L) return 0;
        return (int) Math.ceil(remainingMilli / (double) rateMilli / 20.0D);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        nutrition = clampStat(tag.getInt(TAG_NUTRITION));
        purity = clampStat(tag.getInt(TAG_PURITY));
        dataSignal = clampStat(tag.getInt(TAG_DATA_SIGNAL));
        growthProgress = Math.max(0, tag.getInt(TAG_GROWTH_PROGRESS));
        growthRemainderMilli = Math.max(0, Math.min(999, tag.getInt(TAG_GROWTH_REMAINDER_MILLI)));
        seed = tag.contains(TAG_SEED) ? ItemStack.of(tag.getCompound(TAG_SEED)) : ItemStack.EMPTY;
        nutritionInput = readStack(tag, TAG_NUTRITION_INPUT);
        purityInput = readStack(tag, TAG_PURITY_INPUT);
        signalInput = readStack(tag, TAG_SIGNAL_INPUT);
        resourceOutput = readStack(tag, TAG_RESOURCE_OUTPUT);
        legacyBottleOutput = readStack(tag, TAG_BOTTLE_OUTPUT);
        nextInputInjectionTick = Math.max(0L, tag.getLong(TAG_NEXT_INPUT_INJECTION_TICK));
        // 初始化基因缓存
        if (!seed.isEmpty()) {
            cachedSpeed = GeneticSeedItem.getGene(seed, GeneticSeedItem.GENE_SPEED);
        } else {
            cachedSpeed = 1;
        }
        syncCounter = 0;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(TAG_NUTRITION, nutrition);
        tag.putInt(TAG_PURITY, purity);
        tag.putInt(TAG_DATA_SIGNAL, dataSignal);
        tag.putInt(TAG_GROWTH_PROGRESS, growthProgress);
        tag.putInt(TAG_GROWTH_REMAINDER_MILLI, growthRemainderMilli);
        if (!seed.isEmpty()) {
            tag.put(TAG_SEED, seed.save(new CompoundTag()));
        } else {
            tag.put(TAG_SEED, new CompoundTag()); // sentinel: ensure tag is non-empty for client sync
        }
        saveStack(tag, TAG_NUTRITION_INPUT, nutritionInput);
        saveStack(tag, TAG_PURITY_INPUT, purityInput);
        saveStack(tag, TAG_SIGNAL_INPUT, signalInput);
        saveStack(tag, TAG_RESOURCE_OUTPUT, resourceOutput);
        saveStack(tag, TAG_BOTTLE_OUTPUT, legacyBottleOutput);
        tag.putLong(TAG_NEXT_INPUT_INJECTION_TICK, nextInputInjectionTick);
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
        migrateLegacyBottleOutput(player);
        return new BioIncubatorMenu(containerId, inventory, this, menuData);
    }

    @Override
    public int getContainerSize() {
        return 5;
    }

    @Override
    public boolean isEmpty() {
        return seed.isEmpty() && nutritionInput.isEmpty() && purityInput.isEmpty()
                && signalInput.isEmpty() && resourceOutput.isEmpty() && legacyBottleOutput.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return switch (slot) {
            case SEED_SLOT -> seed;
            case NUTRITION_SLOT -> nutritionInput;
            case PURITY_SLOT -> purityInput;
            case SIGNAL_SLOT -> signalInput;
            case RESOURCE_OUTPUT_SLOT -> resourceOutput;
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
        ItemStack normalized = stack.copy();
        if (slot == SEED_SLOT && !normalized.isEmpty()) {
            normalized.setCount(1);
            if (normalized.getItem() instanceof GeneticSeedItem geneticSeed) {
                geneticSeed.ensureGeneData(normalized);
            }
        }
        setItemInternal(slot, normalized);
        if (slot == SEED_SLOT) {
            cachedSpeed = normalized.isEmpty() ? 1 : GeneticSeedItem.getGene(normalized, GeneticSeedItem.GENE_SPEED);
            growthProgress = 0;
            growthRemainderMilli = 0;
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
            default -> { }
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SEED_SLOT -> stack.getItem() instanceof GeneticSeedItem;
            case NUTRITION_SLOT -> stack.is(ModItems.BIOCHEMICAL_SOLUTION.get());
            case PURITY_SLOT -> stack.is(ModItems.PURIFIED_WATER_BOTTLE.get());
            case SIGNAL_SLOT -> stack.is(ModItems.SILICON_SHARD.get());
            default -> false;
        };
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
        legacyBottleOutput = ItemStack.EMPTY;
        cachedSpeed = 1;
        growthProgress = 0;
        growthRemainderMilli = 0;
        nextInputInjectionTick = 0L;
        syncToClient();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN
                ? new int[]{RESOURCE_OUTPUT_SLOT}
                : new int[]{NUTRITION_SLOT, PURITY_SLOT, SIGNAL_SLOT};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot != SEED_SLOT && slot != RESOURCE_OUTPUT_SLOT && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == RESOURCE_OUTPUT_SLOT && side == Direction.DOWN;
    }

    public ItemStack drainLegacyBottleOutput() {
        if (legacyBottleOutput.isEmpty()) return ItemStack.EMPTY;
        ItemStack migrated = legacyBottleOutput.copy();
        legacyBottleOutput = ItemStack.EMPTY;
        syncToClient();
        return migrated;
    }

    private void migrateLegacyBottleOutput(Player player) {
        ItemStack migrated = drainLegacyBottleOutput();
        if (migrated.isEmpty()) return;
        if (!player.addItem(migrated) && !migrated.isEmpty()) {
            player.drop(migrated, false);
        }
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
}

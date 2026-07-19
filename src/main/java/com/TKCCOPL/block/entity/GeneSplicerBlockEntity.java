package com.TKCCOPL.block.entity;

import com.TKCCOPL.Config;
import com.TKCCOPL.init.ModBlockEntities;
import com.TKCCOPL.init.ModTags;
import com.TKCCOPL.item.GeneticSeedItem;
import com.TKCCOPL.event.GeneSpliceEvent;
import com.TKCCOPL.menu.GeneSplicerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.RandomSource;
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

public class GeneSplicerBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider, MachineRedstoneBlockEntity, MachineInventoryPolicy {
    private static final String TAG_SEED_A = "SeedA";
    private static final String TAG_SEED_B = "SeedB";
    private static final String TAG_OUTPUT = "Output";
    private static final String TAG_SPLICE_PROGRESS = "SpliceProgress";
    private static final String TAG_SPLICING = "Splicing";
    private static final String TAG_AUTOMATIC_WORKFLOW = "AutomaticWorkflow";

    public static final int SPLICE_DURATION_TICKS = 100;

    public static final int SEED_A_SLOT = 0;
    public static final int SEED_B_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;

    // ContainerData 索引：0-4 为原有字段，5-7 为红石字段（mode/powered/processingAllowed）
    private static final int DATA_REDSTONE_BASE = 5;

    private ItemStack seedA = ItemStack.EMPTY;
    private ItemStack seedB = ItemStack.EMPTY;
    private ItemStack output = ItemStack.EMPTY;
    private int spliceProgress;
    private boolean splicing;

    /** v1.1.7 红石控制状态 */
    private final MachineRedstoneState redstone = new MachineRedstoneState();

    /** v1.1.7 比较器信号缓存，仅在变化时通知相邻比较器 */
    private int lastComparatorSignal = 0;

    private final ContainerData menuData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> spliceProgress;
                case 1 -> SPLICE_DURATION_TICKS;
                case 2 -> splicing ? 1 : 0;
                case 3 -> Math.min(Short.MAX_VALUE, getPredictedGeneration());
                case 4 -> getPredictedMutationPermille();
                // 红石字段（mode/powered/processingAllowed）
                case 5 -> redstone.getMenuData(MachineRedstoneState.DATA_MODE);
                case 6 -> redstone.getMenuData(MachineRedstoneState.DATA_POWERED);
                case 7 -> redstone.getMenuData(MachineRedstoneState.DATA_PROCESSING_ALLOWED);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) spliceProgress = Math.max(0, Math.min(SPLICE_DURATION_TICKS, value));
            if (index == 2) splicing = value != 0;
        }

        @Override
        public int getCount() {
            return 8;
        }
    };

    /** 防止同 tick 双次 use() 导致放入两颗种子 */
    private long lastInsertTick = -1;

    public GeneSplicerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GENE_SPLICER.get(), pos, state);
    }

    public boolean tryInsertSeed(ItemStack stack, RandomSource random) {
        if (splicing || !output.isEmpty()) {
            return false;
        }
        if (!(stack.getItem() instanceof GeneticSeedItem)) {
            return false;
        }

        // Forge 1.20.1 单次右键可能触发两次 use()，防抖：同一 tick 只允许插入一次
        long currentTick = level != null ? level.getGameTime() : -1;
        if (currentTick == lastInsertTick) {
            return false;
        }

        if (seedA.isEmpty()) {
            seedA = normalizedSeed(stack);
            lastInsertTick = currentTick;
            syncToClient();
            return true;
        }
        if (seedB.isEmpty()) {
            seedB = normalizedSeed(stack);
            lastInsertTick = currentTick;
            if (!startSplicing()) {
                syncToClient();
            }
            return true;
        }
        return false;
    }

    public ItemStack extractOutput() {
        if (output.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack out = output;
        output = ItemStack.EMPTY;
        resetSplicing();
        // Defensive cleanup for saves created before parents were consumed on completion.
        seedA = ItemStack.EMPTY;
        seedB = ItemStack.EMPTY;
        syncToClient();
        return out;
    }

    public ItemStack extractLastInput() {
        if (!seedB.isEmpty()) {
            ItemStack out = seedB;
            seedB = ItemStack.EMPTY;
            resetSplicing();
            syncToClient();
            return out;
        }
        if (!seedA.isEmpty()) {
            ItemStack out = seedA;
            seedA = ItemStack.EMPTY;
            resetSplicing();
            syncToClient();
            return out;
        }
        return ItemStack.EMPTY;
    }

    public ItemStack getSeedA() {
        return seedA.copy();
    }

    public ItemStack getSeedB() {
        return seedB.copy();
    }

    public ItemStack getOutput() {
        return output.copy();
    }

    public boolean hasOutput() {
        return !output.isEmpty();
    }

    public int getSpliceProgress() {
        return spliceProgress;
    }

    public int getSpliceDuration() {
        return SPLICE_DURATION_TICKS;
    }

    public int getRemainingTicks() {
        return splicing ? Math.max(0, SPLICE_DURATION_TICKS - spliceProgress) : 0;
    }

    public boolean isSplicing() {
        return splicing;
    }

    public int getPredictedGeneration() {
        if (seedA.isEmpty() || seedB.isEmpty()) {
            return 0;
        }
        int maxGeneration = Math.max(
                GeneticSeedItem.getGeneration(seedA),
                GeneticSeedItem.getGeneration(seedB));
        return maxGeneration == Integer.MAX_VALUE ? Integer.MAX_VALUE : maxGeneration + 1;
    }

    public int getPredictedMutationPermille() {
        if (seedA.isEmpty() || seedB.isEmpty()) {
            return 0;
        }
        double chance = Math.max(0.0D, Math.min(1.0D, calculateMutationChance()));
        return (int) Math.round(chance * 1000.0D);
    }

    public int getInputCount() {
        int count = 0;
        if (!seedA.isEmpty()) {
            count++;
        }
        if (!seedB.isEmpty()) {
            count++;
        }
        return count;
    }

    public boolean startSplicing(RandomSource random) {
        return startSplicing();
    }

    public boolean startSplicing() {
        if (splicing || !output.isEmpty() || seedA.isEmpty() || seedB.isEmpty()) return false;
        spliceProgress = 0;
        splicing = true;
        syncToClient();
        return true;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, GeneSplicerBlockEntity blockEntity) {
        if (level.isClientSide) return;

        // v1.1.7 hotfix：执行 clearRemoved 推迟的红石重新采样
        // 注意：必须在 splicing 状态检查之前执行，否则闲置 BE 不会更新供电状态
        if (blockEntity.redstone.consumePendingResample(level, pos)) {
            blockEntity.setChanged();
            level.sendBlockUpdated(pos, state, state, 2);
        }

        if (!blockEntity.splicing) return;
        if (blockEntity.seedA.isEmpty() || blockEntity.seedB.isEmpty() || !blockEntity.output.isEmpty()) {
            blockEntity.resetSplicing();
            blockEntity.syncToClient();
            return;
        }

        // v1.1.7 红石门控：拼接进度推进受红石模式控制
        boolean redstoneAllows = blockEntity.redstone.isProcessingAllowed();
        if (!redstoneAllows) {
            // 红石阻塞时不推进，但仍每 5 tick 同步状态以驱动 GUI 连接条动画
            if (blockEntity.spliceProgress % 5 == 0) {
                blockEntity.syncToClient();
            }
            // 仍检测比较器变化（虽然数值未变，但安全兜底）
            blockEntity.updateComparatorIfChanged(level, pos);
            return;
        }

        blockEntity.spliceProgress++;
        blockEntity.setChanged();
        if (blockEntity.spliceProgress >= SPLICE_DURATION_TICKS) {
            blockEntity.craftOutput(level.getRandom());
            blockEntity.resetSplicing();
            blockEntity.syncToClient();
        } else if (blockEntity.spliceProgress % 5 == 0) {
            blockEntity.syncToClient();
        }

        // v1.1.7 比较器信号变化检测
        blockEntity.updateComparatorIfChanged(level, pos);
    }

    private void resetSplicing() {
        spliceProgress = 0;
        splicing = false;
    }

    private static ItemStack normalizedSeed(ItemStack stack) {
        ItemStack normalized = stack.copy();
        normalized.setCount(1);
        if (normalized.getItem() instanceof GeneticSeedItem geneticSeed) {
            geneticSeed.ensureGeneData(normalized);
        }
        return normalized;
    }

    private void craftOutput(RandomSource random) {
        if (seedA.isEmpty() || seedB.isEmpty()) {
            return;
        }

        // 1. 读取亲本基因
        int speedA = GeneticSeedItem.getGene(seedA, GeneticSeedItem.GENE_SPEED);
        int speedB = GeneticSeedItem.getGene(seedB, GeneticSeedItem.GENE_SPEED);
        int yieldA = GeneticSeedItem.getGene(seedA, GeneticSeedItem.GENE_YIELD);
        int yieldB = GeneticSeedItem.getGene(seedB, GeneticSeedItem.GENE_YIELD);
        int potencyA = GeneticSeedItem.getGene(seedA, GeneticSeedItem.GENE_POTENCY);
        int potencyB = GeneticSeedItem.getGene(seedB, GeneticSeedItem.GENE_POTENCY);

        // 2. 计算突变概率: base + 代数 * perGen + 基因差异 * perGeneDiff
        int genA = GeneticSeedItem.getGeneration(seedA);
        int genB = GeneticSeedItem.getGeneration(seedB);
        int maxGen = Math.max(genA, genB);
        double mutationChance = calculateMutationChance();
        boolean isMutation = random.nextDouble() < mutationChance;

        // 3. 计算子代基因（标准公式 ±mutationRange）
        int mutationRange = Config.mutationRange;
        int newSpeed = GeneticSeedItem.clampGene((speedA + speedB) / 2 + random.nextInt(mutationRange * 2 + 1) - mutationRange);
        int newYield = GeneticSeedItem.clampGene((yieldA + yieldB) / 2 + random.nextInt(mutationRange * 2 + 1) - mutationRange);
        int newPotency = GeneticSeedItem.clampGene((potencyA + potencyB) / 2 + random.nextInt(mutationRange * 2 + 1) - mutationRange);

        // 4. 如果突变触发，应用突变结果
        int mutationType = 0; // 0=未突变, 1=数值突破, 2=协同基因
        String mutationDetail = "";
        if (isMutation) {
            double roll = random.nextDouble();
            if (roll < 0.80) {
                // 数值突破（80%）：随机一个基因变异 ±mutationRange*2（覆盖标准公式结果）
                int target = random.nextInt(3); // 0=Speed, 1=Yield, 2=Potency
                int bonusRange = Config.mutationRange * 2 + 1;
                int bonus = random.nextInt(bonusRange * 2 - 1) - (bonusRange - 1); // -mutationRange*2 to +mutationRange*2

                String geneName;
                if (target == 0) {
                    newSpeed = GeneticSeedItem.clampGene((speedA + speedB) / 2 + bonus);
                    geneName = "Speed";
                } else if (target == 1) {
                    newYield = GeneticSeedItem.clampGene((yieldA + yieldB) / 2 + bonus);
                    geneName = "Yield";
                } else {
                    newPotency = GeneticSeedItem.clampGene((potencyA + potencyB) / 2 + bonus);
                    geneName = "Potency";
                }
                mutationType = 1;
                mutationDetail = geneName + (bonus >= 0 ? "+" : "") + bonus;
            } else {
                // Gene_Synergy 获得（20%）在步骤 6 处理
                mutationType = 2;
            }
        }

        // 5. 设置基因到输出种子
        ItemStack result = new ItemStack(seedA.getItem());
        GeneticSeedItem.setGenes(result, newSpeed, newYield, newPotency);

        // Preserve the stronger inherited synergy so the documented cumulative
        // 0..10 progression is reachable through continued breeding.
        int inheritedSynergy = Math.max(
                GeneticSeedItem.getSynergy(seedA),
                GeneticSeedItem.getSynergy(seedB));
        if (inheritedSynergy > 0) {
            result.getOrCreateTag().putInt(GeneticSeedItem.GENE_SYNERGY, inheritedSynergy);
        }

        // 6. 如果是 Synergy 突变，写入 Gene_Synergy（累加，上限 10）
        if (mutationType == 2) {
            int currentSynergy = GeneticSeedItem.getSynergy(result);
            int synergyGain = 1 + random.nextInt(3); // 1-3
            int newSynergy = Math.min(10, currentSynergy + synergyGain);
            result.getOrCreateTag().putInt(GeneticSeedItem.GENE_SYNERGY, newSynergy);
            mutationDetail = "Synergy+" + synergyGain;
        }

        // 7. 标记突变（整数类型码 + 详情）
        if (mutationType > 0) {
            result.getOrCreateTag().putInt("Mutation", mutationType);
            result.getOrCreateTag().putString("MutationDetail", mutationDetail);
        }

        // 8. 设置 Generation
        int childGen = maxGen == Integer.MAX_VALUE ? Integer.MAX_VALUE : maxGen + 1;
        result.getOrCreateTag().putInt(GeneticSeedItem.GENE_GENERATION, childGen);

        // 9. 触发 GeneSpliceEvent，允许其他 mod 修改结果
        int currentSynergy = result.getOrCreateTag().getInt(GeneticSeedItem.GENE_SYNERGY);
        GeneSpliceEvent event = new GeneSpliceEvent(
                seedA, seedB, newSpeed, newYield, newPotency,
                currentSynergy, childGen, isMutation, mutationType, mutationDetail
        );
        if (net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event)) {
            return; // 事件被取消
        }
        // 使用事件修改后的值（Task 3: 回读所有 setter）
        newSpeed = event.getSpeed();
        newYield = event.getYield();
        newPotency = event.getPotency();
        GeneticSeedItem.setGenes(result, newSpeed, newYield, newPotency);

        // 回读 synergy（无条件写入，支持清零）
        result.getOrCreateTag().putInt(GeneticSeedItem.GENE_SYNERGY,
                Math.max(0, Math.min(10, event.getSynergy())));

        // 回读 mutation 信息
        if (event.isMutation()) {
            result.getOrCreateTag().putInt("Mutation", event.getMutationType());
            String eventDetail = event.getMutationDetail();
            result.getOrCreateTag().putString("MutationDetail", eventDetail == null ? "" : eventDetail);
        } else {
            // 事件监听器清除了突变，移除标签
            result.getOrCreateTag().remove("Mutation");
            result.getOrCreateTag().remove("MutationDetail");
        }

        // 回读 generation
        result.getOrCreateTag().putInt(GeneticSeedItem.GENE_GENERATION, Math.max(0, event.getGeneration()));

        output = result;
        seedA = ItemStack.EMPTY;
        seedB = ItemStack.EMPTY;
    }

    private double calculateMutationChance() {
        int maxGeneration = Math.max(
                GeneticSeedItem.getGeneration(seedA),
                GeneticSeedItem.getGeneration(seedB));
        int maxGeneDifference = Math.max(
                Math.abs(GeneticSeedItem.getGene(seedA, GeneticSeedItem.GENE_SPEED)
                        - GeneticSeedItem.getGene(seedB, GeneticSeedItem.GENE_SPEED)),
                Math.max(
                        Math.abs(GeneticSeedItem.getGene(seedA, GeneticSeedItem.GENE_YIELD)
                                - GeneticSeedItem.getGene(seedB, GeneticSeedItem.GENE_YIELD)),
                        Math.abs(GeneticSeedItem.getGene(seedA, GeneticSeedItem.GENE_POTENCY)
                                - GeneticSeedItem.getGene(seedB, GeneticSeedItem.GENE_POTENCY)))
        );
        return Config.mutationChanceBase
                + maxGeneration * Config.mutationChancePerGen
                + maxGeneDifference * Config.mutationChancePerGeneDiff;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        seedA = tag.contains(TAG_SEED_A) ? ItemStack.of(tag.getCompound(TAG_SEED_A)) : ItemStack.EMPTY;
        seedB = tag.contains(TAG_SEED_B) ? ItemStack.of(tag.getCompound(TAG_SEED_B)) : ItemStack.EMPTY;
        output = tag.contains(TAG_OUTPUT) ? ItemStack.of(tag.getCompound(TAG_OUTPUT)) : ItemStack.EMPTY;
        if (!output.isEmpty()) {
            seedA = ItemStack.EMPTY;
            seedB = ItemStack.EMPTY;
        }
        spliceProgress = Math.max(0, Math.min(SPLICE_DURATION_TICKS - 1, tag.getInt(TAG_SPLICE_PROGRESS)));
        boolean ready = !seedA.isEmpty() && !seedB.isEmpty() && output.isEmpty();
        splicing = ready && (tag.getBoolean(TAG_SPLICING) || !tag.contains(TAG_AUTOMATIC_WORKFLOW));
        if (!splicing) spliceProgress = 0;
        // v1.1.7 红石状态（缺失字段默认 IGNORE，不崩溃）
        redstone.load(tag);
        // 比较器缓存重置，下次 tick 重新检测
        lastComparatorSignal = -1;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!seedA.isEmpty()) {
            tag.put(TAG_SEED_A, seedA.save(new CompoundTag()));
        } else {
            tag.put(TAG_SEED_A, new CompoundTag()); // sentinel: ensure tag is non-empty for sync
        }
        if (!seedB.isEmpty()) {
            tag.put(TAG_SEED_B, seedB.save(new CompoundTag()));
        } else {
            tag.put(TAG_SEED_B, new CompoundTag());
        }
        if (!output.isEmpty()) {
            tag.put(TAG_OUTPUT, output.save(new CompoundTag()));
        } else {
            tag.put(TAG_OUTPUT, new CompoundTag());
        }
        tag.putInt(TAG_SPLICE_PROGRESS, spliceProgress);
        tag.putBoolean(TAG_SPLICING, splicing);
        tag.putBoolean(TAG_AUTOMATIC_WORKFLOW, true);
        // v1.1.7 红石状态持久化
        redstone.save(tag);
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
     *   <li>{@code 15}：拼接产物槽存在可领取的子代种子</li>
     *   <li>{@code 1..14}：拼接中（{@code splicing == true}），按 spliceProgress 比例</li>
     *   <li>{@code 0}：无亲本或无产物</li>
     * </ul>
     * 拼接进度为 0 但 splicing=true 时返回 1（表示已就绪等待产出）。
     */
    @Override
    public int getComparatorSignal() {
        if (!output.isEmpty()) return 15;
        if (!splicing) return 0;
        // 拼接中：按 spliceProgress 比例映射到 1..14
        int max = SPLICE_DURATION_TICKS;
        if (max <= 0) return 1;
        int raw = (int) Math.ceil((double) spliceProgress * 14 / max);
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

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.cybercultivator.gene_splicer");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new GeneSplicerMenu(containerId, inventory, this, menuData);
    }

    @Override
    public int getContainerSize() {
        return 3;
    }

    @Override
    public boolean isEmpty() {
        return seedA.isEmpty() && seedB.isEmpty() && output.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return switch (slot) {
            case SEED_A_SLOT -> seedA;
            case SEED_B_SLOT -> seedB;
            case OUTPUT_SLOT -> output;
            default -> ItemStack.EMPTY;
        };
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = getItem(slot);
        if (stack.isEmpty()) return ItemStack.EMPTY;
        if (slot != OUTPUT_SLOT) resetSplicing();
        ItemStack result = stack.split(amount);
        if (stack.isEmpty()) setItemInternal(slot, ItemStack.EMPTY);
        if (slot == OUTPUT_SLOT && output.isEmpty()) {
            seedA = ItemStack.EMPTY;
            seedB = ItemStack.EMPTY;
        }
        syncToClient();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack result = getItem(slot);
        if (slot != OUTPUT_SLOT) resetSplicing();
        setItemInternal(slot, ItemStack.EMPTY);
        if (slot == OUTPUT_SLOT) {
            seedA = ItemStack.EMPTY;
            seedB = ItemStack.EMPTY;
        }
        setChanged();
        return result;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot != OUTPUT_SLOT && !output.isEmpty()) return;
        ItemStack normalized = (slot != OUTPUT_SLOT && !stack.isEmpty())
                ? normalizeInsertedStack(slot, stack)
                : stack.copy();
        if (slot != OUTPUT_SLOT) resetSplicing();
        setItemInternal(slot, normalized);
        if (slot != OUTPUT_SLOT && startSplicing()) {
            return;
        }
        syncToClient();
    }

    private void setItemInternal(int slot, ItemStack stack) {
        switch (slot) {
            case SEED_A_SLOT -> seedA = stack;
            case SEED_B_SLOT -> seedB = stack;
            case OUTPUT_SLOT -> output = stack;
            default -> { }
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        // 委托给 policy：保持 Container.canPlaceItem 与 capability 路径一致（§9.9）
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
        seedA = ItemStack.EMPTY;
        seedB = ItemStack.EMPTY;
        output = ItemStack.EMPTY;
        resetSplicing();
        syncToClient();
    }

    // === v1.1.7 §9.3 WorldlyContainer 实现（原版漏斗兼容） ===
    // 委托给 MachineInventoryPolicy，与 capability 路径共享谓词（§9.9 一致性）。

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
    // 分面矩阵（§9.1）：顶部/水平面=两个亲本槽只入；底部=子代只出。

    @Override
    public int[] visibleSlots(@Nullable Direction side) {
        if (side == null) {
            // §9.2 无方向查询：暴露全部槽位
            return new int[]{SEED_A_SLOT, SEED_B_SLOT, OUTPUT_SLOT};
        }
        if (side == Direction.DOWN) {
            return new int[]{OUTPUT_SLOT};
        }
        // UP 与四个水平面：暴露两个亲本槽
        return new int[]{SEED_A_SLOT, SEED_B_SLOT};
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction side) {
        if (stack.isEmpty()) return false;
        // §9.1 分面矩阵：DOWN 只出不入；UP/horizontal 只入亲本槽。null side 视为允许（§9.2）
        if (side == Direction.DOWN) return false;
        // §10.4 机器输入验证改用语义标签；默认仅含本 mod 种子
        if (slot == SEED_A_SLOT || slot == SEED_B_SLOT) {
            return !splicing && output.isEmpty() && stack.is(ModTags.SemanticItems.GENETIC_SEEDS);
        }
        return false;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, @Nullable Direction side) {
        // 仅可抽取子代；null side 或 DOWN 允许，其他方向拒绝
        if (slot != OUTPUT_SLOT) return false;
        return side == null || side == Direction.DOWN;
    }

    @Override
    public ItemStack normalizeInsertedStack(int slot, ItemStack stack) {
        if (slot != OUTPUT_SLOT && stack.getItem() instanceof GeneticSeedItem) {
            return normalizedSeed(stack);
        }
        return stack.copy();
    }

    @Override
    public int getSlotLimit(int slot) {
        // 亲本槽限 1（每槽一颗种子）；输出槽不限
        return (slot == SEED_A_SLOT || slot == SEED_B_SLOT) ? 1 : 64;
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

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        this.load(tag);
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
                    // 水平面共享 handler 实例；policy 对所有水平方向行为一致
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
        // LazyOptional 是惰性的，下次 getCapability 时会重新创建
    }
}

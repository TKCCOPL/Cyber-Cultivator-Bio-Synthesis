package com.TKCCOPL.block.entity;

import com.TKCCOPL.api.RedstoneControlMode;
import net.minecraft.nbt.CompoundTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 机器红石控制状态（v1.1.7 内部辅助类）。
 *
 * <p>四台机器 BlockEntity 各持有一个 {@link MachineRedstoneState} 实例，委托红石相关逻辑：
 * 模式持久化、供电状态采样、加工许可计算、ContainerData 同步字段。</p>
 *
 * <p>该类不是公开 API 的一部分；公开查询走 {@link com.TKCCOPL.api.CyberCultivatorAPI}。</p>
 */
public final class MachineRedstoneState {
    private static final Logger LOGGER = LoggerFactory.getLogger("CyberCultivator/MachineRedstone");
    private static final String TAG_MODE = "RedstoneMode";

    // ContainerData 字段索引（相对偏移，BE 在自己的 ContainerData 末尾追加这 3 个字段）
    public static final int DATA_MODE = 0;
    public static final int DATA_POWERED = 1;
    public static final int DATA_PROCESSING_ALLOWED = 2;
    public static final int DATA_COUNT = 3;

    private RedstoneControlMode mode = RedstoneControlMode.IGNORE;
    private boolean powered = false;

    public RedstoneControlMode getMode() {
        return mode;
    }

    public boolean isPowered() {
        return powered;
    }

    /** 当前是否允许加工（综合模式 + 供电）。 */
    public boolean isProcessingAllowed() {
        return mode.isProcessingAllowed(powered);
    }

    /**
     * 切换模式。返回是否实际变化（用于 BE 决定是否触发同步）。
     */
    public boolean setMode(RedstoneControlMode newMode) {
        if (newMode == null || newMode == this.mode) return false;
        this.mode = newMode;
        return true;
    }

    /** GUI 按钮单击循环：IGNORE → HIGH → LOW → IGNORE。 */
    public boolean cycleMode() {
        return setMode(mode.next());
    }

    /**
     * 更新当前红石供电状态。返回是否实际变化（用于 BE 决定是否触发同步）。
     */
    public boolean updatePowered(boolean newPowered) {
        if (newPowered == this.powered) return false;
        this.powered = newPowered;
        return true;
    }

    /** 从 {@code level.hasNeighborSignal(pos)} 重新采样供电状态。 */
    public boolean resamplePowered(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        return updatePowered(level.hasNeighborSignal(pos));
    }

    /**
     * NBT 持久化（写入 BE saveAdditional 的 tag）。
     *
     * <p>v1.2.0 §3.2 采纳：只持久化 {@code RedstoneMode}，不持久化 {@code powered}。
     * 供电状态可从世界重新采样，写入 NBT 会产生陈旧状态。BE 在 {@code clearRemoved()} 时
     * 通过 {@link #resamplePowered} 重新采样。</p>
     */
    public void save(CompoundTag tag) {
        tag.putString(TAG_MODE, mode.getSerializedName());
    }

    /**
     * NBT 加载（从 BE load 的 tag 读取）。
     * 缺失字段默认 {@link RedstoneControlMode#IGNORE}，不崩溃。
     * 未知字符串 → {@link RedstoneControlMode#IGNORE} + 一次 warn（M5）。
     *
     * <p>{@code powered} 不从 NBT 读取，重置为 {@code false}；BE 在 {@code clearRemoved()}
     * 时调用 {@link #resamplePowered} 重新采样当前世界供电状态。</p>
     */
    public void load(CompoundTag tag) {
        if (tag.contains(TAG_MODE)) {
            String raw = tag.getString(TAG_MODE);
            RedstoneControlMode parsed = RedstoneControlMode.byName(raw);
            if (parsed == RedstoneControlMode.IGNORE && !"ignore".equals(raw)) {
                LOGGER.warn(
                        "Unknown redstone mode '{}' at machine BE, defaulting to IGNORE", raw);
            }
            this.mode = parsed;
        } else {
            this.mode = RedstoneControlMode.IGNORE;
        }
        this.powered = false;
    }

    /**
     * ContainerData 读取：按相对索引返回 mode/powered/processingAllowed。
     * BE 在自己的 ContainerData 末尾追加 {@link #DATA_COUNT} 个字段并转发到此处。
     */
    public int getMenuData(int relativeIndex) {
        return switch (relativeIndex) {
            case DATA_MODE -> mode.ordinal();
            case DATA_POWERED -> powered ? 1 : 0;
            case DATA_PROCESSING_ALLOWED -> isProcessingAllowed() ? 1 : 0;
            default -> 0;
        };
    }

    /** ContainerData 字段数（3：mode/powered/processingAllowed）。 */
    public static int getMenuDataCount() {
        return DATA_COUNT;
    }

    /** 仅供 API 测试与内部诊断使用；返回快照。 */
    public MachineRedstoneSnapshot snapshot() {
        return new MachineRedstoneSnapshot(mode, powered, isProcessingAllowed());
    }

    /** 内部只读快照（不暴露到公开 API 包）。 */
    public record MachineRedstoneSnapshot(
            RedstoneControlMode mode,
            boolean powered,
            boolean processingAllowed
    ) {}
}

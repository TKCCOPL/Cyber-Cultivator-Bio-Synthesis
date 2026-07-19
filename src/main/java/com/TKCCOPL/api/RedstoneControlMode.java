package com.TKCCOPL.api;

import net.minecraft.nbt.CompoundTag;

/**
 * 机器红石控制模式（v1.1.7）。
 *
 * <p>持久化使用稳定字符串序列化（{@link #getSerializedName()}），不依赖 {@code ordinal()}，
 * 以便未来版本末尾追加新模式而不破坏旧存档。</p>
 *
 * <p>v1.2.0 候选：{@code PULSE} 模式（待 v1.1.7 玩家反馈验证真实需求后决定是否启用）。</p>
 */
public enum RedstoneControlMode {
    /** 忽略红石输入，机器始终允许加工（默认模式）。 */
    IGNORE,
    /** 有红石信号时允许加工。 */
    HIGH,
    /** 无红石信号时允许加工。 */
    LOW;

    private static final String TAG_NAME = "Name";

    /** 稳定字符串序列化名，用于 NBT 持久化与 API 调用。 */
    public String getSerializedName() {
        return name().toLowerCase();
    }

    /** 按字符串名解析；未知或 null 返回 {@link #IGNORE}。 */
    public static RedstoneControlMode byName(String name) {
        if (name == null) return IGNORE;
        return switch (name) {
            case "high" -> HIGH;
            case "low" -> LOW;
            case "ignore" -> IGNORE;
            default -> IGNORE;
        };
    }

    /** GUI 按钮单击循环顺序：IGNORE → HIGH → LOW → IGNORE。 */
    public RedstoneControlMode next() {
        return switch (this) {
            case IGNORE -> HIGH;
            case HIGH -> LOW;
            case LOW -> IGNORE;
        };
    }

    /** 写入 NBT 子标签，便于 BE 委托调用。 */
    public CompoundTag save(CompoundTag parent, String key) {
        CompoundTag sub = new CompoundTag();
        sub.putString(TAG_NAME, getSerializedName());
        parent.put(key, sub);
        return sub;
    }

    /** 从 NBT 子标签读取；缺失返回 {@link #IGNORE}，未知字符串同样返回 {@link #IGNORE}。 */
    public static RedstoneControlMode load(CompoundTag parent, String key) {
        if (!parent.contains(key)) return IGNORE;
        return byName(parent.getCompound(key).getString(TAG_NAME));
    }

    /** 判断当前模式 + 供电状态是否允许加工。 */
    public boolean isProcessingAllowed(boolean powered) {
        return switch (this) {
            case IGNORE -> true;
            case HIGH -> powered;
            case LOW -> !powered;
        };
    }
}

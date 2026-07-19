package com.TKCCOPL.block.entity;

/**
 * 标记接口：表示该 BlockEntity 支持 v1.1.7 红石链路控制。
 *
 * <p>{@link com.TKCCOPL.block.MachineBlock} 基类通过 instanceof 检测此接口，
 * 统一处理 {@code neighborChanged} 信号采样和 {@code getAnalogOutputSignal} 比较器输出。</p>
 *
 * <p>实现类需持有 {@link MachineRedstoneState} 实例并委托红石逻辑。
 * 比较器三段语义（0/1-14/15）由 {@link #getComparatorSignal()} 提供。</p>
 */
public interface MachineRedstoneBlockEntity {

    /** 获取红石控制状态实例（用于模式读写、供电采样、加工许可判定）。 */
    MachineRedstoneState getRedstoneState();

    /**
     * 当前比较器输出信号（0-15）。
     *
     * <p>三段语义（v1.1.7 统一）：15=主产物就绪；1-14=加工中；0=待机。
     * 实现应缓存 {@code lastComparatorSignal}，仅在数值变化时通过
     * {@code level.updateNeighbourForOutputSignal} 通知相邻比较器。</p>
     */
    int getComparatorSignal();
}

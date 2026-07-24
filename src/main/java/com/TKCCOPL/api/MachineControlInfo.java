package com.TKCCOPL.api;

/**
 * 机器红石与比较器控制状态快照（v1.1.7）。
 *
 * <p>不可变 record，所有字段在构造时确定。{@link CyberCultivatorAPI#getMachineControlInfo}
 * 返回此快照供第三方模组查询机器红石链路状态。</p>
 *
 * @param mode              当前红石控制模式
 * @param powered           当前是否被红石供电（{@code level.hasNeighborSignal(pos)}）
 * @param processingAllowed 综合模式与供电状态后是否允许加工
 * @param comparatorSignal  当前比较器输出信号（0-15，三段语义 0/1-14/15）
 */
public record MachineControlInfo(
        RedstoneControlMode mode,
        boolean powered,
        boolean processingAllowed,
        int comparatorSignal
) {}

package com.TKCCOPL.menu;

/**
 * v1.1.7 机器菜单红石访问接口。
 *
 * <p>菜单实现此接口后，{@link com.TKCCOPL.client.screen.MachineScreen}
 * 会在 {@code init()} 阶段自动添加红石模式循环按钮，
 * 子类 Screen 无需任何额外代码。</p>
 *
 * <p>该接口仅用于客户端 GUI 渲染与服务端按钮分发之间的契约，
 * 不属于公开 API。第三方查询机器红石状态请使用
 * {@link com.TKCCOPL.api.CyberCultivatorAPI}。</p>
 */
public interface RedstoneMenuAccess {
    /** 返回该菜单的红石模式循环按钮 ID（不同机器 ID 可能不同以避开既有按钮）。 */
    int getRedstoneButtonId();

    /** 返回当前红石模式 ordinal（0=IGNORE, 1=HIGH, 2=LOW）。 */
    int getRedstoneModeOrdinal();

    /** 返回当前方块是否被红石供电。 */
    boolean isRedstonePowered();

    /** 返回当前红石模式下是否允许加工。 */
    boolean isRedstoneProcessingAllowed();
}

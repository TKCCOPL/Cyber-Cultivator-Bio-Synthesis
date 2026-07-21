package com.TKCCOPL.advancement;

import net.minecraft.advancements.CriteriaTriggers;

/**
 * 模组自定义触发器注册入口。
 * CriteriaTriggers.register 必须在 mod 构造期间调用一次。
 */
public final class ModTriggers {
    public static final GeneSpliceCompleteTrigger GENE_SPLICE_COMPLETE =
            CriteriaTriggers.register(new GeneSpliceCompleteTrigger());

    private ModTriggers() {
    }

    /** 显式触发一次，方便引用方在静态初始化后调用以确保类被加载 */
    public static void init() {
        // no-op：静态字段已初始化
    }
}

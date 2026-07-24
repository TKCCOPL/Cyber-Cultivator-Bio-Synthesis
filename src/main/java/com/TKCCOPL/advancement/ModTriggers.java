package com.TKCCOPL.advancement;

import com.TKCCOPL.item.GeneticSeedItem;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

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

    /**
     * 玩家从基因拼接机输出槽领取子代种子时的统一进度触发入口。
     * <p>GUI 输出槽 {@code onTake} 与潜行右键直接拾取两条路径都应调用此方法，
     * 保证行为一致；漏斗自动抽取不调用此方法，避免自动化错误获得玩家进度。
     * <p>仅服务端、仅 {@code Generation > 0}（即拼接产物，野生种子 Generation=0 不触发）。
     */
    public static void triggerForOutput(Player player, ItemStack output) {
        if (!(player instanceof ServerPlayer serverPlayer) || output.isEmpty()) return;
        var tag = output.getTag();
        if (tag != null && tag.getInt(GeneticSeedItem.GENE_GENERATION) > 0) {
            GENE_SPLICE_COMPLETE.trigger(serverPlayer);
        }
    }
}

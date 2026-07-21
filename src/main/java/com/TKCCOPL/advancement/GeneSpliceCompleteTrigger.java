package com.TKCCOPL.advancement;

import com.TKCCOPL.cybercultivator;
import com.google.gson.JsonObject;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * 自定义触发器：基因拼接完成。
 * 当玩家从基因拼接机输出槽取出子代种子时触发。
 */
public class GeneSpliceCompleteTrigger extends SimpleCriterionTrigger<GeneSpliceCompleteTrigger.Instance> {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(cybercultivator.MODID, "gene_splice_complete");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    protected Instance createInstance(JsonObject json, ContextAwarePredicate player,
                                     DeserializationContext context) {
        return new Instance(player);
    }

    public void trigger(ServerPlayer player) {
        this.trigger(player, instance -> true);
    }

    public static class Instance extends AbstractCriterionTriggerInstance {
        public Instance(ContextAwarePredicate player) {
            super(ID, player);
        }

        public static Instance forAny() {
            return new Instance(ContextAwarePredicate.ANY);
        }
    }
}

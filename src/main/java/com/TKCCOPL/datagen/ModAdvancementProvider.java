package com.TKCCOPL.datagen;

import com.TKCCOPL.cybercultivator;
import com.TKCCOPL.init.ModItems;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.FrameType;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.ForgeAdvancementProvider;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ModAdvancementProvider extends ForgeAdvancementProvider {
    public ModAdvancementProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                                  ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, existingFileHelper, List.of(new AdvancementGenerator()));
    }

    static class AdvancementGenerator implements ForgeAdvancementProvider.AdvancementGenerator {
        @Override
        public void generate(HolderLookup.Provider registries, Consumer<Advancement> saver, ExistingFileHelper existingFileHelper) {
            // Root: 赛博农夫
            Advancement root = Advancement.Builder.advancement()
                    .display(ModItems.SILICON_SHARD.get(),
                            Component.translatable("advancement.cybercultivator.root.title"),
                            Component.translatable("advancement.cybercultivator.root.description"),
                            new ResourceLocation(cybercultivator.MODID, "textures/block/silicon_ore.png"),
                            FrameType.TASK, false, false, false)
                    .addCriterion("has_silicon_shard", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.SILICON_SHARD.get()))
                    .save(saver, cybercultivator.MODID + ":root");

            // 硅基起步 — 获得硅碎片
            Advancement siliconStart = Advancement.Builder.advancement()
                    .parent(root)
                    .display(ModItems.SILICON_SHARD.get(),
                            Component.translatable("advancement.cybercultivator.silicon_start.title"),
                            Component.translatable("advancement.cybercultivator.silicon_start.description"),
                            null, FrameType.TASK, true, true, false)
                    .addCriterion("has_silicon_shard", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.SILICON_SHARD.get()))
                    .save(saver, cybercultivator.MODID + ":silicon_start");

            // 稀土之源 — 获得稀土粉末
            Advancement rareEarth = Advancement.Builder.advancement()
                    .parent(siliconStart)
                    .display(ModItems.RARE_EARTH_DUST.get(),
                            Component.translatable("advancement.cybercultivator.rare_earth.title"),
                            Component.translatable("advancement.cybercultivator.rare_earth.description"),
                            null, FrameType.TASK, true, true, false)
                    .addCriterion("has_rare_earth_dust", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.RARE_EARTH_DUST.get()))
                    .save(saver, cybercultivator.MODID + ":rare_earth");

            // 生化培育 — 获得生化原液
            Advancement bioCultivate = Advancement.Builder.advancement()
                    .parent(root)
                    .display(ModItems.BIOCHEMICAL_SOLUTION.get(),
                            Component.translatable("advancement.cybercultivator.bio_cultivate.title"),
                            Component.translatable("advancement.cybercultivator.bio_cultivate.description"),
                            null, FrameType.TASK, true, true, false)
                    .addCriterion("has_biochemical_solution", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.BIOCHEMICAL_SOLUTION.get()))
                    .save(saver, cybercultivator.MODID + ":bio_cultivate");

            // 基因密码 — 使用基因拼接机（获得任意种子即视为使用过）
            Advancement geneCode = Advancement.Builder.advancement()
                    .parent(bioCultivate)
                    .display(ModItems.FIBER_REED_SEEDS.get(),
                            Component.translatable("advancement.cybercultivator.gene_code.title"),
                            Component.translatable("advancement.cybercultivator.gene_code.description"),
                            null, FrameType.TASK, true, true, false)
                    .addCriterion("has_any_seed", InventoryChangeTrigger.TriggerInstance.hasItems(
                            ItemPredicate.Builder.item().of(
                                    ModItems.FIBER_REED_SEEDS.get(),
                                    ModItems.PROTEIN_SOY_SEEDS.get(),
                                    ModItems.ALCOHOL_BLOOM_SEEDS.get()).build()))
                    .save(saver, cybercultivator.MODID + ":gene_code");

            // 血清之路 — 获得 S-01 血清
            Advancement serumPath = Advancement.Builder.advancement()
                    .parent(root)
                    .display(ModItems.SYNAPTIC_SERUM_S01.get(),
                            Component.translatable("advancement.cybercultivator.serum_path.title"),
                            Component.translatable("advancement.cybercultivator.serum_path.description"),
                            null, FrameType.GOAL, true, true, false)
                    .addCriterion("has_serum_s01", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.SYNAPTIC_SERUM_S01.get()))
                    .save(saver, cybercultivator.MODID + ":serum_path");

            // 视觉超越 — 获得 S-02 血清
            Advancement visual超越 = Advancement.Builder.advancement()
                    .parent(serumPath)
                    .display(ModItems.SYNAPTIC_SERUM_S02.get(),
                            Component.translatable("advancement.cybercultivator.visual_enhancement.title"),
                            Component.translatable("advancement.cybercultivator.visual_enhancement.description"),
                            null, FrameType.CHALLENGE, true, true, false)
                    .addCriterion("has_serum_s02", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.SYNAPTIC_SERUM_S02.get()))
                    .save(saver, cybercultivator.MODID + ":visual_enhancement");

            // 代谢狂飙 — 获得 S-03 血清
            Advancement metabolicBoost = Advancement.Builder.advancement()
                    .parent(serumPath)
                    .display(ModItems.SYNAPTIC_SERUM_S03.get(),
                            Component.translatable("advancement.cybercultivator.metabolic_boost.title"),
                            Component.translatable("advancement.cybercultivator.metabolic_boost.description"),
                            null, FrameType.CHALLENGE, true, true, false)
                    .addCriterion("has_serum_s03", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.SYNAPTIC_SERUM_S03.get()))
                    .save(saver, cybercultivator.MODID + ":metabolic_boost");

            // 赛博装备 — 获得任一 Curios 饰品
            Advancement cyberEquip = Advancement.Builder.advancement()
                    .parent(root)
                    .display(ModItems.SPECTRUM_MONOCLE.get(),
                            Component.translatable("advancement.cybercultivator.cyber_equip.title"),
                            Component.translatable("advancement.cybercultivator.cyber_equip.description"),
                            null, FrameType.TASK, true, true, false)
                    .addCriterion("has_monocle", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.SPECTRUM_MONOCLE.get()))
                    .addCriterion("has_belt", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.BIO_PULSE_BELT.get()))
                    .addCriterion("has_pack", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.LIFE_SUPPORT_PACK.get()))
                    .save(saver, cybercultivator.MODID + ":cyber_equip");
        }
    }
}

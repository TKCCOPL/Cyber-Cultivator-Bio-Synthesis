package com.TKCCOPL.datagen;

import com.TKCCOPL.init.ModBlocks;
import com.TKCCOPL.init.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ModLootTableProvider extends LootTableProvider {
    public ModLootTableProvider(PackOutput output) {
        super(output, Set.of(), List.of(new SubProviderEntry(ModBlockLootTables::new, LootContextParamSets.BLOCK)));
    }

    public static class ModBlockLootTables extends BlockLootSubProvider {
        protected ModBlockLootTables() {
            super(Set.of(), FeatureFlags.REGISTRY.allFlags());
        }

        @Override
        protected void generate() {
            dropOther(ModBlocks.SILICON_ORE.get(), ModItems.SILICON_SHARD.get());
            dropOther(ModBlocks.RARE_EARTH_ORE.get(), ModItems.RARE_EARTH_DUST.get());
            dropSelf(ModBlocks.BIO_INCUBATOR.get());
            dropSelf(ModBlocks.GENE_SPLICER.get());
            dropSelf(ModBlocks.ATMOSPHERIC_CONDENSER.get());
            dropSelf(ModBlocks.SERUM_BOTTLER.get());
            // Crop drops: mature = harvest + seed, immature = seed only
            add(ModBlocks.FIBER_REED_CROP.get(), createCropDrops(
                    ModBlocks.FIBER_REED_CROP.get(), ModItems.PLANT_FIBER.get(), ModItems.FIBER_REED_SEEDS.get(),
                    LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.FIBER_REED_CROP.get())
                            .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(CropBlock.AGE, 7))));
            add(ModBlocks.PROTEIN_SOY_CROP.get(), createCropDrops(
                    ModBlocks.PROTEIN_SOY_CROP.get(), ModItems.BIOCHEMICAL_SOLUTION.get(), ModItems.PROTEIN_SOY_SEEDS.get(),
                    LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.PROTEIN_SOY_CROP.get())
                            .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(CropBlock.AGE, 7))));
            add(ModBlocks.ALCOHOL_BLOOM_CROP.get(), createCropDrops(
                    ModBlocks.ALCOHOL_BLOOM_CROP.get(), ModItems.INDUSTRIAL_ETHANOL.get(), ModItems.ALCOHOL_BLOOM_SEEDS.get(),
                    LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModBlocks.ALCOHOL_BLOOM_CROP.get())
                            .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(CropBlock.AGE, 7))));
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return ModBlocks.BLOCKS.getEntries().stream().map(block -> (Block) block.get()).collect(Collectors.toList());
        }
    }
}

package com.TKCCOPL.datagen;

import com.TKCCOPL.init.ModBlocks;
import com.TKCCOPL.init.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

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
            dropOther(ModBlocks.FIBER_REED_CROP.get(), ModItems.PLANT_FIBER.get());
            dropOther(ModBlocks.PROTEIN_SOY_CROP.get(), ModItems.BIOCHEMICAL_SOLUTION.get());
            dropOther(ModBlocks.ALCOHOL_BLOOM_CROP.get(), ModItems.INDUSTRIAL_ETHANOL.get());
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return ModBlocks.BLOCKS.getEntries().stream().map(block -> (Block) block.get()).collect(Collectors.toList());
        }
    }
}

package com.TKCCOPL.datagen;

import com.TKCCOPL.cybercultivator;
import com.TKCCOPL.init.ModBlocks;
import com.TKCCOPL.init.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends BlockTagsProvider {
    public ModBlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, cybercultivator.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(
                ModBlocks.SILICON_ORE.get(),
                ModBlocks.RARE_EARTH_ORE.get(),
                ModBlocks.DEEPSLATE_SILICON_ORE.get(),
                ModBlocks.DEEPSLATE_RARE_EARTH_ORE.get(),
                ModBlocks.RAW_SILICON_BLOCK.get(),
                ModBlocks.RAW_RARE_EARTH_BLOCK.get(),
                ModBlocks.SILICON_BLOCK.get(),
                ModBlocks.RARE_EARTH_BLOCK.get(),
                ModBlocks.BIO_INCUBATOR.get(),
                ModBlocks.GENE_SPLICER.get(),
                ModBlocks.ATMOSPHERIC_CONDENSER.get(),
                ModBlocks.SERUM_BOTTLER.get()
        );

        tag(BlockTags.NEEDS_STONE_TOOL).add(
                ModBlocks.SILICON_ORE.get(),
                ModBlocks.RARE_EARTH_ORE.get(),
                ModBlocks.DEEPSLATE_SILICON_ORE.get(),
                ModBlocks.DEEPSLATE_RARE_EARTH_ORE.get(),
                ModBlocks.RAW_SILICON_BLOCK.get(),
                ModBlocks.RAW_RARE_EARTH_BLOCK.get(),
                ModBlocks.SILICON_BLOCK.get(),
                ModBlocks.RARE_EARTH_BLOCK.get()
        );

        tag(BlockTags.CROPS).add(
                ModBlocks.FIBER_REED_CROP.get(),
                ModBlocks.PROTEIN_SOY_CROP.get(),
                ModBlocks.ALCOHOL_BLOOM_CROP.get()
        );

        tag(BlockTags.MAINTAINS_FARMLAND).add(
                ModBlocks.FIBER_REED_CROP.get(),
                ModBlocks.PROTEIN_SOY_CROP.get(),
                ModBlocks.ALCOHOL_BLOOM_CROP.get()
        );

        tag(BlockTags.BEE_GROWABLES).add(
                ModBlocks.FIBER_REED_CROP.get(),
                ModBlocks.PROTEIN_SOY_CROP.get(),
                ModBlocks.ALCOHOL_BLOOM_CROP.get()
        );

        // === v1.1.7 §10.2 跨模组材料标签（方块） ===
        tag(ModTags.ForgeBlocks.ORES_SILICON).add(
                ModBlocks.SILICON_ORE.get(),
                ModBlocks.DEEPSLATE_SILICON_ORE.get());
        tag(ModTags.ForgeBlocks.ORES_RARE_EARTH).add(
                ModBlocks.RARE_EARTH_ORE.get(),
                ModBlocks.DEEPSLATE_RARE_EARTH_ORE.get());
        tag(ModTags.ForgeBlocks.STORAGE_BLOCKS_RAW_SILICON).add(ModBlocks.RAW_SILICON_BLOCK.get());
        tag(ModTags.ForgeBlocks.STORAGE_BLOCKS_SILICON).add(ModBlocks.SILICON_BLOCK.get());
        tag(ModTags.ForgeBlocks.STORAGE_BLOCKS_RAW_RARE_EARTH).add(ModBlocks.RAW_RARE_EARTH_BLOCK.get());
        tag(ModTags.ForgeBlocks.STORAGE_BLOCKS_RARE_EARTH).add(ModBlocks.RARE_EARTH_BLOCK.get());

        // 父标签
        tag(ModTags.ForgeBlocks.ORES)
                .addTag(ModTags.ForgeBlocks.ORES_SILICON)
                .addTag(ModTags.ForgeBlocks.ORES_RARE_EARTH);
        tag(ModTags.ForgeBlocks.STORAGE_BLOCKS)
                .addTag(ModTags.ForgeBlocks.STORAGE_BLOCKS_RAW_SILICON)
                .addTag(ModTags.ForgeBlocks.STORAGE_BLOCKS_SILICON)
                .addTag(ModTags.ForgeBlocks.STORAGE_BLOCKS_RAW_RARE_EARTH)
                .addTag(ModTags.ForgeBlocks.STORAGE_BLOCKS_RARE_EARTH);
    }
}

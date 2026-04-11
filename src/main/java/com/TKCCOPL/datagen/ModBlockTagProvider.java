package com.TKCCOPL.datagen;

import com.TKCCOPL.cybercultivator;
import com.TKCCOPL.init.ModBlocks;
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
                ModBlocks.BIO_INCUBATOR.get(),
                ModBlocks.GENE_SPLICER.get()
        );

        tag(BlockTags.NEEDS_STONE_TOOL).add(
                ModBlocks.SILICON_ORE.get(),
                ModBlocks.RARE_EARTH_ORE.get()
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
    }
}

package com.TKCCOPL.init;

import com.TKCCOPL.block.BasicCropBlock;
import com.TKCCOPL.block.BioIncubatorBlock;
import com.TKCCOPL.block.GeneSplicerBlock;
import com.TKCCOPL.cybercultivator;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, cybercultivator.MODID);

    public static final RegistryObject<Block> SILICON_ORE = BLOCKS.register("silicon_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0F, 3.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)));

    public static final RegistryObject<Block> RARE_EARTH_ORE = BLOCKS.register("rare_earth_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.2F, 3.5F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)));

    public static final RegistryObject<Block> BIO_INCUBATOR = BLOCKS.register("bio_incubator",
            () -> new BioIncubatorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(4.0F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)));

    public static final RegistryObject<Block> GENE_SPLICER = BLOCKS.register("gene_splicer",
            () -> new GeneSplicerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(4.0F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)));

    public static final RegistryObject<CropBlock> FIBER_REED_CROP = BLOCKS.register("fiber_reed_crop",
            () -> new BasicCropBlock(BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.WHEAT), ModItems.FIBER_REED_SEEDS));

    public static final RegistryObject<CropBlock> PROTEIN_SOY_CROP = BLOCKS.register("protein_soy_crop",
            () -> new BasicCropBlock(BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.WHEAT), ModItems.PROTEIN_SOY_SEEDS));

    public static final RegistryObject<CropBlock> ALCOHOL_BLOOM_CROP = BLOCKS.register("alcohol_bloom_crop",
            () -> new BasicCropBlock(BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.WHEAT), ModItems.ALCOHOL_BLOOM_SEEDS));

    private ModBlocks() {
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}

package com.TKCCOPL.datagen;

import com.TKCCOPL.cybercultivator;
import com.TKCCOPL.block.MachineBlock;
import com.TKCCOPL.init.ModBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Function;

public class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, cybercultivator.MODID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        // Ore blocks — simple cube_all
        simpleBlockWithItem(ModBlocks.SILICON_ORE.get(), cubeAll(ModBlocks.SILICON_ORE.get()));
        simpleBlockWithItem(ModBlocks.RARE_EARTH_ORE.get(), cubeAll(ModBlocks.RARE_EARTH_ORE.get()));

        // Machine blocks — multi-face cube with horizontal facing
        machineBlock(ModBlocks.BIO_INCUBATOR.get());
        machineBlock(ModBlocks.GENE_SPLICER.get());
        machineBlock(ModBlocks.ATMOSPHERIC_CONDENSER.get());
        machineBlock(ModBlocks.SERUM_BOTTLER.get());

        // Crop blocks — use crop model
        cropBlock(ModBlocks.FIBER_REED_CROP.get(), CropBlock.AGE, "fiber_reed_stage");
        cropBlock(ModBlocks.PROTEIN_SOY_CROP.get(), CropBlock.AGE, "protein_soy_stage");
        cropBlock(ModBlocks.ALCOHOL_BLOOM_CROP.get(), CropBlock.AGE, "alcohol_bloom_stage");
    }

    private void machineBlock(Block block) {
        String name = ForgeRegistries.BLOCKS.getKey(block).getPath();
        ModelFile model = models().cube(name,
                modLoc("block/machine_bottom"),
                modLoc("block/" + name + "_top"),
                modLoc("block/" + name + "_front"),
                modLoc("block/machine_side"),
                modLoc("block/machine_side"),
                modLoc("block/machine_side"))
                .texture("particle", modLoc("block/machine_side"));

        // Generate blockstate with facing variants
        getVariantBuilder(block).forAllStates(state -> {
            int yRot = switch (state.getValue(MachineBlock.FACING)) {
                case NORTH -> 0;
                case EAST -> 90;
                case SOUTH -> 180;
                case WEST -> 270;
                default -> 0;
            };
            return ConfiguredModel.builder()
                    .modelFile(model)
                    .rotationY(yRot)
                    .build();
        });

        // Generate item model referencing block model
        itemModels().getBuilder(name).parent(model);
    }

    private void cropBlock(Block block, IntegerProperty ageProperty, String texturePrefix) {
        String blockName = ForgeRegistries.BLOCKS.getKey(block).getPath();
        // 8 ages → 4 visual stages (like vanilla wheat)
        // age 0-1 → stage0, age 2-3 → stage1, age 4-5 → stage2, age 6-7 → stage3
        ModelFile[] stageModels = new ModelFile[4];
        for (int s = 0; s < 4; s++) {
            stageModels[s] = models().crop(blockName + "_stage" + s,
                    modLoc("block/" + texturePrefix + s)).renderType("cutout");
        }
        Function<BlockState, ConfiguredModel[]> mapper = state -> {
            int age = state.getValue(ageProperty);
            int stage = age / 2; // 0-1→0, 2-3→1, 4-5→2, 6-7→3
            return ConfiguredModel.builder().modelFile(stageModels[stage]).build();
        };
        getVariantBuilder(block).forAllStatesExcept(mapper);
    }
}

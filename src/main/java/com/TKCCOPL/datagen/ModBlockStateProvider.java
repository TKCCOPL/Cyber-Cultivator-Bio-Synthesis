package com.TKCCOPL.datagen;

import com.TKCCOPL.cybercultivator;
import com.TKCCOPL.init.ModBlocks;
import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

public class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, cybercultivator.MODID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        simpleBlockWithItem(ModBlocks.SILICON_ORE.get(), cubeAll(ModBlocks.SILICON_ORE.get()));
        simpleBlockWithItem(ModBlocks.RARE_EARTH_ORE.get(), cubeAll(ModBlocks.RARE_EARTH_ORE.get()));
    }
}

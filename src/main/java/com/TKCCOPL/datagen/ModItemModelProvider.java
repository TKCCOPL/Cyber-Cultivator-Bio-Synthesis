package com.TKCCOPL.datagen;

import com.TKCCOPL.cybercultivator;
import com.TKCCOPL.init.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, cybercultivator.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        basicItem(ModItems.SILICON_SHARD.get());
        basicItem(ModItems.RARE_EARTH_DUST.get());
        basicItem(ModItems.PLANT_FIBER.get());
        basicItem(ModItems.BIOCHEMICAL_SOLUTION.get());
        basicItem(ModItems.INDUSTRIAL_ETHANOL.get());
        basicItem(ModItems.FIBER_REED_SEEDS.get());
        basicItem(ModItems.PROTEIN_SOY_SEEDS.get());
        basicItem(ModItems.ALCOHOL_BLOOM_SEEDS.get());
        basicItem(ModItems.SPECTRUM_MONOCLE.get());
    }
}

package com.TKCCOPL.init;

import com.TKCCOPL.block.entity.BioIncubatorBlockEntity;
import com.TKCCOPL.block.entity.GeneSplicerBlockEntity;
import com.TKCCOPL.cybercultivator;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, cybercultivator.MODID);

    public static final RegistryObject<BlockEntityType<BioIncubatorBlockEntity>> BIO_INCUBATOR = BLOCK_ENTITIES.register("bio_incubator",
            () -> BlockEntityType.Builder.of(BioIncubatorBlockEntity::new, ModBlocks.BIO_INCUBATOR.get()).build(null));

        public static final RegistryObject<BlockEntityType<GeneSplicerBlockEntity>> GENE_SPLICER = BLOCK_ENTITIES.register("gene_splicer",
            () -> BlockEntityType.Builder.of(GeneSplicerBlockEntity::new, ModBlocks.GENE_SPLICER.get()).build(null));

    private ModBlockEntities() {
    }

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}

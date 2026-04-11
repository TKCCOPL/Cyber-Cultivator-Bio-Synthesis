package com.TKCCOPL.init;

import com.TKCCOPL.cybercultivator;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, cybercultivator.MODID);

    public static final RegistryObject<CreativeModeTab> CYBER_CULTIVATOR_TAB = CREATIVE_MODE_TABS.register("cyber_cultivator_tab",
            () -> CreativeModeTab.builder()
                    .title(net.minecraft.network.chat.Component.translatable("itemGroup.cybercultivator"))
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> ModItems.SPECTRUM_MONOCLE.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.SPECTRUM_MONOCLE.get());
                        output.accept(ModItems.SILICON_ORE_ITEM.get());
                        output.accept(ModItems.RARE_EARTH_ORE_ITEM.get());
                        output.accept(ModItems.BIO_INCUBATOR_ITEM.get());
                        output.accept(ModItems.GENE_SPLICER_ITEM.get());
                        output.accept(ModItems.SILICON_SHARD.get());
                        output.accept(ModItems.RARE_EARTH_DUST.get());
                        output.accept(ModItems.PLANT_FIBER.get());
                        output.accept(ModItems.BIOCHEMICAL_SOLUTION.get());
                        output.accept(ModItems.INDUSTRIAL_ETHANOL.get());
                        output.accept(ModItems.SYNAPTIC_NEURAL_BERRY.get());
                        output.accept(ModItems.SYNAPTIC_SERUM_S01.get());
                        output.accept(ModItems.FIBER_REED_SEEDS.get());
                        output.accept(ModItems.PROTEIN_SOY_SEEDS.get());
                        output.accept(ModItems.ALCOHOL_BLOOM_SEEDS.get());
                    })
                    .build());

    private ModCreativeTabs() {
    }

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}

package com.TKCCOPL.init;

import com.TKCCOPL.cybercultivator;
import com.TKCCOPL.item.SynapticSerumItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, cybercultivator.MODID);

    public static final RegistryObject<CreativeModeTab> CYBER_CULTIVATOR_TAB = CREATIVE_MODE_TABS.register("cyber_cultivator_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.cybercultivator"))
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> ModItems.ALCOHOL_BLOOM_SEEDS.get().getDefaultInstance())
                    .displayItems((parameters, output) -> addMainItems(output))
                    .build());

    public static final RegistryObject<CreativeModeTab> QUALITY_SAMPLES_TAB = CREATIVE_MODE_TABS.register("quality_samples_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.cybercultivator.quality_samples"))
                    .withTabsAfter(CYBER_CULTIVATOR_TAB.getKey())
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> CreativeTabVariants.activityStack(
                            ModItems.SYNAPTIC_NEURAL_BERRY.get(), SynapticSerumItem.DEFAULT_ACTIVITY))
                    .displayItems((parameters, output) -> {
                        CreativeTabVariants.addBalancedSeedVariants(output);
                        CreativeTabVariants.addMaterialVariants(output);
                        CreativeTabVariants.addActivityVariants(output);
                    })
                    .build());

    private ModCreativeTabs() {
    }

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }

    private static void addMainItems(CreativeModeTab.Output output) {
        // 基础设施与加工资源
        output.accept(ModItems.SILICON_ORE_ITEM.get());
        output.accept(ModItems.RAW_SILICON_CRYSTAL.get());
        output.accept(ModItems.RAW_SILICON_BLOCK_ITEM.get());
        output.accept(ModItems.SILICON_SHARD.get());
        output.accept(ModItems.SILICON_BLOCK_ITEM.get());
        output.accept(ModItems.RARE_EARTH_ORE_ITEM.get());
        output.accept(ModItems.DEEPSLATE_SILICON_ORE_ITEM.get());
        output.accept(ModItems.DEEPSLATE_RARE_EARTH_ORE_ITEM.get());
        output.accept(ModItems.RAW_RARE_EARTH.get());
        output.accept(ModItems.RAW_RARE_EARTH_BLOCK_ITEM.get());
        output.accept(ModItems.RARE_EARTH_DUST.get());
        output.accept(ModItems.RARE_EARTH_BLOCK_ITEM.get());
        output.accept(ModItems.BIO_INCUBATOR_ITEM.get());
        output.accept(ModItems.GENE_SPLICER_ITEM.get());
        output.accept(ModItems.ATMOSPHERIC_CONDENSER_ITEM.get());
        output.accept(ModItems.SERUM_BOTTLER_ITEM.get());
        output.accept(ModItems.SPECTRUM_MONOCLE.get());

        // 基础种子统一使用 Speed / Yield / Potency = 5 / 5 / 5
        output.accept(ModItems.FIBER_REED_SEEDS.get().getDefaultInstance());
        output.accept(ModItems.PROTEIN_SOY_SEEDS.get().getDefaultInstance());
        output.accept(ModItems.ALCOHOL_BLOOM_SEEDS.get().getDefaultInstance());
        output.accept(ModItems.PURIFIED_WATER_BOTTLE.get());
        output.accept(ModItems.BIO_PULSE_BELT.get());
        output.accept(ModItems.LIFE_SUPPORT_PACK.get());

        // 主页面只展示平衡品质代表，完整品质范围位于相邻的品质样本页面
        CreativeTabVariants.addDefaultQualityItems(output);
    }
}

package com.TKCCOPL.init;

import com.TKCCOPL.cybercultivator;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
                        output.accept(ModItems.ATMOSPHERIC_CONDENSER_ITEM.get());
                        output.accept(ModItems.SERUM_BOTTLER_ITEM.get());
                        output.accept(ModItems.SILICON_SHARD.get());
                        output.accept(ModItems.RARE_EARTH_DUST.get());
                        output.accept(ModItems.PLANT_FIBER.get());
                        output.accept(ModItems.BIOCHEMICAL_SOLUTION.get());
                        output.accept(ModItems.INDUSTRIAL_ETHANOL.get());
                        output.accept(ModItems.PURIFIED_WATER_BOTTLE.get());
                        output.accept(ModItems.SYNAPTIC_NEURAL_BERRY.get());
                        output.accept(ModItems.SYNAPTIC_SERUM_S01.get());
                        output.accept(ModItems.SYNAPTIC_SERUM_S02.get());
                        output.accept(ModItems.SYNAPTIC_SERUM_S03.get());
                        output.accept(ModItems.BIO_PULSE_BELT.get());
                        output.accept(ModItems.LIFE_SUPPORT_PACK.get());
                        output.accept(ModItems.FIBER_REED_SEEDS.get());
                        output.accept(ModItems.PROTEIN_SOY_SEEDS.get());
                        output.accept(ModItems.ALCOHOL_BLOOM_SEEDS.get());

                        // --- 预置品质物品变体（1-10），方便测试 ---

                        // 植物纤维：Potency 1-10
                        for (int i = 1; i <= 10; i++) output.accept(withTag(ModItems.PLANT_FIBER.get(), "Potency", i));
                        // 工业乙醇：Purity 1-10
                        for (int i = 1; i <= 10; i++) output.accept(withTag(ModItems.INDUSTRIAL_ETHANOL.get(), "Purity", i));
                        // 生化原液：Concentration 1-10
                        for (int i = 1; i <= 10; i++) output.accept(withTag(ModItems.BIOCHEMICAL_SOLUTION.get(), "Concentration", i));
                        // 突触神经莓：SynapticActivity 1-10
                        for (int i = 1; i <= 10; i++) output.accept(withTag(ModItems.SYNAPTIC_NEURAL_BERRY.get(), "SynapticActivity", i));
                        // S-01 血清：SynapticActivity 1-10
                        for (int i = 1; i <= 10; i++) output.accept(withTag(ModItems.SYNAPTIC_SERUM_S01.get(), "SynapticActivity", i));
                        // S-02 血清：SynapticActivity 1-10
                        for (int i = 1; i <= 10; i++) output.accept(withTag(ModItems.SYNAPTIC_SERUM_S02.get(), "SynapticActivity", i));
                        // S-03 血清：SynapticActivity 1-10
                        for (int i = 1; i <= 10; i++) output.accept(withTag(ModItems.SYNAPTIC_SERUM_S03.get(), "SynapticActivity", i));

                        // --- Gene_Synergy 高 Activity 变体（突破 10 上限），方便测试 ---
                        // Gene_Synergy=4 → cap=12, Gene_Synergy=8 → cap=14, Gene_Synergy=10 → cap=15
                        int[] synergyValues = {4, 8, 10};
                        int[] activityCaps = {12, 14, 15};
                        for (int idx = 0; idx < synergyValues.length; idx++) {
                            int synergy = synergyValues[idx];
                            int cap = activityCaps[idx];
                            // 莓
                            output.accept(withSynergyAndActivity(ModItems.SYNAPTIC_NEURAL_BERRY.get(), synergy, cap));
                            // S-01 / S-02 / S-03 血清
                            output.accept(withSynergyAndActivity(ModItems.SYNAPTIC_SERUM_S01.get(), synergy, cap));
                            output.accept(withSynergyAndActivity(ModItems.SYNAPTIC_SERUM_S02.get(), synergy, cap));
                            output.accept(withSynergyAndActivity(ModItems.SYNAPTIC_SERUM_S03.get(), synergy, cap));
                        }

                        // --- Gene_Generation / Gene_Synergy / Mutation 样本种子 ---

                        // 纤维蔗种子：默认 (gen 0)、gen 3、gen 5、gen 10
                        output.accept(ModItems.FIBER_REED_SEEDS.get()); // gen 0 默认
                        ItemStack gen3Fiber = new ItemStack(ModItems.FIBER_REED_SEEDS.get());
                        gen3Fiber.getOrCreateTag().putInt("Gene_Speed", 7);
                        gen3Fiber.getOrCreateTag().putInt("Gene_Yield", 5);
                        gen3Fiber.getOrCreateTag().putInt("Gene_Potency", 8);
                        gen3Fiber.getOrCreateTag().putInt("Gene_Generation", 3);
                        output.accept(gen3Fiber);
                        ItemStack gen5Fiber = new ItemStack(ModItems.FIBER_REED_SEEDS.get());
                        gen5Fiber.getOrCreateTag().putInt("Gene_Speed", 9);
                        gen5Fiber.getOrCreateTag().putInt("Gene_Yield", 7);
                        gen5Fiber.getOrCreateTag().putInt("Gene_Potency", 10);
                        gen5Fiber.getOrCreateTag().putInt("Gene_Generation", 5);
                        output.accept(gen5Fiber);
                        ItemStack gen10Fiber = new ItemStack(ModItems.FIBER_REED_SEEDS.get());
                        gen10Fiber.getOrCreateTag().putInt("Gene_Speed", 10);
                        gen10Fiber.getOrCreateTag().putInt("Gene_Yield", 10);
                        gen10Fiber.getOrCreateTag().putInt("Gene_Potency", 10);
                        gen10Fiber.getOrCreateTag().putInt("Gene_Generation", 10);
                        output.accept(gen10Fiber);

                        // 纤维蔗：突变种子 (Gene_Synergy + Mutation)
                        ItemStack mutFiber = new ItemStack(ModItems.FIBER_REED_SEEDS.get());
                        mutFiber.getOrCreateTag().putInt("Gene_Speed", 8);
                        mutFiber.getOrCreateTag().putInt("Gene_Yield", 6);
                        mutFiber.getOrCreateTag().putInt("Gene_Potency", 9);
                        mutFiber.getOrCreateTag().putInt("Gene_Generation", 5);
                        mutFiber.getOrCreateTag().putInt("Gene_Synergy", 6);
                        mutFiber.getOrCreateTag().putInt("Mutation", 2);
                        mutFiber.getOrCreateTag().putString("MutationDetail", "Synergy+6");
                        output.accept(mutFiber);

                        // 蛋白质豆种子：默认 (gen 0)、gen 3、gen 5、gen 10
                        output.accept(ModItems.PROTEIN_SOY_SEEDS.get()); // gen 0 默认
                        ItemStack gen3Soy = new ItemStack(ModItems.PROTEIN_SOY_SEEDS.get());
                        gen3Soy.getOrCreateTag().putInt("Gene_Speed", 5);
                        gen3Soy.getOrCreateTag().putInt("Gene_Yield", 6);
                        gen3Soy.getOrCreateTag().putInt("Gene_Potency", 8);
                        gen3Soy.getOrCreateTag().putInt("Gene_Generation", 3);
                        output.accept(gen3Soy);
                        ItemStack gen5Soy = new ItemStack(ModItems.PROTEIN_SOY_SEEDS.get());
                        gen5Soy.getOrCreateTag().putInt("Gene_Speed", 8);
                        gen5Soy.getOrCreateTag().putInt("Gene_Yield", 9);
                        gen5Soy.getOrCreateTag().putInt("Gene_Potency", 7);
                        gen5Soy.getOrCreateTag().putInt("Gene_Generation", 5);
                        output.accept(gen5Soy);
                        ItemStack gen10Soy = new ItemStack(ModItems.PROTEIN_SOY_SEEDS.get());
                        gen10Soy.getOrCreateTag().putInt("Gene_Speed", 10);
                        gen10Soy.getOrCreateTag().putInt("Gene_Yield", 10);
                        gen10Soy.getOrCreateTag().putInt("Gene_Potency", 10);
                        gen10Soy.getOrCreateTag().putInt("Gene_Generation", 10);
                        output.accept(gen10Soy);

                        // 蛋白质豆：突变种子 (Gene_Synergy + Mutation)
                        ItemStack mutSoy = new ItemStack(ModItems.PROTEIN_SOY_SEEDS.get());
                        mutSoy.getOrCreateTag().putInt("Gene_Speed", 7);
                        mutSoy.getOrCreateTag().putInt("Gene_Yield", 5);
                        mutSoy.getOrCreateTag().putInt("Gene_Potency", 8);
                        mutSoy.getOrCreateTag().putInt("Gene_Generation", 4);
                        mutSoy.getOrCreateTag().putInt("Gene_Synergy", 4);
                        mutSoy.getOrCreateTag().putInt("Mutation", 2);
                        mutSoy.getOrCreateTag().putString("MutationDetail", "Synergy+4");
                        output.accept(mutSoy);

                        // 酒精花种子：默认 (gen 0)、gen 3、gen 5、gen 10
                        output.accept(ModItems.ALCOHOL_BLOOM_SEEDS.get()); // gen 0 默认
                        ItemStack gen3Bloom = new ItemStack(ModItems.ALCOHOL_BLOOM_SEEDS.get());
                        gen3Bloom.getOrCreateTag().putInt("Gene_Speed", 7);
                        gen3Bloom.getOrCreateTag().putInt("Gene_Yield", 4);
                        gen3Bloom.getOrCreateTag().putInt("Gene_Potency", 6);
                        gen3Bloom.getOrCreateTag().putInt("Gene_Generation", 3);
                        output.accept(gen3Bloom);
                        ItemStack gen5Bloom = new ItemStack(ModItems.ALCOHOL_BLOOM_SEEDS.get());
                        gen5Bloom.getOrCreateTag().putInt("Gene_Speed", 6);
                        gen5Bloom.getOrCreateTag().putInt("Gene_Yield", 8);
                        gen5Bloom.getOrCreateTag().putInt("Gene_Potency", 10);
                        gen5Bloom.getOrCreateTag().putInt("Gene_Generation", 5);
                        output.accept(gen5Bloom);
                        ItemStack gen10Bloom = new ItemStack(ModItems.ALCOHOL_BLOOM_SEEDS.get());
                        gen10Bloom.getOrCreateTag().putInt("Gene_Speed", 10);
                        gen10Bloom.getOrCreateTag().putInt("Gene_Yield", 10);
                        gen10Bloom.getOrCreateTag().putInt("Gene_Potency", 10);
                        gen10Bloom.getOrCreateTag().putInt("Gene_Generation", 10);
                        output.accept(gen10Bloom);

                        // 酒精花：突变种子 (Gene_Synergy + Mutation)
                        ItemStack mutBloom = new ItemStack(ModItems.ALCOHOL_BLOOM_SEEDS.get());
                        mutBloom.getOrCreateTag().putInt("Gene_Speed", 9);
                        mutBloom.getOrCreateTag().putInt("Gene_Yield", 7);
                        mutBloom.getOrCreateTag().putInt("Gene_Potency", 10);
                        mutBloom.getOrCreateTag().putInt("Gene_Generation", 6);
                        mutBloom.getOrCreateTag().putInt("Gene_Synergy", 8);
                        mutBloom.getOrCreateTag().putInt("Mutation", 2);
                        mutBloom.getOrCreateTag().putString("MutationDetail", "Synergy+8");
                        output.accept(mutBloom);
                    })
                    .build());

    private ModCreativeTabs() {
    }

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }

    private static ItemStack withTag(Item item, String key, int value) {
        ItemStack stack = new ItemStack(item);
        stack.getOrCreateTag().putInt(key, value);
        return stack;
    }

    private static ItemStack withSynergyAndActivity(Item item, int geneSynergy, int activity) {
        ItemStack stack = new ItemStack(item);
        stack.getOrCreateTag().putInt("Gene_Synergy", geneSynergy);
        stack.getOrCreateTag().putInt("SynapticActivity", activity);
        return stack;
    }
}

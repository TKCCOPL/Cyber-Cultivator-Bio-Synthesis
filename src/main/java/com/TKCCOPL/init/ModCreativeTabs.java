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

                        // --- Gene_Purity 高 Activity 变体（突破 10 上限），方便测试 ---
                        // Gene_Purity=4 → cap=12, Gene_Purity=8 → cap=14, Gene_Purity=10 → cap=15
                        int[] purityValues = {4, 8, 10};
                        int[] activityCaps = {12, 14, 15};
                        for (int idx = 0; idx < purityValues.length; idx++) {
                            int purity = purityValues[idx];
                            int cap = activityCaps[idx];
                            // 莓
                            output.accept(withPurityAndActivity(ModItems.SYNAPTIC_NEURAL_BERRY.get(), purity, cap));
                            // S-01 / S-02 / S-03 血清
                            output.accept(withPurityAndActivity(ModItems.SYNAPTIC_SERUM_S01.get(), purity, cap));
                            output.accept(withPurityAndActivity(ModItems.SYNAPTIC_SERUM_S02.get(), purity, cap));
                            output.accept(withPurityAndActivity(ModItems.SYNAPTIC_SERUM_S03.get(), purity, cap));
                        }
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

    private static ItemStack withPurityAndActivity(Item item, int genePurity, int activity) {
        ItemStack stack = new ItemStack(item);
        stack.getOrCreateTag().putInt("Gene_Purity", genePurity);
        stack.getOrCreateTag().putInt("SynapticActivity", activity);
        return stack;
    }
}

package com.TKCCOPL.init;

import com.TKCCOPL.cybercultivator;
import com.TKCCOPL.curios.BioPulseBeltItem;
import com.TKCCOPL.curios.CurioAccessoryItem;
import com.TKCCOPL.curios.CuriosCompat;
import com.TKCCOPL.curios.LifeSupportPackItem;
import com.TKCCOPL.item.GeneticSeedItem;
import com.TKCCOPL.item.SynapticSerumItem;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, cybercultivator.MODID);

    public static final RegistryObject<Item> SILICON_SHARD = ITEMS.register("silicon_shard", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> RARE_EARTH_DUST = ITEMS.register("rare_earth_dust", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> PLANT_FIBER = ITEMS.register("plant_fiber", () -> new Item(new Item.Properties()) {
        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
            CompoundTag tag = stack.getTag();
            if (tag != null && tag.contains("Potency")) {
                tooltip.add(Component.translatable("tooltip.cybercultivator.quality_potency", tag.getInt("Potency"))
                        .withStyle(ChatFormatting.GREEN));
            }
        }
    });
    public static final RegistryObject<Item> BIOCHEMICAL_SOLUTION = ITEMS.register("biochemical_solution", () -> new Item(new Item.Properties()) {
        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
            CompoundTag tag = stack.getTag();
            if (tag != null && tag.contains("Concentration")) {
                tooltip.add(Component.translatable("tooltip.cybercultivator.quality_concentration", tag.getInt("Concentration"))
                        .withStyle(ChatFormatting.GREEN));
            }
        }
    });
    public static final RegistryObject<Item> INDUSTRIAL_ETHANOL = ITEMS.register("industrial_ethanol", () -> new Item(new Item.Properties()) {
        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
            CompoundTag tag = stack.getTag();
            if (tag != null && tag.contains("Purity")) {
                tooltip.add(Component.translatable("tooltip.cybercultivator.quality_purity", tag.getInt("Purity"))
                        .withStyle(ChatFormatting.GREEN));
            }
        }
    });
    public static final RegistryObject<Item> SYNAPTIC_NEURAL_BERRY = ITEMS.register("synaptic_neural_berry", () -> new Item(new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(2).saturationMod(0.3F).build())) {
        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
            CompoundTag tag = stack.getTag();
            if (tag != null && tag.contains("SynapticActivity")) {
                tooltip.add(Component.translatable("tooltip.cybercultivator.serum_activity", tag.getInt("SynapticActivity"))
                        .withStyle(ChatFormatting.GOLD));
            }
        }
    });
    public static final RegistryObject<Item> SYNAPTIC_SERUM_S01 = ITEMS.register("synaptic_serum_s01", () -> new SynapticSerumItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));

    public static final RegistryObject<Item> FIBER_REED_SEEDS = ITEMS.register("fiber_reed_seeds",
            () -> new GeneticSeedItem(ModBlocks.FIBER_REED_CROP.get(), new Item.Properties(), 4, 7, 3));
    public static final RegistryObject<Item> PROTEIN_SOY_SEEDS = ITEMS.register("protein_soy_seeds",
            () -> new GeneticSeedItem(ModBlocks.PROTEIN_SOY_CROP.get(), new Item.Properties(), 5, 4, 7));
    public static final RegistryObject<Item> ALCOHOL_BLOOM_SEEDS = ITEMS.register("alcohol_bloom_seeds",
            () -> new GeneticSeedItem(ModBlocks.ALCOHOL_BLOOM_CROP.get(), new Item.Properties(), 6, 3, 5));

    public static final RegistryObject<Item> PURIFIED_WATER_BOTTLE = ITEMS.register("purified_water_bottle", () -> new Item(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> SYNAPTIC_SERUM_S02 = ITEMS.register("synaptic_serum_s02", () -> new SynapticSerumItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), ModEffects.VISUAL_ENHANCEMENT, 20 * 30, 0));
    public static final RegistryObject<Item> SYNAPTIC_SERUM_S03 = ITEMS.register("synaptic_serum_s03", () -> new SynapticSerumItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE), ModEffects.METABOLIC_BOOST, 20 * 15, 0));

    public static final RegistryObject<Item> SPECTRUM_MONOCLE = ITEMS.register("spectrum_monocle",
            () -> new CurioAccessoryItem(new Item.Properties().rarity(Rarity.RARE), "head", "tooltip.cybercultivator.spectrum_monocle") {
                @Override
                public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
                    super.appendHoverText(stack, level, tooltip, flag);
                    if (CuriosCompat.isCuriosLoaded()) {
                        tooltip.add(Component.translatable("tooltip.cybercultivator.curios_loaded"));
                    } else {
                        tooltip.add(Component.translatable("tooltip.cybercultivator.curios_missing"));
                    }
                }
            });

    public static final RegistryObject<Item> SILICON_ORE_ITEM = registerBlockItem("silicon_ore", ModBlocks.SILICON_ORE);
    public static final RegistryObject<Item> RARE_EARTH_ORE_ITEM = registerBlockItem("rare_earth_ore", ModBlocks.RARE_EARTH_ORE);
    public static final RegistryObject<Item> BIO_INCUBATOR_ITEM = registerBlockItem("bio_incubator", ModBlocks.BIO_INCUBATOR);
    public static final RegistryObject<Item> GENE_SPLICER_ITEM = registerBlockItem("gene_splicer", ModBlocks.GENE_SPLICER);
    public static final RegistryObject<Item> ATMOSPHERIC_CONDENSER_ITEM = registerBlockItem("atmospheric_condenser", ModBlocks.ATMOSPHERIC_CONDENSER);
    public static final RegistryObject<Item> SERUM_BOTTLER_ITEM = registerBlockItem("serum_bottler", ModBlocks.SERUM_BOTTLER);

    public static final RegistryObject<Item> BIO_PULSE_BELT = ITEMS.register("bio_pulse_belt",
            () -> new BioPulseBeltItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> LIFE_SUPPORT_PACK = ITEMS.register("life_support_pack",
            () -> new LifeSupportPackItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    private ModItems() {
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

    private static RegistryObject<Item> registerBlockItem(String name, Supplier<net.minecraft.world.level.block.Block> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }
}

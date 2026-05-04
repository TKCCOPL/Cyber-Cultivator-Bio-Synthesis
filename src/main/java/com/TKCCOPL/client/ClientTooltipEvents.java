package com.TKCCOPL.client;

import com.TKCCOPL.cybercultivator;
import com.TKCCOPL.curios.CuriosCompat;
import com.TKCCOPL.init.ModItems;
import com.TKCCOPL.item.GeneticSeedItem;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = cybercultivator.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientTooltipEvents {
    private ClientTooltipEvents() {
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();

        // --- Seed gene display (monocle required) ---
        if (stack.getItem() instanceof GeneticSeedItem seedItem) {
            seedItem.ensureGeneData(stack);

            if (!CuriosCompat.isCuriosLoaded() || event.getEntity() == null || !CuriosCompat.hasSpectrumMonocle(event.getEntity())) {
                event.getToolTip().add(Component.translatable("tooltip.cybercultivator.seed_genes_hidden").withStyle(ChatFormatting.DARK_GRAY));
                return;
            }

            event.getToolTip().add(Component.translatable("tooltip.cybercultivator.gene_speed", GeneticSeedItem.getGene(stack, GeneticSeedItem.GENE_SPEED)).withStyle(ChatFormatting.AQUA));
            event.getToolTip().add(Component.translatable("tooltip.cybercultivator.gene_yield", GeneticSeedItem.getGene(stack, GeneticSeedItem.GENE_YIELD)).withStyle(ChatFormatting.GREEN));
            event.getToolTip().add(Component.translatable("tooltip.cybercultivator.gene_potency", GeneticSeedItem.getGene(stack, GeneticSeedItem.GENE_POTENCY)).withStyle(ChatFormatting.GOLD));

            // Generation
            int generation = GeneticSeedItem.getGeneration(stack);
            if (generation > 0) {
                event.getToolTip().add(Component.translatable(
                        "tooltip.cybercultivator.gene_generation", generation)
                        .withStyle(ChatFormatting.AQUA));
            }

            // Synergy
            int synergy = GeneticSeedItem.getSynergy(stack);
            if (synergy > 0) {
                event.getToolTip().add(Component.translatable(
                        "tooltip.cybercultivator.gene_synergy", synergy)
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
            }

            // Mutation marker (integer type code + detail)
            CompoundTag tag = stack.getTag();
            if (tag != null && tag.contains("Mutation")) {
                int mutationType = tag.getInt("Mutation");
                String detail = tag.contains("MutationDetail") ? tag.getString("MutationDetail") : "";
                switch (mutationType) {
                    case 1 -> event.getToolTip().add(Component.translatable(
                            "tooltip.cybercultivator.mutation_numerical", detail)
                            .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
                    case 2 -> event.getToolTip().add(Component.translatable(
                            "tooltip.cybercultivator.mutation_synergy", detail)
                            .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
                    default -> {}
                }
            }
            return;
        }

        // --- Raw materials / berry Gene_Synergy display (no monocle required) ---
        Item item = stack.getItem();
        if (item == ModItems.PLANT_FIBER.get()
                || item == ModItems.INDUSTRIAL_ETHANOL.get()
                || item == ModItems.BIOCHEMICAL_SOLUTION.get()
                || item == ModItems.SYNAPTIC_NEURAL_BERRY.get()) {
            CompoundTag tag = stack.getTag();
            if (tag != null && tag.contains("Gene_Synergy")) {
                int synergy = tag.getInt("Gene_Synergy");
                if (synergy > 0) {
                    event.getToolTip().add(Component.translatable(
                            "tooltip.cybercultivator.gene_synergy", synergy)
                            .withStyle(ChatFormatting.LIGHT_PURPLE));
                }
            }
        }
    }
}
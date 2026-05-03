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

            // Purity
            int purity = GeneticSeedItem.getPurity(stack);
            if (purity > 0) {
                event.getToolTip().add(Component.translatable(
                        "tooltip.cybercultivator.gene_purity", purity)
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
            }

            // Mutation marker
            CompoundTag tag = stack.getTag();
            if (tag != null && tag.getBoolean("Mutation")) {
                event.getToolTip().add(Component.translatable(
                        "tooltip.cybercultivator.mutation")
                        .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
            }
            return;
        }

        // --- Raw materials / berry Gene_Purity display (no monocle required) ---
        Item item = stack.getItem();
        if (item == ModItems.PLANT_FIBER.get()
                || item == ModItems.INDUSTRIAL_ETHANOL.get()
                || item == ModItems.BIOCHEMICAL_SOLUTION.get()
                || item == ModItems.SYNAPTIC_NEURAL_BERRY.get()) {
            CompoundTag tag = stack.getTag();
            if (tag != null && tag.contains("Gene_Purity")) {
                int purity = tag.getInt("Gene_Purity");
                if (purity > 0) {
                    event.getToolTip().add(Component.translatable(
                            "tooltip.cybercultivator.gene_purity", purity)
                            .withStyle(ChatFormatting.LIGHT_PURPLE));
                }
            }
        }
    }
}
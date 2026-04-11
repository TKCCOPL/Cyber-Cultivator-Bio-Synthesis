package com.TKCCOPL.client;

import com.TKCCOPL.cybercultivator;
import com.TKCCOPL.curios.CuriosCompat;
import com.TKCCOPL.item.GeneticSeedItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
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
        if (!(stack.getItem() instanceof GeneticSeedItem seedItem)) {
            return;
        }

        seedItem.ensureGeneData(stack);

        if (!CuriosCompat.isCuriosLoaded() || event.getEntity() == null || !CuriosCompat.hasSpectrumMonocle(event.getEntity())) {
            event.getToolTip().add(Component.translatable("tooltip.cybercultivator.seed_genes_hidden").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }

        event.getToolTip().add(Component.translatable("tooltip.cybercultivator.gene_speed", GeneticSeedItem.getGene(stack, GeneticSeedItem.GENE_SPEED)).withStyle(ChatFormatting.AQUA));
        event.getToolTip().add(Component.translatable("tooltip.cybercultivator.gene_yield", GeneticSeedItem.getGene(stack, GeneticSeedItem.GENE_YIELD)).withStyle(ChatFormatting.GREEN));
        event.getToolTip().add(Component.translatable("tooltip.cybercultivator.gene_potency", GeneticSeedItem.getGene(stack, GeneticSeedItem.GENE_POTENCY)).withStyle(ChatFormatting.GOLD));
    }
}
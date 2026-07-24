package com.TKCCOPL.client.screen;

import com.TKCCOPL.cybercultivator;
import com.TKCCOPL.menu.SerumBottlerMenu;
import com.TKCCOPL.recipe.SerumRecipe;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class SerumBottlerScreen extends MachineScreen<SerumBottlerMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(cybercultivator.MODID, "textures/gui/serum_bottler.png");

    public SerumBottlerScreen(SerumBottlerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, TEXTURE);
    }

    @Override
    protected void renderMachineState(GuiGraphics graphics, float partialTick) {
        horizontalBar(graphics, 106, 56, 32, menu.getProgress(), menu.getMaxProgress(), 0xFF5DB9C7);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0x373737, false);
        SerumRecipe recipe = menu.getDisplayRecipe();
        if (recipe != null) {
            drawFitted(graphics, Component.translatable("gui.cybercultivator.bottler.output_recipe",
                    recipe.getBaseOutput().getHoverName()), 16, 82, 162, 0x373737);
        }
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }
}

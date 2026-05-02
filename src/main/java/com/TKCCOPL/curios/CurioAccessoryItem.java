package com.TKCCOPL.curios;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

import javax.annotation.Nullable;
import java.util.List;

public class CurioAccessoryItem extends Item {
    private final String slotType;
    private final String tooltipKey;

    public CurioAccessoryItem(Properties properties, String slotType, String tooltipKey) {
        super(properties.stacksTo(1));
        this.slotType = slotType;
        this.tooltipKey = tooltipKey;
    }

    public String getSlotType() {
        return slotType;
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        if (CuriosCompat.isCuriosLoaded()) {
            return CuriosCompat.createEquipableProvider(stack);
        }
        return super.initCapabilities(stack, nbt);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (tooltipKey != null) {
            tooltip.add(Component.translatable(tooltipKey));
        }
    }
}

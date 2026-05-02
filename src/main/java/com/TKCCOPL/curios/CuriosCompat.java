package com.TKCCOPL.curios;

import com.TKCCOPL.init.ModItems;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.ModList;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class CuriosCompat {
    private static final String CURIOS_MOD_ID = "curios";

    private CuriosCompat() {
    }

    public static boolean isCuriosLoaded() {
        return ModList.get().isLoaded(CURIOS_MOD_ID);
    }

    public static boolean hasSpectrumMonocle(LivingEntity entity) {
        return hasCurioItem(entity, ModItems.SPECTRUM_MONOCLE.get());
    }

    public static boolean hasBioPulseBelt(LivingEntity entity) {
        return hasCurioItem(entity, ModItems.BIO_PULSE_BELT.get());
    }

    public static boolean hasLifeSupportPack(LivingEntity entity) {
        return hasCurioItem(entity, ModItems.LIFE_SUPPORT_PACK.get());
    }

    public static boolean hasCurioItem(LivingEntity entity, net.minecraft.world.item.Item item) {
        if (!isCuriosLoaded()) return false;
        try {
            var inventory = CuriosApi.getCuriosInventory(entity).resolve();
            if (inventory.isEmpty()) return false;
            return inventory.get().findFirstCurio(item).isPresent();
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean isMonocle(ItemStack stack) {
        return stack.is(ModItems.SPECTRUM_MONOCLE.get());
    }

    public static ICapabilityProvider createEquipableProvider(ItemStack stack) {
        return new ICapabilityProvider() {
            private final LazyOptional<ICurio> curio = LazyOptional.of(() -> new ICurio() {
                @Override
                public ItemStack getStack() {
                    return stack;
                }

                @Override
                public boolean canEquipFromUse(SlotContext slotContext) {
                    return true;
                }
            });

            @Nonnull
            @Override
            public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
                return CuriosCapability.ITEM.orEmpty(cap, curio);
            }
        };
    }
}

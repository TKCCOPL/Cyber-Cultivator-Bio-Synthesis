package com.TKCCOPL.curios;

import com.TKCCOPL.cybercultivator;
import com.TKCCOPL.init.ModItems;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import top.theillusivec4.curios.api.CuriosApi;

public final class CuriosCompat {
    private static final String CURIOS_MOD_ID = "curios";

    private CuriosCompat() {
    }

    public static boolean isCuriosLoaded() {
        return ModList.get().isLoaded(CURIOS_MOD_ID);
    }

    public static boolean hasSpectrumMonocle(LivingEntity entity) {
        if (!isCuriosLoaded()) {
            return false;
        }

        try {
            var inventory = CuriosApi.getCuriosInventory(entity).resolve();
            if (inventory.isEmpty()) {
                return false;
            }

            return inventory.get().findFirstCurio(ModItems.SPECTRUM_MONOCLE.get()).isPresent();
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean isMonocle(ItemStack stack) {
        return stack.is(ModItems.SPECTRUM_MONOCLE.get());
    }
}

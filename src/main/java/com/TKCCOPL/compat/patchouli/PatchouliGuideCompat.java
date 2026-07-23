package com.TKCCOPL.compat.patchouli;

import com.TKCCOPL.cybercultivator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = cybercultivator.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PatchouliGuideCompat {
    private static final String PATCHOULI_MOD_ID = "patchouli";
    private static final ResourceLocation GUIDE_ITEM_ID =
            ResourceLocation.fromNamespaceAndPath(PATCHOULI_MOD_ID, "guide_book");
    private static final String GUIDE_BOOK_ID = cybercultivator.MODID + ":bio_synthesis_guide";
    private static final String PATCHOULI_BOOK_TAG = "patchouli:book";
    private static final String GRANTED_TAG = "CyberCultivatorGuideGranted";

    private PatchouliGuideCompat() {
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !ModList.get().isLoaded(PATCHOULI_MOD_ID)) {
            return;
        }
        tryGrantGuide(player);
    }

    /** Performs the registry-safe grant without touching Patchouli classes or the network. */
    public static boolean tryGrantGuide(Player player) {
        if (player == null || !ModList.get().isLoaded(PATCHOULI_MOD_ID)) return false;
        CompoundTag persistent = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (persistent.getBoolean(GRANTED_TAG)) return false;
        if (hasGuide(player)) {
            markGranted(player, persistent);
            return false;
        }

        Item guideItem = ForgeRegistries.ITEMS.getValue(GUIDE_ITEM_ID);
        if (guideItem == null || guideItem == Items.AIR) return false;

        ItemStack guide = new ItemStack(guideItem);
        guide.getOrCreateTag().putString(PATCHOULI_BOOK_TAG, GUIDE_BOOK_ID);
        boolean delivered = player.addItem(guide);
        if (!delivered && !guide.isEmpty()) {
            delivered = player.drop(guide, false) != null;
        }
        if (delivered) markGranted(player, persistent);
        return delivered;
    }

    private static boolean hasGuide(Player player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.isEmpty() || !GUIDE_ITEM_ID.equals(ForgeRegistries.ITEMS.getKey(stack.getItem()))) continue;
            CompoundTag tag = stack.getTag();
            if (tag != null && GUIDE_BOOK_ID.equals(tag.getString(PATCHOULI_BOOK_TAG))) return true;
        }
        return false;
    }

    private static void markGranted(Player player, CompoundTag persistent) {
        persistent.putBoolean(GRANTED_TAG, true);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persistent);
    }
}

package com.TKCCOPL.loot;

import com.TKCCOPL.cybercultivator;
import com.mojang.serialization.Codec;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModLootModifiers {
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, cybercultivator.MODID);

    public static final RegistryObject<Codec<CuriosLootModifier>> CURIOS_LOOT =
            LOOT_MODIFIERS.register("curios_loot", CuriosLootModifier.CODEC);

    private ModLootModifiers() {}

    public static void register(IEventBus eventBus) {
        LOOT_MODIFIERS.register(eventBus);
    }
}

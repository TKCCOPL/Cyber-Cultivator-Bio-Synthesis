package com.TKCCOPL.recipe;

import com.TKCCOPL.cybercultivator;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 注册血清灌装机的 RecipeType 和 Serializer。
 */
public final class ModRecipeTypes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, cybercultivator.MODID);

    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, cybercultivator.MODID);

    public static final RegistryObject<RecipeType<SerumRecipe>> SERUM_BOTTLING =
            RECIPE_TYPES.register("serum_bottling",
                    () -> new RecipeType<>() {
                        @Override
                        public String toString() {
                            return "cybercultivator:serum_bottling";
                        }
                    });

    public static final RegistryObject<RecipeSerializer<SerumRecipe>> SERUM_BOTTLING_SERIALIZER =
            SERIALIZERS.register("serum_bottling", SerumRecipeSerializer::new);

    public static final RegistryObject<RecipeType<IncubatorOutputRecipe>> INCUBATOR_OUTPUT =
            RECIPE_TYPES.register("incubator_output",
                    () -> new RecipeType<>() {
                        @Override
                        public String toString() {
                            return "cybercultivator:incubator_output";
                        }
                    });

    public static final RegistryObject<RecipeSerializer<IncubatorOutputRecipe>> INCUBATOR_OUTPUT_SERIALIZER =
            SERIALIZERS.register("incubator_output", IncubatorOutputSerializer::new);

    private ModRecipeTypes() {
    }

    public static void register(IEventBus eventBus) {
        RECIPE_TYPES.register(eventBus);
        SERIALIZERS.register(eventBus);
    }
}

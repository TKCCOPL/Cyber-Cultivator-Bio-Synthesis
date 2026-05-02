package com.TKCCOPL.init;

import com.TKCCOPL.cybercultivator;
import com.TKCCOPL.effect.MetabolicBoostEffect;
import com.TKCCOPL.effect.NeuralOverloadEffect;
import com.TKCCOPL.effect.SynapticOverclockEffect;
import com.TKCCOPL.effect.VisualEnhancementEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, cybercultivator.MODID);

    public static final RegistryObject<MobEffect> SYNAPTIC_OVERCLOCK = MOB_EFFECTS.register("synaptic_overclock", SynapticOverclockEffect::new);
    public static final RegistryObject<MobEffect> NEURAL_OVERLOAD = MOB_EFFECTS.register("neural_overload", NeuralOverloadEffect::new);
    public static final RegistryObject<MobEffect> VISUAL_ENHANCEMENT = MOB_EFFECTS.register("visual_enhancement", VisualEnhancementEffect::new);
    public static final RegistryObject<MobEffect> METABOLIC_BOOST = MOB_EFFECTS.register("metabolic_boost", MetabolicBoostEffect::new);

    private ModEffects() {
    }

    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }
}

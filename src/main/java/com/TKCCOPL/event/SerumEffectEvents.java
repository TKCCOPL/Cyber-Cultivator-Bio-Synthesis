package com.TKCCOPL.event;

import com.TKCCOPL.cybercultivator;
import com.TKCCOPL.init.ModEffects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Applies source-specific neural overload after a serum effect expires or is actively removed. */
@Mod.EventBusSubscriber(modid = cybercultivator.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SerumEffectEvents {
    private SerumEffectEvents() {
    }

    @SubscribeEvent
    public static void onExpired(MobEffectEvent.Expired event) {
        scheduleOverload(event.getEntity(), event.getEffectInstance());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRemoved(MobEffectEvent.Remove event) {
        scheduleOverload(event.getEntity(), event.getEffectInstance());
    }

    private static void scheduleOverload(LivingEntity entity, MobEffectInstance serumEffect) {
        if (serumEffect == null || entity.level().isClientSide) return;
        MobEffect overload = overloadFor(serumEffect.getEffect());
        if (overload == null) return;

        MinecraftServer server = entity.level().getServer();
        if (server == null) return;
        int amplifier = serumEffect.getAmplifier();
        int duration = 20 * (8 + amplifier * 2);
        server.tell(new TickTask(server.getTickCount() + 1, () -> {
            if (entity.isRemoved() || !entity.isAlive()) return;
            entity.addEffect(new MobEffectInstance(overload, duration, amplifier));
        }));
    }

    private static MobEffect overloadFor(MobEffect serumEffect) {
        if (serumEffect == ModEffects.SYNAPTIC_OVERCLOCK.get()) return ModEffects.NEURAL_OVERLOAD_S01.get();
        if (serumEffect == ModEffects.VISUAL_ENHANCEMENT.get()) return ModEffects.NEURAL_OVERLOAD_S02.get();
        if (serumEffect == ModEffects.METABOLIC_BOOST.get()) return ModEffects.NEURAL_OVERLOAD_S03.get();
        return null;
    }
}

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
        // MobEffectEvent.Remove 是 @Cancelable 事件：其他模组取消移除时不应触发神经过载。
        // LOWEST 优先级仍会被已取消事件触发，因此必须显式检查。
        if (event.isCanceled()) return;
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
        MobEffect serumEffectType = serumEffect.getEffect();
        server.tell(new TickTask(server.getTickCount() + 1, () -> {
            if (entity.isRemoved() || !entity.isAlive()) return;
            // 延迟 1 tick 后再次确认原血清效果确实已消失：
            // 血清升级/重新施加/被其他模组恢复时效果仍在，则跳过神经过载，
            // 与 VisualEnhancementEffect 等效果自身的 removeAttributeModifiers 守卫一致。
            if (entity.getEffect(serumEffectType) != null) return;
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

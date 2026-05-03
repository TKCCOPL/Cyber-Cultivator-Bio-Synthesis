package com.TKCCOPL.item;

import com.TKCCOPL.init.ModEffects;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.function.Supplier;

public class SynapticSerumItem extends Item {
    private static final String TAG_ACTIVITY = "SynapticActivity";
    private static final int MAX_AMPLIFIER = 4;

    private final Supplier<MobEffect> effect;
    private final int durationTicks;
    private final int amplifier;

    public SynapticSerumItem(Properties properties) {
        this(properties, ModEffects.SYNAPTIC_OVERCLOCK, 20 * 25, 0);
    }

    public SynapticSerumItem(Properties properties, Supplier<MobEffect> effect, int durationTicks, int amplifier) {
        super(properties);
        this.effect = effect;
        this.durationTicks = durationTicks;
        this.amplifier = amplifier;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 24;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    public static int getActivity(ItemStack stack) {
        net.minecraft.nbt.CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_ACTIVITY)) return 5;
        return Math.max(1, Math.min(10, tag.getInt(TAG_ACTIVITY)));
    }

    public static int getScaledDuration(int baseDuration, int activity) {
        double multiplier = 0.5 + activity * 0.1;
        return (int) Math.round(baseDuration * multiplier);
    }

    public static int getBaseAmplifier(int activity) {
        return activity >= 8 ? 1 : 0;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide) {
            int activity = getActivity(stack);
            int scaledDuration = getScaledDuration(durationTicks, activity);

            int amp;
            MobEffectInstance existing = entity.getEffect(effect.get());
            if (existing != null) {
                amp = Math.min(existing.getAmplifier() + 1, MAX_AMPLIFIER);
            } else {
                amp = getBaseAmplifier(activity);
            }

            entity.addEffect(new MobEffectInstance(effect.get(), scaledDuration, amp));
        }

        if (entity instanceof Player player && !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        int activity = getActivity(stack);
        tooltip.add(net.minecraft.network.chat.Component.translatable(
                "tooltip.cybercultivator.serum_activity", activity).withStyle(net.minecraft.ChatFormatting.GOLD));

        double multiplier = 0.5 + activity * 0.1;
        int baseAmp = getBaseAmplifier(activity);
        String baseLevel = baseAmp >= 1 ? "II" : "I";
        tooltip.add(net.minecraft.network.chat.Component.translatable(
                "tooltip.cybercultivator.serum_base_level", baseLevel).withStyle(net.minecraft.ChatFormatting.GRAY));

        tooltip.add(net.minecraft.network.chat.Component.translatable(
                "tooltip.cybercultivator.serum_duration_mult", String.format("%.1f", multiplier)).withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}

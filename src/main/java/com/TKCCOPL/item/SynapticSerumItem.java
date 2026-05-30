package com.TKCCOPL.item;

import com.TKCCOPL.Config;
import com.TKCCOPL.init.ModEffects;
import com.TKCCOPL.event.SerumConsumeEvent;
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
    private static final int MAX_AMPLIFIER = 7;

    private final Supplier<MobEffect> effect;
    private final Supplier<Integer> durationSupplier;
    private final int amplifier;

    public SynapticSerumItem(Properties properties) {
        this(properties, ModEffects.SYNAPTIC_OVERCLOCK, () -> Config.s01BaseDuration, 0);
    }

    public SynapticSerumItem(Properties properties, Supplier<MobEffect> effect, Supplier<Integer> durationSupplier, int amplifier) {
        super(properties);
        this.effect = effect;
        this.durationSupplier = durationSupplier;
        this.amplifier = amplifier;
    }

    // 保留旧构造函数（向后兼容，固定值转为 Supplier）
    public SynapticSerumItem(Properties properties, Supplier<MobEffect> effect, int durationTicks, int amplifier) {
        this(properties, effect, () -> durationTicks, amplifier);
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
        return Math.max(1, tag.getInt(TAG_ACTIVITY));
    }

    public static int getScaledDuration(int baseDuration, int activity) {
        double multiplier = Config.durationMultiplierBase + activity * Config.durationMultiplierPerActivity;
        return (int) Math.round(baseDuration * multiplier);
    }

    public static int getBaseAmplifier(int activity) {
        return activity >= Config.activityThresholdForBonus ? 1 : 0;
    }

    /**
     * Activity > 10 时，超出部分每 2 点额外 +1 amplifier（上限 MAX_AMPLIFIER）。
     * 首次饮用时调用，叠加饮用走 existing 分支不受影响。
     */
    public static int getActivityBonusAmplifier(int activity) {
        if (activity <= 10) return 0;
        return (activity - 10) / 2;
    }

    private static final String[] ROMAN = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

    private static String toRoman(int level) {
        if (level >= 1 && level <= ROMAN.length) return ROMAN[level - 1];
        return String.valueOf(level);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide) {
            int activity = getActivity(stack);
            int scaledDuration = getScaledDuration(durationSupplier.get(), activity);

            int amp;
            MobEffectInstance existing = entity.getEffect(effect.get());
            if (existing != null) {
                amp = Math.min(existing.getAmplifier() + 1, Config.stackAmplifierCap);
                // 累加剩余持续时间，上限 Config.stackDurationCap
                scaledDuration = Math.min(scaledDuration + existing.getDuration(), Config.stackDurationCap);
            } else {
                amp = Math.min(getBaseAmplifier(activity) + getActivityBonusAmplifier(activity), Config.stackAmplifierCap);
            }

            // 触发 SerumConsumeEvent，允许其他 mod 修改效果参数
            SerumConsumeEvent consumeEvent = new SerumConsumeEvent(
                    entity, stack, effect.get(), activity, scaledDuration, amp
            );
            if (net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(consumeEvent)) {
                // 事件被取消，不施加效果，不消耗物品
                return stack;
            }
            scaledDuration = consumeEvent.getDuration();
            amp = consumeEvent.getAmplifier();

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

        double multiplier = Config.durationMultiplierBase + activity * Config.durationMultiplierPerActivity;
        int totalAmp = Math.min(getBaseAmplifier(activity) + getActivityBonusAmplifier(activity), Config.stackAmplifierCap);
        String baseLevel = toRoman(totalAmp + 1);
        tooltip.add(net.minecraft.network.chat.Component.translatable(
                "tooltip.cybercultivator.serum_base_level", baseLevel).withStyle(net.minecraft.ChatFormatting.GRAY));

        tooltip.add(net.minecraft.network.chat.Component.translatable(
                "tooltip.cybercultivator.serum_duration_mult", String.format("%.1f", multiplier)).withStyle(net.minecraft.ChatFormatting.GRAY));

        if (activity > 10) {
            tooltip.add(net.minecraft.network.chat.Component.translatable(
                    "tooltip.cybercultivator.serum_activity_bonus", activity).withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
        }
    }
}

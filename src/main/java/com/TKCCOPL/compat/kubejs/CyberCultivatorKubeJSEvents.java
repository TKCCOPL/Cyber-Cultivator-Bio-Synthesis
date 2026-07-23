package com.TKCCOPL.compat.kubejs;

import com.TKCCOPL.event.CropMatureEvent;
import com.TKCCOPL.event.GeneSpliceEvent;
import com.TKCCOPL.event.SerumConsumeEvent;
import com.TKCCOPL.event.SerumCraftEvent;
import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.event.EventJS;
import dev.latvian.mods.kubejs.event.EventResult;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.function.Supplier;

/** Reloadable server-script wrappers around the public Forge events. */
public final class CyberCultivatorKubeJSEvents {
    public static final EventGroup GROUP = EventGroup.of("CyberCultivatorEvents");
    public static final EventHandler GENE_SPLICE = GROUP.server("geneSplice", () -> GeneSpliceEventJS.class).hasResult();
    public static final EventHandler CROP_MATURE = GROUP.server("cropMature", () -> CropMatureEventJS.class).hasResult();
    public static final EventHandler SERUM_CRAFT = GROUP.server("serumCraft", () -> SerumCraftEventJS.class).hasResult();
    public static final EventHandler SERUM_CONSUME = GROUP.server("serumConsume", () -> SerumConsumeEventJS.class).hasResult();

    private CyberCultivatorKubeJSEvents() {
    }

    public static void geneSplice(GeneSpliceEvent event) {
        postCancelable(GENE_SPLICE, () -> new GeneSpliceEventJS(event), event);
    }

    public static void cropMature(CropMatureEvent event) {
        postCancelable(CROP_MATURE, () -> new CropMatureEventJS(event), event);
    }

    public static void serumCraft(SerumCraftEvent event) {
        postCancelable(SERUM_CRAFT, () -> new SerumCraftEventJS(event), event);
    }

    public static void serumConsume(SerumConsumeEvent event) {
        postCancelable(SERUM_CONSUME, () -> new SerumConsumeEventJS(event), event);
    }

    private static void postCancelable(EventHandler handler, Supplier<? extends EventJS> wrapper,
                                       net.minecraftforge.eventbus.api.Event forgeEvent) {
        if (!handler.hasListeners()) return;
        EventResult result = handler.post(wrapper.get());
        if (result.interruptFalse()) {
            forgeEvent.setCanceled(true);
        }
    }

    public static final class GeneSpliceEventJS extends EventJS {
        private final GeneSpliceEvent event;

        private GeneSpliceEventJS(GeneSpliceEvent event) { this.event = event; }
        public ItemStack getSeedA() { return event.getSeedA(); }
        public ItemStack getSeedB() { return event.getSeedB(); }
        public int getSpeed() { return event.getSpeed(); }
        public void setSpeed(int value) { event.setSpeed(value); }
        public int getYield() { return event.getYield(); }
        public void setYield(int value) { event.setYield(value); }
        public int getPotency() { return event.getPotency(); }
        public void setPotency(int value) { event.setPotency(value); }
        public int getSynergy() { return event.getSynergy(); }
        public void setSynergy(int value) { event.setSynergy(value); }
        public int getGeneration() { return event.getGeneration(); }
        public void setGeneration(int value) { event.setGeneration(value); }
        public boolean isMutation() { return event.isMutation(); }
        public void setMutation(boolean value) { event.setMutation(value); }
        public int getMutationType() { return event.getMutationType(); }
        public void setMutationType(int value) { event.setMutationType(value); }
        public String getMutationDetail() { return event.getMutationDetail(); }
        public void setMutationDetail(String value) { event.setMutationDetail(value); }
        public int getOffspringCount() { return event.getOffspringCount(); }
        public void setOffspringCount(int value) { event.setOffspringCount(value); }
    }

    public static final class CropMatureEventJS extends EventJS {
        private final CropMatureEvent event;

        private CropMatureEventJS(CropMatureEvent event) { this.event = event; }
        public Level getLevel() { return event.getLevel(); }
        public BlockPos getPos() { return event.getPos(); }
        public ItemStack getSeed() { return event.getSeed(); }
        public ItemStack getOutput() { return event.getOutput(); }
        public void setOutput(ItemStack value) { event.setOutput(value); }
    }

    public static final class SerumCraftEventJS extends EventJS {
        private final SerumCraftEvent event;

        private SerumCraftEventJS(SerumCraftEvent event) { this.event = event; }
        public ItemStack[] getInputs() { return event.getInputs(); }
        public ItemStack getOutput() { return event.getOutput(); }
        public void setOutput(ItemStack value) { event.setOutput(value); }
        public int getActivity() { return event.getActivity(); }
        public void setActivity(int value) { event.setActivity(value); }
        public ResourceLocation getRecipeId() { return event.getRecipeId(); }
    }

    public static final class SerumConsumeEventJS extends EventJS {
        private final SerumConsumeEvent event;

        private SerumConsumeEventJS(SerumConsumeEvent event) { this.event = event; }
        public LivingEntity getEntity() { return event.getEntity(); }
        public ItemStack getSerum() { return event.getSerum(); }
        public MobEffect getEffect() { return event.getEffect(); }
        public int getActivity() { return event.getActivity(); }
        public void setActivity(int value) { event.setActivity(value); }
        public int getDuration() { return event.getDuration(); }
        public void setDuration(int value) { event.setDuration(value); }
        public int getAmplifier() { return event.getAmplifier(); }
        public void setAmplifier(int value) { event.setAmplifier(value); }
    }
}

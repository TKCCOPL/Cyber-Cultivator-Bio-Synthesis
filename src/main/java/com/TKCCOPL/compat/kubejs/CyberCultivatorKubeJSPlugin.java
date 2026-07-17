package com.TKCCOPL.compat.kubejs;

import com.TKCCOPL.cybercultivator;
import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.recipe.schema.RegisterRecipeSchemasEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;

/** Optional KubeJS entrypoint, loaded only when KubeJS scans kubejs.plugins.txt. */
public final class CyberCultivatorKubeJSPlugin extends KubeJSPlugin {
    @Override
    public void init() {
        MinecraftForge.EVENT_BUS.addListener(CyberCultivatorKubeJSEvents::geneSplice);
        MinecraftForge.EVENT_BUS.addListener(CyberCultivatorKubeJSEvents::cropMature);
        MinecraftForge.EVENT_BUS.addListener(CyberCultivatorKubeJSEvents::serumCraft);
        MinecraftForge.EVENT_BUS.addListener(CyberCultivatorKubeJSEvents::serumConsume);
    }

    @Override
    public void registerEvents() {
        CyberCultivatorKubeJSEvents.GROUP.register();
    }

    @Override
    public void registerRecipeSchemas(RegisterRecipeSchemasEvent event) {
        event.register(ResourceLocation.fromNamespaceAndPath(cybercultivator.MODID, "serum_bottling"),
                CyberCultivatorRecipeSchemas.SERUM_BOTTLING);
        event.register(ResourceLocation.fromNamespaceAndPath(cybercultivator.MODID, "incubator_output"),
                CyberCultivatorRecipeSchemas.INCUBATOR_OUTPUT);
    }
}

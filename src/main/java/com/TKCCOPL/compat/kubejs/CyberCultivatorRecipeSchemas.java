package com.TKCCOPL.compat.kubejs;

import dev.latvian.mods.kubejs.item.InputItem;
import dev.latvian.mods.kubejs.item.OutputItem;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.BooleanComponent;
import dev.latvian.mods.kubejs.recipe.component.ItemComponents;
import dev.latvian.mods.kubejs.recipe.component.NumberComponent;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponent;
import dev.latvian.mods.kubejs.recipe.component.RecipeComponentBuilderMap;
import dev.latvian.mods.kubejs.recipe.component.StringComponent;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;

/** KubeJS 6.x schemas for Cyber-Cultivator's native datapack recipes. */
public final class CyberCultivatorRecipeSchemas {
    private static final RecipeKey<OutputItem> RESULT = ItemComponents.OUTPUT.key("result");
    private static final RecipeKey<OutputItem> OUTPUT = ItemComponents.OUTPUT.key("output");
    private static final RecipeKey<InputItem[]> INGREDIENTS = ItemComponents.INPUT_ARRAY.key("ingredients");
    private static final RecipeKey<InputItem> SEED = ItemComponents.INPUT.key("seed");

    private static final RecipeKey<Integer> PROCESSING_TIME = NumberComponent
            .intRange(1, Integer.MAX_VALUE)
            .key("processing_time")
            .preferred("processingTime")
            .optional(300);
    private static final RecipeKey<Boolean> INHERIT_ACTIVITY = BooleanComponent.BOOLEAN
            .key("inherit_activity")
            .preferred("inheritActivity")
            .optional(false);
    private static final RecipeKey<Boolean> INHERIT_MUTATION = BooleanComponent.BOOLEAN
            .key("inherit_mutation")
            .preferred("inheritMutation")
            .optional(false);
    private static final RecipeKey<Integer> PRIORITY = NumberComponent.ANY_INT
            .key("priority")
            .optional(0);

    private static final RecipeKey<String> COUNT_FORMULA = StringComponent.NON_BLANK
            .key("count_formula")
            .preferred("countFormula")
            .optional("2 + yield / 3");
    private static final RecipeKey<String> QUALITY_TAG = StringComponent.ANY
            .key("quality_tag")
            .preferred("qualityTag")
            .optional("");
    private static final RecipeKey<String> CROP_NAME = StringComponent.ANY
            .key("crop_name")
            .preferred("cropName")
            .optional("");

    private static final RecipeKey<Integer> GENE_SPEED = NumberComponent.INT.key("speed").optional(5);
    private static final RecipeKey<Integer> GENE_YIELD = NumberComponent.INT.key("yield").optional(5);
    private static final RecipeKey<Integer> GENE_POTENCY = NumberComponent.INT.key("potency").optional(5);
    private static final RecipeKey<RecipeComponentBuilderMap> DEFAULT_GENES = RecipeComponent
            .builder(GENE_SPEED, GENE_YIELD, GENE_POTENCY)
            .key("default_genes")
            .preferred("defaultGenes")
            .defaultOptional();

    public static final RecipeSchema SERUM_BOTTLING = new RecipeSchema(
            RESULT, INGREDIENTS, PROCESSING_TIME, INHERIT_ACTIVITY, INHERIT_MUTATION, PRIORITY);

    public static final RecipeSchema INCUBATOR_OUTPUT = new RecipeSchema(
            OUTPUT, SEED, COUNT_FORMULA, QUALITY_TAG, DEFAULT_GENES, CROP_NAME, PRIORITY);

    private CyberCultivatorRecipeSchemas() {
    }
}

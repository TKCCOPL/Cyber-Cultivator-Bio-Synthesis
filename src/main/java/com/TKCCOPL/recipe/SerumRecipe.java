package com.TKCCOPL.recipe;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/**
 * 血清灌装机配方类（JSON 数据驱动）。
 * 通过 SerumRecipeSerializer 从 JSON 反序列化。
 */
public class SerumRecipe implements Recipe<Container> {
    private final ResourceLocation id;
    private final Ingredient[] inputs;
    private final ItemStack baseOutput;
    private final int processingTime;
    private final boolean inheritActivity;
    private final boolean inheritMutation;

    public SerumRecipe(ResourceLocation id, Ingredient[] inputs, ItemStack baseOutput,
                       int processingTime, boolean inheritActivity, boolean inheritMutation) {
        this.id = id;
        this.inputs = inputs;
        this.baseOutput = baseOutput;
        this.processingTime = processingTime;
        this.inheritActivity = inheritActivity;
        this.inheritMutation = inheritMutation;
    }

    @Override
    public boolean matches(Container container, Level level) {
        boolean[] matched = new boolean[inputs.length];
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack slotStack = container.getItem(i);
            if (slotStack.isEmpty()) continue;
            boolean slotMatched = false;
            for (int j = 0; j < inputs.length; j++) {
                if (!matched[j] && inputs[j].test(slotStack)) {
                    matched[j] = true;
                    slotMatched = true;
                    break;
                }
            }
            // 非空槽位无法匹配任何配方输入 → 拒绝
            if (!slotMatched) return false;
        }
        for (boolean m : matched) {
            if (!m) return false;
        }
        return true;
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess registryAccess) {
        return baseOutput.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return false; // Not a grid recipe
    }

    @Override
    public ItemStack getResultItem(@javax.annotation.Nullable RegistryAccess registryAccess) {
        return baseOutput.copy();
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.SERUM_BOTTLING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.SERUM_BOTTLING.get();
    }

    // Accessors
    public Ingredient[] getInputs() { return inputs.clone(); }
    public ItemStack getBaseOutput() { return baseOutput.copy(); }
    public int getProcessingTime() { return processingTime; }
    public boolean isInheritActivity() { return inheritActivity; }
    public boolean isInheritMutation() { return inheritMutation; }
}

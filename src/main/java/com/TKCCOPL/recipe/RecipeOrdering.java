package com.TKCCOPL.recipe;

import net.minecraft.world.item.crafting.Recipe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Shared deterministic ordering for machine recipes. */
public final class RecipeOrdering {
    private RecipeOrdering() {
    }

    public static <T extends Recipe<?> & PrioritizedRecipe> List<T> sorted(Collection<T> recipes) {
        List<T> sorted = new ArrayList<>(recipes);
        sorted.sort((left, right) -> {
            int priorityComparison = Integer.compare(right.getPriority(), left.getPriority());
            if (priorityComparison != 0) return priorityComparison;
            return left.getId().compareTo(right.getId());
        });
        return List.copyOf(sorted);
    }
}

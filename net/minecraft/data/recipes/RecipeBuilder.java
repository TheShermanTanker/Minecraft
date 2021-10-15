package net.minecraft.data.recipes;

import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriterionInstance;
import net.minecraft.core.IRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.IMaterial;

public interface RecipeBuilder {
    RecipeBuilder unlockedBy(String name, CriterionInstance conditions);

    RecipeBuilder group(@Nullable String group);

    Item getResult();

    void save(Consumer<FinishedRecipe> exporter, MinecraftKey recipeId);

    default void save(Consumer<FinishedRecipe> exporter) {
        this.save(exporter, getDefaultRecipeId(this.getResult()));
    }

    default void save(Consumer<FinishedRecipe> exporter, String recipePath) {
        MinecraftKey resourceLocation = getDefaultRecipeId(this.getResult());
        MinecraftKey resourceLocation2 = new MinecraftKey(recipePath);
        if (resourceLocation2.equals(resourceLocation)) {
            throw new IllegalStateException("Recipe " + recipePath + " should remove its 'save' argument as it is equal to default one");
        } else {
            this.save(exporter, resourceLocation2);
        }
    }

    static MinecraftKey getDefaultRecipeId(IMaterial item) {
        return IRegistry.ITEM.getKey(item.getItem());
    }
}

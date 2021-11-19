package net.minecraft.data.recipes;

import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriterionInstance;
import net.minecraft.core.IRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.IMaterial;

public interface IRecipeBuilder {
    IRecipeBuilder unlockedBy(String name, CriterionInstance conditions);

    IRecipeBuilder group(@Nullable String group);

    Item getResult();

    void save(Consumer<IFinishedRecipe> exporter, MinecraftKey recipeId);

    default void save(Consumer<IFinishedRecipe> exporter) {
        this.save(exporter, getDefaultRecipeId(this.getResult()));
    }

    default void save(Consumer<IFinishedRecipe> exporter, String recipePath) {
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

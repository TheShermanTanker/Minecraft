package net.minecraft.world.item.crafting;

import java.util.Optional;
import net.minecraft.core.IRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.IInventory;
import net.minecraft.world.level.World;

public interface Recipes<T extends IRecipe<?>> {
    Recipes<RecipeCrafting> CRAFTING = register("crafting");
    Recipes<FurnaceRecipe> SMELTING = register("smelting");
    Recipes<RecipeBlasting> BLASTING = register("blasting");
    Recipes<RecipeSmoking> SMOKING = register("smoking");
    Recipes<RecipeCampfire> CAMPFIRE_COOKING = register("campfire_cooking");
    Recipes<RecipeStonecutting> STONECUTTING = register("stonecutting");
    Recipes<RecipeSmithing> SMITHING = register("smithing");

    static <T extends IRecipe<?>> Recipes<T> register(String id) {
        return IRegistry.register(IRegistry.RECIPE_TYPE, new MinecraftKey(id), new Recipes<T>() {
            @Override
            public String toString() {
                return id;
            }
        });
    }

    default <C extends IInventory> Optional<T> tryMatch(IRecipe<C> recipe, World world, C inventory) {
        return recipe.matches(inventory, world) ? Optional.of((T)recipe) : Optional.empty();
    }
}

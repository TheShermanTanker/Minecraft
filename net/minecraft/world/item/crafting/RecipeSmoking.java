package net.minecraft.world.item.crafting;

import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

public class RecipeSmoking extends RecipeCooking {
    public RecipeSmoking(MinecraftKey id, String group, RecipeItemStack input, ItemStack output, float experience, int cookTime) {
        super(Recipes.SMOKING, id, group, input, output, experience, cookTime);
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(Blocks.SMOKER);
    }

    @Override
    public RecipeSerializer<?> getRecipeSerializer() {
        return RecipeSerializer.SMOKING_RECIPE;
    }
}

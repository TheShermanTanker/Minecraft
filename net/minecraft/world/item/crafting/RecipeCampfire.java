package net.minecraft.world.item.crafting;

import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

public class RecipeCampfire extends RecipeCooking {
    public RecipeCampfire(MinecraftKey id, String group, RecipeItemStack input, ItemStack output, float experience, int cookTime) {
        super(Recipes.CAMPFIRE_COOKING, id, group, input, output, experience, cookTime);
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(Blocks.CAMPFIRE);
    }

    @Override
    public RecipeSerializer<?> getRecipeSerializer() {
        return RecipeSerializer.CAMPFIRE_COOKING_RECIPE;
    }
}

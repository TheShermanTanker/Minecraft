package net.minecraft.world.item.crafting;

import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

public class FurnaceRecipe extends RecipeCooking {
    public FurnaceRecipe(MinecraftKey id, String group, RecipeItemStack input, ItemStack output, float experience, int cookTime) {
        super(Recipes.SMELTING, id, group, input, output, experience, cookTime);
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(Blocks.FURNACE);
    }

    @Override
    public RecipeSerializer<?> getRecipeSerializer() {
        return RecipeSerializer.SMELTING_RECIPE;
    }
}

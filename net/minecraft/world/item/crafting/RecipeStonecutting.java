package net.minecraft.world.item.crafting;

import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.IInventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Blocks;

public class RecipeStonecutting extends RecipeSingleItem {
    public RecipeStonecutting(MinecraftKey id, String group, RecipeItemStack input, ItemStack output) {
        super(Recipes.STONECUTTING, RecipeSerializer.STONECUTTER, id, group, input, output);
    }

    @Override
    public boolean matches(IInventory inventory, World world) {
        return this.ingredient.test(inventory.getItem(0));
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(Blocks.STONECUTTER);
    }
}

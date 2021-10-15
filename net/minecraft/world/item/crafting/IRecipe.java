package net.minecraft.world.item.crafting;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.IInventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Blocks;

public interface IRecipe<C extends IInventory> {
    boolean matches(C inventory, World world);

    ItemStack assemble(C inventory);

    boolean canCraftInDimensions(int width, int height);

    ItemStack getResult();

    default NonNullList<ItemStack> getRemainingItems(C inventory) {
        NonNullList<ItemStack> nonNullList = NonNullList.withSize(inventory.getSize(), ItemStack.EMPTY);

        for(int i = 0; i < nonNullList.size(); ++i) {
            Item item = inventory.getItem(i).getItem();
            if (item.hasCraftingRemainingItem()) {
                nonNullList.set(i, new ItemStack(item.getCraftingRemainingItem()));
            }
        }

        return nonNullList;
    }

    default NonNullList<RecipeItemStack> getIngredients() {
        return NonNullList.create();
    }

    default boolean isComplex() {
        return false;
    }

    default String getGroup() {
        return "";
    }

    default ItemStack getToastSymbol() {
        return new ItemStack(Blocks.CRAFTING_TABLE);
    }

    MinecraftKey getKey();

    RecipeSerializer<?> getRecipeSerializer();

    Recipes<?> getType();

    default boolean isIncomplete() {
        NonNullList<RecipeItemStack> nonNullList = this.getIngredients();
        return nonNullList.isEmpty() || nonNullList.stream().anyMatch((ingredient) -> {
            return ingredient.getItems().length == 0;
        });
    }
}

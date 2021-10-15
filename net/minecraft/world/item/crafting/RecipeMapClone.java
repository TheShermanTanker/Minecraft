package net.minecraft.world.item.crafting;

import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.inventory.InventoryCrafting;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.World;

public class RecipeMapClone extends IRecipeComplex {
    public RecipeMapClone(MinecraftKey id) {
        super(id);
    }

    @Override
    public boolean matches(InventoryCrafting inventory, World world) {
        int i = 0;
        ItemStack itemStack = ItemStack.EMPTY;

        for(int j = 0; j < inventory.getSize(); ++j) {
            ItemStack itemStack2 = inventory.getItem(j);
            if (!itemStack2.isEmpty()) {
                if (itemStack2.is(Items.FILLED_MAP)) {
                    if (!itemStack.isEmpty()) {
                        return false;
                    }

                    itemStack = itemStack2;
                } else {
                    if (!itemStack2.is(Items.MAP)) {
                        return false;
                    }

                    ++i;
                }
            }
        }

        return !itemStack.isEmpty() && i > 0;
    }

    @Override
    public ItemStack assemble(InventoryCrafting inventory) {
        int i = 0;
        ItemStack itemStack = ItemStack.EMPTY;

        for(int j = 0; j < inventory.getSize(); ++j) {
            ItemStack itemStack2 = inventory.getItem(j);
            if (!itemStack2.isEmpty()) {
                if (itemStack2.is(Items.FILLED_MAP)) {
                    if (!itemStack.isEmpty()) {
                        return ItemStack.EMPTY;
                    }

                    itemStack = itemStack2;
                } else {
                    if (!itemStack2.is(Items.MAP)) {
                        return ItemStack.EMPTY;
                    }

                    ++i;
                }
            }
        }

        if (!itemStack.isEmpty() && i >= 1) {
            ItemStack itemStack3 = itemStack.cloneItemStack();
            itemStack3.setCount(i + 1);
            return itemStack3;
        } else {
            return ItemStack.EMPTY;
        }
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public RecipeSerializer<?> getRecipeSerializer() {
        return RecipeSerializer.MAP_CLONING;
    }
}

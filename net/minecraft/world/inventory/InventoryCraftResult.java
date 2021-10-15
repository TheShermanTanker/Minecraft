package net.minecraft.world.inventory;

import javax.annotation.Nullable;
import net.minecraft.core.NonNullList;
import net.minecraft.world.ContainerUtil;
import net.minecraft.world.IInventory;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.IRecipe;

public class InventoryCraftResult implements IInventory, RecipeHolder {
    private final NonNullList<ItemStack> itemStacks = NonNullList.withSize(1, ItemStack.EMPTY);
    @Nullable
    private IRecipe<?> recipeUsed;

    @Override
    public int getSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        for(ItemStack itemStack : this.itemStacks) {
            if (!itemStack.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.itemStacks.get(0);
    }

    @Override
    public ItemStack splitStack(int slot, int amount) {
        return ContainerUtil.takeItem(this.itemStacks, 0);
    }

    @Override
    public ItemStack splitWithoutUpdate(int slot) {
        return ContainerUtil.takeItem(this.itemStacks, 0);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.itemStacks.set(0, stack);
    }

    @Override
    public void update() {
    }

    @Override
    public boolean stillValid(EntityHuman player) {
        return true;
    }

    @Override
    public void clear() {
        this.itemStacks.clear();
    }

    @Override
    public void setRecipeUsed(@Nullable IRecipe<?> recipe) {
        this.recipeUsed = recipe;
    }

    @Nullable
    @Override
    public IRecipe<?> getRecipeUsed() {
        return this.recipeUsed;
    }
}

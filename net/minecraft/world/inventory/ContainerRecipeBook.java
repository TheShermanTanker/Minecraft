package net.minecraft.world.inventory;

import net.minecraft.recipebook.AutoRecipe;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.IInventory;
import net.minecraft.world.entity.player.AutoRecipeStackManager;
import net.minecraft.world.item.crafting.IRecipe;

public abstract class ContainerRecipeBook<C extends IInventory> extends Container {
    public ContainerRecipeBook(Containers<?> type, int syncId) {
        super(type, syncId);
    }

    public void handlePlacement(boolean craftAll, IRecipe<?> recipe, EntityPlayer player) {
        (new AutoRecipe<>(this)).recipeClicked(player, recipe, craftAll);
    }

    public abstract void fillCraftSlotsStackedContents(AutoRecipeStackManager finder);

    public abstract void clearCraftingContent();

    public abstract boolean recipeMatches(IRecipe<? super C> recipe);

    public abstract int getResultSlotIndex();

    public abstract int getGridWidth();

    public abstract int getGridHeight();

    public abstract int getSize();

    public abstract RecipeBookType getRecipeBookType();

    public abstract boolean shouldMoveToInventory(int index);
}

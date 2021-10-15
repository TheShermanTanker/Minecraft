package net.minecraft.world.inventory;

import net.minecraft.world.IInventory;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.item.crafting.Recipes;

public class ContainerSmoker extends ContainerFurnace {
    public ContainerSmoker(int syncId, PlayerInventory playerInventory) {
        super(Containers.SMOKER, Recipes.SMOKING, RecipeBookType.SMOKER, syncId, playerInventory);
    }

    public ContainerSmoker(int syncId, PlayerInventory playerInventory, IInventory inventory, IContainerProperties propertyDelegate) {
        super(Containers.SMOKER, Recipes.SMOKING, RecipeBookType.SMOKER, syncId, playerInventory, inventory, propertyDelegate);
    }
}

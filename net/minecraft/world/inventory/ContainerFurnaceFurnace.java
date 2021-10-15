package net.minecraft.world.inventory;

import net.minecraft.world.IInventory;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.item.crafting.Recipes;

public class ContainerFurnaceFurnace extends ContainerFurnace {
    public ContainerFurnaceFurnace(int syncId, PlayerInventory playerInventory) {
        super(Containers.FURNACE, Recipes.SMELTING, RecipeBookType.FURNACE, syncId, playerInventory);
    }

    public ContainerFurnaceFurnace(int syncId, PlayerInventory playerInventory, IInventory inventory, IContainerProperties propertyDelegate) {
        super(Containers.FURNACE, Recipes.SMELTING, RecipeBookType.FURNACE, syncId, playerInventory, inventory, propertyDelegate);
    }
}

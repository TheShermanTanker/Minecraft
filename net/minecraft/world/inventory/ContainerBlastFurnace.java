package net.minecraft.world.inventory;

import net.minecraft.world.IInventory;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.item.crafting.Recipes;

public class ContainerBlastFurnace extends ContainerFurnace {
    public ContainerBlastFurnace(int syncId, PlayerInventory playerInventory) {
        super(Containers.BLAST_FURNACE, Recipes.BLASTING, RecipeBookType.BLAST_FURNACE, syncId, playerInventory);
    }

    public ContainerBlastFurnace(int syncId, PlayerInventory playerInventory, IInventory inventory, IContainerProperties propertyDelegate) {
        super(Containers.BLAST_FURNACE, Recipes.BLASTING, RecipeBookType.BLAST_FURNACE, syncId, playerInventory, inventory, propertyDelegate);
    }
}

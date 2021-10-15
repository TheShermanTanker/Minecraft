package net.minecraft.world.inventory;

import net.minecraft.world.IInventory;
import net.minecraft.world.item.ItemStack;

public class SlotShulkerBox extends Slot {
    public SlotShulkerBox(IInventory inventory, int index, int x, int y) {
        super(inventory, index, x, y);
    }

    @Override
    public boolean isAllowed(ItemStack stack) {
        return stack.getItem().canFitInsideContainerItems();
    }
}

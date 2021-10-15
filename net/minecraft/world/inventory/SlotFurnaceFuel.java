package net.minecraft.world.inventory;

import net.minecraft.world.IInventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class SlotFurnaceFuel extends Slot {
    private final ContainerFurnace menu;

    public SlotFurnaceFuel(ContainerFurnace handler, IInventory inventory, int index, int x, int y) {
        super(inventory, index, x, y);
        this.menu = handler;
    }

    @Override
    public boolean isAllowed(ItemStack stack) {
        return this.menu.isFuel(stack) || isBucket(stack);
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return isBucket(stack) ? 1 : super.getMaxStackSize(stack);
    }

    public static boolean isBucket(ItemStack stack) {
        return stack.is(Items.BUCKET);
    }
}

package net.minecraft.world;

import javax.annotation.Nullable;
import net.minecraft.core.EnumDirection;
import net.minecraft.world.item.ItemStack;

public interface IWorldInventory extends IInventory {
    int[] getSlotsForFace(EnumDirection side);

    boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable EnumDirection dir);

    boolean canTakeItemThroughFace(int slot, ItemStack stack, EnumDirection dir);
}

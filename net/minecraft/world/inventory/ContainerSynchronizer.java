package net.minecraft.world.inventory;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

public interface ContainerSynchronizer {
    void sendInitialData(Container handler, NonNullList<ItemStack> stacks, ItemStack cursorStack, int[] properties);

    void sendSlotChange(Container handler, int slot, ItemStack stack);

    void sendCarriedChange(Container handler, ItemStack stack);

    void sendDataChange(Container handler, int property, int value);
}

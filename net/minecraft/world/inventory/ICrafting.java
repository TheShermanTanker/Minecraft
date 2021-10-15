package net.minecraft.world.inventory;

import net.minecraft.world.item.ItemStack;

public interface ICrafting {
    void slotChanged(Container handler, int slotId, ItemStack stack);

    void setContainerData(Container handler, int property, int value);
}

package net.minecraft.world;

import java.util.Set;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public interface IInventory extends Clearable {
    int LARGE_MAX_STACK_SIZE = 64;

    int getSize();

    boolean isEmpty();

    ItemStack getItem(int slot);

    ItemStack splitStack(int slot, int amount);

    ItemStack splitWithoutUpdate(int slot);

    void setItem(int slot, ItemStack stack);

    default int getMaxStackSize() {
        return 64;
    }

    void update();

    boolean stillValid(EntityHuman player);

    default void startOpen(EntityHuman player) {
    }

    default void closeContainer(EntityHuman player) {
    }

    default boolean canPlaceItem(int slot, ItemStack stack) {
        return true;
    }

    default int countItem(Item item) {
        int i = 0;

        for(int j = 0; j < this.getSize(); ++j) {
            ItemStack itemStack = this.getItem(j);
            if (itemStack.getItem().equals(item)) {
                i += itemStack.getCount();
            }
        }

        return i;
    }

    default boolean hasAnyOf(Set<Item> items) {
        for(int i = 0; i < this.getSize(); ++i) {
            ItemStack itemStack = this.getItem(i);
            if (items.contains(itemStack.getItem()) && itemStack.getCount() > 0) {
                return true;
            }
        }

        return false;
    }
}

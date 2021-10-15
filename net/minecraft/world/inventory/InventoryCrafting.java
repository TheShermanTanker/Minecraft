package net.minecraft.world.inventory;

import net.minecraft.core.NonNullList;
import net.minecraft.world.ContainerUtil;
import net.minecraft.world.IInventory;
import net.minecraft.world.entity.player.AutoRecipeStackManager;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;

public class InventoryCrafting implements IInventory, AutoRecipeOutput {
    private final NonNullList<ItemStack> items;
    private final int width;
    private final int height;
    public final Container menu;

    public InventoryCrafting(Container handler, int width, int height) {
        this.items = NonNullList.withSize(width * height, ItemStack.EMPTY);
        this.menu = handler;
        this.width = width;
        this.height = height;
    }

    @Override
    public int getSize() {
        return this.items.size();
    }

    @Override
    public boolean isEmpty() {
        for(ItemStack itemStack : this.items) {
            if (!itemStack.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot >= this.getSize() ? ItemStack.EMPTY : this.items.get(slot);
    }

    @Override
    public ItemStack splitWithoutUpdate(int slot) {
        return ContainerUtil.takeItem(this.items, slot);
    }

    @Override
    public ItemStack splitStack(int slot, int amount) {
        ItemStack itemStack = ContainerUtil.removeItem(this.items, slot, amount);
        if (!itemStack.isEmpty()) {
            this.menu.slotsChanged(this);
        }

        return itemStack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.items.set(slot, stack);
        this.menu.slotsChanged(this);
    }

    @Override
    public void update() {
    }

    @Override
    public boolean stillValid(EntityHuman player) {
        return true;
    }

    @Override
    public void clear() {
        this.items.clear();
    }

    public int getHeight() {
        return this.height;
    }

    public int getWidth() {
        return this.width;
    }

    @Override
    public void fillStackedContents(AutoRecipeStackManager finder) {
        for(ItemStack itemStack : this.items) {
            finder.accountSimpleStack(itemStack);
        }

    }
}

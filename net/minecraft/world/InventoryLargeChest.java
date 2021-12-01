package net.minecraft.world;

import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;

public class InventoryLargeChest implements IInventory {
    public final IInventory container1;
    public final IInventory container2;

    public InventoryLargeChest(IInventory first, IInventory second) {
        this.container1 = first;
        this.container2 = second;
    }

    @Override
    public int getSize() {
        return this.container1.getSize() + this.container2.getSize();
    }

    @Override
    public boolean isEmpty() {
        return this.container1.isEmpty() && this.container2.isEmpty();
    }

    public boolean contains(IInventory inventory) {
        return this.container1 == inventory || this.container2 == inventory;
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot >= this.container1.getSize() ? this.container2.getItem(slot - this.container1.getSize()) : this.container1.getItem(slot);
    }

    @Override
    public ItemStack splitStack(int slot, int amount) {
        return slot >= this.container1.getSize() ? this.container2.splitStack(slot - this.container1.getSize(), amount) : this.container1.splitStack(slot, amount);
    }

    @Override
    public ItemStack splitWithoutUpdate(int slot) {
        return slot >= this.container1.getSize() ? this.container2.splitWithoutUpdate(slot - this.container1.getSize()) : this.container1.splitWithoutUpdate(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot >= this.container1.getSize()) {
            this.container2.setItem(slot - this.container1.getSize(), stack);
        } else {
            this.container1.setItem(slot, stack);
        }

    }

    @Override
    public int getMaxStackSize() {
        return this.container1.getMaxStackSize();
    }

    @Override
    public void update() {
        this.container1.update();
        this.container2.update();
    }

    @Override
    public boolean stillValid(EntityHuman player) {
        return this.container1.stillValid(player) && this.container2.stillValid(player);
    }

    @Override
    public void startOpen(EntityHuman player) {
        this.container1.startOpen(player);
        this.container2.startOpen(player);
    }

    @Override
    public void closeContainer(EntityHuman player) {
        this.container1.closeContainer(player);
        this.container2.closeContainer(player);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot >= this.container1.getSize() ? this.container2.canPlaceItem(slot - this.container1.getSize(), stack) : this.container1.canPlaceItem(slot, stack);
    }

    @Override
    public void clear() {
        this.container1.clear();
        this.container2.clear();
    }
}

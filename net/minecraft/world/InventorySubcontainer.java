package net.minecraft.world;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.entity.player.AutoRecipeStackManager;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.inventory.AutoRecipeOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class InventorySubcontainer implements IInventory, AutoRecipeOutput {
    private final int size;
    public final NonNullList<ItemStack> items;
    @Nullable
    private List<IInventoryListener> listeners;

    public InventorySubcontainer(int size) {
        this.size = size;
        this.items = NonNullList.withSize(size, ItemStack.EMPTY);
    }

    public InventorySubcontainer(ItemStack... items) {
        this.size = items.length;
        this.items = NonNullList.of(ItemStack.EMPTY, items);
    }

    public void addListener(IInventoryListener listener) {
        if (this.listeners == null) {
            this.listeners = Lists.newArrayList();
        }

        this.listeners.add(listener);
    }

    public void removeListener(IInventoryListener listener) {
        if (this.listeners != null) {
            this.listeners.remove(listener);
        }

    }

    @Override
    public ItemStack getItem(int slot) {
        return slot >= 0 && slot < this.items.size() ? this.items.get(slot) : ItemStack.EMPTY;
    }

    public List<ItemStack> removeAllItems() {
        List<ItemStack> list = this.items.stream().filter((stack) -> {
            return !stack.isEmpty();
        }).collect(Collectors.toList());
        this.clear();
        return list;
    }

    @Override
    public ItemStack splitStack(int slot, int amount) {
        ItemStack itemStack = ContainerUtil.removeItem(this.items, slot, amount);
        if (!itemStack.isEmpty()) {
            this.update();
        }

        return itemStack;
    }

    public ItemStack removeItemType(Item item, int count) {
        ItemStack itemStack = new ItemStack(item, 0);

        for(int i = this.size - 1; i >= 0; --i) {
            ItemStack itemStack2 = this.getItem(i);
            if (itemStack2.getItem().equals(item)) {
                int j = count - itemStack.getCount();
                ItemStack itemStack3 = itemStack2.cloneAndSubtract(j);
                itemStack.add(itemStack3.getCount());
                if (itemStack.getCount() == count) {
                    break;
                }
            }
        }

        if (!itemStack.isEmpty()) {
            this.update();
        }

        return itemStack;
    }

    public ItemStack addItem(ItemStack stack) {
        ItemStack itemStack = stack.cloneItemStack();
        this.moveItemToOccupiedSlotsWithSameType(itemStack);
        if (itemStack.isEmpty()) {
            return ItemStack.EMPTY;
        } else {
            this.moveItemToEmptySlots(itemStack);
            return itemStack.isEmpty() ? ItemStack.EMPTY : itemStack;
        }
    }

    public boolean canAddItem(ItemStack stack) {
        boolean bl = false;

        for(ItemStack itemStack : this.items) {
            if (itemStack.isEmpty() || ItemStack.isSameItemSameTags(itemStack, stack) && itemStack.getCount() < itemStack.getMaxStackSize()) {
                bl = true;
                break;
            }
        }

        return bl;
    }

    @Override
    public ItemStack splitWithoutUpdate(int slot) {
        ItemStack itemStack = this.items.get(slot);
        if (itemStack.isEmpty()) {
            return ItemStack.EMPTY;
        } else {
            this.items.set(slot, ItemStack.EMPTY);
            return itemStack;
        }
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.items.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > this.getMaxStackSize()) {
            stack.setCount(this.getMaxStackSize());
        }

        this.update();
    }

    @Override
    public int getSize() {
        return this.size;
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
    public void update() {
        if (this.listeners != null) {
            for(IInventoryListener containerListener : this.listeners) {
                containerListener.containerChanged(this);
            }
        }

    }

    @Override
    public boolean stillValid(EntityHuman player) {
        return true;
    }

    @Override
    public void clear() {
        this.items.clear();
        this.update();
    }

    @Override
    public void fillStackedContents(AutoRecipeStackManager finder) {
        for(ItemStack itemStack : this.items) {
            finder.accountStack(itemStack);
        }

    }

    @Override
    public String toString() {
        return this.items.stream().filter((stack) -> {
            return !stack.isEmpty();
        }).collect(Collectors.toList()).toString();
    }

    private void moveItemToEmptySlots(ItemStack stack) {
        for(int i = 0; i < this.size; ++i) {
            ItemStack itemStack = this.getItem(i);
            if (itemStack.isEmpty()) {
                this.setItem(i, stack.cloneItemStack());
                stack.setCount(0);
                return;
            }
        }

    }

    private void moveItemToOccupiedSlotsWithSameType(ItemStack stack) {
        for(int i = 0; i < this.size; ++i) {
            ItemStack itemStack = this.getItem(i);
            if (ItemStack.isSameItemSameTags(itemStack, stack)) {
                this.moveItemsBetweenStacks(stack, itemStack);
                if (stack.isEmpty()) {
                    return;
                }
            }
        }

    }

    private void moveItemsBetweenStacks(ItemStack source, ItemStack target) {
        int i = Math.min(this.getMaxStackSize(), target.getMaxStackSize());
        int j = Math.min(source.getCount(), i - target.getCount());
        if (j > 0) {
            target.add(j);
            source.subtract(j);
            this.update();
        }

    }

    public void fromTag(NBTTagList nbtList) {
        for(int i = 0; i < nbtList.size(); ++i) {
            ItemStack itemStack = ItemStack.of(nbtList.getCompound(i));
            if (!itemStack.isEmpty()) {
                this.addItem(itemStack);
            }
        }

    }

    public NBTTagList createTag() {
        NBTTagList listTag = new NBTTagList();

        for(int i = 0; i < this.getSize(); ++i) {
            ItemStack itemStack = this.getItem(i);
            if (!itemStack.isEmpty()) {
                listTag.add(itemStack.save(new NBTTagCompound()));
            }
        }

        return listTag;
    }
}

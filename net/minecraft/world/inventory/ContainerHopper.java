package net.minecraft.world.inventory;

import net.minecraft.world.IInventory;
import net.minecraft.world.InventorySubcontainer;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.item.ItemStack;

public class ContainerHopper extends Container {
    public static final int CONTAINER_SIZE = 5;
    private final IInventory hopper;

    public ContainerHopper(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new InventorySubcontainer(5));
    }

    public ContainerHopper(int syncId, PlayerInventory playerInventory, IInventory inventory) {
        super(Containers.HOPPER, syncId);
        this.hopper = inventory;
        checkContainerSize(inventory, 5);
        inventory.startOpen(playerInventory.player);
        int i = 51;

        for(int j = 0; j < 5; ++j) {
            this.addSlot(new Slot(inventory, j, 44 + j * 18, 20));
        }

        for(int k = 0; k < 3; ++k) {
            for(int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + k * 9 + 9, 8 + l * 18, k * 18 + 51));
            }
        }

        for(int m = 0; m < 9; ++m) {
            this.addSlot(new Slot(playerInventory, m, 8 + m * 18, 109));
        }

    }

    @Override
    public boolean canUse(EntityHuman player) {
        return this.hopper.stillValid(player);
    }

    @Override
    public ItemStack shiftClick(EntityHuman player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemStack2 = slot.getItem();
            itemStack = itemStack2.cloneItemStack();
            if (index < this.hopper.getSize()) {
                if (!this.moveItemStackTo(itemStack2, this.hopper.getSize(), this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemStack2, 0, this.hopper.getSize(), false)) {
                return ItemStack.EMPTY;
            }

            if (itemStack2.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemStack;
    }

    @Override
    public void removed(EntityHuman player) {
        super.removed(player);
        this.hopper.closeContainer(player);
    }
}

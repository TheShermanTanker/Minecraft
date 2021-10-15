package net.minecraft.world.inventory;

import net.minecraft.world.IInventory;
import net.minecraft.world.InventorySubcontainer;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.item.ItemStack;

public class ContainerChest extends Container {
    private static final int SLOTS_PER_ROW = 9;
    private final IInventory container;
    private final int containerRows;

    private ContainerChest(Containers<?> type, int syncId, PlayerInventory playerInventory, int rows) {
        this(type, syncId, playerInventory, new InventorySubcontainer(9 * rows), rows);
    }

    public static ContainerChest oneRow(int syncId, PlayerInventory playerInventory) {
        return new ContainerChest(Containers.GENERIC_9x1, syncId, playerInventory, 1);
    }

    public static ContainerChest twoRows(int syncId, PlayerInventory playerInventory) {
        return new ContainerChest(Containers.GENERIC_9x2, syncId, playerInventory, 2);
    }

    public static ContainerChest threeRows(int syncId, PlayerInventory playerInventory) {
        return new ContainerChest(Containers.GENERIC_9x3, syncId, playerInventory, 3);
    }

    public static ContainerChest fourRows(int syncId, PlayerInventory playerInventory) {
        return new ContainerChest(Containers.GENERIC_9x4, syncId, playerInventory, 4);
    }

    public static ContainerChest fiveRows(int syncId, PlayerInventory playerInventory) {
        return new ContainerChest(Containers.GENERIC_9x5, syncId, playerInventory, 5);
    }

    public static ContainerChest sixRows(int syncId, PlayerInventory playerInventory) {
        return new ContainerChest(Containers.GENERIC_9x6, syncId, playerInventory, 6);
    }

    public static ContainerChest threeRows(int syncId, PlayerInventory playerInventory, IInventory inventory) {
        return new ContainerChest(Containers.GENERIC_9x3, syncId, playerInventory, inventory, 3);
    }

    public static ContainerChest sixRows(int syncId, PlayerInventory playerInventory, IInventory inventory) {
        return new ContainerChest(Containers.GENERIC_9x6, syncId, playerInventory, inventory, 6);
    }

    public ContainerChest(Containers<?> type, int syncId, PlayerInventory playerInventory, IInventory inventory, int rows) {
        super(type, syncId);
        checkContainerSize(inventory, rows * 9);
        this.container = inventory;
        this.containerRows = rows;
        inventory.startOpen(playerInventory.player);
        int i = (this.containerRows - 4) * 18;

        for(int j = 0; j < this.containerRows; ++j) {
            for(int k = 0; k < 9; ++k) {
                this.addSlot(new Slot(inventory, k + j * 9, 8 + k * 18, 18 + j * 18));
            }
        }

        for(int l = 0; l < 3; ++l) {
            for(int m = 0; m < 9; ++m) {
                this.addSlot(new Slot(playerInventory, m + l * 9 + 9, 8 + m * 18, 103 + l * 18 + i));
            }
        }

        for(int n = 0; n < 9; ++n) {
            this.addSlot(new Slot(playerInventory, n, 8 + n * 18, 161 + i));
        }

    }

    @Override
    public boolean canUse(EntityHuman player) {
        return this.container.stillValid(player);
    }

    @Override
    public ItemStack shiftClick(EntityHuman player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemStack2 = slot.getItem();
            itemStack = itemStack2.cloneItemStack();
            if (index < this.containerRows * 9) {
                if (!this.moveItemStackTo(itemStack2, this.containerRows * 9, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemStack2, 0, this.containerRows * 9, false)) {
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
        this.container.closeContainer(player);
    }

    public IInventory getContainer() {
        return this.container;
    }

    public int getRowCount() {
        return this.containerRows;
    }
}

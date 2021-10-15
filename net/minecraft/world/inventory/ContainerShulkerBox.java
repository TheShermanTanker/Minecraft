package net.minecraft.world.inventory;

import net.minecraft.world.IInventory;
import net.minecraft.world.InventorySubcontainer;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.item.ItemStack;

public class ContainerShulkerBox extends Container {
    private static final int CONTAINER_SIZE = 27;
    private final IInventory container;

    public ContainerShulkerBox(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new InventorySubcontainer(27));
    }

    public ContainerShulkerBox(int syncId, PlayerInventory playerInventory, IInventory inventory) {
        super(Containers.SHULKER_BOX, syncId);
        checkContainerSize(inventory, 27);
        this.container = inventory;
        inventory.startOpen(playerInventory.player);
        int i = 3;
        int j = 9;

        for(int k = 0; k < 3; ++k) {
            for(int l = 0; l < 9; ++l) {
                this.addSlot(new SlotShulkerBox(inventory, l + k * 9, 8 + l * 18, 18 + k * 18));
            }
        }

        for(int m = 0; m < 3; ++m) {
            for(int n = 0; n < 9; ++n) {
                this.addSlot(new Slot(playerInventory, n + m * 9 + 9, 8 + n * 18, 84 + m * 18));
            }
        }

        for(int o = 0; o < 9; ++o) {
            this.addSlot(new Slot(playerInventory, o, 8 + o * 18, 142));
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
            if (index < this.container.getSize()) {
                if (!this.moveItemStackTo(itemStack2, this.container.getSize(), this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemStack2, 0, this.container.getSize(), false)) {
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
}

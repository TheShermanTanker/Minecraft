package net.minecraft.world.inventory;

import net.minecraft.world.IInventory;
import net.minecraft.world.entity.animal.horse.EntityHorseAbstract;
import net.minecraft.world.entity.animal.horse.EntityHorseChestedAbstract;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ContainerHorse extends Container {
    private final IInventory horseContainer;
    private final EntityHorseAbstract horse;

    public ContainerHorse(int syncId, PlayerInventory playerInventory, IInventory inventory, EntityHorseAbstract entity) {
        super((Containers<?>)null, syncId);
        this.horseContainer = inventory;
        this.horse = entity;
        int i = 3;
        inventory.startOpen(playerInventory.player);
        int j = -18;
        this.addSlot(new Slot(inventory, 0, 8, 18) {
            @Override
            public boolean isAllowed(ItemStack stack) {
                return stack.is(Items.SADDLE) && !this.hasItem() && entity.canSaddle();
            }

            @Override
            public boolean isActive() {
                return entity.canSaddle();
            }
        });
        this.addSlot(new Slot(inventory, 1, 8, 36) {
            @Override
            public boolean isAllowed(ItemStack stack) {
                return entity.isArmor(stack);
            }

            @Override
            public boolean isActive() {
                return entity.canWearArmor();
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        if (this.hasChest(entity)) {
            for(int k = 0; k < 3; ++k) {
                for(int l = 0; l < ((EntityHorseChestedAbstract)entity).getInventoryColumns(); ++l) {
                    this.addSlot(new Slot(inventory, 2 + l + k * ((EntityHorseChestedAbstract)entity).getInventoryColumns(), 80 + l * 18, 18 + k * 18));
                }
            }
        }

        for(int m = 0; m < 3; ++m) {
            for(int n = 0; n < 9; ++n) {
                this.addSlot(new Slot(playerInventory, n + m * 9 + 9, 8 + n * 18, 102 + m * 18 + -18));
            }
        }

        for(int o = 0; o < 9; ++o) {
            this.addSlot(new Slot(playerInventory, o, 8 + o * 18, 142));
        }

    }

    @Override
    public boolean canUse(EntityHuman player) {
        return !this.horse.hasInventoryChanged(this.horseContainer) && this.horseContainer.stillValid(player) && this.horse.isAlive() && this.horse.distanceTo(player) < 8.0F;
    }

    private boolean hasChest(EntityHorseAbstract horse) {
        return horse instanceof EntityHorseChestedAbstract && ((EntityHorseChestedAbstract)horse).isCarryingChest();
    }

    @Override
    public ItemStack shiftClick(EntityHuman player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemStack2 = slot.getItem();
            itemStack = itemStack2.cloneItemStack();
            int i = this.horseContainer.getSize();
            if (index < i) {
                if (!this.moveItemStackTo(itemStack2, i, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (this.getSlot(1).isAllowed(itemStack2) && !this.getSlot(1).hasItem()) {
                if (!this.moveItemStackTo(itemStack2, 1, 2, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (this.getSlot(0).isAllowed(itemStack2)) {
                if (!this.moveItemStackTo(itemStack2, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (i <= 2 || !this.moveItemStackTo(itemStack2, 2, i, false)) {
                int k = i + 27;
                int m = k + 9;
                if (index >= k && index < m) {
                    if (!this.moveItemStackTo(itemStack2, i, k, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index >= i && index < k) {
                    if (!this.moveItemStackTo(itemStack2, k, m, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(itemStack2, k, k, false)) {
                    return ItemStack.EMPTY;
                }

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
        this.horseContainer.closeContainer(player);
    }
}

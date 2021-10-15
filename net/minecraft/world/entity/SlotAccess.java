package net.minecraft.world.entity;

import java.util.function.Predicate;
import net.minecraft.world.IInventory;
import net.minecraft.world.item.ItemStack;

public interface SlotAccess {
    SlotAccess NULL = new SlotAccess() {
        @Override
        public ItemStack get() {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean set(ItemStack stack) {
            return false;
        }
    };

    static SlotAccess forContainer(IInventory inventory, int index, Predicate<ItemStack> stackFilter) {
        return new SlotAccess() {
            @Override
            public ItemStack get() {
                return inventory.getItem(index);
            }

            @Override
            public boolean set(ItemStack stack) {
                if (!stackFilter.test(stack)) {
                    return false;
                } else {
                    inventory.setItem(index, stack);
                    return true;
                }
            }
        };
    }

    static SlotAccess forContainer(IInventory inventory, int index) {
        return forContainer(inventory, index, (stack) -> {
            return true;
        });
    }

    static SlotAccess forEquipmentSlot(EntityLiving entity, EnumItemSlot slot, Predicate<ItemStack> filter) {
        return new SlotAccess() {
            @Override
            public ItemStack get() {
                return entity.getEquipment(slot);
            }

            @Override
            public boolean set(ItemStack stack) {
                if (!filter.test(stack)) {
                    return false;
                } else {
                    entity.setSlot(slot, stack);
                    return true;
                }
            }
        };
    }

    static SlotAccess forEquipmentSlot(EntityLiving entity, EnumItemSlot slot) {
        return forEquipmentSlot(entity, slot, (stack) -> {
            return true;
        });
    }

    ItemStack get();

    boolean set(ItemStack stack);
}

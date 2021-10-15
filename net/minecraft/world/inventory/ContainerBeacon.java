package net.minecraft.world.inventory;

import javax.annotation.Nullable;
import net.minecraft.tags.TagsItem;
import net.minecraft.world.IInventory;
import net.minecraft.world.InventorySubcontainer;
import net.minecraft.world.effect.MobEffectList;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

public class ContainerBeacon extends Container {
    private static final int PAYMENT_SLOT = 0;
    private static final int SLOT_COUNT = 1;
    private static final int DATA_COUNT = 3;
    private static final int INV_SLOT_START = 1;
    private static final int INV_SLOT_END = 28;
    private static final int USE_ROW_SLOT_START = 28;
    private static final int USE_ROW_SLOT_END = 37;
    private final IInventory beacon = new InventorySubcontainer(1) {
        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return stack.is(TagsItem.BEACON_PAYMENT_ITEMS);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    };
    private final ContainerBeacon.SlotBeacon paymentSlot;
    private final ContainerAccess access;
    private final IContainerProperties beaconData;

    public ContainerBeacon(int syncId, IInventory inventory) {
        this(syncId, inventory, new ContainerProperties(3), ContainerAccess.NULL);
    }

    public ContainerBeacon(int syncId, IInventory inventory, IContainerProperties propertyDelegate, ContainerAccess context) {
        super(Containers.BEACON, syncId);
        checkContainerDataCount(propertyDelegate, 3);
        this.beaconData = propertyDelegate;
        this.access = context;
        this.paymentSlot = new ContainerBeacon.SlotBeacon(this.beacon, 0, 136, 110);
        this.addSlot(this.paymentSlot);
        this.addDataSlots(propertyDelegate);
        int i = 36;
        int j = 137;

        for(int k = 0; k < 3; ++k) {
            for(int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(inventory, l + k * 9 + 9, 36 + l * 18, 137 + k * 18));
            }
        }

        for(int m = 0; m < 9; ++m) {
            this.addSlot(new Slot(inventory, m, 36 + m * 18, 195));
        }

    }

    @Override
    public void removed(EntityHuman player) {
        super.removed(player);
        if (!player.level.isClientSide) {
            ItemStack itemStack = this.paymentSlot.remove(this.paymentSlot.getMaxStackSize());
            if (!itemStack.isEmpty()) {
                player.drop(itemStack, false);
            }

        }
    }

    @Override
    public boolean canUse(EntityHuman player) {
        return stillValid(this.access, player, Blocks.BEACON);
    }

    @Override
    public void setContainerData(int id, int value) {
        super.setContainerData(id, value);
        this.broadcastChanges();
    }

    @Override
    public ItemStack shiftClick(EntityHuman player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemStack2 = slot.getItem();
            itemStack = itemStack2.cloneItemStack();
            if (index == 0) {
                if (!this.moveItemStackTo(itemStack2, 1, 37, true)) {
                    return ItemStack.EMPTY;
                }

                slot.onQuickCraft(itemStack2, itemStack);
            } else if (!this.paymentSlot.hasItem() && this.paymentSlot.isAllowed(itemStack2) && itemStack2.getCount() == 1) {
                if (!this.moveItemStackTo(itemStack2, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= 1 && index < 28) {
                if (!this.moveItemStackTo(itemStack2, 28, 37, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= 28 && index < 37) {
                if (!this.moveItemStackTo(itemStack2, 1, 28, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemStack2, 1, 37, false)) {
                return ItemStack.EMPTY;
            }

            if (itemStack2.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemStack2.getCount() == itemStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, itemStack2);
        }

        return itemStack;
    }

    public int getLevels() {
        return this.beaconData.getProperty(0);
    }

    @Nullable
    public MobEffectList getPrimaryEffect() {
        return MobEffectList.fromId(this.beaconData.getProperty(1));
    }

    @Nullable
    public MobEffectList getSecondaryEffect() {
        return MobEffectList.fromId(this.beaconData.getProperty(2));
    }

    public void updateEffects(int primaryEffectId, int secondaryEffectId) {
        if (this.paymentSlot.hasItem()) {
            this.beaconData.setProperty(1, primaryEffectId);
            this.beaconData.setProperty(2, secondaryEffectId);
            this.paymentSlot.remove(1);
        }

    }

    public boolean hasPayment() {
        return !this.beacon.getItem(0).isEmpty();
    }

    class SlotBeacon extends Slot {
        public SlotBeacon(IInventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean isAllowed(ItemStack stack) {
            return stack.is(TagsItem.BEACON_PAYMENT_ITEMS);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}

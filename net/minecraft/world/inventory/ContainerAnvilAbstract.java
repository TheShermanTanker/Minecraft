package net.minecraft.world.inventory;

import javax.annotation.Nullable;
import net.minecraft.world.IInventory;
import net.minecraft.world.InventorySubcontainer;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.IBlockData;

public abstract class ContainerAnvilAbstract extends Container {
    public static final int INPUT_SLOT = 0;
    public static final int ADDITIONAL_SLOT = 1;
    public static final int RESULT_SLOT = 2;
    private static final int INV_SLOT_START = 3;
    private static final int INV_SLOT_END = 30;
    private static final int USE_ROW_SLOT_START = 30;
    private static final int USE_ROW_SLOT_END = 39;
    protected final InventoryCraftResult resultSlots = new InventoryCraftResult();
    protected final IInventory inputSlots = new InventorySubcontainer(2) {
        @Override
        public void update() {
            super.update();
            ContainerAnvilAbstract.this.slotsChanged(this);
        }
    };
    protected final ContainerAccess access;
    protected final EntityHuman player;

    protected abstract boolean mayPickup(EntityHuman player, boolean present);

    protected abstract void onTake(EntityHuman player, ItemStack stack);

    protected abstract boolean isValidBlock(IBlockData state);

    public ContainerAnvilAbstract(@Nullable Containers<?> type, int syncId, PlayerInventory playerInventory, ContainerAccess context) {
        super(type, syncId);
        this.access = context;
        this.player = playerInventory.player;
        this.addSlot(new Slot(this.inputSlots, 0, 27, 47));
        this.addSlot(new Slot(this.inputSlots, 1, 76, 47));
        this.addSlot(new Slot(this.resultSlots, 2, 134, 47) {
            @Override
            public boolean isAllowed(ItemStack stack) {
                return false;
            }

            @Override
            public boolean isAllowed(EntityHuman playerEntity) {
                return ContainerAnvilAbstract.this.mayPickup(playerEntity, this.hasItem());
            }

            @Override
            public void onTake(EntityHuman player, ItemStack stack) {
                ContainerAnvilAbstract.this.onTake(player, stack);
            }
        });

        for(int i = 0; i < 3; ++i) {
            for(int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        for(int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(playerInventory, k, 8 + k * 18, 142));
        }

    }

    public abstract void createResult();

    @Override
    public void slotsChanged(IInventory inventory) {
        super.slotsChanged(inventory);
        if (inventory == this.inputSlots) {
            this.createResult();
        }

    }

    @Override
    public void removed(EntityHuman player) {
        super.removed(player);
        this.access.execute((world, pos) -> {
            this.clearContainer(player, this.inputSlots);
        });
    }

    @Override
    public boolean canUse(EntityHuman player) {
        return this.access.evaluate((world, pos) -> {
            return !this.isValidBlock(world.getType(pos)) ? false : player.distanceToSqr((double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D) <= 64.0D;
        }, true);
    }

    protected boolean shouldQuickMoveToAdditionalSlot(ItemStack stack) {
        return false;
    }

    @Override
    public ItemStack shiftClick(EntityHuman player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemStack2 = slot.getItem();
            itemStack = itemStack2.cloneItemStack();
            if (index == 2) {
                if (!this.moveItemStackTo(itemStack2, 3, 39, true)) {
                    return ItemStack.EMPTY;
                }

                slot.onQuickCraft(itemStack2, itemStack);
            } else if (index != 0 && index != 1) {
                if (index >= 3 && index < 39) {
                    int i = this.shouldQuickMoveToAdditionalSlot(itemStack) ? 1 : 0;
                    if (!this.moveItemStackTo(itemStack2, i, 2, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            } else if (!this.moveItemStackTo(itemStack2, 3, 39, false)) {
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
}

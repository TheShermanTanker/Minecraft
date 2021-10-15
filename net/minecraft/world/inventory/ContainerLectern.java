package net.minecraft.world.inventory;

import net.minecraft.world.IInventory;
import net.minecraft.world.InventorySubcontainer;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;

public class ContainerLectern extends Container {
    private static final int DATA_COUNT = 1;
    private static final int SLOT_COUNT = 1;
    public static final int BUTTON_PREV_PAGE = 1;
    public static final int BUTTON_NEXT_PAGE = 2;
    public static final int BUTTON_TAKE_BOOK = 3;
    public static final int BUTTON_PAGE_JUMP_RANGE_START = 100;
    private final IInventory lectern;
    private final IContainerProperties lecternData;

    public ContainerLectern(int syncId) {
        this(syncId, new InventorySubcontainer(1), new ContainerProperties(1));
    }

    public ContainerLectern(int syncId, IInventory inventory, IContainerProperties propertyDelegate) {
        super(Containers.LECTERN, syncId);
        checkContainerSize(inventory, 1);
        checkContainerDataCount(propertyDelegate, 1);
        this.lectern = inventory;
        this.lecternData = propertyDelegate;
        this.addSlot(new Slot(inventory, 0, 0, 0) {
            @Override
            public void setChanged() {
                super.setChanged();
                ContainerLectern.this.slotsChanged(this.container);
            }
        });
        this.addDataSlots(propertyDelegate);
    }

    @Override
    public boolean clickMenuButton(EntityHuman player, int id) {
        if (id >= 100) {
            int i = id - 100;
            this.setContainerData(0, i);
            return true;
        } else {
            switch(id) {
            case 1:
                int k = this.lecternData.getProperty(0);
                this.setContainerData(0, k - 1);
                return true;
            case 2:
                int j = this.lecternData.getProperty(0);
                this.setContainerData(0, j + 1);
                return true;
            case 3:
                if (!player.mayBuild()) {
                    return false;
                }

                ItemStack itemStack = this.lectern.splitWithoutUpdate(0);
                this.lectern.update();
                if (!player.getInventory().pickup(itemStack)) {
                    player.drop(itemStack, false);
                }

                return true;
            default:
                return false;
            }
        }
    }

    @Override
    public void setContainerData(int id, int value) {
        super.setContainerData(id, value);
        this.broadcastChanges();
    }

    @Override
    public boolean canUse(EntityHuman player) {
        return this.lectern.stillValid(player);
    }

    public ItemStack getBook() {
        return this.lectern.getItem(0);
    }

    public int getPage() {
        return this.lecternData.getProperty(0);
    }
}

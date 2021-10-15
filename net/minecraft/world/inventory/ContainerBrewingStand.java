package net.minecraft.world.inventory;

import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.IInventory;
import net.minecraft.world.InventorySubcontainer;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewer;
import net.minecraft.world.item.alchemy.PotionRegistry;
import net.minecraft.world.item.alchemy.PotionUtil;

public class ContainerBrewingStand extends Container {
    private static final int BOTTLE_SLOT_START = 0;
    private static final int BOTTLE_SLOT_END = 2;
    private static final int INGREDIENT_SLOT = 3;
    private static final int FUEL_SLOT = 4;
    private static final int SLOT_COUNT = 5;
    private static final int DATA_COUNT = 2;
    private static final int INV_SLOT_START = 5;
    private static final int INV_SLOT_END = 32;
    private static final int USE_ROW_SLOT_START = 32;
    private static final int USE_ROW_SLOT_END = 41;
    private final IInventory brewingStand;
    private final IContainerProperties brewingStandData;
    private final Slot ingredientSlot;

    public ContainerBrewingStand(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new InventorySubcontainer(5), new ContainerProperties(2));
    }

    public ContainerBrewingStand(int syncId, PlayerInventory playerInventory, IInventory inventory, IContainerProperties propertyDelegate) {
        super(Containers.BREWING_STAND, syncId);
        checkContainerSize(inventory, 5);
        checkContainerDataCount(propertyDelegate, 2);
        this.brewingStand = inventory;
        this.brewingStandData = propertyDelegate;
        this.addSlot(new ContainerBrewingStand.SlotPotionBottle(inventory, 0, 56, 51));
        this.addSlot(new ContainerBrewingStand.SlotPotionBottle(inventory, 1, 79, 58));
        this.addSlot(new ContainerBrewingStand.SlotPotionBottle(inventory, 2, 102, 51));
        this.ingredientSlot = this.addSlot(new ContainerBrewingStand.SlotBrewing(inventory, 3, 79, 17));
        this.addSlot(new ContainerBrewingStand.FuelSlot(inventory, 4, 17, 17));
        this.addDataSlots(propertyDelegate);

        for(int i = 0; i < 3; ++i) {
            for(int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        for(int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(playerInventory, k, 8 + k * 18, 142));
        }

    }

    @Override
    public boolean canUse(EntityHuman player) {
        return this.brewingStand.stillValid(player);
    }

    @Override
    public ItemStack shiftClick(EntityHuman player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemStack2 = slot.getItem();
            itemStack = itemStack2.cloneItemStack();
            if ((index < 0 || index > 2) && index != 3 && index != 4) {
                if (ContainerBrewingStand.FuelSlot.mayPlaceItem(itemStack)) {
                    if (this.moveItemStackTo(itemStack2, 4, 5, false) || this.ingredientSlot.isAllowed(itemStack2) && !this.moveItemStackTo(itemStack2, 3, 4, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (this.ingredientSlot.isAllowed(itemStack2)) {
                    if (!this.moveItemStackTo(itemStack2, 3, 4, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (ContainerBrewingStand.SlotPotionBottle.mayPlaceItem(itemStack) && itemStack.getCount() == 1) {
                    if (!this.moveItemStackTo(itemStack2, 0, 3, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index >= 5 && index < 32) {
                    if (!this.moveItemStackTo(itemStack2, 32, 41, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index >= 32 && index < 41) {
                    if (!this.moveItemStackTo(itemStack2, 5, 32, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(itemStack2, 5, 41, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.moveItemStackTo(itemStack2, 5, 41, true)) {
                    return ItemStack.EMPTY;
                }

                slot.onQuickCraft(itemStack2, itemStack);
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

    public int getFuel() {
        return this.brewingStandData.getProperty(1);
    }

    public int getBrewingTicks() {
        return this.brewingStandData.getProperty(0);
    }

    static class FuelSlot extends Slot {
        public FuelSlot(IInventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean isAllowed(ItemStack stack) {
            return mayPlaceItem(stack);
        }

        public static boolean mayPlaceItem(ItemStack stack) {
            return stack.is(Items.BLAZE_POWDER);
        }

        @Override
        public int getMaxStackSize() {
            return 64;
        }
    }

    static class SlotBrewing extends Slot {
        public SlotBrewing(IInventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean isAllowed(ItemStack stack) {
            return PotionBrewer.isIngredient(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 64;
        }
    }

    static class SlotPotionBottle extends Slot {
        public SlotPotionBottle(IInventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean isAllowed(ItemStack stack) {
            return mayPlaceItem(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public void onTake(EntityHuman player, ItemStack stack) {
            PotionRegistry potion = PotionUtil.getPotion(stack);
            if (player instanceof EntityPlayer) {
                CriterionTriggers.BREWED_POTION.trigger((EntityPlayer)player, potion);
            }

            super.onTake(player, stack);
        }

        public static boolean mayPlaceItem(ItemStack stack) {
            return stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION) || stack.is(Items.GLASS_BOTTLE);
        }
    }
}

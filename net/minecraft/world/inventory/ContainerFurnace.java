package net.minecraft.world.inventory;

import net.minecraft.world.IInventory;
import net.minecraft.world.InventorySubcontainer;
import net.minecraft.world.entity.player.AutoRecipeStackManager;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.IRecipe;
import net.minecraft.world.item.crafting.RecipeCooking;
import net.minecraft.world.item.crafting.Recipes;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.TileEntityFurnace;

public abstract class ContainerFurnace extends ContainerRecipeBook<IInventory> {
    public static final int INGREDIENT_SLOT = 0;
    public static final int FUEL_SLOT = 1;
    public static final int RESULT_SLOT = 2;
    public static final int SLOT_COUNT = 3;
    public static final int DATA_COUNT = 4;
    private static final int INV_SLOT_START = 3;
    private static final int INV_SLOT_END = 30;
    private static final int USE_ROW_SLOT_START = 30;
    private static final int USE_ROW_SLOT_END = 39;
    private final IInventory container;
    private final IContainerProperties data;
    protected final World level;
    private final Recipes<? extends RecipeCooking> recipeType;
    private final RecipeBookType recipeBookType;

    protected ContainerFurnace(Containers<?> type, Recipes<? extends RecipeCooking> recipeType, RecipeBookType category, int syncId, PlayerInventory playerInventory) {
        this(type, recipeType, category, syncId, playerInventory, new InventorySubcontainer(3), new ContainerProperties(4));
    }

    protected ContainerFurnace(Containers<?> type, Recipes<? extends RecipeCooking> recipeType, RecipeBookType category, int syncId, PlayerInventory playerInventory, IInventory inventory, IContainerProperties propertyDelegate) {
        super(type, syncId);
        this.recipeType = recipeType;
        this.recipeBookType = category;
        checkContainerSize(inventory, 3);
        checkContainerDataCount(propertyDelegate, 4);
        this.container = inventory;
        this.data = propertyDelegate;
        this.level = playerInventory.player.level;
        this.addSlot(new Slot(inventory, 0, 56, 17));
        this.addSlot(new SlotFurnaceFuel(this, inventory, 1, 56, 53));
        this.addSlot(new SlotFurnaceResult(playerInventory.player, inventory, 2, 116, 35));

        for(int i = 0; i < 3; ++i) {
            for(int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        for(int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(playerInventory, k, 8 + k * 18, 142));
        }

        this.addDataSlots(propertyDelegate);
    }

    @Override
    public void fillCraftSlotsStackedContents(AutoRecipeStackManager finder) {
        if (this.container instanceof AutoRecipeOutput) {
            ((AutoRecipeOutput)this.container).fillStackedContents(finder);
        }

    }

    @Override
    public void clearCraftingContent() {
        this.getSlot(0).set(ItemStack.EMPTY);
        this.getSlot(2).set(ItemStack.EMPTY);
    }

    @Override
    public boolean recipeMatches(IRecipe<? super IInventory> recipe) {
        return recipe.matches(this.container, this.level);
    }

    @Override
    public int getResultSlotIndex() {
        return 2;
    }

    @Override
    public int getGridWidth() {
        return 1;
    }

    @Override
    public int getGridHeight() {
        return 1;
    }

    @Override
    public int getSize() {
        return 3;
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
            if (index == 2) {
                if (!this.moveItemStackTo(itemStack2, 3, 39, true)) {
                    return ItemStack.EMPTY;
                }

                slot.onQuickCraft(itemStack2, itemStack);
            } else if (index != 1 && index != 0) {
                if (this.canSmelt(itemStack2)) {
                    if (!this.moveItemStackTo(itemStack2, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (this.isFuel(itemStack2)) {
                    if (!this.moveItemStackTo(itemStack2, 1, 2, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index >= 3 && index < 30) {
                    if (!this.moveItemStackTo(itemStack2, 30, 39, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index >= 30 && index < 39 && !this.moveItemStackTo(itemStack2, 3, 30, false)) {
                    return ItemStack.EMPTY;
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

    protected boolean canSmelt(ItemStack itemStack) {
        return this.level.getCraftingManager().craft(this.recipeType, new InventorySubcontainer(itemStack), this.level).isPresent();
    }

    protected boolean isFuel(ItemStack itemStack) {
        return TileEntityFurnace.isFuel(itemStack);
    }

    public int getBurnProgress() {
        int i = this.data.getProperty(2);
        int j = this.data.getProperty(3);
        return j != 0 && i != 0 ? i * 24 / j : 0;
    }

    public int getLitProgress() {
        int i = this.data.getProperty(1);
        if (i == 0) {
            i = 200;
        }

        return this.data.getProperty(0) * 13 / i;
    }

    public boolean isLit() {
        return this.data.getProperty(0) > 0;
    }

    @Override
    public RecipeBookType getRecipeBookType() {
        return this.recipeBookType;
    }

    @Override
    public boolean shouldMoveToInventory(int index) {
        return index != 1;
    }
}

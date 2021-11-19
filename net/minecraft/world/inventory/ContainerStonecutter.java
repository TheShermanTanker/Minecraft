package net.minecraft.world.inventory;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.IInventory;
import net.minecraft.world.InventorySubcontainer;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeStonecutting;
import net.minecraft.world.item.crafting.Recipes;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Blocks;

public class ContainerStonecutter extends Container {
    public static final int INPUT_SLOT = 0;
    public static final int RESULT_SLOT = 1;
    private static final int INV_SLOT_START = 2;
    private static final int INV_SLOT_END = 29;
    private static final int USE_ROW_SLOT_START = 29;
    private static final int USE_ROW_SLOT_END = 38;
    private final ContainerAccess access;
    private final ContainerProperty selectedRecipeIndex = ContainerProperty.standalone();
    private final World level;
    private List<RecipeStonecutting> recipes = Lists.newArrayList();
    private ItemStack input = ItemStack.EMPTY;
    long lastSoundTime;
    final Slot inputSlot;
    final Slot resultSlot;
    Runnable slotUpdateListener = () -> {
    };
    public final IInventory container = new InventorySubcontainer(1) {
        @Override
        public void update() {
            super.update();
            ContainerStonecutter.this.slotsChanged(this);
            ContainerStonecutter.this.slotUpdateListener.run();
        }
    };
    final InventoryCraftResult resultContainer = new InventoryCraftResult();

    public ContainerStonecutter(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, ContainerAccess.NULL);
    }

    public ContainerStonecutter(int syncId, PlayerInventory playerInventory, ContainerAccess context) {
        super(Containers.STONECUTTER, syncId);
        this.access = context;
        this.level = playerInventory.player.level;
        this.inputSlot = this.addSlot(new Slot(this.container, 0, 20, 33));
        this.resultSlot = this.addSlot(new Slot(this.resultContainer, 1, 143, 33) {
            @Override
            public boolean isAllowed(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(EntityHuman player, ItemStack stack) {
                stack.onCraftedBy(player.level, player, stack.getCount());
                ContainerStonecutter.this.resultContainer.awardUsedRecipes(player);
                ItemStack itemStack = ContainerStonecutter.this.inputSlot.remove(1);
                if (!itemStack.isEmpty()) {
                    ContainerStonecutter.this.setupResultSlot();
                }

                context.execute((world, pos) -> {
                    long l = world.getTime();
                    if (ContainerStonecutter.this.lastSoundTime != l) {
                        world.playSound((EntityHuman)null, pos, SoundEffects.UI_STONECUTTER_TAKE_RESULT, EnumSoundCategory.BLOCKS, 1.0F, 1.0F);
                        ContainerStonecutter.this.lastSoundTime = l;
                    }

                });
                super.onTake(player, stack);
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

        this.addDataSlot(this.selectedRecipeIndex);
    }

    public int getSelectedRecipeIndex() {
        return this.selectedRecipeIndex.get();
    }

    public List<RecipeStonecutting> getRecipes() {
        return this.recipes;
    }

    public int getNumRecipes() {
        return this.recipes.size();
    }

    public boolean hasInputItem() {
        return this.inputSlot.hasItem() && !this.recipes.isEmpty();
    }

    @Override
    public boolean canUse(EntityHuman player) {
        return stillValid(this.access, player, Blocks.STONECUTTER);
    }

    @Override
    public boolean clickMenuButton(EntityHuman player, int id) {
        if (this.isValidRecipeIndex(id)) {
            this.selectedRecipeIndex.set(id);
            this.setupResultSlot();
        }

        return true;
    }

    private boolean isValidRecipeIndex(int id) {
        return id >= 0 && id < this.recipes.size();
    }

    @Override
    public void slotsChanged(IInventory inventory) {
        ItemStack itemStack = this.inputSlot.getItem();
        if (!itemStack.is(this.input.getItem())) {
            this.input = itemStack.cloneItemStack();
            this.setupRecipeList(inventory, itemStack);
        }

    }

    private void setupRecipeList(IInventory input, ItemStack stack) {
        this.recipes.clear();
        this.selectedRecipeIndex.set(-1);
        this.resultSlot.set(ItemStack.EMPTY);
        if (!stack.isEmpty()) {
            this.recipes = this.level.getCraftingManager().getRecipesFor(Recipes.STONECUTTING, input, this.level);
        }

    }

    void setupResultSlot() {
        if (!this.recipes.isEmpty() && this.isValidRecipeIndex(this.selectedRecipeIndex.get())) {
            RecipeStonecutting stonecutterRecipe = this.recipes.get(this.selectedRecipeIndex.get());
            this.resultContainer.setRecipeUsed(stonecutterRecipe);
            this.resultSlot.set(stonecutterRecipe.assemble(this.container));
        } else {
            this.resultSlot.set(ItemStack.EMPTY);
        }

        this.broadcastChanges();
    }

    @Override
    public Containers<?> getType() {
        return Containers.STONECUTTER;
    }

    public void registerUpdateListener(Runnable contentsChangedListener) {
        this.slotUpdateListener = contentsChangedListener;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != this.resultContainer && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public ItemStack shiftClick(EntityHuman player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemStack2 = slot.getItem();
            Item item = itemStack2.getItem();
            itemStack = itemStack2.cloneItemStack();
            if (index == 1) {
                item.onCraftedBy(itemStack2, player.level, player);
                if (!this.moveItemStackTo(itemStack2, 2, 38, true)) {
                    return ItemStack.EMPTY;
                }

                slot.onQuickCraft(itemStack2, itemStack);
            } else if (index == 0) {
                if (!this.moveItemStackTo(itemStack2, 2, 38, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (this.level.getCraftingManager().craft(Recipes.STONECUTTING, new InventorySubcontainer(itemStack2), this.level).isPresent()) {
                if (!this.moveItemStackTo(itemStack2, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= 2 && index < 29) {
                if (!this.moveItemStackTo(itemStack2, 29, 38, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= 29 && index < 38 && !this.moveItemStackTo(itemStack2, 2, 29, false)) {
                return ItemStack.EMPTY;
            }

            if (itemStack2.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            }

            slot.setChanged();
            if (itemStack2.getCount() == itemStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, itemStack2);
            this.broadcastChanges();
        }

        return itemStack;
    }

    @Override
    public void removed(EntityHuman player) {
        super.removed(player);
        this.resultContainer.splitWithoutUpdate(1);
        this.access.execute((world, pos) -> {
            this.clearContainer(player, this.container);
        });
    }
}

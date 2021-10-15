package net.minecraft.recipebook;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.protocol.game.PacketPlayOutAutoRecipe;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.IInventory;
import net.minecraft.world.entity.player.AutoRecipeStackManager;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.inventory.ContainerRecipeBook;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.IRecipe;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AutoRecipe<C extends IInventory> implements AutoRecipeAbstract<Integer> {
    protected static final Logger LOGGER = LogManager.getLogger();
    protected final AutoRecipeStackManager stackedContents = new AutoRecipeStackManager();
    protected PlayerInventory inventory;
    protected ContainerRecipeBook<C> menu;

    public AutoRecipe(ContainerRecipeBook<C> handler) {
        this.menu = handler;
    }

    public void recipeClicked(EntityPlayer entity, @Nullable IRecipe<C> recipe, boolean craftAll) {
        if (recipe != null && entity.getRecipeBook().contains(recipe)) {
            this.inventory = entity.getInventory();
            if (this.testClearGrid() || entity.isCreative()) {
                this.stackedContents.clear();
                entity.getInventory().fillStackedContents(this.stackedContents);
                this.menu.fillCraftSlotsStackedContents(this.stackedContents);
                if (this.stackedContents.canCraft(recipe, (IntList)null)) {
                    this.handleRecipeClicked(recipe, craftAll);
                } else {
                    this.clearGrid(true);
                    entity.connection.sendPacket(new PacketPlayOutAutoRecipe(entity.containerMenu.containerId, recipe));
                }

                entity.getInventory().update();
            }
        }
    }

    protected void clearGrid(boolean bl) {
        for(int i = 0; i < this.menu.getSize(); ++i) {
            if (this.menu.shouldMoveToInventory(i)) {
                ItemStack itemStack = this.menu.getSlot(i).getItem().cloneItemStack();
                this.inventory.placeItemBackInInventory(itemStack, false);
                this.menu.getSlot(i).set(itemStack);
            }
        }

        this.menu.clearCraftingContent();
    }

    protected void handleRecipeClicked(IRecipe<C> recipe, boolean craftAll) {
        boolean bl = this.menu.recipeMatches(recipe);
        int i = this.stackedContents.getBiggestCraftableStack(recipe, (IntList)null);
        if (bl) {
            for(int j = 0; j < this.menu.getGridHeight() * this.menu.getGridWidth() + 1; ++j) {
                if (j != this.menu.getResultSlotIndex()) {
                    ItemStack itemStack = this.menu.getSlot(j).getItem();
                    if (!itemStack.isEmpty() && Math.min(i, itemStack.getMaxStackSize()) < itemStack.getCount() + 1) {
                        return;
                    }
                }
            }
        }

        int k = this.getStackSize(craftAll, i, bl);
        IntList intList = new IntArrayList();
        if (this.stackedContents.canCraft(recipe, intList, k)) {
            int l = k;

            for(int m : intList) {
                int n = AutoRecipeStackManager.fromStackingIndex(m).getMaxStackSize();
                if (n < l) {
                    l = n;
                }
            }

            if (this.stackedContents.canCraft(recipe, intList, l)) {
                this.clearGrid(false);
                this.placeRecipe(this.menu.getGridWidth(), this.menu.getGridHeight(), this.menu.getResultSlotIndex(), recipe, intList.iterator(), l);
            }
        }

    }

    @Override
    public void addItemToSlot(Iterator<Integer> inputs, int slot, int amount, int gridX, int gridY) {
        Slot slot2 = this.menu.getSlot(slot);
        ItemStack itemStack = AutoRecipeStackManager.fromStackingIndex(inputs.next());
        if (!itemStack.isEmpty()) {
            for(int i = 0; i < amount; ++i) {
                this.moveItemToGrid(slot2, itemStack);
            }
        }

    }

    protected int getStackSize(boolean craftAll, int limit, boolean recipeInCraftingSlots) {
        int i = 1;
        if (craftAll) {
            i = limit;
        } else if (recipeInCraftingSlots) {
            i = 64;

            for(int j = 0; j < this.menu.getGridWidth() * this.menu.getGridHeight() + 1; ++j) {
                if (j != this.menu.getResultSlotIndex()) {
                    ItemStack itemStack = this.menu.getSlot(j).getItem();
                    if (!itemStack.isEmpty() && i > itemStack.getCount()) {
                        i = itemStack.getCount();
                    }
                }
            }

            if (i < 64) {
                ++i;
            }
        }

        return i;
    }

    protected void moveItemToGrid(Slot slot, ItemStack stack) {
        int i = this.inventory.findSlotMatchingUnusedItem(stack);
        if (i != -1) {
            ItemStack itemStack = this.inventory.getItem(i).cloneItemStack();
            if (!itemStack.isEmpty()) {
                if (itemStack.getCount() > 1) {
                    this.inventory.splitStack(i, 1);
                } else {
                    this.inventory.splitWithoutUpdate(i);
                }

                itemStack.setCount(1);
                if (slot.getItem().isEmpty()) {
                    slot.set(itemStack);
                } else {
                    slot.getItem().add(1);
                }

            }
        }
    }

    private boolean testClearGrid() {
        List<ItemStack> list = Lists.newArrayList();
        int i = this.getAmountOfFreeSlotsInInventory();

        for(int j = 0; j < this.menu.getGridWidth() * this.menu.getGridHeight() + 1; ++j) {
            if (j != this.menu.getResultSlotIndex()) {
                ItemStack itemStack = this.menu.getSlot(j).getItem().cloneItemStack();
                if (!itemStack.isEmpty()) {
                    int k = this.inventory.firstPartial(itemStack);
                    if (k == -1 && list.size() <= i) {
                        for(ItemStack itemStack2 : list) {
                            if (itemStack2.doMaterialsMatch(itemStack) && itemStack2.getCount() != itemStack2.getMaxStackSize() && itemStack2.getCount() + itemStack.getCount() <= itemStack2.getMaxStackSize()) {
                                itemStack2.add(itemStack.getCount());
                                itemStack.setCount(0);
                                break;
                            }
                        }

                        if (!itemStack.isEmpty()) {
                            if (list.size() >= i) {
                                return false;
                            }

                            list.add(itemStack);
                        }
                    } else if (k == -1) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private int getAmountOfFreeSlotsInInventory() {
        int i = 0;

        for(ItemStack itemStack : this.inventory.items) {
            if (itemStack.isEmpty()) {
                ++i;
            }
        }

        return i;
    }
}

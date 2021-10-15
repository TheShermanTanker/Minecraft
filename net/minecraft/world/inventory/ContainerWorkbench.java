package net.minecraft.world.inventory;

import java.util.Optional;
import net.minecraft.network.protocol.game.PacketPlayOutSetSlot;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.IInventory;
import net.minecraft.world.entity.player.AutoRecipeStackManager;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.IRecipe;
import net.minecraft.world.item.crafting.RecipeCrafting;
import net.minecraft.world.item.crafting.Recipes;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Blocks;

public class ContainerWorkbench extends ContainerRecipeBook<InventoryCrafting> {
    public static final int RESULT_SLOT = 0;
    private static final int CRAFT_SLOT_START = 1;
    private static final int CRAFT_SLOT_END = 10;
    private static final int INV_SLOT_START = 10;
    private static final int INV_SLOT_END = 37;
    private static final int USE_ROW_SLOT_START = 37;
    private static final int USE_ROW_SLOT_END = 46;
    private final InventoryCrafting craftSlots = new InventoryCrafting(this, 3, 3);
    private final InventoryCraftResult resultSlots = new InventoryCraftResult();
    public final ContainerAccess access;
    private final EntityHuman player;

    public ContainerWorkbench(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, ContainerAccess.NULL);
    }

    public ContainerWorkbench(int syncId, PlayerInventory playerInventory, ContainerAccess context) {
        super(Containers.CRAFTING, syncId);
        this.access = context;
        this.player = playerInventory.player;
        this.addSlot(new SlotResult(playerInventory.player, this.craftSlots, this.resultSlots, 0, 124, 35));

        for(int i = 0; i < 3; ++i) {
            for(int j = 0; j < 3; ++j) {
                this.addSlot(new Slot(this.craftSlots, j + i * 3, 30 + j * 18, 17 + i * 18));
            }
        }

        for(int k = 0; k < 3; ++k) {
            for(int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + k * 9 + 9, 8 + l * 18, 84 + k * 18));
            }
        }

        for(int m = 0; m < 9; ++m) {
            this.addSlot(new Slot(playerInventory, m, 8 + m * 18, 142));
        }

    }

    protected static void slotChangedCraftingGrid(Container handler, World world, EntityHuman player, InventoryCrafting craftingInventory, InventoryCraftResult resultInventory) {
        if (!world.isClientSide) {
            EntityPlayer serverPlayer = (EntityPlayer)player;
            ItemStack itemStack = ItemStack.EMPTY;
            Optional<RecipeCrafting> optional = world.getMinecraftServer().getCraftingManager().craft(Recipes.CRAFTING, craftingInventory, world);
            if (optional.isPresent()) {
                RecipeCrafting craftingRecipe = optional.get();
                if (resultInventory.setRecipeUsed(world, serverPlayer, craftingRecipe)) {
                    itemStack = craftingRecipe.assemble(craftingInventory);
                }
            }

            resultInventory.setItem(0, itemStack);
            handler.setRemoteSlot(0, itemStack);
            serverPlayer.connection.sendPacket(new PacketPlayOutSetSlot(handler.containerId, handler.incrementStateId(), 0, itemStack));
        }
    }

    @Override
    public void slotsChanged(IInventory inventory) {
        this.access.execute((world, pos) -> {
            slotChangedCraftingGrid(this, world, this.player, this.craftSlots, this.resultSlots);
        });
    }

    @Override
    public void fillCraftSlotsStackedContents(AutoRecipeStackManager finder) {
        this.craftSlots.fillStackedContents(finder);
    }

    @Override
    public void clearCraftingContent() {
        this.craftSlots.clear();
        this.resultSlots.clear();
    }

    @Override
    public boolean recipeMatches(IRecipe<? super InventoryCrafting> recipe) {
        return recipe.matches(this.craftSlots, this.player.level);
    }

    @Override
    public void removed(EntityHuman player) {
        super.removed(player);
        this.access.execute((world, pos) -> {
            this.clearContainer(player, this.craftSlots);
        });
    }

    @Override
    public boolean canUse(EntityHuman player) {
        return stillValid(this.access, player, Blocks.CRAFTING_TABLE);
    }

    @Override
    public ItemStack shiftClick(EntityHuman player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemStack2 = slot.getItem();
            itemStack = itemStack2.cloneItemStack();
            if (index == 0) {
                this.access.execute((world, pos) -> {
                    itemStack2.getItem().onCraftedBy(itemStack2, world, player);
                });
                if (!this.moveItemStackTo(itemStack2, 10, 46, true)) {
                    return ItemStack.EMPTY;
                }

                slot.onQuickCraft(itemStack2, itemStack);
            } else if (index >= 10 && index < 46) {
                if (!this.moveItemStackTo(itemStack2, 1, 10, false)) {
                    if (index < 37) {
                        if (!this.moveItemStackTo(itemStack2, 37, 46, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (!this.moveItemStackTo(itemStack2, 10, 37, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            } else if (!this.moveItemStackTo(itemStack2, 10, 46, false)) {
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
            if (index == 0) {
                player.drop(itemStack2, false);
            }
        }

        return itemStack;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != this.resultSlots && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public int getResultSlotIndex() {
        return 0;
    }

    @Override
    public int getGridWidth() {
        return this.craftSlots.getWidth();
    }

    @Override
    public int getGridHeight() {
        return this.craftSlots.getHeight();
    }

    @Override
    public int getSize() {
        return 10;
    }

    @Override
    public RecipeBookType getRecipeBookType() {
        return RecipeBookType.CRAFTING;
    }

    @Override
    public boolean shouldMoveToInventory(int index) {
        return index != this.getResultSlotIndex();
    }
}

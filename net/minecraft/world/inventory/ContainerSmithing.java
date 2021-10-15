package net.minecraft.world.inventory;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSmithing;
import net.minecraft.world.item.crafting.Recipes;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;

public class ContainerSmithing extends ContainerAnvilAbstract {
    private final World level;
    @Nullable
    private RecipeSmithing selectedRecipe;
    private final List<RecipeSmithing> recipes;

    public ContainerSmithing(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, ContainerAccess.NULL);
    }

    public ContainerSmithing(int syncId, PlayerInventory playerInventory, ContainerAccess context) {
        super(Containers.SMITHING, syncId, playerInventory, context);
        this.level = playerInventory.player.level;
        this.recipes = this.level.getCraftingManager().getAllRecipesFor(Recipes.SMITHING);
    }

    @Override
    protected boolean isValidBlock(IBlockData state) {
        return state.is(Blocks.SMITHING_TABLE);
    }

    @Override
    protected boolean mayPickup(EntityHuman player, boolean present) {
        return this.selectedRecipe != null && this.selectedRecipe.matches(this.inputSlots, this.level);
    }

    @Override
    protected void onTake(EntityHuman player, ItemStack stack) {
        stack.onCraftedBy(player.level, player, stack.getCount());
        this.resultSlots.awardUsedRecipes(player);
        this.shrinkStackInSlot(0);
        this.shrinkStackInSlot(1);
        this.access.execute((world, pos) -> {
            world.triggerEffect(1044, pos, 0);
        });
    }

    private void shrinkStackInSlot(int slot) {
        ItemStack itemStack = this.inputSlots.getItem(slot);
        itemStack.subtract(1);
        this.inputSlots.setItem(slot, itemStack);
    }

    @Override
    public void createResult() {
        List<RecipeSmithing> list = this.level.getCraftingManager().getRecipesFor(Recipes.SMITHING, this.inputSlots, this.level);
        if (list.isEmpty()) {
            this.resultSlots.setItem(0, ItemStack.EMPTY);
        } else {
            this.selectedRecipe = list.get(0);
            ItemStack itemStack = this.selectedRecipe.assemble(this.inputSlots);
            this.resultSlots.setRecipeUsed(this.selectedRecipe);
            this.resultSlots.setItem(0, itemStack);
        }

    }

    @Override
    protected boolean shouldQuickMoveToAdditionalSlot(ItemStack stack) {
        return this.recipes.stream().anyMatch((recipe) -> {
            return recipe.isAdditionIngredient(stack);
        });
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != this.resultSlots && super.canTakeItemForPickAll(stack, slot);
    }
}

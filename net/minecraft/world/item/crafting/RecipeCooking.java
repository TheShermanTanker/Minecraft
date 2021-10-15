package net.minecraft.world.item.crafting;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.IInventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.World;

public abstract class RecipeCooking implements IRecipe<IInventory> {
    protected final Recipes<?> type;
    protected final MinecraftKey id;
    protected final String group;
    protected final RecipeItemStack ingredient;
    protected final ItemStack result;
    protected final float experience;
    protected final int cookingTime;

    public RecipeCooking(Recipes<?> type, MinecraftKey id, String group, RecipeItemStack input, ItemStack output, float experience, int cookTime) {
        this.type = type;
        this.id = id;
        this.group = group;
        this.ingredient = input;
        this.result = output;
        this.experience = experience;
        this.cookingTime = cookTime;
    }

    @Override
    public boolean matches(IInventory inventory, World world) {
        return this.ingredient.test(inventory.getItem(0));
    }

    @Override
    public ItemStack assemble(IInventory inventory) {
        return this.result.cloneItemStack();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public NonNullList<RecipeItemStack> getIngredients() {
        NonNullList<RecipeItemStack> nonNullList = NonNullList.create();
        nonNullList.add(this.ingredient);
        return nonNullList;
    }

    public float getExperience() {
        return this.experience;
    }

    @Override
    public ItemStack getResult() {
        return this.result;
    }

    @Override
    public String getGroup() {
        return this.group;
    }

    public int getCookingTime() {
        return this.cookingTime;
    }

    @Override
    public MinecraftKey getKey() {
        return this.id;
    }

    @Override
    public Recipes<?> getType() {
        return this.type;
    }
}

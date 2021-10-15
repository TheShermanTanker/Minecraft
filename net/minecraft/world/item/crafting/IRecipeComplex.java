package net.minecraft.world.item.crafting;

import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.item.ItemStack;

public abstract class IRecipeComplex implements RecipeCrafting {
    private final MinecraftKey id;

    public IRecipeComplex(MinecraftKey id) {
        this.id = id;
    }

    @Override
    public MinecraftKey getKey() {
        return this.id;
    }

    @Override
    public boolean isComplex() {
        return true;
    }

    @Override
    public ItemStack getResult() {
        return ItemStack.EMPTY;
    }
}

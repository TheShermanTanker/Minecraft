package net.minecraft.world.item.crafting;

import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.inventory.InventoryCrafting;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtil;
import net.minecraft.world.level.World;

public class RecipeTippedArrow extends IRecipeComplex {
    public RecipeTippedArrow(MinecraftKey id) {
        super(id);
    }

    @Override
    public boolean matches(InventoryCrafting inventory, World world) {
        if (inventory.getWidth() == 3 && inventory.getHeight() == 3) {
            for(int i = 0; i < inventory.getWidth(); ++i) {
                for(int j = 0; j < inventory.getHeight(); ++j) {
                    ItemStack itemStack = inventory.getItem(i + j * inventory.getWidth());
                    if (itemStack.isEmpty()) {
                        return false;
                    }

                    if (i == 1 && j == 1) {
                        if (!itemStack.is(Items.LINGERING_POTION)) {
                            return false;
                        }
                    } else if (!itemStack.is(Items.ARROW)) {
                        return false;
                    }
                }
            }

            return true;
        } else {
            return false;
        }
    }

    @Override
    public ItemStack assemble(InventoryCrafting inventory) {
        ItemStack itemStack = inventory.getItem(1 + inventory.getWidth());
        if (!itemStack.is(Items.LINGERING_POTION)) {
            return ItemStack.EMPTY;
        } else {
            ItemStack itemStack2 = new ItemStack(Items.TIPPED_ARROW, 8);
            PotionUtil.setPotion(itemStack2, PotionUtil.getPotion(itemStack));
            PotionUtil.setCustomEffects(itemStack2, PotionUtil.getCustomEffects(itemStack));
            return itemStack2;
        }
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 2 && height >= 2;
    }

    @Override
    public RecipeSerializer<?> getRecipeSerializer() {
        return RecipeSerializer.TIPPED_ARROW;
    }
}

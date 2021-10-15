package net.minecraft.world.item.crafting;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.inventory.InventoryCrafting;
import net.minecraft.world.item.EnumColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemBanner;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.TileEntityBanner;

public class RecipeBannerDuplicate extends IRecipeComplex {
    public RecipeBannerDuplicate(MinecraftKey id) {
        super(id);
    }

    @Override
    public boolean matches(InventoryCrafting inventory, World world) {
        EnumColor dyeColor = null;
        ItemStack itemStack = null;
        ItemStack itemStack2 = null;

        for(int i = 0; i < inventory.getSize(); ++i) {
            ItemStack itemStack3 = inventory.getItem(i);
            if (!itemStack3.isEmpty()) {
                Item item = itemStack3.getItem();
                if (!(item instanceof ItemBanner)) {
                    return false;
                }

                ItemBanner bannerItem = (ItemBanner)item;
                if (dyeColor == null) {
                    dyeColor = bannerItem.getColor();
                } else if (dyeColor != bannerItem.getColor()) {
                    return false;
                }

                int j = TileEntityBanner.getPatternCount(itemStack3);
                if (j > 6) {
                    return false;
                }

                if (j > 0) {
                    if (itemStack != null) {
                        return false;
                    }

                    itemStack = itemStack3;
                } else {
                    if (itemStack2 != null) {
                        return false;
                    }

                    itemStack2 = itemStack3;
                }
            }
        }

        return itemStack != null && itemStack2 != null;
    }

    @Override
    public ItemStack assemble(InventoryCrafting inventory) {
        for(int i = 0; i < inventory.getSize(); ++i) {
            ItemStack itemStack = inventory.getItem(i);
            if (!itemStack.isEmpty()) {
                int j = TileEntityBanner.getPatternCount(itemStack);
                if (j > 0 && j <= 6) {
                    ItemStack itemStack2 = itemStack.cloneItemStack();
                    itemStack2.setCount(1);
                    return itemStack2;
                }
            }
        }

        return ItemStack.EMPTY;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inventory) {
        NonNullList<ItemStack> nonNullList = NonNullList.withSize(inventory.getSize(), ItemStack.EMPTY);

        for(int i = 0; i < nonNullList.size(); ++i) {
            ItemStack itemStack = inventory.getItem(i);
            if (!itemStack.isEmpty()) {
                if (itemStack.getItem().hasCraftingRemainingItem()) {
                    nonNullList.set(i, new ItemStack(itemStack.getItem().getCraftingRemainingItem()));
                } else if (itemStack.hasTag() && TileEntityBanner.getPatternCount(itemStack) > 0) {
                    ItemStack itemStack2 = itemStack.cloneItemStack();
                    itemStack2.setCount(1);
                    nonNullList.set(i, itemStack2);
                }
            }
        }

        return nonNullList;
    }

    @Override
    public RecipeSerializer<?> getRecipeSerializer() {
        return RecipeSerializer.BANNER_DUPLICATE;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }
}

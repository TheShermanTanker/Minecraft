package net.minecraft.world.item.crafting;

import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.inventory.InventoryCrafting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDye;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockShulkerBox;

public class RecipeShulkerBox extends IRecipeComplex {
    public RecipeShulkerBox(MinecraftKey id) {
        super(id);
    }

    @Override
    public boolean matches(InventoryCrafting inventory, World world) {
        int i = 0;
        int j = 0;

        for(int k = 0; k < inventory.getSize(); ++k) {
            ItemStack itemStack = inventory.getItem(k);
            if (!itemStack.isEmpty()) {
                if (Block.asBlock(itemStack.getItem()) instanceof BlockShulkerBox) {
                    ++i;
                } else {
                    if (!(itemStack.getItem() instanceof ItemDye)) {
                        return false;
                    }

                    ++j;
                }

                if (j > 1 || i > 1) {
                    return false;
                }
            }
        }

        return i == 1 && j == 1;
    }

    @Override
    public ItemStack assemble(InventoryCrafting inventory) {
        ItemStack itemStack = ItemStack.EMPTY;
        ItemDye dyeItem = (ItemDye)Items.WHITE_DYE;

        for(int i = 0; i < inventory.getSize(); ++i) {
            ItemStack itemStack2 = inventory.getItem(i);
            if (!itemStack2.isEmpty()) {
                Item item = itemStack2.getItem();
                if (Block.asBlock(item) instanceof BlockShulkerBox) {
                    itemStack = itemStack2;
                } else if (item instanceof ItemDye) {
                    dyeItem = (ItemDye)item;
                }
            }
        }

        ItemStack itemStack3 = BlockShulkerBox.getColoredItemStack(dyeItem.getDyeColor());
        if (itemStack.hasTag()) {
            itemStack3.setTag(itemStack.getTag().c());
        }

        return itemStack3;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getRecipeSerializer() {
        return RecipeSerializer.SHULKER_BOX_COLORING;
    }
}

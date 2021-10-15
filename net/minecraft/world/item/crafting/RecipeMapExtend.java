package net.minecraft.world.item.crafting;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.inventory.InventoryCrafting;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemWorldMap;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.World;
import net.minecraft.world.level.saveddata.maps.WorldMap;

public class RecipeMapExtend extends ShapedRecipes {
    public RecipeMapExtend(MinecraftKey id) {
        super(id, "", 3, 3, NonNullList.of(RecipeItemStack.EMPTY, RecipeItemStack.of(Items.PAPER), RecipeItemStack.of(Items.PAPER), RecipeItemStack.of(Items.PAPER), RecipeItemStack.of(Items.PAPER), RecipeItemStack.of(Items.FILLED_MAP), RecipeItemStack.of(Items.PAPER), RecipeItemStack.of(Items.PAPER), RecipeItemStack.of(Items.PAPER), RecipeItemStack.of(Items.PAPER)), new ItemStack(Items.MAP));
    }

    @Override
    public boolean matches(InventoryCrafting inventory, World world) {
        if (!super.matches(inventory, world)) {
            return false;
        } else {
            ItemStack itemStack = ItemStack.EMPTY;

            for(int i = 0; i < inventory.getSize() && itemStack.isEmpty(); ++i) {
                ItemStack itemStack2 = inventory.getItem(i);
                if (itemStack2.is(Items.FILLED_MAP)) {
                    itemStack = itemStack2;
                }
            }

            if (itemStack.isEmpty()) {
                return false;
            } else {
                WorldMap mapItemSavedData = ItemWorldMap.getSavedMap(itemStack, world);
                if (mapItemSavedData == null) {
                    return false;
                } else if (mapItemSavedData.isExplorationMap()) {
                    return false;
                } else {
                    return mapItemSavedData.scale < 4;
                }
            }
        }
    }

    @Override
    public ItemStack assemble(InventoryCrafting inventory) {
        ItemStack itemStack = ItemStack.EMPTY;

        for(int i = 0; i < inventory.getSize() && itemStack.isEmpty(); ++i) {
            ItemStack itemStack2 = inventory.getItem(i);
            if (itemStack2.is(Items.FILLED_MAP)) {
                itemStack = itemStack2;
            }
        }

        itemStack = itemStack.cloneItemStack();
        itemStack.setCount(1);
        itemStack.getOrCreateTag().setInt("map_scale_direction", 1);
        return itemStack;
    }

    @Override
    public boolean isComplex() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getRecipeSerializer() {
        return RecipeSerializer.MAP_EXTENDING;
    }
}

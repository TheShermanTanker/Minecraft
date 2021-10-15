package net.minecraft.world.item.crafting;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.inventory.InventoryCrafting;
import net.minecraft.world.item.IDyeable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDye;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.World;

public class RecipeArmorDye extends IRecipeComplex {
    public RecipeArmorDye(MinecraftKey id) {
        super(id);
    }

    @Override
    public boolean matches(InventoryCrafting inventory, World world) {
        ItemStack itemStack = ItemStack.EMPTY;
        List<ItemStack> list = Lists.newArrayList();

        for(int i = 0; i < inventory.getSize(); ++i) {
            ItemStack itemStack2 = inventory.getItem(i);
            if (!itemStack2.isEmpty()) {
                if (itemStack2.getItem() instanceof IDyeable) {
                    if (!itemStack.isEmpty()) {
                        return false;
                    }

                    itemStack = itemStack2;
                } else {
                    if (!(itemStack2.getItem() instanceof ItemDye)) {
                        return false;
                    }

                    list.add(itemStack2);
                }
            }
        }

        return !itemStack.isEmpty() && !list.isEmpty();
    }

    @Override
    public ItemStack assemble(InventoryCrafting inventory) {
        List<ItemDye> list = Lists.newArrayList();
        ItemStack itemStack = ItemStack.EMPTY;

        for(int i = 0; i < inventory.getSize(); ++i) {
            ItemStack itemStack2 = inventory.getItem(i);
            if (!itemStack2.isEmpty()) {
                Item item = itemStack2.getItem();
                if (item instanceof IDyeable) {
                    if (!itemStack.isEmpty()) {
                        return ItemStack.EMPTY;
                    }

                    itemStack = itemStack2.cloneItemStack();
                } else {
                    if (!(item instanceof ItemDye)) {
                        return ItemStack.EMPTY;
                    }

                    list.add((ItemDye)item);
                }
            }
        }

        return !itemStack.isEmpty() && !list.isEmpty() ? IDyeable.dyeArmor(itemStack, list) : ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getRecipeSerializer() {
        return RecipeSerializer.ARMOR_DYE;
    }
}

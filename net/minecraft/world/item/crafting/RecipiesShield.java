package net.minecraft.world.item.crafting;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.inventory.InventoryCrafting;
import net.minecraft.world.item.ItemBanner;
import net.minecraft.world.item.ItemBlock;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.entity.TileEntityTypes;

public class RecipiesShield extends IRecipeComplex {
    public RecipiesShield(MinecraftKey id) {
        super(id);
    }

    @Override
    public boolean matches(InventoryCrafting inventory, World world) {
        ItemStack itemStack = ItemStack.EMPTY;
        ItemStack itemStack2 = ItemStack.EMPTY;

        for(int i = 0; i < inventory.getSize(); ++i) {
            ItemStack itemStack3 = inventory.getItem(i);
            if (!itemStack3.isEmpty()) {
                if (itemStack3.getItem() instanceof ItemBanner) {
                    if (!itemStack2.isEmpty()) {
                        return false;
                    }

                    itemStack2 = itemStack3;
                } else {
                    if (!itemStack3.is(Items.SHIELD)) {
                        return false;
                    }

                    if (!itemStack.isEmpty()) {
                        return false;
                    }

                    if (ItemBlock.getBlockEntityData(itemStack3) != null) {
                        return false;
                    }

                    itemStack = itemStack3;
                }
            }
        }

        return !itemStack.isEmpty() && !itemStack2.isEmpty();
    }

    @Override
    public ItemStack assemble(InventoryCrafting inventory) {
        ItemStack itemStack = ItemStack.EMPTY;
        ItemStack itemStack2 = ItemStack.EMPTY;

        for(int i = 0; i < inventory.getSize(); ++i) {
            ItemStack itemStack3 = inventory.getItem(i);
            if (!itemStack3.isEmpty()) {
                if (itemStack3.getItem() instanceof ItemBanner) {
                    itemStack = itemStack3;
                } else if (itemStack3.is(Items.SHIELD)) {
                    itemStack2 = itemStack3.cloneItemStack();
                }
            }
        }

        if (itemStack2.isEmpty()) {
            return itemStack2;
        } else {
            NBTTagCompound compoundTag = ItemBlock.getBlockEntityData(itemStack);
            NBTTagCompound compoundTag2 = compoundTag == null ? new NBTTagCompound() : compoundTag.copy();
            compoundTag2.setInt("Base", ((ItemBanner)itemStack.getItem()).getColor().getColorIndex());
            ItemBlock.setBlockEntityData(itemStack2, TileEntityTypes.BANNER, compoundTag2);
            return itemStack2;
        }
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getRecipeSerializer() {
        return RecipeSerializer.SHIELD_DECORATION;
    }
}

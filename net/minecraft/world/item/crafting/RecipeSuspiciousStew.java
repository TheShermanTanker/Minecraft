package net.minecraft.world.item.crafting;

import net.minecraft.resources.MinecraftKey;
import net.minecraft.tags.TagsItem;
import net.minecraft.world.effect.MobEffectList;
import net.minecraft.world.inventory.InventoryCrafting;
import net.minecraft.world.item.ItemBlock;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemSuspiciousStew;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.BlockFlowers;
import net.minecraft.world.level.block.Blocks;

public class RecipeSuspiciousStew extends IRecipeComplex {
    public RecipeSuspiciousStew(MinecraftKey id) {
        super(id);
    }

    @Override
    public boolean matches(InventoryCrafting inventory, World world) {
        boolean bl = false;
        boolean bl2 = false;
        boolean bl3 = false;
        boolean bl4 = false;

        for(int i = 0; i < inventory.getSize(); ++i) {
            ItemStack itemStack = inventory.getItem(i);
            if (!itemStack.isEmpty()) {
                if (itemStack.is(Blocks.BROWN_MUSHROOM.getItem()) && !bl3) {
                    bl3 = true;
                } else if (itemStack.is(Blocks.RED_MUSHROOM.getItem()) && !bl2) {
                    bl2 = true;
                } else if (itemStack.is(TagsItem.SMALL_FLOWERS) && !bl) {
                    bl = true;
                } else {
                    if (!itemStack.is(Items.BOWL) || bl4) {
                        return false;
                    }

                    bl4 = true;
                }
            }
        }

        return bl && bl3 && bl2 && bl4;
    }

    @Override
    public ItemStack assemble(InventoryCrafting inventory) {
        ItemStack itemStack = ItemStack.EMPTY;

        for(int i = 0; i < inventory.getSize(); ++i) {
            ItemStack itemStack2 = inventory.getItem(i);
            if (!itemStack2.isEmpty() && itemStack2.is(TagsItem.SMALL_FLOWERS)) {
                itemStack = itemStack2;
                break;
            }
        }

        ItemStack itemStack3 = new ItemStack(Items.SUSPICIOUS_STEW, 1);
        if (itemStack.getItem() instanceof ItemBlock && ((ItemBlock)itemStack.getItem()).getBlock() instanceof BlockFlowers) {
            BlockFlowers flowerBlock = (BlockFlowers)((ItemBlock)itemStack.getItem()).getBlock();
            MobEffectList mobEffect = flowerBlock.getSuspiciousStewEffect();
            ItemSuspiciousStew.saveMobEffect(itemStack3, mobEffect, flowerBlock.getEffectDuration());
        }

        return itemStack3;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 2 && height >= 2;
    }

    @Override
    public RecipeSerializer<?> getRecipeSerializer() {
        return RecipeSerializer.SUSPICIOUS_STEW;
    }
}

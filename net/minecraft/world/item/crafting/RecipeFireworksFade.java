package net.minecraft.world.item.crafting;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.inventory.InventoryCrafting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDye;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.World;

public class RecipeFireworksFade extends IRecipeComplex {
    private static final RecipeItemStack STAR_INGREDIENT = RecipeItemStack.of(Items.FIREWORK_STAR);

    public RecipeFireworksFade(MinecraftKey id) {
        super(id);
    }

    @Override
    public boolean matches(InventoryCrafting inventory, World world) {
        boolean bl = false;
        boolean bl2 = false;

        for(int i = 0; i < inventory.getSize(); ++i) {
            ItemStack itemStack = inventory.getItem(i);
            if (!itemStack.isEmpty()) {
                if (itemStack.getItem() instanceof ItemDye) {
                    bl = true;
                } else {
                    if (!STAR_INGREDIENT.test(itemStack)) {
                        return false;
                    }

                    if (bl2) {
                        return false;
                    }

                    bl2 = true;
                }
            }
        }

        return bl2 && bl;
    }

    @Override
    public ItemStack assemble(InventoryCrafting inventory) {
        List<Integer> list = Lists.newArrayList();
        ItemStack itemStack = null;

        for(int i = 0; i < inventory.getSize(); ++i) {
            ItemStack itemStack2 = inventory.getItem(i);
            Item item = itemStack2.getItem();
            if (item instanceof ItemDye) {
                list.add(((ItemDye)item).getDyeColor().getFireworksColor());
            } else if (STAR_INGREDIENT.test(itemStack2)) {
                itemStack = itemStack2.cloneItemStack();
                itemStack.setCount(1);
            }
        }

        if (itemStack != null && !list.isEmpty()) {
            itemStack.getOrCreateTagElement("Explosion").putIntArray("FadeColors", list);
            return itemStack;
        } else {
            return ItemStack.EMPTY;
        }
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getRecipeSerializer() {
        return RecipeSerializer.FIREWORK_STAR_FADE;
    }
}

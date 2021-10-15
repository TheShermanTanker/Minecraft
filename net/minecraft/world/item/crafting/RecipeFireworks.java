package net.minecraft.world.item.crafting;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.inventory.InventoryCrafting;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.World;

public class RecipeFireworks extends IRecipeComplex {
    private static final RecipeItemStack PAPER_INGREDIENT = RecipeItemStack.of(Items.PAPER);
    private static final RecipeItemStack GUNPOWDER_INGREDIENT = RecipeItemStack.of(Items.GUNPOWDER);
    private static final RecipeItemStack STAR_INGREDIENT = RecipeItemStack.of(Items.FIREWORK_STAR);

    public RecipeFireworks(MinecraftKey id) {
        super(id);
    }

    @Override
    public boolean matches(InventoryCrafting inventory, World world) {
        boolean bl = false;
        int i = 0;

        for(int j = 0; j < inventory.getSize(); ++j) {
            ItemStack itemStack = inventory.getItem(j);
            if (!itemStack.isEmpty()) {
                if (PAPER_INGREDIENT.test(itemStack)) {
                    if (bl) {
                        return false;
                    }

                    bl = true;
                } else if (GUNPOWDER_INGREDIENT.test(itemStack)) {
                    ++i;
                    if (i > 3) {
                        return false;
                    }
                } else if (!STAR_INGREDIENT.test(itemStack)) {
                    return false;
                }
            }
        }

        return bl && i >= 1;
    }

    @Override
    public ItemStack assemble(InventoryCrafting inventory) {
        ItemStack itemStack = new ItemStack(Items.FIREWORK_ROCKET, 3);
        NBTTagCompound compoundTag = itemStack.getOrCreateTagElement("Fireworks");
        NBTTagList listTag = new NBTTagList();
        int i = 0;

        for(int j = 0; j < inventory.getSize(); ++j) {
            ItemStack itemStack2 = inventory.getItem(j);
            if (!itemStack2.isEmpty()) {
                if (GUNPOWDER_INGREDIENT.test(itemStack2)) {
                    ++i;
                } else if (STAR_INGREDIENT.test(itemStack2)) {
                    NBTTagCompound compoundTag2 = itemStack2.getTagElement("Explosion");
                    if (compoundTag2 != null) {
                        listTag.add(compoundTag2);
                    }
                }
            }
        }

        compoundTag.setByte("Flight", (byte)i);
        if (!listTag.isEmpty()) {
            compoundTag.set("Explosions", listTag);
        }

        return itemStack;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getResult() {
        return new ItemStack(Items.FIREWORK_ROCKET);
    }

    @Override
    public RecipeSerializer<?> getRecipeSerializer() {
        return RecipeSerializer.FIREWORK_ROCKET;
    }
}

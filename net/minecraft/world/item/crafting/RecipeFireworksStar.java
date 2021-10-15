package net.minecraft.world.item.crafting;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import net.minecraft.SystemUtils;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.world.inventory.InventoryCrafting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDye;
import net.minecraft.world.item.ItemFireworks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.World;

public class RecipeFireworksStar extends IRecipeComplex {
    private static final RecipeItemStack SHAPE_INGREDIENT = RecipeItemStack.of(Items.FIRE_CHARGE, Items.FEATHER, Items.GOLD_NUGGET, Items.SKELETON_SKULL, Items.WITHER_SKELETON_SKULL, Items.CREEPER_HEAD, Items.PLAYER_HEAD, Items.DRAGON_HEAD, Items.ZOMBIE_HEAD);
    private static final RecipeItemStack TRAIL_INGREDIENT = RecipeItemStack.of(Items.DIAMOND);
    private static final RecipeItemStack FLICKER_INGREDIENT = RecipeItemStack.of(Items.GLOWSTONE_DUST);
    private static final Map<Item, ItemFireworks.EffectType> SHAPE_BY_ITEM = SystemUtils.make(Maps.newHashMap(), (hashMap) -> {
        hashMap.put(Items.FIRE_CHARGE, ItemFireworks.EffectType.LARGE_BALL);
        hashMap.put(Items.FEATHER, ItemFireworks.EffectType.BURST);
        hashMap.put(Items.GOLD_NUGGET, ItemFireworks.EffectType.STAR);
        hashMap.put(Items.SKELETON_SKULL, ItemFireworks.EffectType.CREEPER);
        hashMap.put(Items.WITHER_SKELETON_SKULL, ItemFireworks.EffectType.CREEPER);
        hashMap.put(Items.CREEPER_HEAD, ItemFireworks.EffectType.CREEPER);
        hashMap.put(Items.PLAYER_HEAD, ItemFireworks.EffectType.CREEPER);
        hashMap.put(Items.DRAGON_HEAD, ItemFireworks.EffectType.CREEPER);
        hashMap.put(Items.ZOMBIE_HEAD, ItemFireworks.EffectType.CREEPER);
    });
    private static final RecipeItemStack GUNPOWDER_INGREDIENT = RecipeItemStack.of(Items.GUNPOWDER);

    public RecipeFireworksStar(MinecraftKey id) {
        super(id);
    }

    @Override
    public boolean matches(InventoryCrafting inventory, World world) {
        boolean bl = false;
        boolean bl2 = false;
        boolean bl3 = false;
        boolean bl4 = false;
        boolean bl5 = false;

        for(int i = 0; i < inventory.getSize(); ++i) {
            ItemStack itemStack = inventory.getItem(i);
            if (!itemStack.isEmpty()) {
                if (SHAPE_INGREDIENT.test(itemStack)) {
                    if (bl3) {
                        return false;
                    }

                    bl3 = true;
                } else if (FLICKER_INGREDIENT.test(itemStack)) {
                    if (bl5) {
                        return false;
                    }

                    bl5 = true;
                } else if (TRAIL_INGREDIENT.test(itemStack)) {
                    if (bl4) {
                        return false;
                    }

                    bl4 = true;
                } else if (GUNPOWDER_INGREDIENT.test(itemStack)) {
                    if (bl) {
                        return false;
                    }

                    bl = true;
                } else {
                    if (!(itemStack.getItem() instanceof ItemDye)) {
                        return false;
                    }

                    bl2 = true;
                }
            }
        }

        return bl && bl2;
    }

    @Override
    public ItemStack assemble(InventoryCrafting inventory) {
        ItemStack itemStack = new ItemStack(Items.FIREWORK_STAR);
        NBTTagCompound compoundTag = itemStack.getOrCreateTagElement("Explosion");
        ItemFireworks.EffectType shape = ItemFireworks.EffectType.SMALL_BALL;
        List<Integer> list = Lists.newArrayList();

        for(int i = 0; i < inventory.getSize(); ++i) {
            ItemStack itemStack2 = inventory.getItem(i);
            if (!itemStack2.isEmpty()) {
                if (SHAPE_INGREDIENT.test(itemStack2)) {
                    shape = SHAPE_BY_ITEM.get(itemStack2.getItem());
                } else if (FLICKER_INGREDIENT.test(itemStack2)) {
                    compoundTag.setBoolean("Flicker", true);
                } else if (TRAIL_INGREDIENT.test(itemStack2)) {
                    compoundTag.setBoolean("Trail", true);
                } else if (itemStack2.getItem() instanceof ItemDye) {
                    list.add(((ItemDye)itemStack2.getItem()).getDyeColor().getFireworksColor());
                }
            }
        }

        compoundTag.putIntArray("Colors", list);
        compoundTag.setByte("Type", (byte)shape.getId());
        return itemStack;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getResult() {
        return new ItemStack(Items.FIREWORK_STAR);
    }

    @Override
    public RecipeSerializer<?> getRecipeSerializer() {
        return RecipeSerializer.FIREWORK_STAR;
    }
}

package net.minecraft.world.item;

import java.util.function.Supplier;
import net.minecraft.tags.TagsItem;
import net.minecraft.util.LazyInitVar;
import net.minecraft.world.item.crafting.RecipeItemStack;

public enum EnumToolMaterial implements ToolMaterial {
    WOOD(0, 59, 2.0F, 0.0F, 15, () -> {
        return RecipeItemStack.of(TagsItem.PLANKS);
    }),
    STONE(1, 131, 4.0F, 1.0F, 5, () -> {
        return RecipeItemStack.of(TagsItem.STONE_TOOL_MATERIALS);
    }),
    IRON(2, 250, 6.0F, 2.0F, 14, () -> {
        return RecipeItemStack.of(Items.IRON_INGOT);
    }),
    DIAMOND(3, 1561, 8.0F, 3.0F, 10, () -> {
        return RecipeItemStack.of(Items.DIAMOND);
    }),
    GOLD(0, 32, 12.0F, 0.0F, 22, () -> {
        return RecipeItemStack.of(Items.GOLD_INGOT);
    }),
    NETHERITE(4, 2031, 9.0F, 4.0F, 15, () -> {
        return RecipeItemStack.of(Items.NETHERITE_INGOT);
    });

    private final int level;
    private final int uses;
    private final float speed;
    private final float damage;
    private final int enchantmentValue;
    private final LazyInitVar<RecipeItemStack> repairIngredient;

    private EnumToolMaterial(int miningLevel, int itemDurability, float miningSpeed, float attackDamage, int enchantability, Supplier<RecipeItemStack> repairIngredient) {
        this.level = miningLevel;
        this.uses = itemDurability;
        this.speed = miningSpeed;
        this.damage = attackDamage;
        this.enchantmentValue = enchantability;
        this.repairIngredient = new LazyInitVar<>(repairIngredient);
    }

    @Override
    public int getUses() {
        return this.uses;
    }

    @Override
    public float getSpeed() {
        return this.speed;
    }

    @Override
    public float getAttackDamageBonus() {
        return this.damage;
    }

    @Override
    public int getLevel() {
        return this.level;
    }

    @Override
    public int getEnchantmentValue() {
        return this.enchantmentValue;
    }

    @Override
    public RecipeItemStack getRepairIngredient() {
        return this.repairIngredient.get();
    }
}

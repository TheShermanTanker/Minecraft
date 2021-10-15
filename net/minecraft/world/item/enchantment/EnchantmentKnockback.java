package net.minecraft.world.item.enchantment;

import net.minecraft.world.entity.EnumItemSlot;

public class EnchantmentKnockback extends Enchantment {
    protected EnchantmentKnockback(Enchantment.Rarity weight, EnumItemSlot... slot) {
        super(weight, EnchantmentSlotType.WEAPON, slot);
    }

    @Override
    public int getMinCost(int level) {
        return 5 + 20 * (level - 1);
    }

    @Override
    public int getMaxCost(int level) {
        return super.getMinCost(level) + 50;
    }

    @Override
    public int getMaxLevel() {
        return 2;
    }
}

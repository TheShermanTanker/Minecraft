package net.minecraft.world.item.enchantment;

import net.minecraft.world.entity.EnumItemSlot;

public class EnchantmentArrowKnockback extends Enchantment {
    public EnchantmentArrowKnockback(Enchantment.Rarity weight, EnumItemSlot... slotTypes) {
        super(weight, EnchantmentSlotType.BOW, slotTypes);
    }

    @Override
    public int getMinCost(int level) {
        return 12 + (level - 1) * 20;
    }

    @Override
    public int getMaxCost(int level) {
        return this.getMinCost(level) + 25;
    }

    @Override
    public int getMaxLevel() {
        return 2;
    }
}

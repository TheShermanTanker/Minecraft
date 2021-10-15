package net.minecraft.world.item.enchantment;

import net.minecraft.world.entity.EnumItemSlot;

public class EnchantmentLootBonus extends Enchantment {
    protected EnchantmentLootBonus(Enchantment.Rarity weight, EnchantmentSlotType type, EnumItemSlot... slotTypes) {
        super(weight, type, slotTypes);
    }

    @Override
    public int getMinCost(int level) {
        return 15 + (level - 1) * 9;
    }

    @Override
    public int getMaxCost(int level) {
        return super.getMinCost(level) + 50;
    }

    @Override
    public int getMaxLevel() {
        return 3;
    }

    @Override
    public boolean checkCompatibility(Enchantment other) {
        return super.checkCompatibility(other) && other != Enchantments.SILK_TOUCH;
    }
}

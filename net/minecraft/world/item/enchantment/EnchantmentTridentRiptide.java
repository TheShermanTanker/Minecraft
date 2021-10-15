package net.minecraft.world.item.enchantment;

import net.minecraft.world.entity.EnumItemSlot;

public class EnchantmentTridentRiptide extends Enchantment {
    public EnchantmentTridentRiptide(Enchantment.Rarity weight, EnumItemSlot... slotTypes) {
        super(weight, EnchantmentSlotType.TRIDENT, slotTypes);
    }

    @Override
    public int getMinCost(int level) {
        return 10 + level * 7;
    }

    @Override
    public int getMaxCost(int level) {
        return 50;
    }

    @Override
    public int getMaxLevel() {
        return 3;
    }

    @Override
    public boolean checkCompatibility(Enchantment other) {
        return super.checkCompatibility(other) && other != Enchantments.LOYALTY && other != Enchantments.CHANNELING;
    }
}

package net.minecraft.world.item.enchantment;

import net.minecraft.world.entity.EnumItemSlot;

public class EnchantmentTridentLoyalty extends Enchantment {
    public EnchantmentTridentLoyalty(Enchantment.Rarity weight, EnumItemSlot... slotTypes) {
        super(weight, EnchantmentSlotType.TRIDENT, slotTypes);
    }

    @Override
    public int getMinCost(int level) {
        return 5 + level * 7;
    }

    @Override
    public int getMaxCost(int level) {
        return 50;
    }

    @Override
    public int getMaxLevel() {
        return 3;
    }
}

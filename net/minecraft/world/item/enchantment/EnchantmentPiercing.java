package net.minecraft.world.item.enchantment;

import net.minecraft.world.entity.EnumItemSlot;

public class EnchantmentPiercing extends Enchantment {
    public EnchantmentPiercing(Enchantment.Rarity weight, EnumItemSlot... slotTypes) {
        super(weight, EnchantmentSlotType.CROSSBOW, slotTypes);
    }

    @Override
    public int getMinCost(int level) {
        return 1 + (level - 1) * 10;
    }

    @Override
    public int getMaxCost(int level) {
        return 50;
    }

    @Override
    public int getMaxLevel() {
        return 4;
    }

    @Override
    public boolean checkCompatibility(Enchantment other) {
        return super.checkCompatibility(other) && other != Enchantments.MULTISHOT;
    }
}

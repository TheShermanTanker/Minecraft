package net.minecraft.world.item.enchantment;

import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.EnumMonsterType;

public class EnchantmentTridentImpaling extends Enchantment {
    public EnchantmentTridentImpaling(Enchantment.Rarity weight, EnumItemSlot... slotTypes) {
        super(weight, EnchantmentSlotType.TRIDENT, slotTypes);
    }

    @Override
    public int getMinCost(int level) {
        return 1 + (level - 1) * 8;
    }

    @Override
    public int getMaxCost(int level) {
        return this.getMinCost(level) + 20;
    }

    @Override
    public int getMaxLevel() {
        return 5;
    }

    @Override
    public float getDamageBonus(int level, EnumMonsterType group) {
        return group == EnumMonsterType.WATER ? (float)level * 2.5F : 0.0F;
    }
}

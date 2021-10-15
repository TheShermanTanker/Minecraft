package net.minecraft.world.item.enchantment;

import net.minecraft.world.entity.EnumItemSlot;

public class EnchantmentSweeping extends Enchantment {
    public EnchantmentSweeping(Enchantment.Rarity weight, EnumItemSlot... slotTypes) {
        super(weight, EnchantmentSlotType.WEAPON, slotTypes);
    }

    @Override
    public int getMinCost(int level) {
        return 5 + (level - 1) * 9;
    }

    @Override
    public int getMaxCost(int level) {
        return this.getMinCost(level) + 15;
    }

    @Override
    public int getMaxLevel() {
        return 3;
    }

    public static float getSweepingDamageRatio(int level) {
        return 1.0F - 1.0F / (float)(level + 1);
    }
}

package net.minecraft.world.item.enchantment;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EnumItemSlot;
import net.minecraft.world.entity.EnumMonsterType;
import net.minecraft.world.item.ItemAxe;
import net.minecraft.world.item.ItemStack;

public class EnchantmentWeaponDamage extends Enchantment {
    public static final int ALL = 0;
    public static final int UNDEAD = 1;
    public static final int ARTHROPODS = 2;
    private static final String[] NAMES = new String[]{"all", "undead", "arthropods"};
    private static final int[] MIN_COST = new int[]{1, 5, 5};
    private static final int[] LEVEL_COST = new int[]{11, 8, 8};
    private static final int[] LEVEL_COST_SPAN = new int[]{20, 20, 20};
    public final int type;

    public EnchantmentWeaponDamage(Enchantment.Rarity weight, int typeIndex, EnumItemSlot... slots) {
        super(weight, EnchantmentSlotType.WEAPON, slots);
        this.type = typeIndex;
    }

    @Override
    public int getMinCost(int level) {
        return MIN_COST[this.type] + (level - 1) * LEVEL_COST[this.type];
    }

    @Override
    public int getMaxCost(int level) {
        return this.getMinCost(level) + LEVEL_COST_SPAN[this.type];
    }

    @Override
    public int getMaxLevel() {
        return 5;
    }

    @Override
    public float getDamageBonus(int level, EnumMonsterType group) {
        if (this.type == 0) {
            return 1.0F + (float)Math.max(0, level - 1) * 0.5F;
        } else if (this.type == 1 && group == EnumMonsterType.UNDEAD) {
            return (float)level * 2.5F;
        } else {
            return this.type == 2 && group == EnumMonsterType.ARTHROPOD ? (float)level * 2.5F : 0.0F;
        }
    }

    @Override
    public boolean checkCompatibility(Enchantment other) {
        return !(other instanceof EnchantmentWeaponDamage);
    }

    @Override
    public boolean canEnchant(ItemStack stack) {
        return stack.getItem() instanceof ItemAxe ? true : super.canEnchant(stack);
    }

    @Override
    public void doPostAttack(EntityLiving user, Entity target, int level) {
        if (target instanceof EntityLiving) {
            EntityLiving livingEntity = (EntityLiving)target;
            if (this.type == 2 && level > 0 && livingEntity.getMonsterType() == EnumMonsterType.ARTHROPOD) {
                int i = 20 + user.getRandom().nextInt(10 * level);
                livingEntity.addEffect(new MobEffect(MobEffects.MOVEMENT_SLOWDOWN, i, 3));
            }
        }

    }
}

package net.minecraft.world.item.enchantment;

import net.minecraft.util.MathHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EnumItemSlot;

public class EnchantmentProtection extends Enchantment {
    public final EnchantmentProtection.DamageType type;

    public EnchantmentProtection(Enchantment.Rarity weight, EnchantmentProtection.DamageType protectionType, EnumItemSlot... slotTypes) {
        super(weight, protectionType == EnchantmentProtection.DamageType.FALL ? EnchantmentSlotType.ARMOR_FEET : EnchantmentSlotType.ARMOR, slotTypes);
        this.type = protectionType;
    }

    @Override
    public int getMinCost(int level) {
        return this.type.getMinCost() + (level - 1) * this.type.getLevelCost();
    }

    @Override
    public int getMaxCost(int level) {
        return this.getMinCost(level) + this.type.getLevelCost();
    }

    @Override
    public int getMaxLevel() {
        return 4;
    }

    @Override
    public int getDamageProtection(int level, DamageSource source) {
        if (source.ignoresInvulnerability()) {
            return 0;
        } else if (this.type == EnchantmentProtection.DamageType.ALL) {
            return level;
        } else if (this.type == EnchantmentProtection.DamageType.FIRE && source.isFire()) {
            return level * 2;
        } else if (this.type == EnchantmentProtection.DamageType.FALL && source.isFall()) {
            return level * 3;
        } else if (this.type == EnchantmentProtection.DamageType.EXPLOSION && source.isExplosion()) {
            return level * 2;
        } else {
            return this.type == EnchantmentProtection.DamageType.PROJECTILE && source.isProjectile() ? level * 2 : 0;
        }
    }

    @Override
    public boolean checkCompatibility(Enchantment other) {
        if (other instanceof EnchantmentProtection) {
            EnchantmentProtection protectionEnchantment = (EnchantmentProtection)other;
            if (this.type == protectionEnchantment.type) {
                return false;
            } else {
                return this.type == EnchantmentProtection.DamageType.FALL || protectionEnchantment.type == EnchantmentProtection.DamageType.FALL;
            }
        } else {
            return super.checkCompatibility(other);
        }
    }

    public static int getFireAfterDampener(EntityLiving entity, int duration) {
        int i = EnchantmentManager.getEnchantmentLevel(Enchantments.FIRE_PROTECTION, entity);
        if (i > 0) {
            duration -= MathHelper.floor((float)duration * (float)i * 0.15F);
        }

        return duration;
    }

    public static double getExplosionKnockbackAfterDampener(EntityLiving entity, double velocity) {
        int i = EnchantmentManager.getEnchantmentLevel(Enchantments.BLAST_PROTECTION, entity);
        if (i > 0) {
            velocity -= (double)MathHelper.floor(velocity * (double)((float)i * 0.15F));
        }

        return velocity;
    }

    public static enum DamageType {
        ALL(1, 11),
        FIRE(10, 8),
        FALL(5, 6),
        EXPLOSION(5, 8),
        PROJECTILE(3, 6);

        private final int minCost;
        private final int levelCost;

        private DamageType(int basePower, int powerPerLevel) {
            this.minCost = basePower;
            this.levelCost = powerPerLevel;
        }

        public int getMinCost() {
            return this.minCost;
        }

        public int getLevelCost() {
            return this.levelCost;
        }
    }
}

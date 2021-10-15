package net.minecraft.world.entity.monster.hoglin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.phys.Vec3D;

public interface IOglin {
    int ATTACK_ANIMATION_DURATION = 10;

    int getAttackAnimationRemainingTicks();

    static boolean hurtAndThrowTarget(EntityLiving attacker, EntityLiving target) {
        float f = (float)attacker.getAttributeValue(GenericAttributes.ATTACK_DAMAGE);
        float g;
        if (!attacker.isBaby() && (int)f > 0) {
            g = f / 2.0F + (float)attacker.level.random.nextInt((int)f);
        } else {
            g = f;
        }

        boolean bl = target.damageEntity(DamageSource.mobAttack(attacker), g);
        if (bl) {
            attacker.doEnchantDamageEffects(attacker, target);
            if (!attacker.isBaby()) {
                throwTarget(attacker, target);
            }
        }

        return bl;
    }

    static void throwTarget(EntityLiving attacker, EntityLiving target) {
        double d = attacker.getAttributeValue(GenericAttributes.ATTACK_KNOCKBACK);
        double e = target.getAttributeValue(GenericAttributes.KNOCKBACK_RESISTANCE);
        double f = d - e;
        if (!(f <= 0.0D)) {
            double g = target.locX() - attacker.locX();
            double h = target.locZ() - attacker.locZ();
            float i = (float)(attacker.level.random.nextInt(21) - 10);
            double j = f * (double)(attacker.level.random.nextFloat() * 0.5F + 0.2F);
            Vec3D vec3 = (new Vec3D(g, 0.0D, h)).normalize().scale(j).yRot(i);
            double k = f * (double)attacker.level.random.nextFloat() * 0.5D;
            target.push(vec3.x, k, vec3.z);
            target.hurtMarked = true;
        }
    }
}

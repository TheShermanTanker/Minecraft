package net.minecraft.world.entity.boss.enderdragon.phases;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.boss.enderdragon.EntityEnderDragon;
import net.minecraft.world.entity.projectile.EntityArrow;

public abstract class DragonControllerLandedAbstract extends DragonControllerAbstract {
    public DragonControllerLandedAbstract(EntityEnderDragon dragon) {
        super(dragon);
    }

    @Override
    public boolean isSitting() {
        return true;
    }

    @Override
    public float onHurt(DamageSource damageSource, float damage) {
        if (damageSource.getDirectEntity() instanceof EntityArrow) {
            damageSource.getDirectEntity().setOnFire(1);
            return 0.0F;
        } else {
            return super.onHurt(damageSource, damage);
        }
    }
}

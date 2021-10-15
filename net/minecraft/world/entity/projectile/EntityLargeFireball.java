package net.minecraft.world.entity.projectile;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.World;
import net.minecraft.world.phys.MovingObjectPosition;
import net.minecraft.world.phys.MovingObjectPositionEntity;

public class EntityLargeFireball extends EntityFireballFireball {
    public int explosionPower = 1;

    public EntityLargeFireball(EntityTypes<? extends EntityLargeFireball> type, World world) {
        super(type, world);
    }

    public EntityLargeFireball(World world, EntityLiving owner, double velocityX, double velocityY, double velocityZ, int explosionPower) {
        super(EntityTypes.FIREBALL, owner, velocityX, velocityY, velocityZ, world);
        this.explosionPower = explosionPower;
    }

    @Override
    protected void onHit(MovingObjectPosition hitResult) {
        super.onHit(hitResult);
        if (!this.level.isClientSide) {
            boolean bl = this.level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
            this.level.createExplosion((Entity)null, this.locX(), this.locY(), this.locZ(), (float)this.explosionPower, bl, bl ? Explosion.Effect.DESTROY : Explosion.Effect.NONE);
            this.die();
        }

    }

    @Override
    protected void onHitEntity(MovingObjectPositionEntity entityHitResult) {
        super.onHitEntity(entityHitResult);
        if (!this.level.isClientSide) {
            Entity entity = entityHitResult.getEntity();
            Entity entity2 = this.getShooter();
            entity.damageEntity(DamageSource.fireball(this, entity2), 6.0F);
            if (entity2 instanceof EntityLiving) {
                this.doEnchantDamageEffects((EntityLiving)entity2, entity);
            }

        }
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        nbt.setByte("ExplosionPower", (byte)this.explosionPower);
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        if (nbt.hasKeyOfType("ExplosionPower", 99)) {
            this.explosionPower = nbt.getByte("ExplosionPower");
        }

    }
}

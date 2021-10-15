package net.minecraft.world.entity.projectile;

import java.util.List;
import net.minecraft.core.particles.ParticleParam;
import net.minecraft.core.particles.Particles;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAreaEffectCloud;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.World;
import net.minecraft.world.phys.MovingObjectPosition;
import net.minecraft.world.phys.MovingObjectPositionEntity;

public class EntityDragonFireball extends EntityFireball {
    public static final float SPLASH_RANGE = 4.0F;

    public EntityDragonFireball(EntityTypes<? extends EntityDragonFireball> type, World world) {
        super(type, world);
    }

    public EntityDragonFireball(World world, EntityLiving owner, double directionX, double directionY, double directionZ) {
        super(EntityTypes.DRAGON_FIREBALL, owner, directionX, directionY, directionZ, world);
    }

    @Override
    protected void onHit(MovingObjectPosition hitResult) {
        super.onHit(hitResult);
        if (hitResult.getType() != MovingObjectPosition.EnumMovingObjectType.ENTITY || !this.ownedBy(((MovingObjectPositionEntity)hitResult).getEntity())) {
            if (!this.level.isClientSide) {
                List<EntityLiving> list = this.level.getEntitiesOfClass(EntityLiving.class, this.getBoundingBox().grow(4.0D, 2.0D, 4.0D));
                EntityAreaEffectCloud areaEffectCloud = new EntityAreaEffectCloud(this.level, this.locX(), this.locY(), this.locZ());
                Entity entity = this.getShooter();
                if (entity instanceof EntityLiving) {
                    areaEffectCloud.setSource((EntityLiving)entity);
                }

                areaEffectCloud.setParticle(Particles.DRAGON_BREATH);
                areaEffectCloud.setRadius(3.0F);
                areaEffectCloud.setDuration(600);
                areaEffectCloud.setRadiusPerTick((7.0F - areaEffectCloud.getRadius()) / (float)areaEffectCloud.getDuration());
                areaEffectCloud.addEffect(new MobEffect(MobEffects.HARM, 1, 1));
                if (!list.isEmpty()) {
                    for(EntityLiving livingEntity : list) {
                        double d = this.distanceToSqr(livingEntity);
                        if (d < 16.0D) {
                            areaEffectCloud.setPosition(livingEntity.locX(), livingEntity.locY(), livingEntity.locZ());
                            break;
                        }
                    }
                }

                this.level.triggerEffect(2006, this.getChunkCoordinates(), this.isSilent() ? -1 : 1);
                this.level.addEntity(areaEffectCloud);
                this.die();
            }

        }
    }

    @Override
    public boolean isInteractable() {
        return false;
    }

    @Override
    public boolean damageEntity(DamageSource source, float amount) {
        return false;
    }

    @Override
    protected ParticleParam getTrailParticle() {
        return Particles.DRAGON_BREATH;
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }
}

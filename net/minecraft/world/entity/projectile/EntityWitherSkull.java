package net.minecraft.world.entity.projectile;

import net.minecraft.core.BlockPosition;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.boss.wither.EntityWither;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.MovingObjectPosition;
import net.minecraft.world.phys.MovingObjectPositionEntity;

public class EntityWitherSkull extends EntityFireball {
    private static final DataWatcherObject<Boolean> DATA_DANGEROUS = DataWatcher.defineId(EntityWitherSkull.class, DataWatcherRegistry.BOOLEAN);

    public EntityWitherSkull(EntityTypes<? extends EntityWitherSkull> type, World world) {
        super(type, world);
    }

    public EntityWitherSkull(World world, EntityLiving owner, double directionX, double directionY, double directionZ) {
        super(EntityTypes.WITHER_SKULL, owner, directionX, directionY, directionZ, world);
    }

    @Override
    protected float getInertia() {
        return this.isCharged() ? 0.73F : super.getInertia();
    }

    @Override
    public boolean isBurning() {
        return false;
    }

    @Override
    public float getBlockExplosionResistance(Explosion explosion, IBlockAccess world, BlockPosition pos, IBlockData blockState, Fluid fluidState, float max) {
        return this.isCharged() && EntityWither.canDestroy(blockState) ? Math.min(0.8F, max) : max;
    }

    @Override
    protected void onHitEntity(MovingObjectPositionEntity entityHitResult) {
        super.onHitEntity(entityHitResult);
        if (!this.level.isClientSide) {
            Entity entity = entityHitResult.getEntity();
            Entity entity2 = this.getShooter();
            boolean bl;
            if (entity2 instanceof EntityLiving) {
                EntityLiving livingEntity = (EntityLiving)entity2;
                bl = entity.damageEntity(DamageSource.witherSkull(this, livingEntity), 8.0F);
                if (bl) {
                    if (entity.isAlive()) {
                        this.doEnchantDamageEffects(livingEntity, entity);
                    } else {
                        livingEntity.heal(5.0F);
                    }
                }
            } else {
                bl = entity.damageEntity(DamageSource.MAGIC, 5.0F);
            }

            if (bl && entity instanceof EntityLiving) {
                int i = 0;
                if (this.level.getDifficulty() == EnumDifficulty.NORMAL) {
                    i = 10;
                } else if (this.level.getDifficulty() == EnumDifficulty.HARD) {
                    i = 40;
                }

                if (i > 0) {
                    ((EntityLiving)entity).addEffect(new MobEffect(MobEffectList.WITHER, 20 * i, 1), this.getEffectSource());
                }
            }

        }
    }

    @Override
    protected void onHit(MovingObjectPosition hitResult) {
        super.onHit(hitResult);
        if (!this.level.isClientSide) {
            Explosion.Effect blockInteraction = this.level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING) ? Explosion.Effect.DESTROY : Explosion.Effect.NONE;
            this.level.createExplosion(this, this.locX(), this.locY(), this.locZ(), 1.0F, false, blockInteraction);
            this.die();
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
    protected void initDatawatcher() {
        this.entityData.register(DATA_DANGEROUS, false);
    }

    public boolean isCharged() {
        return this.entityData.get(DATA_DANGEROUS);
    }

    public void setCharged(boolean charged) {
        this.entityData.set(DATA_DANGEROUS, charged);
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }
}

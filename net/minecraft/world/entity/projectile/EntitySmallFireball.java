package net.minecraft.world.entity.projectile;

import net.minecraft.core.BlockPosition;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.BlockFireAbstract;
import net.minecraft.world.phys.MovingObjectPosition;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.MovingObjectPositionEntity;

public class EntitySmallFireball extends EntityFireballFireball {
    public EntitySmallFireball(EntityTypes<? extends EntitySmallFireball> type, World world) {
        super(type, world);
    }

    public EntitySmallFireball(World world, EntityLiving owner, double velocityX, double velocityY, double velocityZ) {
        super(EntityTypes.SMALL_FIREBALL, owner, velocityX, velocityY, velocityZ, world);
    }

    public EntitySmallFireball(World world, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
        super(EntityTypes.SMALL_FIREBALL, x, y, z, velocityX, velocityY, velocityZ, world);
    }

    @Override
    protected void onHitEntity(MovingObjectPositionEntity entityHitResult) {
        super.onHitEntity(entityHitResult);
        if (!this.level.isClientSide) {
            Entity entity = entityHitResult.getEntity();
            if (!entity.isFireProof()) {
                Entity entity2 = this.getShooter();
                int i = entity.getFireTicks();
                entity.setOnFire(5);
                boolean bl = entity.damageEntity(DamageSource.fireball(this, entity2), 5.0F);
                if (!bl) {
                    entity.setFireTicks(i);
                } else if (entity2 instanceof EntityLiving) {
                    this.doEnchantDamageEffects((EntityLiving)entity2, entity);
                }
            }

        }
    }

    @Override
    protected void onHitBlock(MovingObjectPositionBlock blockHitResult) {
        super.onHitBlock(blockHitResult);
        if (!this.level.isClientSide) {
            Entity entity = this.getShooter();
            if (!(entity instanceof EntityInsentient) || this.level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
                BlockPosition blockPos = blockHitResult.getBlockPosition().relative(blockHitResult.getDirection());
                if (this.level.isEmpty(blockPos)) {
                    this.level.setTypeUpdate(blockPos, BlockFireAbstract.getState(this.level, blockPos));
                }
            }

        }
    }

    @Override
    protected void onHit(MovingObjectPosition hitResult) {
        super.onHit(hitResult);
        if (!this.level.isClientSide) {
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
}

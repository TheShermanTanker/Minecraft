package net.minecraft.world.entity.projectile;

import net.minecraft.core.particles.ParticleParamItem;
import net.minecraft.core.particles.Particles;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.EntityChicken;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.World;
import net.minecraft.world.phys.MovingObjectPosition;
import net.minecraft.world.phys.MovingObjectPositionEntity;

public class EntityEgg extends EntityProjectileThrowable {
    public EntityEgg(EntityTypes<? extends EntityEgg> type, World world) {
        super(type, world);
    }

    public EntityEgg(World world, EntityLiving owner) {
        super(EntityTypes.EGG, owner, world);
    }

    public EntityEgg(World world, double x, double y, double z) {
        super(EntityTypes.EGG, x, y, z, world);
    }

    @Override
    public void handleEntityEvent(byte status) {
        if (status == 3) {
            double d = 0.08D;

            for(int i = 0; i < 8; ++i) {
                this.level.addParticle(new ParticleParamItem(Particles.ITEM, this.getSuppliedItem()), this.locX(), this.locY(), this.locZ(), ((double)this.random.nextFloat() - 0.5D) * 0.08D, ((double)this.random.nextFloat() - 0.5D) * 0.08D, ((double)this.random.nextFloat() - 0.5D) * 0.08D);
            }
        }

    }

    @Override
    protected void onHitEntity(MovingObjectPositionEntity entityHitResult) {
        super.onHitEntity(entityHitResult);
        entityHitResult.getEntity().damageEntity(DamageSource.projectile(this, this.getShooter()), 0.0F);
    }

    @Override
    protected void onHit(MovingObjectPosition hitResult) {
        super.onHit(hitResult);
        if (!this.level.isClientSide) {
            if (this.random.nextInt(8) == 0) {
                int i = 1;
                if (this.random.nextInt(32) == 0) {
                    i = 4;
                }

                for(int j = 0; j < i; ++j) {
                    EntityChicken chicken = EntityTypes.CHICKEN.create(this.level);
                    chicken.setAgeRaw(-24000);
                    chicken.setPositionRotation(this.locX(), this.locY(), this.locZ(), this.getYRot(), 0.0F);
                    this.level.addEntity(chicken);
                }
            }

            this.level.broadcastEntityEffect(this, (byte)3);
            this.die();
        }

    }

    @Override
    protected Item getDefaultItem() {
        return Items.EGG;
    }
}

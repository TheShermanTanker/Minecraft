package net.minecraft.world.entity.projectile;

import net.minecraft.core.particles.ParticleParam;
import net.minecraft.core.particles.ParticleParamItem;
import net.minecraft.core.particles.Particles;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.EntityBlaze;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.World;
import net.minecraft.world.phys.MovingObjectPosition;
import net.minecraft.world.phys.MovingObjectPositionEntity;

public class EntitySnowball extends EntityProjectileThrowable {
    public EntitySnowball(EntityTypes<? extends EntitySnowball> type, World world) {
        super(type, world);
    }

    public EntitySnowball(World world, EntityLiving owner) {
        super(EntityTypes.SNOWBALL, owner, world);
    }

    public EntitySnowball(World world, double x, double y, double z) {
        super(EntityTypes.SNOWBALL, x, y, z, world);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.SNOWBALL;
    }

    private ParticleParam getParticle() {
        ItemStack itemStack = this.getItem();
        return (ParticleParam)(itemStack.isEmpty() ? Particles.ITEM_SNOWBALL : new ParticleParamItem(Particles.ITEM, itemStack));
    }

    @Override
    public void handleEntityEvent(byte status) {
        if (status == 3) {
            ParticleParam particleOptions = this.getParticle();

            for(int i = 0; i < 8; ++i) {
                this.level.addParticle(particleOptions, this.locX(), this.locY(), this.locZ(), 0.0D, 0.0D, 0.0D);
            }
        }

    }

    @Override
    protected void onHitEntity(MovingObjectPositionEntity entityHitResult) {
        super.onHitEntity(entityHitResult);
        Entity entity = entityHitResult.getEntity();
        int i = entity instanceof EntityBlaze ? 3 : 0;
        entity.damageEntity(DamageSource.projectile(this, this.getShooter()), (float)i);
    }

    @Override
    protected void onHit(MovingObjectPosition hitResult) {
        super.onHit(hitResult);
        if (!this.level.isClientSide) {
            this.level.broadcastEntityEffect(this, (byte)3);
            this.die();
        }

    }
}

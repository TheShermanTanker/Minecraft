package net.minecraft.world.entity.projectile;

import net.minecraft.core.particles.Particles;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.horse.EntityLlama;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.phys.MovingObjectPosition;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.MovingObjectPositionEntity;
import net.minecraft.world.phys.Vec3D;

public class EntityLlamaSpit extends IProjectile {
    public EntityLlamaSpit(EntityTypes<? extends EntityLlamaSpit> type, World world) {
        super(type, world);
    }

    public EntityLlamaSpit(World world, EntityLlama owner) {
        this(EntityTypes.LLAMA_SPIT, world);
        this.setShooter(owner);
        this.setPosition(owner.locX() - (double)(owner.getWidth() + 1.0F) * 0.5D * (double)MathHelper.sin(owner.yBodyRot * ((float)Math.PI / 180F)), owner.getHeadY() - (double)0.1F, owner.locZ() + (double)(owner.getWidth() + 1.0F) * 0.5D * (double)MathHelper.cos(owner.yBodyRot * ((float)Math.PI / 180F)));
    }

    @Override
    public void tick() {
        super.tick();
        Vec3D vec3 = this.getMot();
        MovingObjectPosition hitResult = ProjectileHelper.getHitResult(this, this::canHitEntity);
        this.onHit(hitResult);
        double d = this.locX() + vec3.x;
        double e = this.locY() + vec3.y;
        double f = this.locZ() + vec3.z;
        this.updateRotation();
        float g = 0.99F;
        float h = 0.06F;
        if (this.level.getBlockStates(this.getBoundingBox()).noneMatch(BlockBase.BlockData::isAir)) {
            this.die();
        } else if (this.isInWaterOrBubble()) {
            this.die();
        } else {
            this.setMot(vec3.scale((double)0.99F));
            if (!this.isNoGravity()) {
                this.setMot(this.getMot().add(0.0D, (double)-0.06F, 0.0D));
            }

            this.setPosition(d, e, f);
        }
    }

    @Override
    protected void onHitEntity(MovingObjectPositionEntity entityHitResult) {
        super.onHitEntity(entityHitResult);
        Entity entity = this.getShooter();
        if (entity instanceof EntityLiving) {
            entityHitResult.getEntity().damageEntity(DamageSource.indirectMobAttack(this, (EntityLiving)entity).setProjectile(), 1.0F);
        }

    }

    @Override
    protected void onHitBlock(MovingObjectPositionBlock blockHitResult) {
        super.onHitBlock(blockHitResult);
        if (!this.level.isClientSide) {
            this.die();
        }

    }

    @Override
    protected void initDatawatcher() {
    }

    @Override
    public void recreateFromPacket(PacketPlayOutSpawnEntity packet) {
        super.recreateFromPacket(packet);
        double d = packet.getXa();
        double e = packet.getYa();
        double f = packet.getZa();

        for(int i = 0; i < 7; ++i) {
            double g = 0.4D + 0.1D * (double)i;
            this.level.addParticle(Particles.SPIT, this.locX(), this.locY(), this.locZ(), d * g, e, f * g);
        }

        this.setMot(d, e, f);
    }
}

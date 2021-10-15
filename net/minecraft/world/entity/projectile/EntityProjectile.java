package net.minecraft.world.entity.projectile;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.particles.Particles;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TileEntity;
import net.minecraft.world.level.block.entity.TileEntityEndGateway;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.MovingObjectPosition;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.Vec3D;

public abstract class EntityProjectile extends IProjectile {
    protected EntityProjectile(EntityTypes<? extends EntityProjectile> type, World world) {
        super(type, world);
    }

    protected EntityProjectile(EntityTypes<? extends EntityProjectile> type, double x, double y, double z, World world) {
        this(type, world);
        this.setPosition(x, y, z);
    }

    protected EntityProjectile(EntityTypes<? extends EntityProjectile> type, EntityLiving owner, World world) {
        this(type, owner.locX(), owner.getHeadY() - (double)0.1F, owner.locZ(), world);
        this.setShooter(owner);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double d = this.getBoundingBox().getSize() * 4.0D;
        if (Double.isNaN(d)) {
            d = 4.0D;
        }

        d = d * 64.0D;
        return distance < d * d;
    }

    @Override
    public void tick() {
        super.tick();
        MovingObjectPosition hitResult = ProjectileHelper.getHitResult(this, this::canHitEntity);
        boolean bl = false;
        if (hitResult.getType() == MovingObjectPosition.EnumMovingObjectType.BLOCK) {
            BlockPosition blockPos = ((MovingObjectPositionBlock)hitResult).getBlockPosition();
            IBlockData blockState = this.level.getType(blockPos);
            if (blockState.is(Blocks.NETHER_PORTAL)) {
                this.handleInsidePortal(blockPos);
                bl = true;
            } else if (blockState.is(Blocks.END_GATEWAY)) {
                TileEntity blockEntity = this.level.getTileEntity(blockPos);
                if (blockEntity instanceof TileEntityEndGateway && TileEntityEndGateway.canEntityTeleport(this)) {
                    TileEntityEndGateway.teleportEntity(this.level, blockPos, blockState, this, (TileEntityEndGateway)blockEntity);
                }

                bl = true;
            }
        }

        if (hitResult.getType() != MovingObjectPosition.EnumMovingObjectType.MISS && !bl) {
            this.onHit(hitResult);
        }

        this.checkBlockCollisions();
        Vec3D vec3 = this.getMot();
        double d = this.locX() + vec3.x;
        double e = this.locY() + vec3.y;
        double f = this.locZ() + vec3.z;
        this.updateRotation();
        float h;
        if (this.isInWater()) {
            for(int i = 0; i < 4; ++i) {
                float g = 0.25F;
                this.level.addParticle(Particles.BUBBLE, d - vec3.x * 0.25D, e - vec3.y * 0.25D, f - vec3.z * 0.25D, vec3.x, vec3.y, vec3.z);
            }

            h = 0.8F;
        } else {
            h = 0.99F;
        }

        this.setMot(vec3.scale((double)h));
        if (!this.isNoGravity()) {
            Vec3D vec32 = this.getMot();
            this.setMot(vec32.x, vec32.y - (double)this.getGravity(), vec32.z);
        }

        this.setPosition(d, e, f);
    }

    protected float getGravity() {
        return 0.03F;
    }
}

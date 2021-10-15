package net.minecraft.world.entity.projectile;

import net.minecraft.core.particles.ParticleParam;
import net.minecraft.core.particles.Particles;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.World;
import net.minecraft.world.phys.MovingObjectPosition;
import net.minecraft.world.phys.Vec3D;

public abstract class EntityFireball extends IProjectile {
    public double xPower;
    public double yPower;
    public double zPower;

    protected EntityFireball(EntityTypes<? extends EntityFireball> type, World world) {
        super(type, world);
    }

    public EntityFireball(EntityTypes<? extends EntityFireball> type, double x, double y, double z, double directionX, double directionY, double directionZ, World world) {
        this(type, world);
        this.setPositionRotation(x, y, z, this.getYRot(), this.getXRot());
        this.reapplyPosition();
        double d = Math.sqrt(directionX * directionX + directionY * directionY + directionZ * directionZ);
        if (d != 0.0D) {
            this.xPower = directionX / d * 0.1D;
            this.yPower = directionY / d * 0.1D;
            this.zPower = directionZ / d * 0.1D;
        }

    }

    public EntityFireball(EntityTypes<? extends EntityFireball> type, EntityLiving owner, double directionX, double directionY, double directionZ, World world) {
        this(type, owner.locX(), owner.locY(), owner.locZ(), directionX, directionY, directionZ, world);
        this.setShooter(owner);
        this.setYawPitch(owner.getYRot(), owner.getXRot());
    }

    @Override
    protected void initDatawatcher() {
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
        Entity entity = this.getShooter();
        if (this.level.isClientSide || (entity == null || !entity.isRemoved()) && this.level.isLoaded(this.getChunkCoordinates())) {
            super.tick();
            if (this.shouldBurn()) {
                this.setOnFire(1);
            }

            MovingObjectPosition hitResult = ProjectileHelper.getHitResult(this, this::canHitEntity);
            if (hitResult.getType() != MovingObjectPosition.EnumMovingObjectType.MISS) {
                this.onHit(hitResult);
            }

            this.checkBlockCollisions();
            Vec3D vec3 = this.getMot();
            double d = this.locX() + vec3.x;
            double e = this.locY() + vec3.y;
            double f = this.locZ() + vec3.z;
            ProjectileHelper.rotateTowardsMovement(this, 0.2F);
            float g = this.getInertia();
            if (this.isInWater()) {
                for(int i = 0; i < 4; ++i) {
                    float h = 0.25F;
                    this.level.addParticle(Particles.BUBBLE, d - vec3.x * 0.25D, e - vec3.y * 0.25D, f - vec3.z * 0.25D, vec3.x, vec3.y, vec3.z);
                }

                g = 0.8F;
            }

            this.setMot(vec3.add(this.xPower, this.yPower, this.zPower).scale((double)g));
            this.level.addParticle(this.getTrailParticle(), d, e + 0.5D, f, 0.0D, 0.0D, 0.0D);
            this.setPosition(d, e, f);
        } else {
            this.die();
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity) && !entity.noPhysics;
    }

    protected boolean shouldBurn() {
        return true;
    }

    protected ParticleParam getTrailParticle() {
        return Particles.SMOKE;
    }

    protected float getInertia() {
        return 0.95F;
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        nbt.set("power", this.newDoubleList(new double[]{this.xPower, this.yPower, this.zPower}));
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        if (nbt.hasKeyOfType("power", 9)) {
            NBTTagList listTag = nbt.getList("power", 6);
            if (listTag.size() == 3) {
                this.xPower = listTag.getDouble(0);
                this.yPower = listTag.getDouble(1);
                this.zPower = listTag.getDouble(2);
            }
        }

    }

    @Override
    public boolean isInteractable() {
        return true;
    }

    @Override
    public float getPickRadius() {
        return 1.0F;
    }

    @Override
    public boolean damageEntity(DamageSource source, float amount) {
        if (this.isInvulnerable(source)) {
            return false;
        } else {
            this.velocityChanged();
            Entity entity = source.getEntity();
            if (entity != null) {
                Vec3D vec3 = entity.getLookDirection();
                this.setMot(vec3);
                this.xPower = vec3.x * 0.1D;
                this.yPower = vec3.y * 0.1D;
                this.zPower = vec3.z * 0.1D;
                this.setShooter(entity);
                return true;
            } else {
                return false;
            }
        }
    }

    @Override
    public float getBrightness() {
        return 1.0F;
    }

    @Override
    public Packet<?> getPacket() {
        Entity entity = this.getShooter();
        int i = entity == null ? 0 : entity.getId();
        return new PacketPlayOutSpawnEntity(this.getId(), this.getUniqueID(), this.locX(), this.locY(), this.locZ(), this.getXRot(), this.getYRot(), this.getEntityType(), i, new Vec3D(this.xPower, this.yPower, this.zPower));
    }

    @Override
    public void recreateFromPacket(PacketPlayOutSpawnEntity packet) {
        super.recreateFromPacket(packet);
        double d = packet.getXa();
        double e = packet.getYa();
        double f = packet.getZa();
        double g = Math.sqrt(d * d + e * e + f * f);
        if (g != 0.0D) {
            this.xPower = d / g * 0.1D;
            this.yPower = e / g * 0.1D;
            this.zPower = f / g * 0.1D;
        }

    }
}

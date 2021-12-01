package net.minecraft.world.entity.projectile;

import com.google.common.base.MoreObjects;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntity;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.MovingObjectPosition;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.MovingObjectPositionEntity;
import net.minecraft.world.phys.Vec3D;

public abstract class IProjectile extends Entity {
    @Nullable
    public UUID ownerUUID;
    @Nullable
    public Entity cachedOwner;
    private boolean leftOwner;
    private boolean hasBeenShot;

    IProjectile(EntityTypes<? extends IProjectile> type, World world) {
        super(type, world);
    }

    public void setShooter(@Nullable Entity entity) {
        if (entity != null) {
            this.ownerUUID = entity.getUniqueID();
            this.cachedOwner = entity;
        }

    }

    @Nullable
    public Entity getShooter() {
        if (this.cachedOwner != null && !this.cachedOwner.isRemoved()) {
            return this.cachedOwner;
        } else if (this.ownerUUID != null && this.level instanceof WorldServer) {
            this.cachedOwner = ((WorldServer)this.level).getEntity(this.ownerUUID);
            return this.cachedOwner;
        } else {
            return null;
        }
    }

    public Entity getEffectSource() {
        return MoreObjects.firstNonNull(this.getShooter(), this);
    }

    @Override
    protected void saveData(NBTTagCompound nbt) {
        if (this.ownerUUID != null) {
            nbt.putUUID("Owner", this.ownerUUID);
        }

        if (this.leftOwner) {
            nbt.setBoolean("LeftOwner", true);
        }

        nbt.setBoolean("HasBeenShot", this.hasBeenShot);
    }

    protected boolean ownedBy(Entity entity) {
        return entity.getUniqueID().equals(this.ownerUUID);
    }

    @Override
    protected void loadData(NBTTagCompound nbt) {
        if (nbt.hasUUID("Owner")) {
            this.ownerUUID = nbt.getUUID("Owner");
        }

        this.leftOwner = nbt.getBoolean("LeftOwner");
        this.hasBeenShot = nbt.getBoolean("HasBeenShot");
    }

    @Override
    public void tick() {
        if (!this.hasBeenShot) {
            this.gameEvent(GameEvent.PROJECTILE_SHOOT, this.getShooter(), this.getChunkCoordinates());
            this.hasBeenShot = true;
        }

        if (!this.leftOwner) {
            this.leftOwner = this.checkLeftOwner();
        }

        super.tick();
    }

    private boolean checkLeftOwner() {
        Entity entity = this.getShooter();
        if (entity != null) {
            for(Entity entity2 : this.level.getEntities(this, this.getBoundingBox().expandTowards(this.getMot()).inflate(1.0D), (entityx) -> {
                return !entityx.isSpectator() && entityx.isInteractable();
            })) {
                if (entity2.getRootVehicle() == entity.getRootVehicle()) {
                    return false;
                }
            }
        }

        return true;
    }

    public void shoot(double x, double y, double z, float speed, float divergence) {
        Vec3D vec3 = (new Vec3D(x, y, z)).normalize().add(this.random.nextGaussian() * (double)0.0075F * (double)divergence, this.random.nextGaussian() * (double)0.0075F * (double)divergence, this.random.nextGaussian() * (double)0.0075F * (double)divergence).scale((double)speed);
        this.setMot(vec3);
        double d = vec3.horizontalDistance();
        this.setYRot((float)(MathHelper.atan2(vec3.x, vec3.z) * (double)(180F / (float)Math.PI)));
        this.setXRot((float)(MathHelper.atan2(vec3.y, d) * (double)(180F / (float)Math.PI)));
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    public void shootFromRotation(Entity shooter, float pitch, float yaw, float roll, float speed, float divergence) {
        float f = -MathHelper.sin(yaw * ((float)Math.PI / 180F)) * MathHelper.cos(pitch * ((float)Math.PI / 180F));
        float g = -MathHelper.sin((pitch + roll) * ((float)Math.PI / 180F));
        float h = MathHelper.cos(yaw * ((float)Math.PI / 180F)) * MathHelper.cos(pitch * ((float)Math.PI / 180F));
        this.shoot((double)f, (double)g, (double)h, speed, divergence);
        Vec3D vec3 = shooter.getMot();
        this.setMot(this.getMot().add(vec3.x, shooter.isOnGround() ? 0.0D : vec3.y, vec3.z));
    }

    protected void onHit(MovingObjectPosition hitResult) {
        MovingObjectPosition.EnumMovingObjectType type = hitResult.getType();
        if (type == MovingObjectPosition.EnumMovingObjectType.ENTITY) {
            this.onHitEntity((MovingObjectPositionEntity)hitResult);
        } else if (type == MovingObjectPosition.EnumMovingObjectType.BLOCK) {
            this.onHitBlock((MovingObjectPositionBlock)hitResult);
        }

        if (type != MovingObjectPosition.EnumMovingObjectType.MISS) {
            this.gameEvent(GameEvent.PROJECTILE_LAND, this.getShooter());
        }

    }

    protected void onHitEntity(MovingObjectPositionEntity entityHitResult) {
    }

    protected void onHitBlock(MovingObjectPositionBlock blockHitResult) {
        IBlockData blockState = this.level.getType(blockHitResult.getBlockPosition());
        blockState.onProjectileHit(this.level, blockState, blockHitResult, this);
    }

    @Override
    public void lerpMotion(double x, double y, double z) {
        this.setMot(x, y, z);
        if (this.xRotO == 0.0F && this.yRotO == 0.0F) {
            double d = Math.sqrt(x * x + z * z);
            this.setXRot((float)(MathHelper.atan2(y, d) * (double)(180F / (float)Math.PI)));
            this.setYRot((float)(MathHelper.atan2(x, z) * (double)(180F / (float)Math.PI)));
            this.xRotO = this.getXRot();
            this.yRotO = this.getYRot();
            this.setPositionRotation(this.locX(), this.locY(), this.locZ(), this.getYRot(), this.getXRot());
        }

    }

    protected boolean canHitEntity(Entity entity) {
        if (!entity.isSpectator() && entity.isAlive() && entity.isInteractable()) {
            Entity entity2 = this.getShooter();
            return entity2 == null || this.leftOwner || !entity2.isSameVehicle(entity);
        } else {
            return false;
        }
    }

    protected void updateRotation() {
        Vec3D vec3 = this.getMot();
        double d = vec3.horizontalDistance();
        this.setXRot(lerpRotation(this.xRotO, (float)(MathHelper.atan2(vec3.y, d) * (double)(180F / (float)Math.PI))));
        this.setYRot(lerpRotation(this.yRotO, (float)(MathHelper.atan2(vec3.x, vec3.z) * (double)(180F / (float)Math.PI))));
    }

    protected static float lerpRotation(float prevRot, float newRot) {
        while(newRot - prevRot < -180.0F) {
            prevRot -= 360.0F;
        }

        while(newRot - prevRot >= 180.0F) {
            prevRot += 360.0F;
        }

        return MathHelper.lerp(0.2F, prevRot, newRot);
    }

    @Override
    public Packet<?> getPacket() {
        Entity entity = this.getShooter();
        return new PacketPlayOutSpawnEntity(this, entity == null ? 0 : entity.getId());
    }

    @Override
    public void recreateFromPacket(PacketPlayOutSpawnEntity packet) {
        super.recreateFromPacket(packet);
        Entity entity = this.level.getEntity(packet.getData());
        if (entity != null) {
            this.setShooter(entity);
        }

    }

    @Override
    public boolean mayInteract(World world, BlockPosition pos) {
        Entity entity = this.getShooter();
        if (entity instanceof EntityHuman) {
            return entity.mayInteract(world, pos);
        } else {
            return entity == null || world.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
        }
    }
}

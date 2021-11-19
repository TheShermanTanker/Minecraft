package net.minecraft.world.entity.projectile;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.particles.Particles;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntity;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.World;
import net.minecraft.world.phys.MovingObjectPosition;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.MovingObjectPositionEntity;
import net.minecraft.world.phys.Vec3D;

public class EntityShulkerBullet extends IProjectile {
    private static final double SPEED = 0.15D;
    @Nullable
    private Entity finalTarget;
    @Nullable
    private EnumDirection currentMoveDirection;
    private int flightSteps;
    private double targetDeltaX;
    private double targetDeltaY;
    private double targetDeltaZ;
    @Nullable
    private UUID targetId;

    public EntityShulkerBullet(EntityTypes<? extends EntityShulkerBullet> type, World world) {
        super(type, world);
        this.noPhysics = true;
    }

    public EntityShulkerBullet(World world, EntityLiving owner, Entity target, EnumDirection.EnumAxis axis) {
        this(EntityTypes.SHULKER_BULLET, world);
        this.setShooter(owner);
        BlockPosition blockPos = owner.getChunkCoordinates();
        double d = (double)blockPos.getX() + 0.5D;
        double e = (double)blockPos.getY() + 0.5D;
        double f = (double)blockPos.getZ() + 0.5D;
        this.setPositionRotation(d, e, f, this.getYRot(), this.getXRot());
        this.finalTarget = target;
        this.currentMoveDirection = EnumDirection.UP;
        this.selectNextMoveDirection(axis);
    }

    @Override
    public EnumSoundCategory getSoundCategory() {
        return EnumSoundCategory.HOSTILE;
    }

    @Override
    protected void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        if (this.finalTarget != null) {
            nbt.putUUID("Target", this.finalTarget.getUniqueID());
        }

        if (this.currentMoveDirection != null) {
            nbt.setInt("Dir", this.currentMoveDirection.get3DDataValue());
        }

        nbt.setInt("Steps", this.flightSteps);
        nbt.setDouble("TXD", this.targetDeltaX);
        nbt.setDouble("TYD", this.targetDeltaY);
        nbt.setDouble("TZD", this.targetDeltaZ);
    }

    @Override
    protected void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        this.flightSteps = nbt.getInt("Steps");
        this.targetDeltaX = nbt.getDouble("TXD");
        this.targetDeltaY = nbt.getDouble("TYD");
        this.targetDeltaZ = nbt.getDouble("TZD");
        if (nbt.hasKeyOfType("Dir", 99)) {
            this.currentMoveDirection = EnumDirection.fromType1(nbt.getInt("Dir"));
        }

        if (nbt.hasUUID("Target")) {
            this.targetId = nbt.getUUID("Target");
        }

    }

    @Override
    protected void initDatawatcher() {
    }

    @Nullable
    private EnumDirection getMoveDirection() {
        return this.currentMoveDirection;
    }

    private void setMoveDirection(@Nullable EnumDirection direction) {
        this.currentMoveDirection = direction;
    }

    private void selectNextMoveDirection(@Nullable EnumDirection.EnumAxis axis) {
        double d = 0.5D;
        BlockPosition blockPos;
        if (this.finalTarget == null) {
            blockPos = this.getChunkCoordinates().below();
        } else {
            d = (double)this.finalTarget.getHeight() * 0.5D;
            blockPos = new BlockPosition(this.finalTarget.locX(), this.finalTarget.locY() + d, this.finalTarget.locZ());
        }

        double e = (double)blockPos.getX() + 0.5D;
        double f = (double)blockPos.getY() + d;
        double g = (double)blockPos.getZ() + 0.5D;
        EnumDirection direction = null;
        if (!blockPos.closerThan(this.getPositionVector(), 2.0D)) {
            BlockPosition blockPos3 = this.getChunkCoordinates();
            List<EnumDirection> list = Lists.newArrayList();
            if (axis != EnumDirection.EnumAxis.X) {
                if (blockPos3.getX() < blockPos.getX() && this.level.isEmpty(blockPos3.east())) {
                    list.add(EnumDirection.EAST);
                } else if (blockPos3.getX() > blockPos.getX() && this.level.isEmpty(blockPos3.west())) {
                    list.add(EnumDirection.WEST);
                }
            }

            if (axis != EnumDirection.EnumAxis.Y) {
                if (blockPos3.getY() < blockPos.getY() && this.level.isEmpty(blockPos3.above())) {
                    list.add(EnumDirection.UP);
                } else if (blockPos3.getY() > blockPos.getY() && this.level.isEmpty(blockPos3.below())) {
                    list.add(EnumDirection.DOWN);
                }
            }

            if (axis != EnumDirection.EnumAxis.Z) {
                if (blockPos3.getZ() < blockPos.getZ() && this.level.isEmpty(blockPos3.south())) {
                    list.add(EnumDirection.SOUTH);
                } else if (blockPos3.getZ() > blockPos.getZ() && this.level.isEmpty(blockPos3.north())) {
                    list.add(EnumDirection.NORTH);
                }
            }

            direction = EnumDirection.getRandom(this.random);
            if (list.isEmpty()) {
                for(int i = 5; !this.level.isEmpty(blockPos3.relative(direction)) && i > 0; --i) {
                    direction = EnumDirection.getRandom(this.random);
                }
            } else {
                direction = list.get(this.random.nextInt(list.size()));
            }

            e = this.locX() + (double)direction.getAdjacentX();
            f = this.locY() + (double)direction.getAdjacentY();
            g = this.locZ() + (double)direction.getAdjacentZ();
        }

        this.setMoveDirection(direction);
        double h = e - this.locX();
        double j = f - this.locY();
        double k = g - this.locZ();
        double l = Math.sqrt(h * h + j * j + k * k);
        if (l == 0.0D) {
            this.targetDeltaX = 0.0D;
            this.targetDeltaY = 0.0D;
            this.targetDeltaZ = 0.0D;
        } else {
            this.targetDeltaX = h / l * 0.15D;
            this.targetDeltaY = j / l * 0.15D;
            this.targetDeltaZ = k / l * 0.15D;
        }

        this.hasImpulse = true;
        this.flightSteps = 10 + this.random.nextInt(5) * 10;
    }

    @Override
    public void checkDespawn() {
        if (this.level.getDifficulty() == EnumDifficulty.PEACEFUL) {
            this.die();
        }

    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level.isClientSide) {
            if (this.finalTarget == null && this.targetId != null) {
                this.finalTarget = ((WorldServer)this.level).getEntity(this.targetId);
                if (this.finalTarget == null) {
                    this.targetId = null;
                }
            }

            if (this.finalTarget == null || !this.finalTarget.isAlive() || this.finalTarget instanceof EntityHuman && this.finalTarget.isSpectator()) {
                if (!this.isNoGravity()) {
                    this.setMot(this.getMot().add(0.0D, -0.04D, 0.0D));
                }
            } else {
                this.targetDeltaX = MathHelper.clamp(this.targetDeltaX * 1.025D, -1.0D, 1.0D);
                this.targetDeltaY = MathHelper.clamp(this.targetDeltaY * 1.025D, -1.0D, 1.0D);
                this.targetDeltaZ = MathHelper.clamp(this.targetDeltaZ * 1.025D, -1.0D, 1.0D);
                Vec3D vec3 = this.getMot();
                this.setMot(vec3.add((this.targetDeltaX - vec3.x) * 0.2D, (this.targetDeltaY - vec3.y) * 0.2D, (this.targetDeltaZ - vec3.z) * 0.2D));
            }

            MovingObjectPosition hitResult = ProjectileHelper.getHitResult(this, this::canHitEntity);
            if (hitResult.getType() != MovingObjectPosition.EnumMovingObjectType.MISS) {
                this.onHit(hitResult);
            }
        }

        this.checkBlockCollisions();
        Vec3D vec32 = this.getMot();
        this.setPosition(this.locX() + vec32.x, this.locY() + vec32.y, this.locZ() + vec32.z);
        ProjectileHelper.rotateTowardsMovement(this, 0.5F);
        if (this.level.isClientSide) {
            this.level.addParticle(Particles.END_ROD, this.locX() - vec32.x, this.locY() - vec32.y + 0.15D, this.locZ() - vec32.z, 0.0D, 0.0D, 0.0D);
        } else if (this.finalTarget != null && !this.finalTarget.isRemoved()) {
            if (this.flightSteps > 0) {
                --this.flightSteps;
                if (this.flightSteps == 0) {
                    this.selectNextMoveDirection(this.currentMoveDirection == null ? null : this.currentMoveDirection.getAxis());
                }
            }

            if (this.currentMoveDirection != null) {
                BlockPosition blockPos = this.getChunkCoordinates();
                EnumDirection.EnumAxis axis = this.currentMoveDirection.getAxis();
                if (this.level.loadedAndEntityCanStandOn(blockPos.relative(this.currentMoveDirection), this)) {
                    this.selectNextMoveDirection(axis);
                } else {
                    BlockPosition blockPos2 = this.finalTarget.getChunkCoordinates();
                    if (axis == EnumDirection.EnumAxis.X && blockPos.getX() == blockPos2.getX() || axis == EnumDirection.EnumAxis.Z && blockPos.getZ() == blockPos2.getZ() || axis == EnumDirection.EnumAxis.Y && blockPos.getY() == blockPos2.getY()) {
                        this.selectNextMoveDirection(axis);
                    }
                }
            }
        }

    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity) && !entity.noPhysics;
    }

    @Override
    public boolean isBurning() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 16384.0D;
    }

    @Override
    public float getBrightness() {
        return 1.0F;
    }

    @Override
    protected void onHitEntity(MovingObjectPositionEntity entityHitResult) {
        super.onHitEntity(entityHitResult);
        Entity entity = entityHitResult.getEntity();
        Entity entity2 = this.getShooter();
        EntityLiving livingEntity = entity2 instanceof EntityLiving ? (EntityLiving)entity2 : null;
        boolean bl = entity.damageEntity(DamageSource.indirectMobAttack(this, livingEntity).setProjectile(), 4.0F);
        if (bl) {
            this.doEnchantDamageEffects(livingEntity, entity);
            if (entity instanceof EntityLiving) {
                ((EntityLiving)entity).addEffect(new MobEffect(MobEffectList.LEVITATION, 200), MoreObjects.firstNonNull(entity2, this));
            }
        }

    }

    @Override
    protected void onHitBlock(MovingObjectPositionBlock blockHitResult) {
        super.onHitBlock(blockHitResult);
        ((WorldServer)this.level).sendParticles(Particles.EXPLOSION, this.locX(), this.locY(), this.locZ(), 2, 0.2D, 0.2D, 0.2D, 0.0D);
        this.playSound(SoundEffects.SHULKER_BULLET_HIT, 1.0F, 1.0F);
    }

    @Override
    protected void onHit(MovingObjectPosition hitResult) {
        super.onHit(hitResult);
        this.die();
    }

    @Override
    public boolean isInteractable() {
        return true;
    }

    @Override
    public boolean damageEntity(DamageSource source, float amount) {
        if (!this.level.isClientSide) {
            this.playSound(SoundEffects.SHULKER_BULLET_HURT, 1.0F, 1.0F);
            ((WorldServer)this.level).sendParticles(Particles.CRIT, this.locX(), this.locY(), this.locZ(), 15, 0.2D, 0.2D, 0.2D, 0.0D);
            this.die();
        }

        return true;
    }

    @Override
    public void recreateFromPacket(PacketPlayOutSpawnEntity packet) {
        super.recreateFromPacket(packet);
        double d = packet.getXa();
        double e = packet.getYa();
        double f = packet.getZa();
        this.setMot(d, e, f);
    }
}

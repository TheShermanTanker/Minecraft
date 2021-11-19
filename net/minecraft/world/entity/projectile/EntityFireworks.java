package net.minecraft.world.entity.projectile;

import java.util.OptionalInt;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.particles.Particles;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.sounds.EnumSoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMoveType;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.RayTrace;
import net.minecraft.world.level.World;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.MovingObjectPosition;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.MovingObjectPositionEntity;
import net.minecraft.world.phys.Vec3D;

public class EntityFireworks extends IProjectile implements ItemSupplier {
    public static final DataWatcherObject<ItemStack> DATA_ID_FIREWORKS_ITEM = DataWatcher.defineId(EntityFireworks.class, DataWatcherRegistry.ITEM_STACK);
    private static final DataWatcherObject<OptionalInt> DATA_ATTACHED_TO_TARGET = DataWatcher.defineId(EntityFireworks.class, DataWatcherRegistry.OPTIONAL_UNSIGNED_INT);
    public static final DataWatcherObject<Boolean> DATA_SHOT_AT_ANGLE = DataWatcher.defineId(EntityFireworks.class, DataWatcherRegistry.BOOLEAN);
    private int life;
    public int lifetime;
    @Nullable
    public EntityLiving attachedToEntity;

    public EntityFireworks(EntityTypes<? extends EntityFireworks> type, World world) {
        super(type, world);
    }

    public EntityFireworks(World world, double x, double y, double z, ItemStack stack) {
        super(EntityTypes.FIREWORK_ROCKET, world);
        this.life = 0;
        this.setPosition(x, y, z);
        int i = 1;
        if (!stack.isEmpty() && stack.hasTag()) {
            this.entityData.set(DATA_ID_FIREWORKS_ITEM, stack.cloneItemStack());
            i += stack.getOrCreateTagElement("Fireworks").getByte("Flight");
        }

        this.setMot(this.random.nextGaussian() * 0.001D, 0.05D, this.random.nextGaussian() * 0.001D);
        this.lifetime = 10 * i + this.random.nextInt(6) + this.random.nextInt(7);
    }

    public EntityFireworks(World world, @Nullable Entity entity, double x, double y, double z, ItemStack stack) {
        this(world, x, y, z, stack);
        this.setShooter(entity);
    }

    public EntityFireworks(World world, ItemStack stack, EntityLiving shooter) {
        this(world, shooter, shooter.locX(), shooter.locY(), shooter.locZ(), stack);
        this.entityData.set(DATA_ATTACHED_TO_TARGET, OptionalInt.of(shooter.getId()));
        this.attachedToEntity = shooter;
    }

    public EntityFireworks(World world, ItemStack stack, double x, double y, double z, boolean shotAtAngle) {
        this(world, x, y, z, stack);
        this.entityData.set(DATA_SHOT_AT_ANGLE, shotAtAngle);
    }

    public EntityFireworks(World world, ItemStack stack, Entity entity, double x, double y, double z, boolean shotAtAngle) {
        this(world, stack, x, y, z, shotAtAngle);
        this.setShooter(entity);
    }

    @Override
    protected void initDatawatcher() {
        this.entityData.register(DATA_ID_FIREWORKS_ITEM, ItemStack.EMPTY);
        this.entityData.register(DATA_ATTACHED_TO_TARGET, OptionalInt.empty());
        this.entityData.register(DATA_SHOT_AT_ANGLE, false);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 4096.0D && !this.isAttachedToEntity();
    }

    @Override
    public boolean shouldRender(double cameraX, double cameraY, double cameraZ) {
        return super.shouldRender(cameraX, cameraY, cameraZ) && !this.isAttachedToEntity();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isAttachedToEntity()) {
            if (this.attachedToEntity == null) {
                this.entityData.get(DATA_ATTACHED_TO_TARGET).ifPresent((i) -> {
                    Entity entity = this.level.getEntity(i);
                    if (entity instanceof EntityLiving) {
                        this.attachedToEntity = (EntityLiving)entity;
                    }

                });
            }

            if (this.attachedToEntity != null) {
                if (this.attachedToEntity.isGliding()) {
                    Vec3D vec3 = this.attachedToEntity.getLookDirection();
                    double d = 1.5D;
                    double e = 0.1D;
                    Vec3D vec32 = this.attachedToEntity.getMot();
                    this.attachedToEntity.setMot(vec32.add(vec3.x * 0.1D + (vec3.x * 1.5D - vec32.x) * 0.5D, vec3.y * 0.1D + (vec3.y * 1.5D - vec32.y) * 0.5D, vec3.z * 0.1D + (vec3.z * 1.5D - vec32.z) * 0.5D));
                }

                this.setPosition(this.attachedToEntity.locX(), this.attachedToEntity.locY(), this.attachedToEntity.locZ());
                this.setMot(this.attachedToEntity.getMot());
            }
        } else {
            if (!this.isShotAtAngle()) {
                double f = this.horizontalCollision ? 1.0D : 1.15D;
                this.setMot(this.getMot().multiply(f, 1.0D, f).add(0.0D, 0.04D, 0.0D));
            }

            Vec3D vec33 = this.getMot();
            this.move(EnumMoveType.SELF, vec33);
            this.setMot(vec33);
        }

        MovingObjectPosition hitResult = ProjectileHelper.getHitResult(this, this::canHitEntity);
        if (!this.noPhysics) {
            this.onHit(hitResult);
            this.hasImpulse = true;
        }

        this.updateRotation();
        if (this.life == 0 && !this.isSilent()) {
            this.level.playSound((EntityHuman)null, this.locX(), this.locY(), this.locZ(), SoundEffects.FIREWORK_ROCKET_LAUNCH, EnumSoundCategory.AMBIENT, 3.0F, 1.0F);
        }

        ++this.life;
        if (this.level.isClientSide && this.life % 2 < 2) {
            this.level.addParticle(Particles.FIREWORK, this.locX(), this.locY() - 0.3D, this.locZ(), this.random.nextGaussian() * 0.05D, -this.getMot().y * 0.5D, this.random.nextGaussian() * 0.05D);
        }

        if (!this.level.isClientSide && this.life > this.lifetime) {
            this.explode();
        }

    }

    private void explode() {
        this.level.broadcastEntityEffect(this, (byte)17);
        this.gameEvent(GameEvent.EXPLODE, this.getShooter());
        this.dealExplosionDamage();
        this.die();
    }

    @Override
    protected void onHitEntity(MovingObjectPositionEntity entityHitResult) {
        super.onHitEntity(entityHitResult);
        if (!this.level.isClientSide) {
            this.explode();
        }
    }

    @Override
    protected void onHitBlock(MovingObjectPositionBlock blockHitResult) {
        BlockPosition blockPos = new BlockPosition(blockHitResult.getBlockPosition());
        this.level.getType(blockPos).entityInside(this.level, blockPos, this);
        if (!this.level.isClientSide() && this.hasExplosions()) {
            this.explode();
        }

        super.onHitBlock(blockHitResult);
    }

    private boolean hasExplosions() {
        ItemStack itemStack = this.entityData.get(DATA_ID_FIREWORKS_ITEM);
        NBTTagCompound compoundTag = itemStack.isEmpty() ? null : itemStack.getTagElement("Fireworks");
        NBTTagList listTag = compoundTag != null ? compoundTag.getList("Explosions", 10) : null;
        return listTag != null && !listTag.isEmpty();
    }

    private void dealExplosionDamage() {
        float f = 0.0F;
        ItemStack itemStack = this.entityData.get(DATA_ID_FIREWORKS_ITEM);
        NBTTagCompound compoundTag = itemStack.isEmpty() ? null : itemStack.getTagElement("Fireworks");
        NBTTagList listTag = compoundTag != null ? compoundTag.getList("Explosions", 10) : null;
        if (listTag != null && !listTag.isEmpty()) {
            f = 5.0F + (float)(listTag.size() * 2);
        }

        if (f > 0.0F) {
            if (this.attachedToEntity != null) {
                this.attachedToEntity.damageEntity(DamageSource.fireworks(this, this.getShooter()), 5.0F + (float)(listTag.size() * 2));
            }

            double d = 5.0D;
            Vec3D vec3 = this.getPositionVector();

            for(EntityLiving livingEntity : this.level.getEntitiesOfClass(EntityLiving.class, this.getBoundingBox().inflate(5.0D))) {
                if (livingEntity != this.attachedToEntity && !(this.distanceToSqr(livingEntity) > 25.0D)) {
                    boolean bl = false;

                    for(int i = 0; i < 2; ++i) {
                        Vec3D vec32 = new Vec3D(livingEntity.locX(), livingEntity.getY(0.5D * (double)i), livingEntity.locZ());
                        MovingObjectPosition hitResult = this.level.rayTrace(new RayTrace(vec3, vec32, RayTrace.BlockCollisionOption.COLLIDER, RayTrace.FluidCollisionOption.NONE, this));
                        if (hitResult.getType() == MovingObjectPosition.EnumMovingObjectType.MISS) {
                            bl = true;
                            break;
                        }
                    }

                    if (bl) {
                        float g = f * (float)Math.sqrt((5.0D - (double)this.distanceTo(livingEntity)) / 5.0D);
                        livingEntity.damageEntity(DamageSource.fireworks(this, this.getShooter()), g);
                    }
                }
            }
        }

    }

    private boolean isAttachedToEntity() {
        return this.entityData.get(DATA_ATTACHED_TO_TARGET).isPresent();
    }

    public boolean isShotAtAngle() {
        return this.entityData.get(DATA_SHOT_AT_ANGLE);
    }

    @Override
    public void handleEntityEvent(byte status) {
        if (status == 17 && this.level.isClientSide) {
            if (!this.hasExplosions()) {
                for(int i = 0; i < this.random.nextInt(3) + 2; ++i) {
                    this.level.addParticle(Particles.POOF, this.locX(), this.locY(), this.locZ(), this.random.nextGaussian() * 0.05D, 0.005D, this.random.nextGaussian() * 0.05D);
                }
            } else {
                ItemStack itemStack = this.entityData.get(DATA_ID_FIREWORKS_ITEM);
                NBTTagCompound compoundTag = itemStack.isEmpty() ? null : itemStack.getTagElement("Fireworks");
                Vec3D vec3 = this.getMot();
                this.level.createFireworks(this.locX(), this.locY(), this.locZ(), vec3.x, vec3.y, vec3.z, compoundTag);
            }
        }

        super.handleEntityEvent(status);
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        nbt.setInt("Life", this.life);
        nbt.setInt("LifeTime", this.lifetime);
        ItemStack itemStack = this.entityData.get(DATA_ID_FIREWORKS_ITEM);
        if (!itemStack.isEmpty()) {
            nbt.set("FireworksItem", itemStack.save(new NBTTagCompound()));
        }

        nbt.setBoolean("ShotAtAngle", this.entityData.get(DATA_SHOT_AT_ANGLE));
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        this.life = nbt.getInt("Life");
        this.lifetime = nbt.getInt("LifeTime");
        ItemStack itemStack = ItemStack.of(nbt.getCompound("FireworksItem"));
        if (!itemStack.isEmpty()) {
            this.entityData.set(DATA_ID_FIREWORKS_ITEM, itemStack);
        }

        if (nbt.hasKey("ShotAtAngle")) {
            this.entityData.set(DATA_SHOT_AT_ANGLE, nbt.getBoolean("ShotAtAngle"));
        }

    }

    @Override
    public ItemStack getSuppliedItem() {
        ItemStack itemStack = this.entityData.get(DATA_ID_FIREWORKS_ITEM);
        return itemStack.isEmpty() ? new ItemStack(Items.FIREWORK_ROCKET) : itemStack;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }
}

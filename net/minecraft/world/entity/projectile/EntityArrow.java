package net.minecraft.world.entity.projectile;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.IRegistry;
import net.minecraft.core.particles.Particles;
import net.minecraft.nbt.GameProfileSerializer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.protocol.game.PacketPlayOutGameStateChange;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.resources.MinecraftKey;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.util.MathHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityPose;
import net.minecraft.world.entity.EntitySize;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMoveType;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentManager;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.RayTrace;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.MovingObjectPosition;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.MovingObjectPositionEntity;
import net.minecraft.world.phys.Vec3D;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class EntityArrow extends IProjectile {
    private static final double ARROW_BASE_DAMAGE = 2.0D;
    private static final DataWatcherObject<Byte> ID_FLAGS = DataWatcher.defineId(EntityArrow.class, DataWatcherRegistry.BYTE);
    private static final DataWatcherObject<Byte> PIERCE_LEVEL = DataWatcher.defineId(EntityArrow.class, DataWatcherRegistry.BYTE);
    private static final int FLAG_CRIT = 1;
    private static final int FLAG_NOPHYSICS = 2;
    private static final int FLAG_CROSSBOW = 4;
    @Nullable
    private IBlockData lastState;
    public boolean inGround;
    protected int inGroundTime;
    public EntityArrow.PickupStatus pickup = EntityArrow.PickupStatus.DISALLOWED;
    public int shakeTime;
    public int life;
    private double baseDamage = 2.0D;
    public int knockback;
    private SoundEffect soundEvent = this.getDefaultHitGroundSoundEvent();
    @Nullable
    private IntOpenHashSet piercingIgnoreEntityIds;
    @Nullable
    private List<Entity> piercedAndKilledEntities;

    protected EntityArrow(EntityTypes<? extends EntityArrow> type, World world) {
        super(type, world);
    }

    protected EntityArrow(EntityTypes<? extends EntityArrow> type, double x, double y, double z, World world) {
        this(type, world);
        this.setPosition(x, y, z);
    }

    protected EntityArrow(EntityTypes<? extends EntityArrow> type, EntityLiving owner, World world) {
        this(type, owner.locX(), owner.getHeadY() - (double)0.1F, owner.locZ(), world);
        this.setShooter(owner);
        if (owner instanceof EntityHuman) {
            this.pickup = EntityArrow.PickupStatus.ALLOWED;
        }

    }

    public void setSoundEvent(SoundEffect sound) {
        this.soundEvent = sound;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double d = this.getBoundingBox().getSize() * 10.0D;
        if (Double.isNaN(d)) {
            d = 1.0D;
        }

        d = d * 64.0D * getViewScale();
        return distance < d * d;
    }

    @Override
    protected void initDatawatcher() {
        this.entityData.register(ID_FLAGS, (byte)0);
        this.entityData.register(PIERCE_LEVEL, (byte)0);
    }

    @Override
    public void shoot(double x, double y, double z, float speed, float divergence) {
        super.shoot(x, y, z, speed, divergence);
        this.life = 0;
    }

    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int interpolationSteps, boolean interpolate) {
        this.setPosition(x, y, z);
        this.setYawPitch(yaw, pitch);
    }

    @Override
    public void lerpMotion(double x, double y, double z) {
        super.lerpMotion(x, y, z);
        this.life = 0;
    }

    @Override
    public void tick() {
        super.tick();
        boolean bl = this.isNoPhysics();
        Vec3D vec3 = this.getMot();
        if (this.xRotO == 0.0F && this.yRotO == 0.0F) {
            double d = vec3.horizontalDistance();
            this.setYRot((float)(MathHelper.atan2(vec3.x, vec3.z) * (double)(180F / (float)Math.PI)));
            this.setXRot((float)(MathHelper.atan2(vec3.y, d) * (double)(180F / (float)Math.PI)));
            this.yRotO = this.getYRot();
            this.xRotO = this.getXRot();
        }

        BlockPosition blockPos = this.getChunkCoordinates();
        IBlockData blockState = this.level.getType(blockPos);
        if (!blockState.isAir() && !bl) {
            VoxelShape voxelShape = blockState.getCollisionShape(this.level, blockPos);
            if (!voxelShape.isEmpty()) {
                Vec3D vec32 = this.getPositionVector();

                for(AxisAlignedBB aABB : voxelShape.toList()) {
                    if (aABB.move(blockPos).contains(vec32)) {
                        this.inGround = true;
                        break;
                    }
                }
            }
        }

        if (this.shakeTime > 0) {
            --this.shakeTime;
        }

        if (this.isInWaterOrRain() || blockState.is(Blocks.POWDER_SNOW)) {
            this.extinguish();
        }

        if (this.inGround && !bl) {
            if (this.lastState != blockState && this.shouldFall()) {
                this.startFalling();
            } else if (!this.level.isClientSide) {
                this.tickDespawn();
            }

            ++this.inGroundTime;
        } else {
            this.inGroundTime = 0;
            Vec3D vec33 = this.getPositionVector();
            Vec3D vec34 = vec33.add(vec3);
            MovingObjectPosition hitResult = this.level.rayTrace(new RayTrace(vec33, vec34, RayTrace.BlockCollisionOption.COLLIDER, RayTrace.FluidCollisionOption.NONE, this));
            if (hitResult.getType() != MovingObjectPosition.EnumMovingObjectType.MISS) {
                vec34 = hitResult.getPos();
            }

            while(!this.isRemoved()) {
                MovingObjectPositionEntity entityHitResult = this.findHitEntity(vec33, vec34);
                if (entityHitResult != null) {
                    hitResult = entityHitResult;
                }

                if (hitResult != null && hitResult.getType() == MovingObjectPosition.EnumMovingObjectType.ENTITY) {
                    Entity entity = ((MovingObjectPositionEntity)hitResult).getEntity();
                    Entity entity2 = this.getShooter();
                    if (entity instanceof EntityHuman && entity2 instanceof EntityHuman && !((EntityHuman)entity2).canHarmPlayer((EntityHuman)entity)) {
                        hitResult = null;
                        entityHitResult = null;
                    }
                }

                if (hitResult != null && !bl) {
                    this.onHit(hitResult);
                    this.hasImpulse = true;
                }

                if (entityHitResult == null || this.getPierceLevel() <= 0) {
                    break;
                }

                hitResult = null;
            }

            vec3 = this.getMot();
            double e = vec3.x;
            double f = vec3.y;
            double g = vec3.z;
            if (this.isCritical()) {
                for(int i = 0; i < 4; ++i) {
                    this.level.addParticle(Particles.CRIT, this.locX() + e * (double)i / 4.0D, this.locY() + f * (double)i / 4.0D, this.locZ() + g * (double)i / 4.0D, -e, -f + 0.2D, -g);
                }
            }

            double h = this.locX() + e;
            double j = this.locY() + f;
            double k = this.locZ() + g;
            double l = vec3.horizontalDistance();
            if (bl) {
                this.setYRot((float)(MathHelper.atan2(-e, -g) * (double)(180F / (float)Math.PI)));
            } else {
                this.setYRot((float)(MathHelper.atan2(e, g) * (double)(180F / (float)Math.PI)));
            }

            this.setXRot((float)(MathHelper.atan2(f, l) * (double)(180F / (float)Math.PI)));
            this.setXRot(lerpRotation(this.xRotO, this.getXRot()));
            this.setYRot(lerpRotation(this.yRotO, this.getYRot()));
            float m = 0.99F;
            float n = 0.05F;
            if (this.isInWater()) {
                for(int o = 0; o < 4; ++o) {
                    float p = 0.25F;
                    this.level.addParticle(Particles.BUBBLE, h - e * 0.25D, j - f * 0.25D, k - g * 0.25D, e, f, g);
                }

                m = this.getWaterInertia();
            }

            this.setMot(vec3.scale((double)m));
            if (!this.isNoGravity() && !bl) {
                Vec3D vec35 = this.getMot();
                this.setMot(vec35.x, vec35.y - (double)0.05F, vec35.z);
            }

            this.setPosition(h, j, k);
            this.checkBlockCollisions();
        }
    }

    private boolean shouldFall() {
        return this.inGround && this.level.noCollision((new AxisAlignedBB(this.getPositionVector(), this.getPositionVector())).inflate(0.06D));
    }

    private void startFalling() {
        this.inGround = false;
        Vec3D vec3 = this.getMot();
        this.setMot(vec3.multiply((double)(this.random.nextFloat() * 0.2F), (double)(this.random.nextFloat() * 0.2F), (double)(this.random.nextFloat() * 0.2F)));
        this.life = 0;
    }

    @Override
    public void move(EnumMoveType movementType, Vec3D movement) {
        super.move(movementType, movement);
        if (movementType != EnumMoveType.SELF && this.shouldFall()) {
            this.startFalling();
        }

    }

    protected void tickDespawn() {
        ++this.life;
        if (this.life >= 1200) {
            this.die();
        }

    }

    private void resetPiercedEntities() {
        if (this.piercedAndKilledEntities != null) {
            this.piercedAndKilledEntities.clear();
        }

        if (this.piercingIgnoreEntityIds != null) {
            this.piercingIgnoreEntityIds.clear();
        }

    }

    @Override
    protected void onHitEntity(MovingObjectPositionEntity entityHitResult) {
        super.onHitEntity(entityHitResult);
        Entity entity = entityHitResult.getEntity();
        float f = (float)this.getMot().length();
        int i = MathHelper.ceil(MathHelper.clamp((double)f * this.baseDamage, 0.0D, 2.147483647E9D));
        if (this.getPierceLevel() > 0) {
            if (this.piercingIgnoreEntityIds == null) {
                this.piercingIgnoreEntityIds = new IntOpenHashSet(5);
            }

            if (this.piercedAndKilledEntities == null) {
                this.piercedAndKilledEntities = Lists.newArrayListWithCapacity(5);
            }

            if (this.piercingIgnoreEntityIds.size() >= this.getPierceLevel() + 1) {
                this.die();
                return;
            }

            this.piercingIgnoreEntityIds.add(entity.getId());
        }

        if (this.isCritical()) {
            long l = (long)this.random.nextInt(i / 2 + 2);
            i = (int)Math.min(l + (long)i, 2147483647L);
        }

        Entity entity2 = this.getShooter();
        DamageSource damageSource;
        if (entity2 == null) {
            damageSource = DamageSource.arrow(this, this);
        } else {
            damageSource = DamageSource.arrow(this, entity2);
            if (entity2 instanceof EntityLiving) {
                ((EntityLiving)entity2).setLastHurtMob(entity);
            }
        }

        boolean bl = entity.getEntityType() == EntityTypes.ENDERMAN;
        int j = entity.getFireTicks();
        if (this.isBurning() && !bl) {
            entity.setOnFire(5);
        }

        if (entity.damageEntity(damageSource, (float)i)) {
            if (bl) {
                return;
            }

            if (entity instanceof EntityLiving) {
                EntityLiving livingEntity = (EntityLiving)entity;
                if (!this.level.isClientSide && this.getPierceLevel() <= 0) {
                    livingEntity.setArrowCount(livingEntity.getArrowCount() + 1);
                }

                if (this.knockback > 0) {
                    Vec3D vec3 = this.getMot().multiply(1.0D, 0.0D, 1.0D).normalize().scale((double)this.knockback * 0.6D);
                    if (vec3.lengthSqr() > 0.0D) {
                        livingEntity.push(vec3.x, 0.1D, vec3.z);
                    }
                }

                if (!this.level.isClientSide && entity2 instanceof EntityLiving) {
                    EnchantmentManager.doPostHurtEffects(livingEntity, entity2);
                    EnchantmentManager.doPostDamageEffects((EntityLiving)entity2, livingEntity);
                }

                this.doPostHurtEffects(livingEntity);
                if (entity2 != null && livingEntity != entity2 && livingEntity instanceof EntityHuman && entity2 instanceof EntityPlayer && !this.isSilent()) {
                    ((EntityPlayer)entity2).connection.sendPacket(new PacketPlayOutGameStateChange(PacketPlayOutGameStateChange.ARROW_HIT_PLAYER, 0.0F));
                }

                if (!entity.isAlive() && this.piercedAndKilledEntities != null) {
                    this.piercedAndKilledEntities.add(livingEntity);
                }

                if (!this.level.isClientSide && entity2 instanceof EntityPlayer) {
                    EntityPlayer serverPlayer = (EntityPlayer)entity2;
                    if (this.piercedAndKilledEntities != null && this.isShotFromCrossbow()) {
                        CriterionTriggers.KILLED_BY_CROSSBOW.trigger(serverPlayer, this.piercedAndKilledEntities);
                    } else if (!entity.isAlive() && this.isShotFromCrossbow()) {
                        CriterionTriggers.KILLED_BY_CROSSBOW.trigger(serverPlayer, Arrays.asList(entity));
                    }
                }
            }

            this.playSound(this.soundEvent, 1.0F, 1.2F / (this.random.nextFloat() * 0.2F + 0.9F));
            if (this.getPierceLevel() <= 0) {
                this.die();
            }
        } else {
            entity.setFireTicks(j);
            this.setMot(this.getMot().scale(-0.1D));
            this.setYRot(this.getYRot() + 180.0F);
            this.yRotO += 180.0F;
            if (!this.level.isClientSide && this.getMot().lengthSqr() < 1.0E-7D) {
                if (this.pickup == EntityArrow.PickupStatus.ALLOWED) {
                    this.spawnAtLocation(this.getItemStack(), 0.1F);
                }

                this.die();
            }
        }

    }

    @Override
    protected void onHitBlock(MovingObjectPositionBlock blockHitResult) {
        this.lastState = this.level.getType(blockHitResult.getBlockPosition());
        super.onHitBlock(blockHitResult);
        Vec3D vec3 = blockHitResult.getPos().subtract(this.locX(), this.locY(), this.locZ());
        this.setMot(vec3);
        Vec3D vec32 = vec3.normalize().scale((double)0.05F);
        this.setPositionRaw(this.locX() - vec32.x, this.locY() - vec32.y, this.locZ() - vec32.z);
        this.playSound(this.getSoundHit(), 1.0F, 1.2F / (this.random.nextFloat() * 0.2F + 0.9F));
        this.inGround = true;
        this.shakeTime = 7;
        this.setCritical(false);
        this.setPierceLevel((byte)0);
        this.setSoundEvent(SoundEffects.ARROW_HIT);
        this.setShotFromCrossbow(false);
        this.resetPiercedEntities();
    }

    protected SoundEffect getDefaultHitGroundSoundEvent() {
        return SoundEffects.ARROW_HIT;
    }

    protected final SoundEffect getSoundHit() {
        return this.soundEvent;
    }

    protected void doPostHurtEffects(EntityLiving target) {
    }

    @Nullable
    protected MovingObjectPositionEntity findHitEntity(Vec3D currentPosition, Vec3D nextPosition) {
        return ProjectileHelper.getEntityHitResult(this.level, this, currentPosition, nextPosition, this.getBoundingBox().expandTowards(this.getMot()).inflate(1.0D), this::canHitEntity);
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity) && (this.piercingIgnoreEntityIds == null || !this.piercingIgnoreEntityIds.contains(entity.getId()));
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        nbt.setShort("life", (short)this.life);
        if (this.lastState != null) {
            nbt.set("inBlockState", GameProfileSerializer.writeBlockState(this.lastState));
        }

        nbt.setByte("shake", (byte)this.shakeTime);
        nbt.setBoolean("inGround", this.inGround);
        nbt.setByte("pickup", (byte)this.pickup.ordinal());
        nbt.setDouble("damage", this.baseDamage);
        nbt.setBoolean("crit", this.isCritical());
        nbt.setByte("PierceLevel", this.getPierceLevel());
        nbt.setString("SoundEvent", IRegistry.SOUND_EVENT.getKey(this.soundEvent).toString());
        nbt.setBoolean("ShotFromCrossbow", this.isShotFromCrossbow());
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        this.life = nbt.getShort("life");
        if (nbt.hasKeyOfType("inBlockState", 10)) {
            this.lastState = GameProfileSerializer.readBlockState(nbt.getCompound("inBlockState"));
        }

        this.shakeTime = nbt.getByte("shake") & 255;
        this.inGround = nbt.getBoolean("inGround");
        if (nbt.hasKeyOfType("damage", 99)) {
            this.baseDamage = nbt.getDouble("damage");
        }

        this.pickup = EntityArrow.PickupStatus.byOrdinal(nbt.getByte("pickup"));
        this.setCritical(nbt.getBoolean("crit"));
        this.setPierceLevel(nbt.getByte("PierceLevel"));
        if (nbt.hasKeyOfType("SoundEvent", 8)) {
            this.soundEvent = IRegistry.SOUND_EVENT.getOptional(new MinecraftKey(nbt.getString("SoundEvent"))).orElse(this.getDefaultHitGroundSoundEvent());
        }

        this.setShotFromCrossbow(nbt.getBoolean("ShotFromCrossbow"));
    }

    @Override
    public void setShooter(@Nullable Entity entity) {
        super.setShooter(entity);
        if (entity instanceof EntityHuman) {
            this.pickup = ((EntityHuman)entity).getAbilities().instabuild ? EntityArrow.PickupStatus.CREATIVE_ONLY : EntityArrow.PickupStatus.ALLOWED;
        }

    }

    @Override
    public void pickup(EntityHuman player) {
        if (!this.level.isClientSide && (this.inGround || this.isNoPhysics()) && this.shakeTime <= 0) {
            if (this.tryPickup(player)) {
                player.receive(this, 1);
                this.die();
            }

        }
    }

    protected boolean tryPickup(EntityHuman player) {
        switch(this.pickup) {
        case ALLOWED:
            return player.getInventory().pickup(this.getItemStack());
        case CREATIVE_ONLY:
            return player.getAbilities().instabuild;
        default:
            return false;
        }
    }

    public abstract ItemStack getItemStack();

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.NONE;
    }

    public void setDamage(double damage) {
        this.baseDamage = damage;
    }

    public double getDamage() {
        return this.baseDamage;
    }

    public void setKnockbackStrength(int punch) {
        this.knockback = punch;
    }

    public int getKnockback() {
        return this.knockback;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    protected float getHeadHeight(EntityPose pose, EntitySize dimensions) {
        return 0.13F;
    }

    public void setCritical(boolean critical) {
        this.setFlag(1, critical);
    }

    public void setPierceLevel(byte level) {
        this.entityData.set(PIERCE_LEVEL, level);
    }

    private void setFlag(int index, boolean flag) {
        byte b = this.entityData.get(ID_FLAGS);
        if (flag) {
            this.entityData.set(ID_FLAGS, (byte)(b | index));
        } else {
            this.entityData.set(ID_FLAGS, (byte)(b & ~index));
        }

    }

    public boolean isCritical() {
        byte b = this.entityData.get(ID_FLAGS);
        return (b & 1) != 0;
    }

    public boolean isShotFromCrossbow() {
        byte b = this.entityData.get(ID_FLAGS);
        return (b & 4) != 0;
    }

    public byte getPierceLevel() {
        return this.entityData.get(PIERCE_LEVEL);
    }

    public void setEnchantmentEffectsFromEntity(EntityLiving entity, float damageModifier) {
        int i = EnchantmentManager.getEnchantmentLevel(Enchantments.POWER_ARROWS, entity);
        int j = EnchantmentManager.getEnchantmentLevel(Enchantments.PUNCH_ARROWS, entity);
        this.setDamage((double)(damageModifier * 2.0F) + this.random.nextGaussian() * 0.25D + (double)((float)this.level.getDifficulty().getId() * 0.11F));
        if (i > 0) {
            this.setDamage(this.getDamage() + (double)i * 0.5D + 0.5D);
        }

        if (j > 0) {
            this.setKnockbackStrength(j);
        }

        if (EnchantmentManager.getEnchantmentLevel(Enchantments.FLAMING_ARROWS, entity) > 0) {
            this.setOnFire(100);
        }

    }

    protected float getWaterInertia() {
        return 0.6F;
    }

    public void setNoPhysics(boolean noClip) {
        this.noPhysics = noClip;
        this.setFlag(2, noClip);
    }

    public boolean isNoPhysics() {
        if (!this.level.isClientSide) {
            return this.noPhysics;
        } else {
            return (this.entityData.get(ID_FLAGS) & 2) != 0;
        }
    }

    public void setShotFromCrossbow(boolean shotFromCrossbow) {
        this.setFlag(4, shotFromCrossbow);
    }

    public static enum PickupStatus {
        DISALLOWED,
        ALLOWED,
        CREATIVE_ONLY;

        public static EntityArrow.PickupStatus byOrdinal(int ordinal) {
            if (ordinal < 0 || ordinal > values().length) {
                ordinal = 0;
            }

            return values()[ordinal];
        }
    }
}

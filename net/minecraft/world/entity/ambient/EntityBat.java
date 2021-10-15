package net.minecraft.world.entity.ambient;

import java.time.LocalDate;
import java.time.temporal.ChronoField;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.util.MathHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityPose;
import net.minecraft.world.entity.EntitySize;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.targeting.PathfinderTargetCondition;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.Vec3D;

public class EntityBat extends EntityAmbient {
    public static final float FLAP_DEGREES_PER_TICK = 74.48451F;
    public static final int TICKS_PER_FLAP = MathHelper.ceil(2.4166098F);
    private static final DataWatcherObject<Byte> DATA_ID_FLAGS = DataWatcher.defineId(EntityBat.class, DataWatcherRegistry.BYTE);
    private static final int FLAG_RESTING = 1;
    private static final PathfinderTargetCondition BAT_RESTING_TARGETING = PathfinderTargetCondition.forNonCombat().range(4.0D);
    private BlockPosition targetPosition;

    public EntityBat(EntityTypes<? extends EntityBat> type, World world) {
        super(type, world);
        this.setAsleep(true);
    }

    @Override
    public boolean isFlapping() {
        return !this.isAsleep() && this.tickCount % TICKS_PER_FLAP == 0;
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.entityData.register(DATA_ID_FLAGS, (byte)0);
    }

    @Override
    public float getSoundVolume() {
        return 0.1F;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.95F;
    }

    @Nullable
    @Override
    public SoundEffect getSoundAmbient() {
        return this.isAsleep() && this.random.nextInt(4) != 0 ? null : SoundEffects.BAT_AMBIENT;
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        return SoundEffects.BAT_HURT;
    }

    @Override
    public SoundEffect getSoundDeath() {
        return SoundEffects.BAT_DEATH;
    }

    @Override
    public boolean isCollidable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    protected void collideNearby() {
    }

    public static AttributeProvider.Builder createAttributes() {
        return EntityInsentient.createMobAttributes().add(GenericAttributes.MAX_HEALTH, 6.0D);
    }

    public boolean isAsleep() {
        return (this.entityData.get(DATA_ID_FLAGS) & 1) != 0;
    }

    public void setAsleep(boolean roosting) {
        byte b = this.entityData.get(DATA_ID_FLAGS);
        if (roosting) {
            this.entityData.set(DATA_ID_FLAGS, (byte)(b | 1));
        } else {
            this.entityData.set(DATA_ID_FLAGS, (byte)(b & -2));
        }

    }

    @Override
    public void tick() {
        super.tick();
        if (this.isAsleep()) {
            this.setMot(Vec3D.ZERO);
            this.setPositionRaw(this.locX(), (double)MathHelper.floor(this.locY()) + 1.0D - (double)this.getHeight(), this.locZ());
        } else {
            this.setMot(this.getMot().multiply(1.0D, 0.6D, 1.0D));
        }

    }

    @Override
    protected void mobTick() {
        super.mobTick();
        BlockPosition blockPos = this.getChunkCoordinates();
        BlockPosition blockPos2 = blockPos.above();
        if (this.isAsleep()) {
            boolean bl = this.isSilent();
            if (this.level.getType(blockPos2).isOccluding(this.level, blockPos)) {
                if (this.random.nextInt(200) == 0) {
                    this.yHeadRot = (float)this.random.nextInt(360);
                }

                if (this.level.getNearestPlayer(BAT_RESTING_TARGETING, this) != null) {
                    this.setAsleep(false);
                    if (!bl) {
                        this.level.levelEvent((EntityHuman)null, 1025, blockPos, 0);
                    }
                }
            } else {
                this.setAsleep(false);
                if (!bl) {
                    this.level.levelEvent((EntityHuman)null, 1025, blockPos, 0);
                }
            }
        } else {
            if (this.targetPosition != null && (!this.level.isEmpty(this.targetPosition) || this.targetPosition.getY() <= this.level.getMinBuildHeight())) {
                this.targetPosition = null;
            }

            if (this.targetPosition == null || this.random.nextInt(30) == 0 || this.targetPosition.closerThan(this.getPositionVector(), 2.0D)) {
                this.targetPosition = new BlockPosition(this.locX() + (double)this.random.nextInt(7) - (double)this.random.nextInt(7), this.locY() + (double)this.random.nextInt(6) - 2.0D, this.locZ() + (double)this.random.nextInt(7) - (double)this.random.nextInt(7));
            }

            double d = (double)this.targetPosition.getX() + 0.5D - this.locX();
            double e = (double)this.targetPosition.getY() + 0.1D - this.locY();
            double f = (double)this.targetPosition.getZ() + 0.5D - this.locZ();
            Vec3D vec3 = this.getMot();
            Vec3D vec32 = vec3.add((Math.signum(d) * 0.5D - vec3.x) * (double)0.1F, (Math.signum(e) * (double)0.7F - vec3.y) * (double)0.1F, (Math.signum(f) * 0.5D - vec3.z) * (double)0.1F);
            this.setMot(vec32);
            float g = (float)(MathHelper.atan2(vec32.z, vec32.x) * (double)(180F / (float)Math.PI)) - 90.0F;
            float h = MathHelper.wrapDegrees(g - this.getYRot());
            this.zza = 0.5F;
            this.setYRot(this.getYRot() + h);
            if (this.random.nextInt(100) == 0 && this.level.getType(blockPos2).isOccluding(this.level, blockPos2)) {
                this.setAsleep(true);
            }
        }

    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.EVENTS;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    @Override
    protected void checkFallDamage(double heightDifference, boolean onGround, IBlockData landedState, BlockPosition landedPosition) {
    }

    @Override
    public boolean isIgnoreBlockTrigger() {
        return true;
    }

    @Override
    public boolean damageEntity(DamageSource source, float amount) {
        if (this.isInvulnerable(source)) {
            return false;
        } else {
            if (!this.level.isClientSide && this.isAsleep()) {
                this.setAsleep(false);
            }

            return super.damageEntity(source, amount);
        }
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        this.entityData.set(DATA_ID_FLAGS, nbt.getByte("BatFlags"));
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        nbt.setByte("BatFlags", this.entityData.get(DATA_ID_FLAGS));
    }

    public static boolean checkBatSpawnRules(EntityTypes<EntityBat> type, GeneratorAccess world, EnumMobSpawn spawnReason, BlockPosition pos, Random random) {
        if (pos.getY() >= world.getSeaLevel()) {
            return false;
        } else {
            int i = world.getLightLevel(pos);
            int j = 4;
            if (isHalloween()) {
                j = 7;
            } else if (random.nextBoolean()) {
                return false;
            }

            return i > random.nextInt(j) ? false : checkMobSpawnRules(type, world, spawnReason, pos, random);
        }
    }

    private static boolean isHalloween() {
        LocalDate localDate = LocalDate.now();
        int i = localDate.get(ChronoField.DAY_OF_MONTH);
        int j = localDate.get(ChronoField.MONTH_OF_YEAR);
        return j == 10 && i >= 20 || j == 11 && i <= 3;
    }

    @Override
    protected float getStandingEyeHeight(EntityPose pose, EntitySize dimensions) {
        return dimensions.height / 2.0F;
    }
}

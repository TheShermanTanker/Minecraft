package net.minecraft.world.entity.animal;

import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.util.MathHelper;
import net.minecraft.util.TimeRange;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.DifficultyDamageScaler;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAgeable;
import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityPose;
import net.minecraft.world.entity.EntitySize;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.GroupDataEntity;
import net.minecraft.world.entity.IEntityAngerable;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.goal.PathfinderGoalFloat;
import net.minecraft.world.entity.ai.goal.PathfinderGoalFollowParent;
import net.minecraft.world.entity.ai.goal.PathfinderGoalLookAtPlayer;
import net.minecraft.world.entity.ai.goal.PathfinderGoalMeleeAttack;
import net.minecraft.world.entity.ai.goal.PathfinderGoalPanic;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomLookaround;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomStroll;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalHurtByTarget;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalNearestAttackableTarget;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalUniversalAngerReset;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GeneratorAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.WorldAccess;
import net.minecraft.world.level.biome.BiomeBase;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;

public class EntityPolarBear extends EntityAnimal implements IEntityAngerable {
    private static final DataWatcherObject<Boolean> DATA_STANDING_ID = DataWatcher.defineId(EntityPolarBear.class, DataWatcherRegistry.BOOLEAN);
    private static final float STAND_ANIMATION_TICKS = 6.0F;
    private float clientSideStandAnimationO;
    private float clientSideStandAnimation;
    private int warningSoundTicks;
    private static final UniformInt PERSISTENT_ANGER_TIME = TimeRange.rangeOfSeconds(20, 39);
    private int remainingPersistentAngerTime;
    private UUID persistentAngerTarget;

    public EntityPolarBear(EntityTypes<? extends EntityPolarBear> type, World world) {
        super(type, world);
    }

    @Override
    public EntityAgeable createChild(WorldServer world, EntityAgeable entity) {
        return EntityTypes.POLAR_BEAR.create(world);
    }

    @Override
    public boolean isBreedItem(ItemStack stack) {
        return false;
    }

    @Override
    protected void initPathfinder() {
        super.initPathfinder();
        this.goalSelector.addGoal(0, new PathfinderGoalFloat(this));
        this.goalSelector.addGoal(1, new EntityPolarBear.PolarBearMeleeAttackGoal());
        this.goalSelector.addGoal(1, new EntityPolarBear.PolarBearPanicGoal());
        this.goalSelector.addGoal(4, new PathfinderGoalFollowParent(this, 1.25D));
        this.goalSelector.addGoal(5, new PathfinderGoalRandomStroll(this, 1.0D));
        this.goalSelector.addGoal(6, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 6.0F));
        this.goalSelector.addGoal(7, new PathfinderGoalRandomLookaround(this));
        this.targetSelector.addGoal(1, new EntityPolarBear.PolarBearHurtByTargetGoal());
        this.targetSelector.addGoal(2, new EntityPolarBear.PolarBearAttackPlayersGoal());
        this.targetSelector.addGoal(3, new PathfinderGoalNearestAttackableTarget<>(this, EntityHuman.class, 10, true, false, this::isAngryAt));
        this.targetSelector.addGoal(4, new PathfinderGoalNearestAttackableTarget<>(this, EntityFox.class, 10, true, true, (Predicate<EntityLiving>)null));
        this.targetSelector.addGoal(5, new PathfinderGoalUniversalAngerReset<>(this, false));
    }

    public static AttributeProvider.Builder createAttributes() {
        return EntityInsentient.createMobAttributes().add(GenericAttributes.MAX_HEALTH, 30.0D).add(GenericAttributes.FOLLOW_RANGE, 20.0D).add(GenericAttributes.MOVEMENT_SPEED, 0.25D).add(GenericAttributes.ATTACK_DAMAGE, 6.0D);
    }

    public static boolean checkPolarBearSpawnRules(EntityTypes<EntityPolarBear> type, GeneratorAccess world, EnumMobSpawn spawnReason, BlockPosition pos, Random random) {
        Optional<ResourceKey<BiomeBase>> optional = world.getBiomeName(pos);
        if (!Objects.equals(optional, Optional.of(Biomes.FROZEN_OCEAN)) && !Objects.equals(optional, Optional.of(Biomes.DEEP_FROZEN_OCEAN))) {
            return checkAnimalSpawnRules(type, world, spawnReason, pos, random);
        } else {
            return world.getLightLevel(pos, 0) > 8 && world.getType(pos.below()).is(Blocks.ICE);
        }
    }

    @Override
    public void loadData(NBTTagCompound nbt) {
        super.loadData(nbt);
        this.readPersistentAngerSaveData(this.level, nbt);
    }

    @Override
    public void saveData(NBTTagCompound nbt) {
        super.saveData(nbt);
        this.addPersistentAngerSaveData(nbt);
    }

    @Override
    public void anger() {
        this.setAnger(PERSISTENT_ANGER_TIME.sample(this.random));
    }

    @Override
    public void setAnger(int ticks) {
        this.remainingPersistentAngerTime = ticks;
    }

    @Override
    public int getAnger() {
        return this.remainingPersistentAngerTime;
    }

    @Override
    public void setAngerTarget(@Nullable UUID uuid) {
        this.persistentAngerTarget = uuid;
    }

    @Override
    public UUID getAngerTarget() {
        return this.persistentAngerTarget;
    }

    @Override
    protected SoundEffect getSoundAmbient() {
        return this.isBaby() ? SoundEffects.POLAR_BEAR_AMBIENT_BABY : SoundEffects.POLAR_BEAR_AMBIENT;
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        return SoundEffects.POLAR_BEAR_HURT;
    }

    @Override
    public SoundEffect getSoundDeath() {
        return SoundEffects.POLAR_BEAR_DEATH;
    }

    @Override
    protected void playStepSound(BlockPosition pos, IBlockData state) {
        this.playSound(SoundEffects.POLAR_BEAR_STEP, 0.15F, 1.0F);
    }

    protected void playWarningSound() {
        if (this.warningSoundTicks <= 0) {
            this.playSound(SoundEffects.POLAR_BEAR_WARNING, 1.0F, this.getVoicePitch());
            this.warningSoundTicks = 40;
        }

    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.entityData.register(DATA_STANDING_ID, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level.isClientSide) {
            if (this.clientSideStandAnimation != this.clientSideStandAnimationO) {
                this.updateSize();
            }

            this.clientSideStandAnimationO = this.clientSideStandAnimation;
            if (this.isStanding()) {
                this.clientSideStandAnimation = MathHelper.clamp(this.clientSideStandAnimation + 1.0F, 0.0F, 6.0F);
            } else {
                this.clientSideStandAnimation = MathHelper.clamp(this.clientSideStandAnimation - 1.0F, 0.0F, 6.0F);
            }
        }

        if (this.warningSoundTicks > 0) {
            --this.warningSoundTicks;
        }

        if (!this.level.isClientSide) {
            this.updatePersistentAnger((WorldServer)this.level, true);
        }

    }

    @Override
    public EntitySize getDimensions(EntityPose pose) {
        if (this.clientSideStandAnimation > 0.0F) {
            float f = this.clientSideStandAnimation / 6.0F;
            float g = 1.0F + f;
            return super.getDimensions(pose).scale(1.0F, g);
        } else {
            return super.getDimensions(pose);
        }
    }

    @Override
    public boolean attackEntity(Entity target) {
        boolean bl = target.damageEntity(DamageSource.mobAttack(this), (float)((int)this.getAttributeValue(GenericAttributes.ATTACK_DAMAGE)));
        if (bl) {
            this.doEnchantDamageEffects(this, target);
        }

        return bl;
    }

    public boolean isStanding() {
        return this.entityData.get(DATA_STANDING_ID);
    }

    public void setStanding(boolean warning) {
        this.entityData.set(DATA_STANDING_ID, warning);
    }

    public float getStandingAnimationScale(float tickDelta) {
        return MathHelper.lerp(tickDelta, this.clientSideStandAnimationO, this.clientSideStandAnimation) / 6.0F;
    }

    @Override
    protected float getWaterSlowDown() {
        return 0.98F;
    }

    @Override
    public GroupDataEntity prepare(WorldAccess world, DifficultyDamageScaler difficulty, EnumMobSpawn spawnReason, @Nullable GroupDataEntity entityData, @Nullable NBTTagCompound entityNbt) {
        if (entityData == null) {
            entityData = new EntityAgeable.GroupDataAgeable(1.0F);
        }

        return super.prepare(world, difficulty, spawnReason, entityData, entityNbt);
    }

    class PolarBearAttackPlayersGoal extends PathfinderGoalNearestAttackableTarget<EntityHuman> {
        public PolarBearAttackPlayersGoal() {
            super(EntityPolarBear.this, EntityHuman.class, 20, true, true, (Predicate<EntityLiving>)null);
        }

        @Override
        public boolean canUse() {
            if (EntityPolarBear.this.isBaby()) {
                return false;
            } else {
                if (super.canUse()) {
                    for(EntityPolarBear polarBear : EntityPolarBear.this.level.getEntitiesOfClass(EntityPolarBear.class, EntityPolarBear.this.getBoundingBox().grow(8.0D, 4.0D, 8.0D))) {
                        if (polarBear.isBaby()) {
                            return true;
                        }
                    }
                }

                return false;
            }
        }

        @Override
        protected double getFollowDistance() {
            return super.getFollowDistance() * 0.5D;
        }
    }

    class PolarBearHurtByTargetGoal extends PathfinderGoalHurtByTarget {
        public PolarBearHurtByTargetGoal() {
            super(EntityPolarBear.this);
        }

        @Override
        public void start() {
            super.start();
            if (EntityPolarBear.this.isBaby()) {
                this.alertOthers();
                this.stop();
            }

        }

        @Override
        protected void alertOther(EntityInsentient mob, EntityLiving target) {
            if (mob instanceof EntityPolarBear && !mob.isBaby()) {
                super.alertOther(mob, target);
            }

        }
    }

    class PolarBearMeleeAttackGoal extends PathfinderGoalMeleeAttack {
        public PolarBearMeleeAttackGoal() {
            super(EntityPolarBear.this, 1.25D, true);
        }

        @Override
        protected void checkAndPerformAttack(EntityLiving target, double squaredDistance) {
            double d = this.getAttackReachSqr(target);
            if (squaredDistance <= d && this.isTimeToAttack()) {
                this.resetAttackCooldown();
                this.mob.attackEntity(target);
                EntityPolarBear.this.setStanding(false);
            } else if (squaredDistance <= d * 2.0D) {
                if (this.isTimeToAttack()) {
                    EntityPolarBear.this.setStanding(false);
                    this.resetAttackCooldown();
                }

                if (this.getTicksUntilNextAttack() <= 10) {
                    EntityPolarBear.this.setStanding(true);
                    EntityPolarBear.this.playWarningSound();
                }
            } else {
                this.resetAttackCooldown();
                EntityPolarBear.this.setStanding(false);
            }

        }

        @Override
        public void stop() {
            EntityPolarBear.this.setStanding(false);
            super.stop();
        }

        @Override
        protected double getAttackReachSqr(EntityLiving entity) {
            return (double)(4.0F + entity.getWidth());
        }
    }

    class PolarBearPanicGoal extends PathfinderGoalPanic {
        public PolarBearPanicGoal() {
            super(EntityPolarBear.this, 2.0D);
        }

        @Override
        public boolean canUse() {
            return !EntityPolarBear.this.isBaby() && !EntityPolarBear.this.isBurning() ? false : super.canUse();
        }
    }
}

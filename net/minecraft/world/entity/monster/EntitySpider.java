package net.minecraft.world.entity.monster;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.DifficultyDamageScaler;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectBase;
import net.minecraft.world.effect.MobEffectList;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityPose;
import net.minecraft.world.entity.EntitySize;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.EnumMonsterType;
import net.minecraft.world.entity.GroupDataEntity;
import net.minecraft.world.entity.ai.attributes.AttributeProvider;
import net.minecraft.world.entity.ai.attributes.GenericAttributes;
import net.minecraft.world.entity.ai.goal.PathfinderGoalFloat;
import net.minecraft.world.entity.ai.goal.PathfinderGoalLeapAtTarget;
import net.minecraft.world.entity.ai.goal.PathfinderGoalLookAtPlayer;
import net.minecraft.world.entity.ai.goal.PathfinderGoalMeleeAttack;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomLookaround;
import net.minecraft.world.entity.ai.goal.PathfinderGoalRandomStrollLand;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalHurtByTarget;
import net.minecraft.world.entity.ai.goal.target.PathfinderGoalNearestAttackableTarget;
import net.minecraft.world.entity.ai.navigation.NavigationAbstract;
import net.minecraft.world.entity.ai.navigation.NavigationSpider;
import net.minecraft.world.entity.animal.EntityIronGolem;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.level.World;
import net.minecraft.world.level.WorldAccess;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.phys.Vec3D;

public class EntitySpider extends EntityMonster {
    private static final DataWatcherObject<Byte> DATA_FLAGS_ID = DataWatcher.defineId(EntitySpider.class, DataWatcherRegistry.BYTE);
    private static final float SPIDER_SPECIAL_EFFECT_CHANCE = 0.1F;

    public EntitySpider(EntityTypes<? extends EntitySpider> type, World world) {
        super(type, world);
    }

    @Override
    protected void initPathfinder() {
        this.goalSelector.addGoal(1, new PathfinderGoalFloat(this));
        this.goalSelector.addGoal(3, new PathfinderGoalLeapAtTarget(this, 0.4F));
        this.goalSelector.addGoal(4, new EntitySpider.PathfinderGoalSpiderMeleeAttack(this));
        this.goalSelector.addGoal(5, new PathfinderGoalRandomStrollLand(this, 0.8D));
        this.goalSelector.addGoal(6, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 8.0F));
        this.goalSelector.addGoal(6, new PathfinderGoalRandomLookaround(this));
        this.targetSelector.addGoal(1, new PathfinderGoalHurtByTarget(this));
        this.targetSelector.addGoal(2, new EntitySpider.PathfinderGoalSpiderNearestAttackableTarget<>(this, EntityHuman.class));
        this.targetSelector.addGoal(3, new EntitySpider.PathfinderGoalSpiderNearestAttackableTarget<>(this, EntityIronGolem.class));
    }

    @Override
    public double getPassengersRidingOffset() {
        return (double)(this.getHeight() * 0.5F);
    }

    @Override
    protected NavigationAbstract createNavigation(World world) {
        return new NavigationSpider(this, world);
    }

    @Override
    protected void initDatawatcher() {
        super.initDatawatcher();
        this.entityData.register(DATA_FLAGS_ID, (byte)0);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level.isClientSide) {
            this.setClimbing(this.horizontalCollision);
        }

    }

    public static AttributeProvider.Builder createAttributes() {
        return EntityMonster.createMonsterAttributes().add(GenericAttributes.MAX_HEALTH, 16.0D).add(GenericAttributes.MOVEMENT_SPEED, (double)0.3F);
    }

    @Override
    protected SoundEffect getSoundAmbient() {
        return SoundEffects.SPIDER_AMBIENT;
    }

    @Override
    protected SoundEffect getSoundHurt(DamageSource source) {
        return SoundEffects.SPIDER_HURT;
    }

    @Override
    public SoundEffect getSoundDeath() {
        return SoundEffects.SPIDER_DEATH;
    }

    @Override
    protected void playStepSound(BlockPosition pos, IBlockData state) {
        this.playSound(SoundEffects.SPIDER_STEP, 0.15F, 1.0F);
    }

    @Override
    public boolean isCurrentlyClimbing() {
        return this.isClimbing();
    }

    @Override
    public void makeStuckInBlock(IBlockData state, Vec3D multiplier) {
        if (!state.is(Blocks.COBWEB)) {
            super.makeStuckInBlock(state, multiplier);
        }

    }

    @Override
    public EnumMonsterType getMonsterType() {
        return EnumMonsterType.ARTHROPOD;
    }

    @Override
    public boolean canBeAffected(MobEffect effect) {
        return effect.getMobEffect() == MobEffectList.POISON ? false : super.canBeAffected(effect);
    }

    public boolean isClimbing() {
        return (this.entityData.get(DATA_FLAGS_ID) & 1) != 0;
    }

    public void setClimbing(boolean climbing) {
        byte b = this.entityData.get(DATA_FLAGS_ID);
        if (climbing) {
            b = (byte)(b | 1);
        } else {
            b = (byte)(b & -2);
        }

        this.entityData.set(DATA_FLAGS_ID, b);
    }

    @Nullable
    @Override
    public GroupDataEntity prepare(WorldAccess world, DifficultyDamageScaler difficulty, EnumMobSpawn spawnReason, @Nullable GroupDataEntity entityData, @Nullable NBTTagCompound entityNbt) {
        entityData = super.prepare(world, difficulty, spawnReason, entityData, entityNbt);
        if (world.getRandom().nextInt(100) == 0) {
            EntitySkeleton skeleton = EntityTypes.SKELETON.create(this.level);
            skeleton.setPositionRotation(this.locX(), this.locY(), this.locZ(), this.getYRot(), 0.0F);
            skeleton.prepare(world, difficulty, spawnReason, (GroupDataEntity)null, (NBTTagCompound)null);
            skeleton.startRiding(this);
        }

        if (entityData == null) {
            entityData = new EntitySpider.GroupDataSpider();
            if (world.getDifficulty() == EnumDifficulty.HARD && world.getRandom().nextFloat() < 0.1F * difficulty.getSpecialMultiplier()) {
                ((EntitySpider.GroupDataSpider)entityData).setRandomEffect(world.getRandom());
            }
        }

        if (entityData instanceof EntitySpider.GroupDataSpider) {
            MobEffectBase mobEffect = ((EntitySpider.GroupDataSpider)entityData).effect;
            if (mobEffect != null) {
                this.addEffect(new MobEffect(mobEffect, Integer.MAX_VALUE));
            }
        }

        return entityData;
    }

    @Override
    protected float getStandingEyeHeight(EntityPose pose, EntitySize dimensions) {
        return 0.65F;
    }

    public static class GroupDataSpider implements GroupDataEntity {
        @Nullable
        public MobEffectBase effect;

        public void setRandomEffect(Random random) {
            int i = random.nextInt(5);
            if (i <= 1) {
                this.effect = MobEffectList.MOVEMENT_SPEED;
            } else if (i <= 2) {
                this.effect = MobEffectList.DAMAGE_BOOST;
            } else if (i <= 3) {
                this.effect = MobEffectList.REGENERATION;
            } else if (i <= 4) {
                this.effect = MobEffectList.INVISIBILITY;
            }

        }
    }

    static class PathfinderGoalSpiderMeleeAttack extends PathfinderGoalMeleeAttack {
        public PathfinderGoalSpiderMeleeAttack(EntitySpider spider) {
            super(spider, 1.0D, true);
        }

        @Override
        public boolean canUse() {
            return super.canUse() && !this.mob.isVehicle();
        }

        @Override
        public boolean canContinueToUse() {
            float f = this.mob.getBrightness();
            if (f >= 0.5F && this.mob.getRandom().nextInt(100) == 0) {
                this.mob.setGoalTarget((EntityLiving)null);
                return false;
            } else {
                return super.canContinueToUse();
            }
        }

        @Override
        protected double getAttackReachSqr(EntityLiving entity) {
            return (double)(4.0F + entity.getWidth());
        }
    }

    static class PathfinderGoalSpiderNearestAttackableTarget<T extends EntityLiving> extends PathfinderGoalNearestAttackableTarget<T> {
        public PathfinderGoalSpiderNearestAttackableTarget(EntitySpider spider, Class<T> targetEntityClass) {
            super(spider, targetEntityClass, true);
        }

        @Override
        public boolean canUse() {
            float f = this.mob.getBrightness();
            return f >= 0.5F ? false : super.canUse();
        }
    }
}
